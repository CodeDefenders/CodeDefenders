package org.codedefenders.notification.web;

import java.io.IOException;

import javax.websocket.EncodeException;
import javax.websocket.Session;

import org.codedefenders.notification.model.MutantLifecycleEvent;
import org.codedefenders.notification.model.Notification;
import org.codedefenders.notification.model.TestLifecycleEvent;

import com.google.common.eventbus.Subscribe;

public class ProgressBarEventHandler {

    private int playerId;
    private Session session;
    
    public ProgressBarEventHandler(int playerId, Session session) {
        this.playerId = playerId;
        this.session = session;
    }
    
    @Subscribe
    public void updateProgressBar(TestLifecycleEvent e) throws IOException, EncodeException {
        if (this.playerId == e.getTest().getPlayerId()) {
            Notification notification = new Notification("PROGRESS_BAR");
            notification.setMessage(e.getEventType());
            session.getBasicRemote().sendObject(notification);
        }
    }
    
    // For some reason mutants have owner but tests do not...
    @Subscribe
    public void updateProgressBar(MutantLifecycleEvent e) throws IOException, EncodeException {
        if (this.playerId == e.getMutant().getPlayerId()) {
            Notification notification = new Notification("PROGRESS_BAR");
            notification.setMessage(e.getEventType());
            session.getBasicRemote().sendObject(notification);
        }
    }
}
