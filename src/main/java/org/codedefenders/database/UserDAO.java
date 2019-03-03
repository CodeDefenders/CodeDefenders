package org.codedefenders.database;

import org.codedefenders.database.DB.RSMapper;
import org.codedefenders.game.Role;
import org.codedefenders.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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

        return new User(userId, userName, password, email, validated, active);
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
                "        AND games.Finish_Time > NOW()",
                "        AND players.Role IN ('ATTACKER', 'DEFENDER')",
                "        AND Active = TRUE",
                "    )",
                "ORDER BY Username, User_ID;");
        return DB.executeQueryReturnList(query, UserDAO::userFromRS);
    }

    /**
     * Returns the last rose the user with the given id had in any game.
     */
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

}
