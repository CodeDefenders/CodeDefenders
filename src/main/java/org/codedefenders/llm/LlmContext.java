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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.model.llm.LlModel;
import org.codedefenders.model.llm.LlmConversation;
import org.codedefenders.model.llm.LlmConversationBatch;
import org.codedefenders.model.llm.LlmDefaultStrategies;
import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.util.Constants;
import org.codedefenders.util.LlmUtils;

public class LlmContext {
    private final LlModel model;
    private final LlmStrategy strategy;
    private final LlmStrategy equivalenceStrategy;
    private final LlmConversationBatch conversationBatch;
    private final SimpleUser user;
    private final Role role;
    private final Random random;
    private final Map<Object, Object> baggages = new HashMap<>();

    private AbstractGame game;
    private LlmConversation conversation;
    private LlmOrganizer.ThreadState threadState;
    private String errorMessage;

    LlmContext(LlModel model, LlmStrategy strategy, AbstractGame game) {
        this.model = model;
        this.strategy = strategy;
        this.equivalenceStrategy = LlmStrategy.of(LlmDefaultStrategies.EQUIVALENCE_DEFAULT);
        this.user = game instanceof MeleeGame
                ? new SimpleUser(Constants.AI_PLAYER_USER_ID, "AI_PLAYER")
                : LlmUtils.getUserFromStrategy(strategy);
        this.conversationBatch = new LlmConversationBatch(
                game,
                user,
                strategy
        );
        this.game = game;
        this.threadState = LlmOrganizer.ThreadState.ACTIVE;
        this.role = LlmUtils.getRoleFromStrategy(strategy);
        this.random = new Random();
    }

    static LlmContext finishingModel(LlModel model, LlmStrategy strategy, AbstractGame game) {
        LlmContext result = new LlmContext(model, strategy, game);
        result.setThreadState(LlmOrganizer.ThreadState.FINISHING);
        return result;
    }

    public void updateGame() {
        game = CDIUtil.getBeanFromCDI(GameRepository.class).getGame(game.getId());
    }

    public LlModel model() {
        return model;
    }

    public LlmStrategy strategy() {
        return strategy;
    }

    public LlmStrategy equivalenceStrategy() {
        return equivalenceStrategy;
    }

    public LlmConversationBatch conversationBatch() {
        return conversationBatch;
    }

    public LlmOrganizer.ThreadState threadState() {
        return threadState;
    }

    public void setThreadState(LlmOrganizer.ThreadState threadState) {
        this.threadState = threadState;
    }

    public SimpleUser user() {
        return user;
    }

    public Role role() {
        return role;
    }

    public Random random() {
        return random;
    }

    public AbstractGame game() {
        return game;
    }

    public LlmConversation getConversation() {
        return conversation;
    }

    public Map<Object, Object> getBaggages() {
        return baggages;
    }


    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(Throwable e) {
        String timestamp = LocalDateTime.now().toString();
        this.errorMessage = timestamp + ": " + e.toString();
    }
}
