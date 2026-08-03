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
import java.util.Optional;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.codedefenders.dto.MutantDTO;
import org.codedefenders.game.Mutant;
import org.codedefenders.model.llm.LlmPromptType;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.LlmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For attackers and melee llm players, this service creates and submits the tests to resolve equivalence duels for
 * all flagged mutants. In order to not bog the LLM player down too much, this is the only place where many prompt
 * can be sent to the LLM without waiting for the normal cooldown. The LLM always tries to resolve the duel. If the
 * test does not validate or compile, there will be {@link EquivalenceStrategy#numberOfRepairAttempts} attempts
 * to get a better result from the LLM, after that the mutant is accepted as equivalent.
 */
@RequestScoped
public class EquivalenceStrategy extends AbstractStrategy {
    Logger logger = LoggerFactory.getLogger(EquivalenceStrategy.class);

    @Inject
    MutantRepository mutantRepository;

    private MutantDTO flagged;

    private static final String EQUIVALENCE = "EQUIVALENCE";

    @Override
    protected void run(LlmContext context) {
        this.context = context;
        for (MutantDTO flagged : gameService.getFlaggedMutants(this.context.user(), this.context.game())) {
            this.flagged = flagged;
            do {
                if (conversation == null) {
                    setConversationType(EQUIVALENCE);
                }
                super.run(context);
                if (conversation == null) {
                    setConversationType(EQUIVALENCE);
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
            conversation.addSystemMessage(context.equivalenceStrategy().getPrompt(
                    LlmPromptType.EQUIVALENCE_DEFAULT_DEFAULT_SYSTEM),
                    context.model()
            );
            conversation.addUserMessage(context.game().getCUT().getSourceCode()//TODO customize
                    + "\n###\n" + flagged.getPatchString(), context.model());
        }
        String response = promptService.getResponse(context.model(), conversation);
        return Optional.of(LlmUtils.testTemplateFromReply(response, context.game()));
    }

    @Override
    protected void submit(String testSource) {
        if (!conversation.getType().equals(EQUIVALENCE)) {
            logger.error("Conversation may not be of type {} in submitEquivalenceTest", conversation.getType());
            throw new RuntimeException("Conversation may not be of type " + conversation.getType()
                    + " in submitEquivalenceTest");
        }

        Mutant equivalentMutant = mutantRepository.getMutantById(flagged.getId());
        try {
            GameManagingUtils.RejectBattlegroundEquivalenceResult result =
                    gameManagingUtils.rejectBattlegroundEquivalence(context.game(), context.user().getId(),
                            equivalentMutant, testSource);
            //TODO Duplicate code can be removed after refactoring Results and FailureReasons to common types
            if (result.testValid()) {
                finishConversation(true);
            } else {
                if (conversation.numberOfTries() <= numberOfRepairAttempts) {
                    StringBuilder correction = new StringBuilder();
                    switch (result.failureReason().orElseThrow()) {
                        case VALIDATION_FAILED -> {
                            correction.append("Your test has violated these rules: \n");
                            result.validationErrorMessages().ifPresent(correction::append);

                        }
                        case COMPILATION_FAILED -> correction.append("Your test failed to compile for this reason: ")
                                .append(result.compilationError());
                        case TEST_DID_NOT_PASS_ON_CUT -> correction.append(
                                        "Your test did not pass on the original code for the following reason: ")
                                .append(result.testCutError().orElseThrow());
                        default -> throw new RuntimeException();
                    }
                    correction.append("\nFix these problems.");
                    conversation.addSystemMessage(correction.toString(), context.model());
                } else {
                    gameManagingUtils.acceptBattlegroundEquivalence(context.game(), context.user().getId(),
                            equivalentMutant);
                    finishConversation(false);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
