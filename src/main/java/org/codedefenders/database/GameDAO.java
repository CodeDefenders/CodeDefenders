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

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.model.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class handles the common database logic between games types.
 *
 * @see AbstractGame
 * @see MultiplayerGame
 * @see PuzzleGame
 */
public class GameDAO {

    /**
     * Retrieves a game for which we don't know the type yet.
     *
     * @param gameId The game ID we want to query a game.
     * @return The {@link AbstractGame} with the given ID or null if no game found.
     */
    public static AbstractGame getGame(int gameId) {
        switch (getGameMode(gameId)) {
            case PARTY:
                return MultiplayerGameDAO.getMultiplayerGame(gameId);
            case MELEE:
                return MeleeGameDAO.getMeleeGame(gameId);
            case PUZZLE:
                return PuzzleDAO.getPuzzleGameForId(gameId);
            default:
                return null;
        }
    }

    /**
     * Retrieves a game for which we don't know the type yet, from the playerID
     *
     * @param gameId The game ID we want to query a game.
     * @return The {@link AbstractGame} with the given ID or null if no game found.
     */
    public static AbstractGame getGameWherePlayerPlays(int playerId) {
        // TODO This can be improved
        AbstractGame game = MultiplayerGameDAO.getGameWherePlayerPlays(playerId);
        if( game != null ) {
            return game;
        } 
        game = MeleeGameDAO.getGameWherePlayerPlays(playerId);
        if( game != null ) {
            return game;
        }
        // Not sure we need to check for PUzzle games
        return null;
        
    }

    
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
                DatabaseValue.of(gameId),
                DatabaseValue.of(userId),
                DatabaseValue.of(role.toString()),
                DatabaseValue.of(role.toString())
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
    public static List<Player> getPlayersForGame(int gameId, Role role) {
        String query = String.join("\n",
                "SELECT *",
                "FROM view_players_with_userdata",
                "WHERE Game_ID = ?",
                "  AND Role = ?",
                "  AND Active=TRUE;");
        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(gameId),
                DatabaseValue.of(role.toString())
        };

        return DB.executeQueryReturnList(query, PlayerDAO::playerWithUserFromRS, values);
    }

    /**
     * Retrieves all {@link Player Players} for a given game identifier.
     *
     * @param gameId the game the players are retrieved for as an {@code int}.
     * @return a list of {@link Player Players}, can be empty but never {@code null}.
     */
    public static List<Player> getAllPlayersForGame(int gameId) {
        String query = String.join("\n",
                "SELECT *",
                "FROM view_players_with_userdata",
                "WHERE Game_ID = ?",
                "  AND Active = TRUE;");

        return DB.executeQueryReturnList(query, PlayerDAO::playerWithUserFromRS, DatabaseValue.of(gameId));
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
                DatabaseValue.of(gameId),
                DatabaseValue.of(userId)
        };

        return DB.executeUpdateQuery(query, values);
    }

    public static Integer getCurrentRound(int gameId) {
        String query = String.join("\n",
                "SELECT CurrentRound",
                "FROM games",
                "WHERE games.ID = ?;");
        return DB.executeQueryReturnValue(query, rs -> rs.getInt("CurrentRound"), DatabaseValue.of(gameId));
    }

    /**
     * Checks for which of the given IDs games exist in the database.
     *
     * @param ids The game IDs to check.
     * @return The given game IDs for which games exist.
     */
    public static List<Integer> filterExistingGameIDs(List<Integer> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        String idsString = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        String query = "SELECT ID FROM games WHERE ID in (" + idsString + ")";
        return DB.executeQueryReturnList(query, rs -> rs.getInt("ID"));
    }

     
    /**
     * Returns a game's mode for given game identifier.
     *
     * @param gameId the identifier of the game.
     * @return the game mode of the queried game.
     */
    public static GameMode getGameMode(int gameId) {
        String query = "SELECT Mode FROM games WHERE ID = ?";
        return DB.executeQueryReturnValue(query, rs -> GameMode.valueOf(rs.getString("Mode")),
                DatabaseValue.of(gameId));
    }
}
