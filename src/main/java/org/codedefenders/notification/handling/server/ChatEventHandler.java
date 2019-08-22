package org.codedefenders.notification.handling.server;

import java.io.IOException;

import javax.websocket.EncodeException;
import javax.websocket.Session;

import org.codedefenders.notification.events.server.ServerChatEvent;

import com.google.common.eventbus.Subscribe;

public class ChatEventHandler {

    private int userId;
    private Session session;

    public ChatEventHandler(int userId, Session session) {
        this.userId = userId;
        this.session = session;
    }

    @Subscribe
    public void pushChatMessage(ServerChatEvent e) throws IOException, EncodeException {
        if (e.hasRecipient(this.userId)) {
            session.getBasicRemote().sendObject(e);
        }
    }
}
