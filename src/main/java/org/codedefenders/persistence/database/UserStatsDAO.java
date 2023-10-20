/*
 * Copyright (C) 2023 Code Defenders contributors
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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.dto.UserStats;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.GameType;
import org.codedefenders.game.Role;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.ResultSetUtils.oneFromRS;

/**
 * Provides methods to query user statistics from the database. This includes metrics like written tests and mutants,
 * points, games played.
 */
@Named(value = "userStats")
@ApplicationScoped
public class UserStatsDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserStatsDAO.class);

    private final QueryRunner queryRunner;

    @Inject
    private UserStatsDAO(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    /**
     * Fetches the amount of killed mutants a given user has written from the database.
     *
     * @param userId The id of the user
     * @return amount of killed mutants written by a user
     */
    public int getNumKilledMutantsByUser(int userId, GameType gameType) {
        return getNumMutantsByUser(userId, false, gameType);
    }

    /**
     * Fetches the amount of alive mutants a given user has written from the database.
     *
     * @param userId The id of the user
     * @return amount of alive mutants written by a user
     */
    public int getNumAliveMutantsByUser(int userId, GameType gameType) {
        return getNumMutantsByUser(userId, true, gameType);
    }

    private String getViewForGameType(GameType gameType) {
        switch (gameType) {
            case MULTIPLAYER:
                return "view_battleground_games";
            case MELEE:
                return "view_melee_games";
            default:
                throw new IllegalArgumentException("Unknown game type: " + gameType);
        }
    }

    private int getNumMutantsByUser(int userId, boolean alive, GameType gameType) {
        @Language("SQL") final String query = """
                SELECT count(Mutant_ID) AS mutants
                FROM view_valid_user_mutants mutants
                JOIN %s AS games
                ON mutants.Game_ID = games.ID
                WHERE User_ID = ?
                AND Alive = ?;
        """.formatted(
                getViewForGameType(gameType)
        );

        try {
            return queryRunner.query(
                    query,
                    resultSet -> oneFromRS(resultSet, rs -> rs.getInt("mutants")),
                    userId,
                    alive ? 1 : 0
            ).orElse(0);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Fetches the amount of tests written by a user that killed mutants.
     *
     * @param userId   The id of the user
     * @param gameType The game type
     * @return amount of killing tests written by a user
     */
    public int getNumKillingTestsByUser(int userId, GameType gameType) {
        return getNumTestsByUser(userId, gameType, true);
    }

    /**
     * Fetches the amount of tests written by a user that did not (yet) kill mutants.
     *
     * @param userId   The id of the user
     * @param gameType The game type
     * @return amount of non-killing tests written by a user
     */
    public int getNumNonKillingTestsByUser(int userId, GameType gameType) {
        return getNumTestsByUser(userId, gameType, false);
    }

    private int getNumTestsByUser(int userId, GameType gameType, boolean killingTest) {
        @Language("SQL") final String query = """
                SELECT count(Test_ID) AS tests
                FROM view_valid_user_tests tests
                JOIN %s AS games
                ON tests.Game_ID = games.ID
                WHERE User_ID = ?
                AND MutantsKilled %s 0;
        """.formatted(
                getViewForGameType(gameType),
                killingTest ? ">" : "="
        );

        try {
            return queryRunner.query(
                    query,
                    resultSet -> oneFromRS(resultSet, rs -> rs.getInt("tests")),
                    userId
            ).orElse(0);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Fetches the amount of points a given user received through a test on average.
     *
     * @param userId   The id of the user
     * @param gameType The game type
     * @return average points received through tests
     */
    public double getAveragePointsTestByUser(int userId, GameType gameType) {
        return getPointsTestByUser(userId, gameType, true);
    }

    /**
     * Fetches the amount of total points a given user received through all of their tests.
     *
     * @param userId   The id of the user
     * @param gameType The game type
     * @return total points received through tests
     */
    public int getTotalPointsTestsByUser(int userId, GameType gameType) {
        return (int) getPointsTestByUser(userId, gameType, false);
    }

    private double getPointsTestByUser(int userId, GameType gameType, boolean avg) {
        @Language("SQL") final String query = """
                SELECT %s(Points) AS points
                FROM view_valid_user_tests tests
                JOIN %s AS games
                ON tests.Game_ID = games.ID
                WHERE User_ID = ?;
        """.formatted(
                avg ? "avg" : "sum",
                getViewForGameType(gameType)
        );

        try {
            return queryRunner.query(
                    query,
                    resultSet -> oneFromRS(resultSet, rs -> rs.getDouble("points")),
                    userId
            ).orElse(0d);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Fetches the amount of points a given user received through a mutant on average.
     *
     * @param userId   The id of the user
     * @param gameType The game type
     * @return average points received through mutants
     */
    public double getAveragePointsMutantByUser(int userId, GameType gameType) {
        return getPointsMutantByUser(userId, gameType, true);
    }

    /**
     * Fetches the amount of total points a given user received through all of their mutants.
     *
     * @param userId   The id of the user
     * @param gameType The game type
     * @return total points received through mutants
     */
    public int getTotalPointsMutantByUser(int userId, GameType gameType) {
        return (int) getPointsMutantByUser(userId, gameType, false);
    }

    private double getPointsMutantByUser(int userId, GameType gameType, boolean avg) {
        @Language("SQL") final String query = """
                SELECT %s(Points) AS points
                FROM view_valid_user_mutants mutants
                JOIN %s AS games
                ON mutants.Game_ID = games.ID
                WHERE User_ID = ?;
        """.formatted(
                avg ? "avg" : "sum",
                getViewForGameType(gameType)
        );

        try {
            return queryRunner.query(
                    query,
                    resultSet -> oneFromRS(resultSet, rs -> rs.getDouble("points")),
                    userId
            ).orElse(0d);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Fetches the amount of total games a user played as attacker.
     *
     * @param userId   The id of the user
     * @param gameType The game type
     * @return games played as attacker
     */
    public int getAttackerGamesByUser(int userId, GameType gameType) {
        return getGamesOfRoleByUser(userId, gameType, Role.ATTACKER);
    }

    /**
     * Fetches the amount of total games a user played as defender.
     *
     * @param userId   The id of the user
     * @param gameType The game type
     * @return games played as defender
     */
    public int getDefenderGamesByUser(int userId, GameType gameType) {
        return getGamesOfRoleByUser(userId, gameType, Role.DEFENDER);
    }

    private int getGamesOfRoleByUser(int userId, GameType gameType, Role role) {
        if (gameType == GameType.MELEE && (role == Role.DEFENDER || role == Role.ATTACKER)) {
            logger.warn("Role {} is not used for game type {}", role, gameType);
            return 0;
        }

        @Language("SQL") final String query = """
                SELECT count(view_players.Game_ID) AS games
                FROM view_players
                JOIN %s AS g
                ON view_players.Game_ID = g.ID
                WHERE User_ID = ?
                AND Role = ?
        """.formatted(
                getViewForGameType(gameType)
        );

        try {
            return queryRunner.query(
                    query,
                    resultSet -> oneFromRS(resultSet, rs -> rs.getInt("games")),
                    userId,
                    role.toString()
            ).orElse(0);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public UserStats.PuzzleStats getPuzzleStatsByUser(int userId) {
        @Language("SQL") final String query = """
                SELECT puzzle_chapters.Position AS chapter, MAX(puzzles.Position) AS puzzle
                FROM puzzles, puzzle_chapters, games
                WHERE puzzles.Chapter_ID = puzzle_chapters.Chapter_ID
                  AND games.Puzzle_ID = puzzles.Puzzle_ID
                  AND games.Mode = ?
                  AND games.State = ?
                  AND games.Creator_ID = ?
                GROUP BY puzzle_chapters.Position
        """;

        try {
            return queryRunner.query(
                    query,
                    resultSet -> {
                        final UserStats.PuzzleStats stats = new UserStats.PuzzleStats();
                        while (resultSet.next()) {
                            stats.addPuzzle(
                                    resultSet.getInt("chapter"),
                                    resultSet.getInt("puzzle")
                            );
                        }
                        return stats;
                    },
                    GameMode.PUZZLE.toString(), GameState.SOLVED.toString(), userId
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public int getTotalGamesByUser(int userId, GameType gameType) {
        @Language("SQL") final String query = """
                SELECT count(view_players.Game_ID) AS games
                FROM view_players
                JOIN %s AS g
                ON view_players.Game_ID = g.ID
                WHERE User_ID = ?
        """.formatted(
            getViewForGameType(gameType)
        );

        try {
            return queryRunner.query(
                    query,
                    resultSet -> oneFromRS(resultSet, rs -> rs.getInt("games")),
                    userId
            ).orElse(0);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }
}
