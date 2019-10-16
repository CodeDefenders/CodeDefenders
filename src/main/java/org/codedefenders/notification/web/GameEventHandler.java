/*
 * Copyright (C) 2016-2019 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.notification.web;

import java.io.IOException;

import javax.websocket.EncodeException;
import javax.websocket.Session;

import org.codedefenders.notification.model.GameLifecycleEvent;
import org.codedefenders.notification.model.MutantLifecycleEvent;
import org.codedefenders.notification.model.TestLifecycleEvent;
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
