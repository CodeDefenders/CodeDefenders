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
package org.codedefenders.notification.events.server.equivalence;

import org.codedefenders.notification.events.server.ServerEvent;

import com.google.gson.annotations.Expose;

/**
 * Equivalence duel won event.
 */
public class EquivalenceDuelWonEvent extends ServerEvent {
    @Expose
    private int userId;

    @Expose
    private int gameId;

    @Expose
    private int mutantId;

    /**
     * Returns the user id of the winner.
     *
     * @return the user id of the winner
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Sets the user id of the winner.
     *
     * @param userId the user id of the winner
     */
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getMutantId() {
        return mutantId;
    }

    public void setMutantId(int mutantId) {
        this.mutantId = mutantId;
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }
}
