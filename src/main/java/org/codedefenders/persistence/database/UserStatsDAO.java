package org.codedefenders.persistence.database;


import java.sql.SQLException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.database.UncheckedSQLException;
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
    public int getNumKilledMutantsByUser(int userId) {
        return getNumMutantsByUser(userId, false);
    }

    /**
     * Fetches the amount of alive mutants a given user has written from the database.
     *
     * @param userId The id of the user
     * @return amount of alive mutants written by a user
     */
    public int getNumAliveMutantsByUser(int userId) {
        return getNumMutantsByUser(userId, true);
    }

    private int getNumMutantsByUser(int userId, boolean alive) {
        final String query = String.join("\n",
                "SELECT count(Mutant_ID) AS mutants",
                "FROM view_valid_mutants",
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
     * @param userId The id of the user
     * @return amount of killing tests written by a user
     */
    public int getNumKillingTestsByUser(int userId) {
        return getNumTestsByUser(userId, true);
    }

    /**
     * Fetches the amount of tests written by a user that did not (yet) kill mutants.
     *
     * @param userId The id of the user
     * @return amount of non-killing tests written by a user
     */
    public int getNumNonKillingTestsByUser(int userId) {
        return getNumTestsByUser(userId, false);
    }

    private int getNumTestsByUser(int userId, boolean killingTest) {
        final String query = String.join("\n",
                "SELECT count(Test_ID) AS tests",
                "FROM view_valid_tests",
                "WHERE Player_ID IN (SELECT ID as Player_ID FROM view_players WHERE User_ID = ?)",
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
     * @param userId The id of the user
     * @return average points received through tests
     */
    public double getAveragePointsTestByUser(int userId) {
        return getPointsTestByUser(userId, true);
    }

    /**
     * Fetches the amount of total points a given user received through all of their tests.
     *
     * @param userId The id of the user
     * @return total points received through tests
     */
    public int getTotalPointsTestsByUser(int userId) {
        return (int) getPointsTestByUser(userId, false);
    }

    private double getPointsTestByUser(int userId, boolean avg) {
        final String acc = avg ? "avg" : "sum";
        final String query = String.join("\n",
                "SELECT " + acc + "(Points) AS points",
                "FROM view_valid_tests",
                "WHERE Player_ID IN (SELECT ID as Player_ID FROM view_players WHERE User_ID = ?);"
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
     * @param userId The id of the user
     * @return average points received through mutants
     */
    public double getAveragePointsMutantByUser(int userId) {
        return getPointsMutantByUser(userId, true);
    }

    /**
     * Fetches the amount of total points a given user received through all of their mutants.
     *
     * @param userId The id of the user
     * @return total points received through mutants
     */
    public int getTotalPointsMutantByUser(int userId) {
        return (int) getPointsMutantByUser(userId, false);
    }

    private double getPointsMutantByUser(int userId, boolean avg) {
        final String acc = avg ? "avg" : "sum";
        final String query = String.join("\n",
                "SELECT " + acc + "(Points) AS points",
                "FROM view_valid_mutants",
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
     * @param userId The id of the user
     * @return games played as attacker
     */
    public int getAttackerGamesByUser(int userId) {
        return getGamesOfRoleByUser(userId, false);
    }

    /**
     * Fetches the amount of total games a user played as defender.
     *
     * @param userId The id of the user
     * @return games played as defender
     */
    public int getDefenderGamesByUser(int userId) {
        return getGamesOfRoleByUser(userId, true);
    }

    private int getGamesOfRoleByUser(int userId, boolean defender) {
        final Role role = defender ? Role.DEFENDER : Role.ATTACKER;
        final String query = String.join("\n",
                "SELECT count(ID) AS games",
                "FROM view_players ",
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
