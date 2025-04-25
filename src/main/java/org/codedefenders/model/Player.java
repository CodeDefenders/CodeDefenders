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
package org.codedefenders.model;

import org.codedefenders.game.Role;

/**
 * This class serves as a container for players.
 *
 * <p>Users in a game as represented as players and have a certain role.
 *
 * @see Role
 */
public class Player {
    private int id;
    private UserEntity user;
    private int gameId;
    private int points;
    private Role role;
    private boolean active;

    public Player(int id, UserEntity user, int gameId, int points, Role role, boolean active) {
        this.id = id;
        this.user = user;
        this.gameId = gameId;
        this.points = points;
        this.role = role;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public int getGameId() {
        return gameId;
    }

    public int getPoints() {
        return points;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }
}
