/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Role;
import org.codedefenders.game.duel.DuelGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.model.Player;

import java.util.List;

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
     *
     * @param gameId The game ID to add the player to.
     * @param userId The user ID.
     * @param role   The role.
     * @return {@code true} if the player was successfully added, {@code false} otherwise.
     */
    public static boolean addPlayerToGame(int gameId, int userId, Role role) {
        String query = String.join("\n",
                "INSERT INTO players (Game_ID,User_ID, Points, Role)",
                "VALUES (?, ?, 0, ?)",
                "ON DUPLICATE KEY UPDATE Role = ?, Active = TRUE;"
        );

        DatabaseValue[] values = {
                DB.getDBV(gameId),
                DB.getDBV(userId),
                DB.getDBV(role.toString()),
                DB.getDBV(role.toString())
        };

        return DB.executeUpdateQuery(query, values);
    }

    /**
     * Retrieves all players identifiers for a given game identifier and {@link Role}.
     *
     * @param gameId the game the players are retrieved for as an {@code int}.
     * @param role the queried player role.
     * @return a list of player identifiers as {@link Integer Integers}, can be empty but never {@code null}.
     */
    public static List<Integer> getPlayersForGame(int gameId, Role role) {
        String query = String.join("\n",
                "SELECT ID",
                "FROM players",
                "WHERE Game_ID = ?",
                "  AND Role = ?",
                "  AND Active=TRUE;");
        DatabaseValue[] values = new DatabaseValue[]{
                DB.getDBV(gameId),
                DB.getDBV(role.toString())
        };

        return DB.executeQueryReturnList(query, rs -> rs.getInt("ID"), values);
    }

    /**
     * Retrieves all {@link Player Players} for a given game identifier.
     *
     * @param gameId the game the players are retrieved for as an {@code int}.
     * @return a list of {@link Player Players}, can be empty but never {@code null}.
     */
    public static List<Player> getAllPlayersForGame(int gameId) {
        String query = String.join("\n",
                "SELECT players.*,",
                "  users.Password AS usersPassword,",
                "  users.Username AS usersUsername,",
                "  users.Email AS usersEmail,",
                "  users.Validated AS usersValidated,",
                "  users.Active AS usersActive",
                "FROM players, users",
                "WHERE players.Game_ID = ?",
                "  AND players.User_ID = users.User_ID",
                "  AND players.Active = TRUE;");

        return DB.executeQueryReturnList(query, PlayerDAO::playerWithUserFromRS, DB.getDBV(gameId));
    }

    /**
     * Removes given user from a given game.
     *
     * @param gameId the game the given user is removed from.
     * @param userId the user that is removed.
     * @return whether removing was successful or not.
     */
    public static boolean removeUserFromGame(int gameId, int userId) {
        String query = String.join("\n",
                "UPDATE players",
                "SET Active = FALSE",
                "WHERE Game_ID = ?",
                "  AND User_ID = ?;");
        DatabaseValue[] values = new DatabaseValue[]{
                DB.getDBV(gameId),
                DB.getDBV(userId)
        };

        return DB.executeUpdateQuery(query, values);
    }
}
