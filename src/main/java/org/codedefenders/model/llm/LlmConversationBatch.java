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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.llm.strategy.LlmStrategy;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

/**
 * Contains mutable lists of {@link LlmConversation}s, representing the back and forth
 * conversation between the client and the LLM. Every prompt type has its own associated conversation, in order to allow
 * for several conversations. This way history about mutant generation attempts is not lost when trying to solve an
 * equivalence duel, for example.
 * <p>
 * Within one LLM action, the prompt type of this conversation should be established in the beginning,
 * before querying the LLM, and remain unchanged thereafter. This prompt type is represented by
 * {@link LlmConversationBatch#currentType}.
 */
public class LlmConversationBatch {
    private final Map<PromptType, LlmConversation> messageLists = new HashMap<>();
    private final AbstractGame game;
    private final SimpleUser user;

    private PromptType currentType;

    private final LlmStrategy strategy;

    public LlmConversationBatch(AbstractGame game, SimpleUser user, LlmStrategy strategy) {
        this.game = game;
        this.user = user;
        this.strategy = strategy;
    }

    public LlmConversation currentConversation() {
        if (currentType == null) {
            throw new RuntimeException("currentType has not been set yet.");
        }
        LlmConversation result = messageLists.get(currentType);
        if (result == null) {
            throw new IllegalStateException("currentType has been set, but there is no conversation.");
        }
        return result;
    }

    public void addAiMessage(AiMessage msg, LlModel model, int inputTokens, int outputTokens) {
        if (currentConversation().isEmpty()) {
            throw new IllegalStateException("AiMessage cannot be the first message in a conversation");
        }
        currentConversation().add(msg, model, inputTokens, outputTokens);
    }

    public void addSystemMessage(String content, LlModel model) {
        currentConversation().add(SystemMessage.from(content), model);
    }

    public void addUserMessage(String content, LlModel model) {
        currentConversation().add(UserMessage.from(content), model);
    }



    public boolean isEmpty() {
        return currentConversation().isEmpty();
    }

    public void resetCurrent(boolean success) {
        if (currentType == null) {
            throw new IllegalStateException("The current type is already null");
        }
        currentConversation().finish(success);
        messageLists.remove(currentType);
        currentType = null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("LlmConversationBatch:{");
        for (PromptType t : messageLists.keySet()) {
            sb.append(t.name()).append(":{");
            sb.append(messageLists.get(t));
            sb.append("}");
        }
        return sb.toString();
    }

    public PromptType getCurrentType() {
        return currentType;
    }

    public void resetCurrentType() {
        currentType = null;
    }

    /**
     * Set the current prompt type of the batch. If there is no conversation for this type yet, a new one is created
     */
    public void setCurrentType(PromptType currentType) {
        if (this.currentType != null) {
            throw new RuntimeException("Current type has already been set. " +
                    "It should only be reset at the end of an llm action");
        }
        if (currentType == null) {
            throw new IllegalArgumentException("null is not allowed here. Use 'resetCurrentType()' instead");
        }
        this.currentType = currentType;
        if (!messageLists.containsKey(currentType)) {
            LlmConversation con = new LlmConversation(currentType, game, user, strategy.getName(), true, false);
            messageLists.put(currentType, con);
        }
    }

    public Collection<LlmConversation> getConversations() {
        return messageLists.values();
    }

    public boolean lastMessageWasError() {
        return currentConversation().lastMessageWasError();
    }

    public boolean hasSystemMessage() {
        return currentConversation().hasSystemMessage();
    }
}
