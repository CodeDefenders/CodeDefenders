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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.model.Player;
import org.intellij.lang.annotations.Language;

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
        GameMode gameMode = getGameMode(gameId);
        if (gameMode == null) {
            return null;
        }

        switch (gameMode) {
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
     * Retrieves a game for which we don't know the type yet, from the playerID.
     *
     * @param playerId The id of the player for who we want to query a game.
     * @return The {@link AbstractGame} with the given ID or null if no game found.
     */
    public static AbstractGame getGameWherePlayerPlays(int playerId) {
        // TODO This can be improved
        AbstractGame game = MultiplayerGameDAO.getGameWherePlayerPlays(playerId);
        if (game != null) {
            return game;
        }
        game = MeleeGameDAO.getGameWherePlayerPlays(playerId);
        if (game != null) {
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
        @Language("SQL") String query = """
                INSERT INTO players (Game_ID,User_ID, Points, Role)
                VALUES (?, ?, 0, ?)
                ON DUPLICATE KEY UPDATE Role = ?, Active = TRUE;
        """;

        DatabaseValue<?>[] values = {
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
        @Language("SQL") String query = """
                SELECT *
                FROM view_players_with_userdata
                WHERE Game_ID = ?
                  AND Role = ?
                  AND Active=TRUE;
        """;
        DatabaseValue<?>[] values = new DatabaseValue[]{
                DatabaseValue.of(gameId),
                DatabaseValue.of(role.toString())
        };

        return DB.executeQueryReturnList(query, PlayerDAO::playerWithUserFromRS, values);
    }

    /**
     * Retrieves all {@link Player Players}, that belong to valid users, for a given game id.
     *
     * @param gameId The id of the game the players are retrieved for.
     * @return A list of {@link Player Players}, that belong to valid users. Can be empty but never {@code null}.
     */
    public static List<Player> getValidPlayersForGame(int gameId) {
        @Language("SQL") String query = """
                SELECT *
                FROM view_players_with_userdata
                WHERE Game_ID = ?
                  AND Active = TRUE;
        """;

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
        @Language("SQL") String query = """
                UPDATE players
                SET Active = FALSE
                WHERE Game_ID = ?
                  AND User_ID = ?;
        """;
        DatabaseValue<?>[] values = new DatabaseValue[]{
                DatabaseValue.of(gameId),
                DatabaseValue.of(userId)
        };

        return DB.executeUpdateQuery(query, values);
    }

    public static Integer getCurrentRound(int gameId) {
        @Language("SQL") String query = """
                SELECT CurrentRound
                FROM games
                WHERE games.ID = ?;
        """;
        return DB.executeQueryReturnValue(query, rs -> rs.getInt("CurrentRound"), DatabaseValue.of(gameId));
    }

    /**
     * Checks for which of the given IDs games exist in the database.
     *
     * @param ids The game IDs to check.
     * @return The given game IDs for which games exist.
     */
    public static List<Integer> filterExistingGameIDs(Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        String idsString = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        @Language("SQL") String query = "SELECT ID FROM games WHERE ID in (" + idsString + ")";
        return DB.executeQueryReturnList(query, rs -> rs.getInt("ID"));
    }


    /**
     * Returns a game's mode for given game identifier.
     *
     * @param gameId the identifier of the game.
     * @return the game mode of the queried game.
     */
    public static GameMode getGameMode(int gameId) {
        @Language("SQL") String query = "SELECT Mode FROM games WHERE ID = ?";
        return DB.executeQueryReturnValue(query, rs -> GameMode.valueOf(rs.getString("Mode")),
                DatabaseValue.of(gameId));
    }

    /**
     * Fetches a List of Multiplayer- and Melee-Games that have expired.
     *
     * @return the expired games.
     */
    public static List<AbstractGame> getExpiredGames() {
        List<AbstractGame> games = new ArrayList<>();
        games.addAll(MultiplayerGameDAO.getExpiredGames());
        games.addAll(MeleeGameDAO.getExpiredGames());
        return games;
    }

    /**
     * Checks if a game is expired.
     *
     * @param gameId the game to check.
     * @return {@code true} if the game is active but expired.
     */
    public static boolean isGameExpired(int gameId) {
        // do not use TIMESTAMPADD here to avoid errors with daylight saving
        @Language("SQL") final String sql = """
                SELECT FROM_UNIXTIME(UNIX_TIMESTAMP(Start_Time) + Game_Duration_Minutes * 60) <= NOW() AS isExpired
                FROM games
                WHERE ID = ?;
        """;
        return DB.executeQueryReturnValue(sql,
                l -> l.getBoolean("isExpired"),
                DatabaseValue.of(gameId)
        );
    }

    public static Role getRole(int userId, int gameId) {
        @Language("SQL") String query = """
                SELECT *
                FROM games AS m
                LEFT JOIN players AS p
                  ON p.Game_ID = m.ID
                  AND p.Active=TRUE
                WHERE m.ID = ?
                  AND (p.User_ID=?
                      AND p.Game_ID=?)
        """;
        DatabaseValue<?>[] values = new DatabaseValue[]{
                DatabaseValue.of(gameId),
                DatabaseValue.of(userId),
                DatabaseValue.of(gameId)};

        DB.RSMapper<Role> mapper = rs -> Role.valueOrNull(rs.getString("Role"));

        final Role role = DB.executeQueryReturnValue(query, mapper, values);

        if (role == null) {
            AbstractGame game = getGame(gameId);
            if (game != null && game.getCreatorId() == userId) {
                return Role.OBSERVER;
            }
        }

        return Optional.ofNullable(role).orElse(Role.NONE);
    }

    public static boolean storeStartTime(int gameId) {
        @Language("SQL") String query = """
                UPDATE games
                SET Start_Time = NOW()
                WHERE ID = ?
        """;
        DatabaseValue<?>[] values = {DatabaseValue.of(gameId)};
        return DB.executeUpdateQuery(query, values);
    }
}
