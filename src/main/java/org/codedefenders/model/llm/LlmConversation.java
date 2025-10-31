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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;

/**
 * This represents the prompts and responses of an LLM.
 */
public class LlmConversation {
    private final List<ChatMessageDTO> messages = new ArrayList<>();
    private boolean active = true;
    private boolean success = false;
    private final PromptType type;

    public LlmConversation(PromptType type) {
        this.type = type;
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public boolean isSuccess() {
        return success;
    }

    public PromptType getType() {
        return type;
    }

    public void add(ChatMessage message, LlModel model) {
        messages.add(new ChatMessageDTO(message, LocalDateTime.now(), model));
    }

    public ChatMessage[] toArray() {
        ChatMessage[] result = new ChatMessage[messages.size()];
        for (int i = 0; i < messages.size(); i++) {
            result[i] = messages.get(i).msg();
        }
        return result;
    }

    public List<ChatMessageDTO> copyMessages() {
        return List.copyOf(messages);
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

    public boolean isActive() {
        return active;
    }

    public String toString() {
        return String.join("", messages.stream().map(m -> "[" + m + "]").toList());
    }
}
