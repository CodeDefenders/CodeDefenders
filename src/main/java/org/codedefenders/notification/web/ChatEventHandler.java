package org.codedefenders.notification.web;

import java.io.IOException;

import javax.websocket.EncodeException;
import javax.websocket.Session;

import org.codedefenders.notification.model.ChatEvent;
import org.codedefenders.notification.model.Notification;

import com.google.common.eventbus.Subscribe;

public class ChatEventHandler {

    // TODO Why not simply a string?
    // TODO Message is never set?
    private final Notification notification = new Notification("CHAT");

    private int userId;
    private Session session;

    public ChatEventHandler(int userId, Session session) {
        this.userId = userId;
        this.session = session;
    }

    @Subscribe
    public void pushChatMessage(ChatEvent e) throws IOException, EncodeException {
        if (e.addRecipient(this.userId)) {
            session.getBasicRemote().sendObject(notification);
        }
    }
}
