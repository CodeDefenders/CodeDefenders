package org.codedefenders.notification.web;

import java.io.IOException;

import javax.websocket.EncodeException;
import javax.websocket.Session;

import org.codedefenders.notification.events.server.GameLifecycleEvent;
import org.codedefenders.notification.events.server.MutantLifecycleEvent;
import org.codedefenders.notification.events.server.TestLifecycleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

public class GameEventHandler {

    private final static Logger logger = LoggerFactory.getLogger(GameEventHandler.class);

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
            // TODO
            // session.getBasicRemote().sendObject(notification);
        }
    }

    @Subscribe
    public void pushGameEvent(TestLifecycleEvent e) throws IOException, EncodeException {
        if (this.playerId != e.getTest().getPlayerId()) {
            // TODO
            // session.getBasicRemote().sendObject(notification);
        }
    }

    @Subscribe
    public void pushGameEvent(MutantLifecycleEvent e) throws IOException, EncodeException {
        if (this.playerId != e.getMutant().getPlayerId()) {
            // TODO
            // session.getBasicRemote().sendObject(notification);
        }
    }

}
