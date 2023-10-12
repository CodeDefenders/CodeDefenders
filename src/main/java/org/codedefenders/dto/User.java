/*
 * Copyright (C) 2021 Code Defenders contributors
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

package org.codedefenders.dto;

import java.util.Objects;

import org.codedefenders.model.KeyMap;

/**
 * This object contains all the properties of a User we expose to the application.
 */
public class User extends SimpleUser {

    private final boolean active;

    private final String email;
    private final boolean emailValidated;
    private final boolean contactingAllowed;

    private final KeyMap keyMap;

    public User(int id, String name, boolean active, String email, boolean emailValidated,
            boolean contactingAllowed, KeyMap keyMap) {
        super(id, name);
        this.active = active;
        this.email = email;
        this.emailValidated = emailValidated;
        this.contactingAllowed = contactingAllowed;
        this.keyMap = keyMap;
    }

    public boolean isActive() {
        return active;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailValidated() {
        return emailValidated;
    }

    public boolean isContactingAllowed() {
        return contactingAllowed;
    }

    public KeyMap getKeyMap() {
        return keyMap;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        User user = (User) other;
        return getId() == user.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
