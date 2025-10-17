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

import java.util.List;
import java.util.Optional;
import java.util.Random;

import jakarta.inject.Inject;

import org.codedefenders.dto.MutantDTO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.llm.LlModel;
import org.codedefenders.model.llm.LlmConversation;
import org.codedefenders.model.llm.PromptType;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.LlmRepository;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.CDIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides fields and methods common to its subclasses, but is not to be instantiated itself.
 */
abstract class LlmSubActionService {
    private static final Logger logger = LoggerFactory.getLogger(LlmSubActionService.class);

    private static final int EQUIVALENT_POINT_RESTRICTION = 10; //TODO Als system setting??

    @Inject
    protected LlmPromptService promptService;

    @Inject
    protected GameManagingUtils gameManagingUtils;

    @Inject
    private GameRepository gameRepository;

    @Inject
    protected GameService gameService;

    protected LlModel model;
    protected AbstractGame game;
    protected LlmConversation conversation;
    protected SimpleUser user;
    protected Random random;

    protected void init(AbstractGame game, SimpleUser user, LlModel model, LlmConversation conversation, Random random) {
        this.game = game;
        this.user = user;
        this.model = model;
        this.conversation = conversation;
        this.random = random;
    }

    protected void updateGame() {
        game = gameRepository.getGame(game.getId());
    }

    /**
     * Adds the source code of the CuT as the user message. If the prompt specifies that dependencies have to be
     * included, the source code of the dependencies is included as well.
     */
    protected void addUserMessage() {
        if (conversation.getCurrentType() == PromptType.DEFEND_DEPENDENCIES
                || conversation.getCurrentType() == PromptType.ATTACK_DEPENDENCIES) {
            conversation.addUserMessage(game.getCUT().getSourceCode() + "\n" + String.join("\n", game.getCUT().getDependencyCode()));
        } else {
            conversation.addUserMessage(game.getCUT().getSourceCode());
        }
    }

    /**
     * Try to claim a random potentially equivalent mutant as equivalent, or do nothing if no such mutant is available.
     */
    protected void claimEquivalent() {
        Optional<MutantDTO> potentialEquivalent = getRandomPossiblyEquivalentMutant(game, user, random);
        if (potentialEquivalent.isPresent()) {
            logger.info("Claiming equivalence on mutant {}", potentialEquivalent.get());
            gameManagingUtils.claimBattlegroundEquivalence(game, user.getId(),
                    potentialEquivalent.get().getLines());
        }
    }

    /**
     * Return one random mutant from the set of all mutants that can be marked as equivalent by the user
     * and have acquired at least {@link LlmSubActionService#EQUIVALENT_POINT_RESTRICTION} points.
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

    /**
     * Returns the model's system prompt of this type, or the default prompt of this type if the model has no prompt
     * of its own. The model is <b>not</b> updated before.
     */
    protected String getSystemPrompt(LlModel model, PromptType type) {
        try {
            LlmRepository llmRepo = CDIUtil.getBeanFromCDI(LlmRepository.class);
            llmRepo.loadModel(model);
            Optional<String> specificPrompt = model.getPrompt(type);
            if (specificPrompt.isPresent()) {
                return specificPrompt.get();
            } else {
                LlModel defaultModel = llmRepo.getDefaultModel().orElseThrow();
                return defaultModel.getPrompt(type).orElseThrow();
            }
        } catch (NoSuchModelException e) {
            throw new RuntimeException("The model was deactivated:", e);
        }
    }
}
