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

import java.io.Serializable;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a user object in the database.
 *
 * @apiNote Should not be used directly in the frontend, instead it should only be used in the database layer
 * (repositories) and in the business layer (services), but shouldn't be exposed in the API from the later ones.
 */
public class UserEntity implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(UserEntity.class);

    private int id;
    private String username;
    private String encodedPassword;
    private String email;
    private boolean validated;
    private boolean active;
    private boolean allowContact;
    private KeyMap keyMap;

    public UserEntity(String username, String encodedPassword) {
        this(username, encodedPassword, "");
    }

    public UserEntity(String username, String encodedPassword, String email) {
        this(0, username, encodedPassword, email);
    }

    public UserEntity(int id, String username, String encodedPassword, String email) {
        this(id, username, encodedPassword, email, false, true, false, KeyMap.DEFAULT);
    }

    public UserEntity(int id, String username, String encodedPassword, String email, boolean validated,
            boolean active, boolean allowContact, KeyMap keyMap) {
        this.id = id;
        this.username = username;
        this.encodedPassword = encodedPassword;
        this.email = email.toLowerCase();
        this.validated = validated;
        this.active = active;
        this.allowContact = allowContact;
        this.keyMap = keyMap;
    }

    public boolean isValidated() {
        return validated;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEncodedPassword() {
        return encodedPassword;
    }

    public void setEncodedPassword(String encodedPassword) {
        this.encodedPassword = encodedPassword;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }


    public boolean getAllowContact() {
        return allowContact;
    }

    public void setAllowContact(boolean allowContact) {
        this.allowContact = allowContact;
    }

    public KeyMap getKeyMap() {
        return keyMap;
    }

    public void setKeyMap(KeyMap keyMap) {
        this.keyMap = keyMap;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        UserEntity user = (UserEntity) other;
        return getId() == user.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
