package org.codedefenders.notification.web;

import java.io.IOException;

import javax.websocket.EncodeException;
import javax.websocket.Session;

import org.codedefenders.notification.events.server.MutantLifecycleEvent;
import org.codedefenders.notification.events.server.TestLifecycleEvent;

import com.google.common.eventbus.Subscribe;

public class ProgressBarEventHandler {

    private int playerId;
    private final Session session;

    public ProgressBarEventHandler(int playerId, Session session) {
        this.playerId = playerId;
        this.session = session;
    }

    @Subscribe
    public void updateProgressBar(TestLifecycleEvent e) throws IOException, EncodeException {
        if (this.playerId == e.getTest().getPlayerId()) {
            // TODO: send progressbar event instead of notification
            // Notification notification = new Notification("PROGRESSBAR");
            // notification.setMessage(e.getEventType());
            synchronized (session) {
                if (session.isOpen()) {
                    // TODO
                    // session.getBasicRemote().sendObject(notification);
                } else {
                   // TODO Log this ?
                }
            }
        }
    }

    // For some reason mutants have owner but tests do not...
    @Subscribe
    public void updateProgressBar(MutantLifecycleEvent e) throws IOException, EncodeException {
        if (this.playerId == e.getMutant().getPlayerId()) {
            // TODO: send progressbar event instead of notification
            // Notification notification = new Notification("PROGRESSBAR");
            // notification.setMessage(e.getEventType());
            synchronized (session) {
                if (session.isOpen()) {
                    // TODO
                    // session.getBasicRemote().sendObject(notification);
                } else {
                    // TODO Log this ?
                }
            }
        }
    }
}
