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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

/**
 * Contains mutable lists of {@link dev.langchain4j.data.message.ChatMessage}s, representing the back and forth
 * conversation between the client and the LLM. Every prompt type has its own associated conversation, in order to allow
 * for several conversations.
 * <p>
 * Within one LLM action, the prompt type of this conversation should be established in the beginning,
 * before querying the LLM, and remain unchanged thereafter. This prompt type is represented by
 * {@link LlmConversation#currentType}.
 */
public class LlmConversation {
    private final Map<PromptType, List<ChatMessage>> messageLists = new HashMap<>();

    private PromptType currentType;

    public LlmConversation() {
        for (PromptType t : PromptType.values()) {
            messageLists.put(t, new ArrayList<>());
        }
    }

    private List<ChatMessage> currentMessages() {
        if (currentType == null) {
            throw new RuntimeException("currentType has not been set yet.");
        }
        return messageLists.get(currentType);
    }

    public void addAiMessage(AiMessage msg) {
        if (currentMessages().isEmpty()) {
            throw new IllegalStateException("AiMessage cannot be the first message in a conversation");
        }
        currentMessages().add(msg);
    }

    public void addSystemMessage(String content) {
        currentMessages().add(SystemMessage.from(content));
    }

    public void addUserMessage(String content) {
        currentMessages().add(UserMessage.from(content));
    }

    public ChatMessage[] toArray() {
        List<ChatMessage> messages = currentMessages();
        ChatMessage[] result = new ChatMessage[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            result[i] = messages.get(i);
        }
        return result;
    }

    public boolean isEmpty() {
        return currentMessages().isEmpty();
    }

    public void clear() {
        currentMessages().clear();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("LlmConversation:{");
        for (PromptType t : messageLists.keySet()) {
            sb.append(t.name()).append(":{");
            for (ChatMessage msg : messageLists.get(t)) {
                sb.append("[").append(msg).append("]");
            }
            sb.append("}");
        }
        return sb.toString();
    }

    /**
     * Returns the number of responses the llm has already returned.
     */
    public int numberOfTries() {
        int result = 0;
        for (ChatMessage msg : currentMessages()) {
            if (msg instanceof AiMessage) {
                result++;
            }
        }
        return result;
    }

    public PromptType getCurrentType() {
        return currentType;
    }

    public void resetCurrentType() {
        currentType = null;
    }

    public void setCurrentType(PromptType currentType) {
        if (this.currentType != null) {
            throw new RuntimeException("Current type has already been set. " +
                    "It should only be reset at the end of an llm action");
        }
        this.currentType = currentType;
    }
}
