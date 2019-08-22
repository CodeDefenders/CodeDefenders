package org.codedefenders.notification.web;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.codedefenders.notification.events.client.ClientEvent;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

public class EventDecoder implements Decoder.Text<ClientEvent> {
    @Override
    public void init(EndpointConfig endpointConfig) {
    }

    @Override
    public void destroy() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public ClientEvent decode(String s) throws DecodeException {
        JsonObject obj;
        String type;
        JsonElement data;
        Class<ClientEvent> eventClass;

        try {
            obj = new JsonParser().parse(s).getAsJsonObject();
            type = obj.getAsJsonPrimitive("type").getAsString();
            data = obj.get("data");
        } catch (ClassCastException | IllegalStateException e) {
            throw new DecodeException(s, "Could not decode client event message type or data.", e);
        }

        try {
            eventClass = (Class<ClientEvent>) Class.forName("org.codedefenders.notification.events.client." + type);
        } catch (ClassNotFoundException e) {
            throw new DecodeException(s, "Invalid type for client message: " + type + ".", e);
        }

        try {
            return new Gson().fromJson(data, eventClass);
        } catch (ClassCastException | IllegalStateException e) {
            throw new DecodeException(s, "Could not decode client event message data.", e);
        }
    }

    @Override
    public boolean willDecode(String s) {
        return true;
    }
}
