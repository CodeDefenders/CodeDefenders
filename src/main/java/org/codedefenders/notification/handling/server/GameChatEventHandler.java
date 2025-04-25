/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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
package org.codedefenders.notification.handling.server;

import java.io.IOException;
import java.util.Objects;

import jakarta.websocket.EncodeException;

import org.codedefenders.game.Role;
import org.codedefenders.notification.events.server.chat.ServerGameChatEvent;
import org.codedefenders.notification.events.server.chat.ServerSystemChatEvent;
import org.codedefenders.notification.web.PushSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

public class GameChatEventHandler implements ServerEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(GameChatEventHandler.class);

    private PushSocket socket;
    private int gameId;
    private Role role;
    private String ticket;

    public GameChatEventHandler(PushSocket socket, int gameId, Role role, String ticket) {
        this.socket = socket;
        this.gameId = gameId;
        this.role = role;
        this.ticket = ticket;
    }

    public int getGameId() {
        return gameId;
    }

    public Role getRole() {
        return role;
    }

    @Subscribe
    public void sendChatMessage(ServerGameChatEvent event) throws IOException, EncodeException {
        if (event.getGameId() == this.gameId) {
            if (this.role == Role.OBSERVER
                    || event.isAllChat()
                    || event.getRole() == role
                    || event.getRole() == Role.OBSERVER) {
                socket.sendEvent(event);
            }
        }
    }

    @Subscribe
    public void sendSystemChatMessage(ServerSystemChatEvent event) throws IOException, EncodeException {
        if (this.ticket.equals(event.getTicket())) {
            socket.sendEvent(event);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GameChatEventHandler that = (GameChatEventHandler) o;
        return gameId == that.gameId
                && role == that.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId, role, ticket);
    }
}
