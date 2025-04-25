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
package org.codedefenders.notification.events.client.registration;

import org.codedefenders.notification.events.server.game.GameCreatedEvent;
import org.codedefenders.notification.events.server.game.GameJoinedEvent;
import org.codedefenders.notification.events.server.game.GameLeftEvent;
import org.codedefenders.notification.events.server.game.GameStartedEvent;
import org.codedefenders.notification.events.server.game.GameStoppedEvent;
import org.codedefenders.notification.handling.ClientEventHandler;

import com.google.gson.annotations.Expose;

/**
 * A message used to register for game lifecycle events.
 * <br>
 * This includes:
 * <ul>
 * <li>{@link GameCreatedEvent}</li>
 * <li>{@link GameJoinedEvent}</li>
 * <li>{@link GameLeftEvent}</li>
 * <li>{@link GameStartedEvent}</li>
 * <li>{@link GameStoppedEvent}</li>
 * </ul>
 */
public class GameLifecycleRegistrationEvent extends RegistrationEvent {
    @Expose
    private int gameId;

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    @Override
    public void accept(ClientEventHandler visitor) {
        visitor.visit(this);
    }
}
