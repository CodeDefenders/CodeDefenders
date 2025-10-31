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

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.llm.ChatMessageDTO;
import org.codedefenders.model.llm.LlmConversation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;


@ApplicationScoped
public class LlmInspectionService {

    /*
        These lists are intended for human consumption, for analysis and curiosity, not for further interaction
    */
    private final Map<AbstractGame, List<LlmConversation>> conversations = new HashMap<>();

    /**
     * Adds a new conversation to the map, only if it doesn't exist already.
     */
    public void addConversation(AbstractGame game, LlmConversation con) {
        conversations.putIfAbsent(game, new ArrayList<>());
        List<LlmConversation> l = conversations.get(game);
        if (!l.contains(con)) {
            l.add(con);
        }
    }

    public List<LlmConversation> getConversations(AbstractGame game) {
        List<LlmConversation> result = conversations.get(game);
        if (result == null) {
            return List.of();
        } else {
            return List.copyOf(result);
        }
    }

    public String toJson(List<LlmConversation> conversations) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(ChatMessageDTO.class, (JsonSerializer<ChatMessageDTO>) (src, typeOfSrc, context) -> {
            JsonObject msgDtoObject = new JsonObject();
            msgDtoObject.addProperty("message", src.getText());
            msgDtoObject.addProperty("timestamp",
                    src.time().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            msgDtoObject.addProperty("model", src.target().getType() + ":" + src.target().getName());
            msgDtoObject.addProperty("messageType", src.msg().type().toString());
            return msgDtoObject;
        });
        return gsonBuilder.create().toJson(conversations);
    }
}
