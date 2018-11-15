package org.codedefenders.database;

import org.codedefenders.game.Role;
import org.codedefenders.model.User;
import org.codedefenders.database.DB.RSMapper;
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
    public static User userFromRS(ResultSet rs) throws SQLException {
        int userId = rs.getInt("User_ID");
        String password = rs.getString("Password");
        String userName = rs.getString("Username");
        String email = rs.getString("Email");
        boolean validated = rs.getBoolean("Validated");
        boolean active = rs.getBoolean("Active");

        return new User(userId, userName, password, email, validated, active);
    }

    public static User getUserById(int userId) {
        String query = "SELECT * FROM users WHERE User_ID = ?;";
        return DB.executeQueryReturnValue(query, UserDAO::userFromRS, DB.getDBV(userId));
    }

    public static User getUserByName(String name) {
        String query = "SELECT * FROM users WHERE Username=?;";
        return DB.executeQueryReturnValue(query, UserDAO::userFromRS, DB.getDBV(name));
    }

    public static User getUserByEmail(String email) {
        String query = "SELECT * FROM users WHERE Email = ?;";
        return DB.executeQueryReturnValue(query, UserDAO::userFromRS, DB.getDBV(email));
    }

    public static User getUserForPlayer(int playerId) {
        String query = String.join("\n",
                "SELECT users.*",
                "FROM users, players",
                "WHERE players.User_ID = users.User_ID",
                "  AND players.ID = ?;");
        return DB.executeQueryReturnValue(query, UserDAO::userFromRS, DB.getDBV(playerId));
    }

    public static List<User> getUsers() {
        String query = "SELECT * FROM users";
        return DB.executeQueryReturnList(query, UserDAO::userFromRS);
    }

    public static List<User> getUnassignedUsers() {
        String query = String.join("\n",
                "SELECT DISTINCT users.*",
                "FROM users",
                "WHERE users.User_ID > 4",
                "  AND users.User_ID NOT IN",
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

    public static Role getLastRoleOfUser(int userId) {
        String query = String.join("\n",
                "SELECT players.Role",
                "FROM players",
                "WHERE players.User_ID = ?",
                "  AND players.Game_ID = (",
                "    SELECT MAX(innerPlayers.Game_ID)",
                "    FROM players innerPlayers",
                "    WHERE innerPlayers.User_ID = players.User_ID",
                "  );");
        return DB.executeQueryReturnValue(query, rs -> Role.valueOf(rs.getString("Role")), DB.getDBV(userId));
    }

}
