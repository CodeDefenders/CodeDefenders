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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.codedefenders.game.Role;
import org.codedefenders.model.KeyMap;
import org.codedefenders.model.Player;
import org.codedefenders.model.UserEntity;
import org.intellij.lang.annotations.Language;

/**
 * This class handles the database logic for players.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 * @see Player
 */
public class PlayerDAO {

    /**
     * Constructs a player from a {@link ResultSet} entry.
     *
     * <p>Requires the user information to have the following column names. Naming
     * these columns should be done with SQL aliasing.
     *
     * <ul>
     * <li>{@code usersPassword}</li>
     * <li>{@code usersUsername}</li>
     * <li>{@code usersEmail}</li>
     * <li>{@code usersValidated}</li>
     * <li>{@code usersActive}</li>
     * <li>{@code usersAllowContact}</li>
     * <li>{@code usersKeyMap}</li>
     * </ul>
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed player together with an {@link UserEntity} instance.
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

        final UserEntity user = new UserEntity(userId, userName, password, email, validated, userActive, allowContact, keyMap);

        return new Player(id, user, gameId, points, role, active);
    }

    /**
     * Retrieves the identifier of a player of a given user in a given game.
     * TODO: Return Integer instead of int, and null instead of -1?
     *
     * @param userId the user identifier as an {@code int}.
     * @param gameId the game identifier as an {@code int}.
     * @return the playerId for a user in a game.
     */
    public static int getPlayerIdForUserAndGame(int userId, int gameId) {
        @Language("SQL") String query = """
                SELECT players.ID
                FROM players
                WHERE User_ID = ?
                  AND Game_ID = ?
        """;
        DatabaseValue<?>[] values = new DatabaseValue[] { DatabaseValue.of(userId), DatabaseValue.of(gameId) };
        final Integer id = DB.executeQueryReturnValue(query, rs -> rs.getInt("ID"), values);
        return Optional.ofNullable(id).orElse(-1);
    }

    /**
     * Retrieves a player given its id.
     *
     * <p>TODO What happens if the player does not exist?
     *
     * @param playerId the player identifier as an {@code int}.
     * @return player instance
     */
    public static Player getPlayer(int playerId) {
        @Language("SQL") String query = """
                SELECT *
                FROM view_players_with_userdata
                WHERE ID = ?;
        """;

        return DB.executeQueryReturnValue(query, PlayerDAO::playerWithUserFromRS, DatabaseValue.of(playerId));

    }

    /**
     * Retrieves the player of a given user in a given game.
     *
     * @param userId The user identifier as an {@code int}.
     * @param gameId The game identifier as an {@code int}.
     * @return The player for a user in a game.
     */
    public static Player getPlayerForUserAndGame(int userId, int gameId) {
        @Language("SQL") String query = """
                SELECT *
                FROM view_players_with_userdata
                WHERE Game_ID = ?
                  AND User_ID = ?
                  AND Active = TRUE;
        """;
        return DB.executeQueryReturnValue(query, PlayerDAO::playerWithUserFromRS, DatabaseValue.of(gameId),
                DatabaseValue.of(userId));
    }

    public static void setPlayerPoints(int points, int player) {
        @Language("SQL") String query = "UPDATE players SET Points = ? WHERE ID = ?";
        DatabaseValue<?>[] values = new DatabaseValue[]{
                DatabaseValue.of(points),
                DatabaseValue.of(player)
        };
        DB.executeUpdateQuery(query, values);
    }

    public static void increasePlayerPoints(int points, int player) {
        @Language("SQL") String query = "UPDATE players SET Points = Points + ? WHERE ID = ?";
        DatabaseValue<?>[] values = new DatabaseValue[]{
                DatabaseValue.of(points),
                DatabaseValue.of(player)
        };
        DB.executeUpdateQuery(query, values);
    }

    public static int getPlayerPoints(int playerId) {
        @Language("SQL") String query = "SELECT Points FROM players WHERE ID = ?;";
        final Integer points = DB.executeQueryReturnValue(query,
                rs -> rs.getInt("Points"), DatabaseValue.of(playerId));
        return Optional.ofNullable(points).orElse(0);
    }
}
