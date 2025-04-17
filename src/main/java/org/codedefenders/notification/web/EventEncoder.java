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
package org.codedefenders.notification.web;

import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

import org.codedefenders.notification.events.EventNames;
import org.codedefenders.notification.events.server.ServerEvent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Encodes server events to a JSON string with a "{type: string, data: {}}" format.
 * @see ServerEvent
 * @see EventNames
 * @see PushSocket
 */
public class EventEncoder implements Encoder.Text<ServerEvent> {
    @Override
    public void init(EndpointConfig endpointConfig) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public String encode(ServerEvent event) {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();

        JsonObject message = new JsonObject();
        message.addProperty("type", EventNames.toServerEventName(event.getClass()));
        message.add("data", gson.toJsonTree(event));
        return gson.toJson(message);
    }
}
