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
package org.codedefenders.model;

import org.codedefenders.game.Role;

import java.sql.Timestamp;

/**
 * This class serves as a container class for {@link User Users} and
 * additional information.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
public class UserInfo {
    private User user;
    private Timestamp lastLogin;
    private Role lastRole;
    private int totalScore;

    public UserInfo(User user, Timestamp lastLogin, Role lastRole, int totalScore) {
        this.user = user;
        this.lastLogin = lastLogin;
        this.lastRole = lastRole;
        this.totalScore = totalScore;
    }

    public User getUser() {
        return user;
    }

    public String getLastLoginString() {
        return lastLogin == null ? "-- never --" : lastLogin.toString().substring(0, lastLogin.toString().length() - 5);
    }

    public Role getLastRole() {
        return lastRole;
    }

    public int getTotalScore() {
        return totalScore;
    }
}
