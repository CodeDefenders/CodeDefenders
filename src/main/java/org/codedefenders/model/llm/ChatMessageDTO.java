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
import java.time.LocalDateTime;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

/**
 * Wraps {@link dev.langchain4j.data.message.ChatMessage}s with data about creation time and targeted {@link LlModel}
 */
public record ChatMessageDTO (
    ChatMessage msg,
    Timestamp time,
    LlModel target,
    int inputTokens,
    int outputTokens
    ){
    public ChatMessageDTO(ChatMessage msg, Timestamp time, LlModel target) {
        this(msg, time, target, 0, 0);
    }

    public String getText() {
        if (msg instanceof UserMessage um) {
            return um.singleText();
        } else if (msg instanceof SystemMessage sm) {
            return sm.text();
        } else if (msg instanceof AiMessage am) {
            return am.text();
        } else throw new IllegalStateException("msg is of type " + msg.getClass() + ", that is not supported");
    }
}
