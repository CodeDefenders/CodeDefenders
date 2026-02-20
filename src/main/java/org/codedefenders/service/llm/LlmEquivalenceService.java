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
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.codedefenders.dto.MutantDTO;
import org.codedefenders.game.Mutant;
import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.model.llm.PromptType;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.LlmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For attackers and melee llm players, this service creates and submits the tests to resolve equivalence duels for
 * all flagged mutants. In order to not bog the LLM player down too much, this is the only place where many prompt
 * can be sent to the LLM without waiting for the normal cooldown. The LLM always tries to resolve the duel. If the
 * test does not validate or compile, there will be {@link LlmEquivalenceService#numberOfRepairAttempts} attempts
 * to get a better result from the LLM, after that the mutant is accepted as equivalent.
 */
@RequestScoped
@Strategy(LlmStrategy.EQUIVALENCE_DEFAULT)
class LlmEquivalenceService extends LlmSubActionService {
    Logger logger = LoggerFactory.getLogger(LlmEquivalenceService.class);

    private static final String systemPrompt = """
            You are an experienced Java developer.

            You will see two things, separated by "###":
            1: The code of a java class.
            2: The git diff of a change to that class.

            Your task is to write a test that succeeds on the class as seen, but fails after the diff is applied.

            There is a strict rule of using at most 2 assertions. Always abide by it.

            Write nothing but the code of the single test.

            Use JUnit 4.

            Never reply in natural language.
            """;

    @Inject
    LlmPromptService promptService;

    @Inject
    GameService gameService;

    @Inject
    MutantRepository mutantRepository;

    private MutantDTO flagged;

    @Override
    protected void run() {
        for (MutantDTO flagged : gameService.getFlaggedMutants(user, game)) {
            this.flagged = flagged;
            do {
                if (conversation == null) {
                    setConversationType(PromptType.ATTACK_EQUIVALENCE);
                }
                super.run();
                if (conversation == null) {
                    setConversationType(PromptType.ATTACK_EQUIVALENCE);
                }
            } while (!conversation.isEmpty());

            conversation = null;
        }
    }

    /**
     * Generate a test that should kill a mutant that was flagged as equivalent. Waits until the generation by the
     * llm is complete.
     */
    @Override
    protected Optional<String> generate() {
        if (conversation.isEmpty()) {
            //conversation.addSystemMessage(getSystemPrompt(model, PromptType.ATTACK_EQUIVALENCE), model);
            conversation.addSystemMessage(systemPrompt, model); //TODO Eventually add customizability back in
            conversation.addUserMessage(game.getCUT().getSourceCode()
                    + "\n###\n" + flagged.getPatchString(), model);
        }
        String response = promptService.getResponse(model, conversation);
        return Optional.of(LlmUtils.testTemplateFromReply(response, game));
    }

    @Override
    protected void submit(String testSource) {
        if (conversation.getType() != PromptType.ATTACK_EQUIVALENCE) {
            logger.error("Conversation may not be of type {} in submitEquivalenceTest", conversation.getType());
            throw new RuntimeException("Conversation may not be of type " + conversation.getType()
                    + " in submitEquivalenceTest");
        }

        Mutant equivalentMutant = mutantRepository.getMutantById(flagged.getId());
        try {
            GameManagingUtils.RejectBattlegroundEquivalenceResult result =
                    gameManagingUtils.rejectBattlegroundEquivalence(game, user.getId(), equivalentMutant, testSource);

            if (result.testValid()) { //TODO Duplicate code can be removed after refactoring Results and FailureReasons to common types
                finishConversation(true);
            } else {
                if (conversation.numberOfTries() <= numberOfRepairAttempts) {
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
                } else {
                    gameManagingUtils.acceptBattlegroundEquivalence(game, user.getId(), equivalentMutant);
                    finishConversation(false);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static Class<? extends LlmEquivalenceService> getService(LlmStrategy strategy) {
        List<Class<? extends LlmSubActionService>> l = List.of(LlmEquivalenceService.class);
        return getServiceClass(l, strategy).asSubclass(LlmEquivalenceService.class);
    }


}
