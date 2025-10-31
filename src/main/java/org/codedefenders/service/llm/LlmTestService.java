/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.service.llm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.codedefenders.analysis.gameclass.MethodDescription;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.game.GameAccordionMapping;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.llm.PromptType;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.Constants;
import org.codedefenders.util.LlmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequestScoped
class LlmTestService extends LlmSubActionService {
    private static final Logger logger = LoggerFactory.getLogger(LlmTestService.class);

    private static final String OUTSIDE_OF_METHOD_DESCRIPTION = "(The code outside of methods)"; //TODO Make this adjustable?

    @Inject
    GameService gameService;

    @Inject
    MutantRepository mutantRepository;


    @Override
    protected String generate() {

        PromptType promptType = getCorrectDefendPromptType();
        setConversationType(promptType);
        resetConversationAfterTooManyTries();
        if (conversation.isEmpty()) {
            String systemMessage = getSystemPrompt(model, promptType);
            if (promptType == PromptType.DEFEND_FOCUS) {
                Optional<String> methodName = getRandomMethodWithLivingMutant();
                if (methodName.isPresent()) {
                    systemMessage = String.format(systemMessage, methodName.get());
                }
            }
            conversation.addSystemMessage(systemMessage, model);

            conversation.addUserMessage(getSourceCodeForUserMessage(), model);
        }

        String response = promptService.getResponse(model, conversation);
        String testSrc = LlmUtils.testTemplateFromResponse(response, game);
        logger.info("AI defender generated test: {}", testSrc);
        return testSrc;
    }

    /**
     * This method submits the generated test code to the game. It should only be called from inside an LLM action,
     * after the test code has been generated and the game has been refreshed.
     *
     * @param testSrc      The formatted test code. All formatting heuristics should have already been performed.
     */
    @Override
    protected void submit(String testSrc) {
        switch (conversation.getCurrentType()) {
            case DEFEND_DEFAULT, DEFEND_DEPENDENCIES, DEFEND_FOCUS -> {
            }
            default -> throw new RuntimeException("Conversation during test submission may not be of type " +
                    conversation.getCurrentType());
        }
        try {
            GameManagingUtils.CreateBattlegroundTestResult result;
            if (game instanceof MultiplayerGame multiplayerGame) {
                result = gameManagingUtils.createBattlegroundTest(multiplayerGame, Constants.AI_DEFENDER_USER_ID, testSrc);
            } else {
                result = gameManagingUtils.createBattlegroundTest(game, Constants.AI_PLAYER_USER_ID, testSrc);
            }
            if (result.isSuccess()) {
                conversation.resetCurrent(true);
            } else {
                StringBuilder correction = new StringBuilder();
                switch (result.failureReason().orElseThrow()) {
                    case VALIDATION_FAILED -> {
                        correction.append("Your test has violated these rules: \n");
                        result.validationErrorMessages().orElseThrow().forEach(
                                correction::append
                        );

                    }
                    case COMPILATION_FAILED ->
                            correction.append("Your test failed to compile for this reason: ").append(result.compilationError());
                    case TEST_DID_NOT_PASS_ON_CUT ->
                            correction.append("Your test did not pass on the original code for the following reason: ")
                                    .append(result.testCutError().orElseThrow());
                }
                correction.append("\nFix these problems.");
                conversation.addSystemMessage(correction.toString(), model);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private PromptType getCorrectDefendPromptType() {
        if (model.isDefenderMethodFocus() && hasLivingMutants()) {
            return PromptType.DEFEND_FOCUS;
        }
        if (model.isDefenderDependencies() && !game.getCUT().getDependencyNames().isEmpty()) {
            return PromptType.DEFEND_DEPENDENCIES;
        }
        return PromptType.DEFEND_DEFAULT;
    }

    /**
     * Returns a list of all methods descriptions (as in {@link MethodDescription#getDescription()}) that contain a
     * living mutant that hasn't been created by the user.
     * If one method contains several living mutants, it occurs several times in the list.
     * If a mutant exists outside a method, the String is {@link LlmTestService#OUTSIDE_OF_METHOD_DESCRIPTION}.
     */
    private List<String> getMethodsWithLivingMutants() {
        List<MutantDTO> mutants = gameService.getMutants(user, game);
        List<MethodDescription> methods = game.getCUT().getMethodDescriptions();
        GameAccordionMapping mapping = GameAccordionMapping.computeForMutants(methods, mutants);
        HashMap<MethodDescription, SortedSet<Integer>> map = mapping.elementsPerMethod;
        List<String> listOfPossibilities = new ArrayList<>();
        for (MethodDescription m : map.keySet()) {
            for (Integer mutantId : map.get(m)) {
                Mutant mutant = mutantRepository.getMutantById(mutantId);
                if (mutant.isAlive() && mutant.getCreatorId() != user.getId()) {
                    listOfPossibilities.add(m.getDescription());
                }
            }
        }
        mapping.elementsOutsideMethods.stream().
                map(mutantId -> mutantRepository.getMutantById(mutantId))
                .filter(mutant -> mutant.isAlive() && mutant.getCreatorId() != user.getId())
                .forEach(mutant -> listOfPossibilities.add(OUTSIDE_OF_METHOD_DESCRIPTION));
        return listOfPossibilities;
    }

    /**
     * Returns if the game has living mutants that are not created by the user.
     */
    private boolean hasLivingMutants() {
        return gameService.getMutants(user, game).stream().anyMatch(
                mutantDTO -> mutantDTO.getState() == Mutant.State.ALIVE);
    }

    /**
     * Returns a random method that contains a living mutant that hasn't been created by the user. The more living
     * mutants there are in a method, the more likely it is to be selected.
     */
    private Optional<String> getRandomMethodWithLivingMutant() {
        List<String> methods = getMethodsWithLivingMutants();
        if (!methods.isEmpty()) {
            return Optional.of(methods.get(random.nextInt(methods.size())));
        } else {
            return Optional.empty();
        }
    }
}
