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
 * Equivalence duel triggered or result event.
 */
public class EquivalenceDuelEvent extends ServerEvent {

    @Expose
    private int defenderId;

    @Expose
    private int attackerId;

    @Expose
    private int gameId;

    @Expose
    private int mutantId;


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

    public int getDefenderId() {
        return defenderId;
    }

    public void setDefenderId(int defenderId) {
        this.defenderId = defenderId;
    }

    public int getAttackerId() {
        return attackerId;
    }

    public void setAttackerId(int attackerId) {
        this.attackerId = attackerId;
    }
}
