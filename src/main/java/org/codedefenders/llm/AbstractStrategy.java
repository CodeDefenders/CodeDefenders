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

import java.util.List;
import java.util.Optional;
import java.util.Random;

import jakarta.inject.Inject;

import org.codedefenders.dto.MutantDTO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.llm.LlmConversation;
import org.codedefenders.persistence.database.LlmConversationRepository;
import org.codedefenders.service.LlmPromptService;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides fields and methods common to its subclasses, but is not to be instantiated itself.
 */
public abstract class AbstractStrategy {
    private static final Logger logger = LoggerFactory.getLogger(AbstractStrategy.class);

    //TODO Make this customizable
    private static final int EQUIVALENT_POINT_RESTRICTION = 10;

    @Inject
    protected LlmPromptService promptService;

    @Inject
    protected GameManagingUtils gameManagingUtils;

    @Inject
    protected GameService gameService;

    @Inject
    protected LlmConversationRepository conversationRepository;

    protected LlmContext context;
    protected LlmConversation conversation = null;
    protected int numberOfRepairAttempts = 3;

    protected void run(LlmContext context) {
        this.context = context;
        Optional<String> reply = generate();
        if (reply.isPresent()) {
            this.context.updateGame();
            submit(reply.get());
        }

        if (conversation != null) {
            conversationRepository.saveConversation(conversation);
        }

    }

    /**
     * Generate a test/mutant to submit. This is responsible for creating the prompt, getting the response
     * from the LLM and formatting the response.
     *
     * @return The code of the generated test/mutant
     */
    protected abstract Optional<String> generate();

    /**
     * Submit the generated test/mutant.
     *
     * @param reply The code of the generated test/mutant.
     */
    protected abstract void submit(String reply);


    /**
     * Utility method to reset conversations. If the maximum number of failed generation attempts has been reached,
     * reset the conversation - which means a fresh start for generating mutants/tests.
     * Otherwise, nothing happens.
     */
    protected void resetConversationAfterTooManyTries() {
        String tmp = conversation.getType();
        if (conversation.numberOfTries() > numberOfRepairAttempts) {
            finishConversation(false);
            conversation = context.conversationBatch().getConversation(tmp);
        }
    }

    protected void setConversationType(String type) {
        conversation = context.conversationBatch().getConversation(type);
    }

    protected void finishConversation(boolean success) {
        conversation.finish(success);
        conversationRepository.saveConversation(conversation);
        context.conversationBatch().remove(conversation);
        conversation = null;
    }

    /**
     * Adds the source code of the CuT as the user message. If the prompt specifies that dependencies have to be
     * included, the source code of the dependencies is included as well.
     */
    protected String getSourceCodeForUserMessage(boolean withDependencies) {
        if (withDependencies) {
            return context.game().getCUT().getSourceCode() + "\n####\n"
                    + String.join("\n####\n", context.game().getCUT().getDependencyCode());
        } else {
            return context.game().getCUT().getSourceCode();
        }
    }

    /**
     * Try to claim a random potentially equivalent mutant as equivalent, or do nothing if no such mutant is available.
     */
    protected void claimEquivalent(LlmContext context) {
        Optional<MutantDTO> potentialEquivalent = getRandomPossiblyEquivalentMutant(
                context.game(), context.user(), context.random());
        if (potentialEquivalent.isPresent()) {
            logger.info("Claiming equivalence on mutant {}", potentialEquivalent.get());
            gameManagingUtils.claimBattlegroundEquivalence(context.game(), context.user().getId(),
                    potentialEquivalent.get().getLines());
        }
    }

    /**
     * Return one random mutant from the set of all mutants that can be marked as equivalent by the user
     * and have acquired at least {@link AbstractStrategy#EQUIVALENT_POINT_RESTRICTION} points.
     * Returns an empty Optional if there is no mutant that can be marked as equivalent.
     */
    private Optional<MutantDTO> getRandomPossiblyEquivalentMutant(AbstractGame game, SimpleUser user, Random random) {
        List<MutantDTO> candidates = gameService.getMutants(user, game).stream()
                .filter(MutantDTO::isCanMarkEquivalent)
                .filter(m -> m.getPoints() >= EQUIVALENT_POINT_RESTRICTION)
                .filter(m -> !m.getCreator().equals(user))
                .toList();
        if (candidates.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(candidates.get(random.nextInt(candidates.size())));
        }
    }
}
