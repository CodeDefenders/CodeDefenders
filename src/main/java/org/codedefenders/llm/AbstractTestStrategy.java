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
package org.codedefenders.llm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;

import jakarta.inject.Inject;

import org.codedefenders.analysis.gameclass.MethodDescription;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.game.GameAccordionMapping;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Superclass of all TestStrategies. It is not to be instantiated itself. Provides some commonly used structures
 * and utility methods.
 */
abstract class AbstractTestStrategy extends AbstractStrategy {
    private static final Logger logger = LoggerFactory.getLogger(AbstractTestStrategy.class);

    //TODO Make this adjustable
    private static final String OUTSIDE_OF_METHOD_DESCRIPTION = "(The code outside of methods)";

    @Inject
    GameService gameService;

    @Inject
    MutantRepository mutantRepository;


    /**
     * This method submits the generated test code to the game. It should only be called from inside an LLM action,
     * after the test code has been generated and the game has been refreshed.
     *
     * @param testSrc  The formatted test code. All formatting heuristics should have already been performed.
     */
    @Override
    protected void submit(String testSrc) {
        try {
            GameManagingUtils.CreateBattlegroundTestResult result;
            if (context.game() instanceof MultiplayerGame multiplayerGame) {
                result = gameManagingUtils.createBattlegroundTest(multiplayerGame,
                        Constants.AI_DEFENDER_USER_ID,
                        testSrc);
            } else {
                result = gameManagingUtils.createBattlegroundTest(context.game(), Constants.AI_PLAYER_USER_ID, testSrc);
            }
            if (result.isSuccess()) {
                logger.info("LLM successfully submitted test.");
                conversation.setTestId(result.test().orElseThrow().getId());
                onSubmitSuccess();
            } else {
                onSubmitFailure(result, testSrc);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Defines what to do when submission was successful.
     */
    protected abstract void onSubmitSuccess();

    /**
     * Defines what to do when submission fails.
     */
    protected abstract void onSubmitFailure(GameManagingUtils.CreateBattlegroundTestResult result,
                                            String testSrc);

    /**
     * Returns a list of all methods descriptions (as in {@link MethodDescription#getDescription()}) that contain a
     * living mutant that hasn't been created by the user.
     * If one method contains several living mutants, it occurs several times in the list.
     * If a mutant exists outside a method, the String is {@link AbstractTestStrategy#OUTSIDE_OF_METHOD_DESCRIPTION}.
     */
    private List<String> getMethodsWithLivingMutants() {
        List<MutantDTO> mutants = gameService.getMutants(context.user(), context.game());
        List<MethodDescription> methods = context.game().getCUT().getMethodDescriptions();
        GameAccordionMapping mapping = GameAccordionMapping.computeForMutants(methods, mutants);
        HashMap<MethodDescription, SortedSet<Integer>> map = mapping.elementsPerMethod;
        List<String> listOfPossibilities = new ArrayList<>();
        for (MethodDescription m : map.keySet()) {
            for (Integer mutantId : map.get(m)) {
                Mutant mutant = mutantRepository.getMutantById(mutantId);
                if (mutant.isAlive() && mutant.getCreatorId() != context.user().getId()) {
                    listOfPossibilities.add(m.getDescription());
                }
            }
        }
        mapping.elementsOutsideMethods.stream()
                .map(mutantId -> mutantRepository.getMutantById(mutantId))
                .filter(mutant -> mutant.isAlive() && mutant.getCreatorId() != context.user().getId())
                .forEach(mutant -> listOfPossibilities.add(OUTSIDE_OF_METHOD_DESCRIPTION));
        return listOfPossibilities;
    }

    /**
     * Returns if the game has living mutants that are not created by the user.
     */
    protected boolean hasLivingMutants() {
        return gameService.getMutants(context.user(), context.game()).stream().anyMatch(
                mutantDTO -> mutantDTO.getState() == Mutant.State.ALIVE);
    }

    /**
     * Returns a random method that contains a living mutant that hasn't been created by the user. The more living
     * mutants there are in a method, the more likely it is to be selected.
     */
    protected Optional<String> getRandomMethodWithLivingMutant() {
        List<String> methods = getMethodsWithLivingMutants();
        if (!methods.isEmpty()) {
            return Optional.of(methods.get(context.random().nextInt(methods.size())));
        } else {
            return Optional.empty();
        }
    }

    /**
     * The default error handling for test strategies. This can be called after validation or compilation failed.
     * @param result This contains the reason why the test could not be submitted.
     * @param testSrc The source of the test that was submitted and rejected.
     */
    protected void standardSubmitFailure(GameManagingUtils.CreateBattlegroundTestResult result, String testSrc) {
        StringBuilder correction = new StringBuilder("This test could not be submitted: \n" + testSrc);
        switch (result.failureReason().orElseThrow()) {
            case VALIDATION_FAILED -> {
                correction.append("It has violated these rules: \n");
                result.validationErrorMessages().ifPresent(correction::append);

            }
            case COMPILATION_FAILED ->
                    correction.append("It has failed to compile for this reason: ").append(result.compilationError());
            case TEST_DID_NOT_PASS_ON_CUT ->
                    correction.append("It did not pass on the original code for the following reason: ")
                            .append(result.testCutError().orElseThrow());
            default -> throw new RuntimeException("Checkstyle");
        }
        correction.append("\nFix these problems.");
        conversation.addSystemMessage(correction.toString(), context.model());
    }
}
