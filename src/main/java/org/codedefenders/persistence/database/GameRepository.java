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
package org.codedefenders.persistence.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.model.Player;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.ResultSetUtils.listFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.nextFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.oneFromRS;

/**
 * This class handles the common database logic between games types.
 *
 * @see AbstractGame
 * @see MultiplayerGame
 * @see PuzzleGame
 */
@ApplicationScoped
public class GameRepository {
    private static final Logger logger = LoggerFactory.getLogger(GameRepository.class);

    private final QueryRunner queryRunner;
    private final MeleeGameRepository meleeGameRepo;
    private final MultiplayerGameRepository multiplayerGameRepo;
    private final PuzzleRepository puzzleRepo;

    @Inject
    public GameRepository(QueryRunner queryRunner, MeleeGameRepository meleeGameRepo,
                          MultiplayerGameRepository multiplayerGameRepo, PuzzleRepository puzzleRepo) {
        this.queryRunner = queryRunner;
        this.meleeGameRepo = meleeGameRepo;
        this.multiplayerGameRepo = multiplayerGameRepo;
        this.puzzleRepo = puzzleRepo;
    }

    /**
     * Retrieves a game for which we don't know the type yet.
     *
     * @param gameId The game ID.
     * @return The {@link AbstractGame} with the given ID or null if no game found.
     */
    public AbstractGame getGame(int gameId) {
        GameMode gameMode = getGameMode(gameId);
        if (gameMode == null) {
            return null;
        }

        return switch (gameMode) {
            case PARTY -> multiplayerGameRepo.getMultiplayerGame(gameId);
            case MELEE -> meleeGameRepo.getMeleeGame(gameId);
            case PUZZLE -> puzzleRepo.getPuzzleGameForId(gameId);
            default -> null;
        };
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
    public boolean addPlayerToGame(int gameId, int userId, Role role) {
        @Language("SQL") String query = """
                INSERT INTO players (Game_ID, User_ID, Points, Role)
                VALUES (?, ?, 0, ?)
                ON DUPLICATE KEY UPDATE Role = ?, Active = TRUE;
        """;

        try {
            var key = queryRunner.insert(query, nextFromRS(rs -> rs.getInt(1)),
                    gameId,
                    userId,
                    role.toString(),
                    role.toString()
            );
            key.ifPresent(System.out::println);
            // TODO: This will return false if a player with the same values already exists
            return key.isPresent();
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Retrieves all players identifiers for a given game ID and {@link Role}.
     *
     * @param gameId The game ID.
     * @param role The role to get players for.
     * @return The list of players.
     */
    public List<Player> getPlayersForGame(int gameId, Role role) {
        @Language("SQL") String query = """
                SELECT *
                FROM view_players_with_userdata
                WHERE Game_ID = ?
                  AND Role = ?
                  AND Active = TRUE;
        """;

        try {
            return queryRunner.query(query, listFromRS(PlayerRepository::playerWithUserFromRS),
                    gameId,
                    role.toString());
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Retrieves all {@link Player Players}, that belong to valid users, for a given game id.
     *
     * @param gameId The id of the game the players are retrieved for.
     * @return The list of players.
     */
    public List<Player> getValidPlayersForGame(int gameId) {
        @Language("SQL") String query = """
                SELECT *
                FROM view_players_with_userdata
                WHERE Game_ID = ?
                  AND Active = TRUE;
        """;

        try {
            return queryRunner.query(query, listFromRS(PlayerRepository::playerWithUserFromRS), gameId);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Removes given user from a given game.
     *
     * @param gameId The game ID.
     * @param userId The user ID that is removed.
     * @return whether removing was successful or not.
     */
    public boolean removeUserFromGame(int gameId, int userId) {
        @Language("SQL") String query = """
                UPDATE players
                SET Active = FALSE
                WHERE Game_ID = ?
                  AND User_ID = ?;
        """;

        try {
            int updatedRows = queryRunner.update(query, gameId, userId);
            return updatedRows > 0;
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public int getCurrentRound(int gameId) {
        @Language("SQL") String query = """
                SELECT CurrentRound
                FROM games
                WHERE games.ID = ?;
        """;

        try {
            var currentRound = queryRunner.query(query,
                    oneFromRS(rs -> rs.getInt("CurrentRound")),
                    gameId);
            return currentRound.orElseThrow();
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Checks for which of the given IDs games exist in the database.
     *
     * @param ids The game IDs to check.
     * @return The game IDs for which games exist.
     */
    public List<Integer> filterExistingGameIDs(Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        String range = Stream.generate(() -> "?")
                .limit(ids.size())
                .collect(Collectors.joining(","));

        @Language("SQL") String query = "SELECT ID FROM games WHERE ID in (%s)"
                .formatted(range);

        try {
            return queryRunner.query(query,
                    listFromRS(rs -> rs.getInt("ID")),
                    ids.toArray());
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }


    /**
     * Returns the game mode for given game ID.
     *
     * @param gameId The game ID.
     * @return The game mode.
     */
    public GameMode getGameMode(int gameId) {
        @Language("SQL") String query = "SELECT Mode FROM games WHERE ID = ?";

        try {
            var gameMode = queryRunner.query(query,
                    oneFromRS(rs -> GameMode.valueOf(rs.getString("Mode"))),
                    gameId);
            return gameMode.orElseThrow();
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Fetches a List of Multiplayer- and Melee-Games that have expired.
     *
     * @return the expired games.
     */
    public List<AbstractGame> getExpiredGames() {
        List<AbstractGame> games = new ArrayList<>();
        games.addAll(multiplayerGameRepo.getExpiredGames());
        games.addAll(meleeGameRepo.getExpiredGames());
        return games;
    }

    /**
     * Checks if a game is expired.
     *
     * @param gameId the game to check.
     * @return {@code true} if the game is active but expired.
     */
    public boolean isGameExpired(int gameId) {
        // do not use TIMESTAMPADD here to avoid errors with daylight saving
        @Language("SQL") final String query = """
                SELECT FROM_UNIXTIME(UNIX_TIMESTAMP(Start_Time) + Game_Duration_Minutes * 60) <= NOW() AS isExpired
                FROM games
                WHERE ID = ?;
        """;
        try {
            var isExpired = queryRunner.query(query,
                    oneFromRS(l -> l.getBoolean("isExpired")),
                    gameId
            );
            return isExpired.orElseThrow();
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public boolean storeStartTime(int gameId) {
        @Language("SQL") String query = """
                UPDATE games
                SET Start_Time = NOW()
                WHERE ID = ?
        """;

        try {
            int updatedRows = queryRunner.update(query, gameId);
            return updatedRows > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
