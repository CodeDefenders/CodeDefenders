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
package org.codedefenders.notification.events.server.highlighting;

import org.codedefenders.notification.events.server.ServerEvent;

import com.google.gson.annotations.Expose;

/**
 * Event sent to trigger a change of the game highlighting (mutant/tests/coverage).
 * Contains the game id of the game for which the highlighting should be refreshed.
 */
public abstract class GameHighlightingEvent extends ServerEvent {

    @Expose
    private int gameId;

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }
}
