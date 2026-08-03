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
package org.codedefenders.model.llm;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Role;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

/**
 * This represents the prompts and responses of an LLM.
 * It includes data about the associated strategy, LLM user, game, test/mutant id and success state.
 */
public class LlmConversation {
    private final String strategyName; //This is only used for analysis/recordkeeping, the name is enough
    private final AbstractGame game;
    private final SimpleUser user;
    private final List<ChatMessageDTO> messages = new ArrayList<>();
    private boolean active;
    private boolean success;
    private final String type;
    private final Role role;
    private int id = -1;
    private int testId;
    private int mutantId;


    public LlmConversation(String type, Role role, AbstractGame game, SimpleUser user, String strategyName,
                           boolean active, boolean success, int testId, int mutantId) {
        this.type = type;
        this.role = role;
        this.game = game;
        this.user = user;
        this.strategyName = strategyName;
        this.active = active;
        this.success = success;
        this.testId = testId;
        this.mutantId = mutantId;
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getType() {
        return type;
    }

    public void add(ChatMessageDTO dto) {
        messages.add(dto);
    }

    public void add(ChatMessage message, LlModel model, int inputTokens, int outputTokens) {
        add(new ChatMessageDTO(message, Timestamp.from(Instant.now()), model, inputTokens, outputTokens));
    }

    public void add(ChatMessage message, LlModel model) {
        add(message, model, 0, 0);
    }

    public void addAiMessage(AiMessage msg, LlModel model, int inputTokens, int outputTokens) {
        add(msg, model, inputTokens, outputTokens);
    }

    public void addSystemMessage(String msg, LlModel model) {
        add(SystemMessage.from(msg), model);
    }

    public void addUserMessage(String msg, LlModel model) {
        add(UserMessage.from(msg), model);
    }

    public ChatMessage[] toArray() {
        ChatMessage[] result = new ChatMessage[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            result[i] = messages.get(i).msg();
        }
        return result;
    }

    public List<ChatMessageDTO> getMessages() {
        return Collections.unmodifiableList(messages);
    }


    public int numberOfTries() {
        int result = 0;
        for (ChatMessageDTO dto : messages) {
            if (dto.msg() instanceof AiMessage) {
                result++;
            }
        }
        return result;
    }

    public void finish(boolean success) {
        this.success = success;
        active = false;
    }

    public boolean lastMessageWasError() {
        if (isEmpty()) {
            return false;
        } else {
            return !(messages.get(messages.size() - 1).msg() instanceof AiMessage);
        }
    }

    public boolean hasSystemMessage() {
        return messages.stream().anyMatch(m -> m.msg() instanceof SystemMessage);
    }

    public boolean isActive() {
        return active;
    }

    public String toString() {
        return String.join("", messages.stream().map(m -> "[" + m + "]").toList());
    }

    public String getStrategyName() {
        return strategyName;
    }

    public SimpleUser getUser() {
        return user;
    }

    public AbstractGame getGame() {
        return game;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTestId() {
        return testId;
    }

    public void setTestId(int testId) {
        this.testId = testId;
    }

    public int getMutantId() {
        return mutantId;
    }

    public void setMutantId(int mutantId) {
        this.mutantId = mutantId;
    }

    public Role getRole() {
        return role;
    }
}
