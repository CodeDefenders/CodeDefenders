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
import java.util.Optional;

import javax.enterprise.inject.spi.CDI;

import org.codedefenders.persistence.database.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
    private transient String encodedPassword;
    private String email;
    private boolean validated;
    private boolean active;
    private boolean allowContact;
    private KeyMap keyMap;
    private String token;
    private boolean external;

    public UserEntity(String username) {
        this(username, UserEntity.encodePassword(""));
    }

    public UserEntity(String username, String encodedPassword) {
        this(username, encodedPassword, "");
    }

    public UserEntity(String username, String encodedPassword, String email) {
        this(username, encodedPassword, email, false);
    }

    public UserEntity(String username, String encodedPassword, String email, boolean external) {
        this(0, username, encodedPassword, email, external);
    }

    public UserEntity(int id, String username, String encodedPassword, String email) {
        this(id, username, encodedPassword, email, false);
    }

    public UserEntity(int id, String username, String encodedPassword, String email, boolean external) {
        this(id, username, encodedPassword, email, false, true, false, KeyMap.DEFAULT, null, external);
    }
    public UserEntity(int id, String username, String encodedPassword, String email, boolean validated,
            boolean active, boolean allowContact, KeyMap keyMap, String token, boolean external) {
        this.id = id;
        this.username = username;
        this.encodedPassword = encodedPassword;
        this.email = email.toLowerCase();
        this.validated = validated;
        this.active = active;
        this.allowContact = allowContact;
        this.keyMap = keyMap;
        this.token = token;
        this.external = external;
    }

    /**
     * @deprecated Use {@link org.codedefenders.persistence.database.UserRepository#insert(UserEntity)} instead.
     */
    @Deprecated
    public boolean insert() {
        // TODO: Remove workaround
        Optional<Integer> result = CDI.current().select(UserRepository.class).get().insert(this);
        if (!result.isPresent()) {
            return false;
        } else {
            id = result.get();
            return true;
        }
    }

    /**
     * @deprecated Use {@link org.codedefenders.persistence.database.UserRepository#update(UserEntity)} instead.
     */
    @Deprecated
    public boolean update() {
        // TODO: Remove workaround
        return CDI.current().select(UserRepository.class).get().update(this);
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isExternal() {
        return external;
    }

    @Deprecated // TODO(Alex): Where to put this method?
    public static String encodePassword(String password) {
        return new BCryptPasswordEncoder().encode(password);
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
