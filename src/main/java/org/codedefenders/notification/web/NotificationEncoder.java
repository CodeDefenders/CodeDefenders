package org.codedefenders.notification.web;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import com.google.gson.Gson;
import org.codedefenders.notification.model.PushEvent;

public class NotificationEncoder implements Encoder.Text<PushEvent> {

    @Override
    public void init(EndpointConfig endpointConfig) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public String encode(PushEvent object) throws EncodeException {
        return new Gson().toJson(object);
    }
}
