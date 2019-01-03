package org.codedefenders.database;

import org.codedefenders.game.Role;
import org.codedefenders.model.Player;
import org.codedefenders.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;

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
        final User user = new User(userId, userName, password, email, validated, userActive);

        return new Player(id, user, gameId, points, role, active);
    }
}
