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
package org.codedefenders.database;

import org.codedefenders.game.Role;
import org.codedefenders.model.KeyMap;
import org.codedefenders.model.Player;
import org.codedefenders.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * This class handles the database logic for players.
 * A
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 * @see Player
 */
public class PlayerDAO {

    /**
     * Constructs a player from a {@link ResultSet} entry.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed player together with an {@link User} instance.
     * @see DB.RSMapper
     */
    static Player playerFromRS(ResultSet rs) throws SQLException {
        int id = rs.getInt("ID");
        int userId = rs.getInt("User_ID");
        int gameId = rs.getInt("Game_ID");
        int points = rs.getInt("Points");
        Role role = Role.valueOf(rs.getString("Role"));
        boolean active = rs.getBoolean("Active");

        return new Player(id, userId, gameId, points, role, active);
    }

    /**
     * Constructs a player from a {@link ResultSet} entry.
     * <p>
     * Requires the user information to have the following column names. Naming these columns should be done with SQL aliasing.
     *
     * <ul>
     *     <li>{@code usersPassword}</li>
     *     <li>{@code usersUsername}</li>
     *     <li>{@code usersEmail}</li>
     *     <li>{@code usersValidated}</li>
     *     <li>{@code usersActive}</li>
     *     <li>{@code usersAllowContact}</li>
     *     <li>{@code usersKeyMap}</li>
     * </ul>
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed player together with an {@link User} instance.
     * @see DB.RSMapper
     */
    static Player playerWithUserFromRS(ResultSet rs) throws SQLException {
        int id = rs.getInt("ID");
        int userId = rs.getInt("User_ID");
        int gameId = rs.getInt("Game_ID");
        int points = rs.getInt("Points");
        Role role = Role.valueOf(rs.getString("Role"));
        boolean active = rs.getBoolean("Active");

        String password = rs.getString("usersPassword");
        String userName = rs.getString("usersUsername");
        String email = rs.getString("usersEmail");
        boolean validated = rs.getBoolean("usersValidated");
        boolean userActive = rs.getBoolean("usersActive");
        boolean allowContact = rs.getBoolean("usersAllowContact");
        KeyMap keyMap = KeyMap.valueOrDefault(rs.getString("usersKeyMap"));

        final User user = new User(userId, userName, password, email, validated, userActive, allowContact, keyMap);

        return new Player(id, user, gameId, points, role, active);
    }

    /**
     * Retrieves the identifier of a player of a given user in a given game.
     *
     * @param userId the user identifier as an {@code int}.
     * @param gameId the game identifier as an {@code int}.
     * @return the playerId for a user in a game.
     */
    public static int getPlayerIdForUserAndGame(int userId, int gameId) {
        String query = String.join("\n",
                "SELECT players.ID",
                "FROM players",
                "WHERE User_ID = ?",
                "  AND Game_ID = ?");
        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(userId),
                DatabaseValue.of(gameId)
        };
        final Integer id = DB.executeQueryReturnValue(query, rs -> rs.getInt("ID"), values);
        return Optional.ofNullable(id).orElse(-1);
    }
}
