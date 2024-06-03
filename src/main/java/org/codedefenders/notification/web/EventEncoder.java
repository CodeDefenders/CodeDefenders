package org.codedefenders.notification.web;

import org.codedefenders.notification.events.EventNames;
import org.codedefenders.notification.events.server.ServerEvent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

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
