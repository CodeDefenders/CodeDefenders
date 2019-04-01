package org.codedefenders.notification.web;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import org.codedefenders.notification.model.Notification;

import com.google.gson.Gson;

public class NotificationEncoder implements Encoder.Text<Notification> {

    @Override
    public void init(EndpointConfig endpointConfig) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public String encode(Notification object) throws EncodeException {
        return new Gson().toJson(object);
    }

}
