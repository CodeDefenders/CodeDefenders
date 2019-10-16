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

import org.codedefenders.notification.model.MutantLifecycleEvent;
import org.codedefenders.notification.model.TestLifecycleEvent;

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
