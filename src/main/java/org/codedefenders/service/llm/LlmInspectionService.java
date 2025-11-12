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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.llm.ChatMessageDTO;
import org.codedefenders.model.llm.LlmConversation;
import org.codedefenders.persistence.database.LlmConversationRepository;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;


@ApplicationScoped
public class LlmInspectionService {

    @Inject
    LlmConversationRepository conversationRepository;

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public List<LlmConversation> getConversations(AbstractGame game) {
        return conversationRepository.getConversations(game);
    }

    public String toJson(List<LlmConversation> conversations) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(ChatMessageDTO.class, (JsonSerializer<ChatMessageDTO>) (src, typeOfSrc, context) -> {
            JsonObject msgDtoObject = new JsonObject();
            msgDtoObject.addProperty("message", src.getText());
            Timestamp stamp = src.time();
            long millis = stamp.getTime();
            Instant instant = Instant.ofEpochMilli(millis);
            String format = dateTimeFormatter.format(instant);

            msgDtoObject.addProperty("timestamp",
                    format);
            msgDtoObject.addProperty("model", src.target().getType() + ":" + src.target().getName());
            msgDtoObject.addProperty("messageType", src.msg().type().toString());
            return msgDtoObject;
        });
        return gsonBuilder.create().toJson(conversations);
    }
}
