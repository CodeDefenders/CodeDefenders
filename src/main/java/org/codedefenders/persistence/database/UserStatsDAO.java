package org.codedefenders.persistence.database;


import java.sql.SQLException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.game.GameType;
import org.codedefenders.game.Role;
import org.codedefenders.persistence.database.util.QueryRunner;
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
        final String query = String.join("\n",
                "SELECT count(Mutant_ID) AS mutants",
                "FROM view_valid_mutants",
                "JOIN " + getViewForGameType(gameType) + " AS g",
                "ON view_valid_mutants.Game_ID = g.ID",
                "WHERE User_ID = ?",
                "AND Alive = ?;"
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
        final String query = String.join("\n",
                "SELECT count(Test_ID) AS tests",
                "FROM view_valid_tests",
                "JOIN " + getViewForGameType(gameType) + " AS g",
                "ON view_valid_tests.Game_ID = g.ID",
                "WHERE User_ID = ?",
                "AND MutantsKilled " + (killingTest ? ">" : "=") + " 0;"
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
        final String acc = avg ? "avg" : "sum";
        final String query = String.join("\n",
                "SELECT " + acc + "(Points) AS points",
                "FROM view_valid_tests",
                "JOIN " + getViewForGameType(gameType) + " AS g",
                "ON view_valid_tests.Game_ID = g.ID",
                "WHERE User_ID = ?;"
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
        final String acc = avg ? "avg" : "sum";
        final String query = String.join("\n",
                "SELECT " + acc + "(Points) AS points",
                "FROM view_valid_mutants",
                "JOIN " + getViewForGameType(gameType) + " AS g",
                "ON view_valid_mutants.Game_ID = g.ID",
                "WHERE User_ID = ?;"
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

        final String query = String.join("\n",
                "SELECT count(view_players.Game_ID) AS games",
                "FROM view_players",
                "JOIN " + getViewForGameType(gameType) + " AS g",
                "ON view_players.Game_ID = g.ID",
                "WHERE User_ID = ? ",
                "AND Role = ?"
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
}
