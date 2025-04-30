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

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.codedefenders.game.Role;

/**
 * This class serves as a container class for {@link UserEntity Users} and
 * additional information.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
public class UserInfo implements Serializable {
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final UserEntity user;
    private final Instant lastLogin;
    private final Role lastRole;
    private final int totalScore;

    public UserInfo(UserEntity user, Instant lastLogin, Role lastRole, int totalScore) {
        this.user = user;
        this.lastLogin = lastLogin;
        this.lastRole = lastRole;
        this.totalScore = totalScore;
    }

    public UserEntity getUser() {
        return user;
    }

    public String getLastLoginString() {
        return lastLogin == null
                ? "-- never --"
                : dateTimeFormatter.format(lastLogin);
    }

    public Instant getLastLogin() {
        return lastLogin;
    }

    public Role getLastRole() {
        return lastRole;
    }

    public int getTotalScore() {
        return totalScore;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        UserInfo userInfo = (UserInfo) other;
        return getUser().equals(userInfo.getUser());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUser());
    }
}
