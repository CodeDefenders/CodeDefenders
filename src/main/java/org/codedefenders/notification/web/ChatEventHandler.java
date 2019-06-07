package org.codedefenders.notification.web;

import java.io.IOException;

import javax.websocket.EncodeException;
import javax.websocket.Session;

import org.codedefenders.notification.model.ChatEvent;

import com.google.common.eventbus.Subscribe;

public class ChatEventHandler {

    private int userId;
    private Session session;

    public ChatEventHandler(int userId, Session session) {
        this.userId = userId;
        this.session = session;
    }

    @Subscribe
    public void pushChatMessage(ChatEvent e) throws IOException, EncodeException {
        if (e.hasRecipient(this.userId)) {
            // TODO
            // session.getBasicRemote().sendObject(notification);
        }
    }
}
