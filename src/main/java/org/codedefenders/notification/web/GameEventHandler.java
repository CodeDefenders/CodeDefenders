package org.codedefenders.notification.web;

import java.io.IOException;

import javax.websocket.EncodeException;
import javax.websocket.Session;

import org.codedefenders.notification.model.GameLifecycleEvent;
import org.codedefenders.notification.model.Notification;
import org.codedefenders.notification.model.TestLifecycleEvent;

import com.google.common.eventbus.Subscribe;

public class GameEventHandler {

    // TODO Why not simply a string?
    private final Notification notification = new Notification("GAME");
    
    private int gameId;
    private int playerId;
    
    private Session session;

    public GameEventHandler(int playerId, int gameId, Session session) {
        super();
        this.playerId = playerId;
        this.gameId = gameId;
        this.session = session;
    }

    @Subscribe
    public void pushGameEvent(GameLifecycleEvent e) throws IOException, EncodeException {
        if (this.gameId == e.getGame().getId()) {
            session.getBasicRemote().sendObject( notification );
        } 
    }
    
    @Subscribe
    public void pushGameEvent(TestLifecycleEvent e) throws IOException, EncodeException {
        if (this.playerId == e.getTest().getPlayerId() ){
            session.getBasicRemote().sendObject( notification );
        } 
    }
    
}
