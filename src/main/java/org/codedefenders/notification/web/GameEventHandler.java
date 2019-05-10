package org.codedefenders.notification.web;

import java.io.IOException;

import javax.websocket.EncodeException;
import javax.websocket.Session;

import org.codedefenders.notification.model.GameLifecycleEvent;
import org.codedefenders.notification.model.MutantLifecycleEvent;
import org.codedefenders.notification.model.Notification;
import org.codedefenders.notification.model.TestLifecycleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

public class GameEventHandler {

    private final static Logger logger = LoggerFactory.getLogger(GameEventHandler.class);

    /*
     * Note that here we "only" send a notification. We can include a custom
     * message in the notification object
     */
    private final Notification notification = new Notification("GAME");

    /*
     * Filter events by Game
     */
    private int gameId;

    /*
     * Filter events by Player
     */
    private int playerId;

    /*
     * WebSocket Session
     */
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
            session.getBasicRemote().sendObject(notification);
        }
    }

    @Subscribe
    public void pushGameEvent(TestLifecycleEvent e) throws IOException, EncodeException {
        if (this.playerId != e.getTest().getPlayerId()) {
            session.getBasicRemote().sendObject(notification);
        }
    }

    @Subscribe
    public void pushGameEvent(MutantLifecycleEvent e) throws IOException, EncodeException {
        if (this.playerId != e.getMutant().getPlayerId()) {
            session.getBasicRemote().sendObject(notification);
        }
    }

}
