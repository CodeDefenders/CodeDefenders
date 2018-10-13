package org.codedefenders.database;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Role;
import org.codedefenders.game.duel.DuelGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.puzzle.PuzzleGame;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * This class handles the common database logic between games types.
 *
 * @see AbstractGame
 * @see DuelGame
 * @see MultiplayerGame
 * @see PuzzleGame
 */
public class GameDAO {

    /**
     * Adds a player with the given user ID and {@link Role} to the game.
     * If user is already a player in the game, the {@link Role} is updated.
     * @param gameId The game ID to add the player to.
     * @param userId The user ID.
     * @param role The role.
     * @return {@code true} if the player was successfully added, {@code false} otherwise.
     */
    public static boolean addPlayerToGame(int gameId, int userId, Role role) {
        String query = String.join("\n",
        "INSERT INTO players",
                "(Game_ID,User_ID, Points, Role)",
                "VALUES (?, ?, 0, ?)",
                "ON DUPLICATE KEY UPDATE Role = ?, Active = TRUE;"
        );

        DatabaseValue[] valueList = {
                DB.getDBV(gameId),
                DB.getDBV(userId),
                DB.getDBV(role.toString()),
                DB.getDBV(role.toString())
        };

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return DB.executeUpdate(stmt, conn);
    }
}
