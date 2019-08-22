package org.codedefenders.notification.web;

import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.codedefenders.notification.events.server.ServerEvent;

public class EventEncoder implements Encoder.Text<ServerEvent> {
    @Override
    public void init(EndpointConfig endpointConfig) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public String encode(ServerEvent event) {
        JsonObject message = new JsonObject();
        message.addProperty("type", event.getClass().getSimpleName());
        message.add("data", event.toJson());
        return new Gson().toJson(message);
    }
}
