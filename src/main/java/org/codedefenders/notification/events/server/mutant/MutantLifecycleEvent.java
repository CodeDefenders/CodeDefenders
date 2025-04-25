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
package org.codedefenders.notification.events.server.mutant;

import org.codedefenders.notification.events.server.ServerEvent;

import com.google.gson.annotations.Expose;

public abstract class MutantLifecycleEvent extends ServerEvent {
    /* TODO: Mutants don't have an Id until compiled, why not give them and Id before? */
    @Expose private Integer mutantId;
    @Expose private int userId;
    @Expose private int gameId;

    public Integer getMutantId() {
        return mutantId;
    }

    public int getUserId() {
        return userId;
    }

    public int getGameId() {
        return gameId;
    }

    public void setMutantId(Integer mutantId) {
        this.mutantId = mutantId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }
}
