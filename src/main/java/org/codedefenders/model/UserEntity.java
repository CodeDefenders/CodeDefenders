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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Objects;

import javax.enterprise.inject.spi.CDI;

import org.codedefenders.database.DB;
import org.codedefenders.database.DatabaseValue;
import org.codedefenders.persistence.database.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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

    public UserEntity(String username) {
        this(username, UserEntity.encodePassword(""));
    }

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

    /**
     * @deprecated Use {@link org.codedefenders.persistence.database.UserRepository#insert(UserEntity)} instead.
     */
    @Deprecated
    public boolean insert() {
        // TODO: Remove workaround
        Integer result = CDI.current().select(UserRepository.class).get().insert(this);
        if (result == null) {
            return false;
        } else {
            id = result;
            return true;
        }

        /*
        // TODO Phil 12/12/18: Update this like Test#insert() to use DAO insert method but update identifier
        DatabaseValue[] valueList;
        String query;
        Connection conn = DB.getConnection();

        if (id <= 0) {
            query = "INSERT INTO users (Username, Password, Email) VALUES (?, ?, ?);";
            valueList = new DatabaseValue[]{DatabaseValue.of(username),
                    DatabaseValue.of(encodedPassword),
                    DatabaseValue.of(email)};
        } else {
            query = "INSERT INTO users (User_ID, Username, Password, Email) VALUES (?, ?, ?, ?);";
            valueList = new DatabaseValue[]{DatabaseValue.of(id),
                    DatabaseValue.of(username),
                    DatabaseValue.of(encodedPassword),
                    DatabaseValue.of(email)};
        }
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        int key = DB.executeUpdateGetKeys(stmt, conn);
        if (key != -1) {
            this.id = key;
            return true;
        } else {
            return false;
        }
         */
    }

    /**
     * @deprecated Use {@link org.codedefenders.persistence.database.UserRepository#update(UserEntity)} instead.
     */
    public boolean update() {
        return CDI.current().select(UserRepository.class).get().update(this);
        /*
        DatabaseValue[] valueList;
        Connection conn = DB.getConnection();

        String query = String.join("\n",
                "UPDATE users",
                "SET Username = ?,",
                "  Email = ?,",
                "  Password = ?,",
                "  Validated = ?,",
                "  Active = ?,",
                "  AllowContact = ?,",
                "  KeyMap = ?",
                "WHERE User_ID = ?;");
        valueList = new DatabaseValue[]{
                DatabaseValue.of(username),
                DatabaseValue.of(email),
                DatabaseValue.of(encodedPassword),
                DatabaseValue.of(validated),
                DatabaseValue.of(active),
                DatabaseValue.of(allowContact),
                DatabaseValue.of(keyMap.name()),
                DatabaseValue.of(id)
        };
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return DB.executeUpdate(stmt, conn);
         */
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

    public static String encodePassword(String password) {
        return new BCryptPasswordEncoder().encode(password);
    }

    public static boolean passwordMatches(String rawPassword, String encodedPassword) {
        return new BCryptPasswordEncoder().matches(rawPassword, encodedPassword);
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
