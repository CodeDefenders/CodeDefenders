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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codedefenders.database.DB.RSMapper;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Role;
import org.codedefenders.model.KeyMap;
import org.codedefenders.model.User;

/**
 * This class handles the database logic for mutants.
 * @see User
 */
public class UserDAO {

    /**
     * Constructs a user from a {@link ResultSet} entry.
     * @param rs The {@link ResultSet}.
     * @return The constructed user.
     * @see RSMapper
     */
    static User userFromRS(ResultSet rs) throws SQLException {
        int userId = rs.getInt("User_ID");
        String password = rs.getString("Password");
        String userName = rs.getString("Username");
        String email = rs.getString("Email");
        boolean validated = rs.getBoolean("Validated");
        boolean active = rs.getBoolean("Active");
        boolean allowContact = rs.getBoolean("AllowContact");
        KeyMap keyMap = KeyMap.valueOrDefault(rs.getString("KeyMap"));

        return new User(userId, userName, password, email, validated, active, allowContact, keyMap);
    }

    /**
     * Returns the user for the given user id.
     */
    public static User getUserById(int userId) throws UncheckedSQLException, SQLMappingException {
        String query = "SELECT * FROM users WHERE User_ID = ?;";
        return DB.executeQueryReturnValue(query, UserDAO::userFromRS, DatabaseValue.of(userId));
    }

    /**
     * Returns the user with the given name.
     */
    public static User getUserByName(String name) throws UncheckedSQLException, SQLMappingException {
        String query = "SELECT * FROM users WHERE Username=?;";
        return DB.executeQueryReturnValue(query, UserDAO::userFromRS, DatabaseValue.of(name));
    }

    /**
     * Returns the user with the given email.
     */
    public static User getUserByEmail(String email) throws UncheckedSQLException, SQLMappingException {
        String query = "SELECT * FROM users WHERE Email = ?;";
        return DB.executeQueryReturnValue(query, UserDAO::userFromRS, DatabaseValue.of(email));
    }

    public static User getUserForPlayer(int playerId) throws UncheckedSQLException, SQLMappingException {
        String query = String.join("\n",
                "SELECT users.*",
                "FROM users, players",
                "WHERE players.User_ID = users.User_ID",
                "  AND players.ID = ?;");
        return DB.executeQueryReturnValue(query, UserDAO::userFromRS, DatabaseValue.of(playerId));
    }

    /**
     * Returns a list of all users (including dummy / system users).
     */
    public static List<User> getUsers() throws UncheckedSQLException, SQLMappingException {
        String query = "SELECT * FROM users";
        return DB.executeQueryReturnList(query, UserDAO::userFromRS);
    }

    /**
     * Returns a list of real users (not including dummy / system users), which are not taking part in a currently
     * active game.
     */
    public static List<User> getUnassignedUsers() throws UncheckedSQLException, SQLMappingException {
        String query = String.join("\n",
                "SELECT DISTINCT u.*",
                "FROM view_valid_users u",
                "WHERE u.User_ID NOT IN",
                "    (",
                "      SELECT DISTINCT players.User_ID",
                "      FROM players, games",
                "      WHERE players.Game_ID = games.ID",
                "        AND (games.State = 'ACTIVE' OR games.State = 'CREATED')",
                "        AND players.Role IN ('ATTACKER', 'DEFENDER')",
                "        AND Active = TRUE",
                "    )",
                "ORDER BY Username, User_ID;");
        return DB.executeQueryReturnList(query, UserDAO::userFromRS);
    }

    /**
     * Returns the last rose the user with the given id had in any game or null if there's no data.
     */
    // TODO Consider to return Role.NONE instead of null ? What's happens if rs.getString("Role") == null ?!
    public static Role getLastRoleOfUser(int userId) throws UncheckedSQLException, SQLMappingException {
        String query = String.join("\n",
                "SELECT players.Role",
                "FROM players",
                "WHERE players.User_ID = ?",
                "  AND players.Game_ID = (",
                "    SELECT MAX(innerPlayers.Game_ID)",
                "    FROM players innerPlayers",
                "    WHERE innerPlayers.User_ID = players.User_ID",
                "  );");
        return DB.executeQueryReturnValue(query, rs -> Role.valueOf(rs.getString("Role")), DatabaseValue.of(userId));
    }

    /**
     * @return a mapping from game identifiers to the name of the game's creators.
     */
    public static Map<Integer, String> getGamesCreatorNames(final List<? extends AbstractGame> games) {
        if (games.isEmpty()) {
            return new HashMap<>();
        }

        final StringBuilder bob = new StringBuilder("(");
        for (int i = 0; i < games.size() - 1; i++) {
            bob.append("?,");
        }
        bob.append("?)");
        final String range = bob.toString();

        String query = String.join("\n",
                "SELECT games.ID, users.Username",
                "FROM users,games",
                "WHERE games.ID in " + range,
                "AND games.Creator_ID = users.User_ID",
                ";");
        DatabaseValue[] values = games
                .stream()
                .map(AbstractGame::getId)
                .map(DatabaseValue::of)
                .toArray(DatabaseValue[]::new);

        class GameCreator {
            int gameId;

            GameCreator gameId(int gameId) {
                this.gameId = gameId;
                return this;
            }

            String name;

            GameCreator name(String name) {
                this.name = name;
                return this;
            }
        }

        final RSMapper<GameCreator> mapper = (rs) -> new GameCreator()
                .gameId(rs.getInt("games.ID"))
                .name(rs.getString("users.Username"));

        final List<GameCreator> content = DB.executeQueryReturnList(query, mapper, values);

        return content.stream().collect(Collectors.toMap(data -> data.gameId, data -> data.name));
    }
}
