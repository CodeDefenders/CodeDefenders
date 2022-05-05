package org.codedefenders.persistence.database;


import java.sql.SQLException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.database.ConnectionFactory;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.game.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named(value = "userStats")
@ApplicationScoped
public class UserStatsDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserStatsDAO.class);

    private final ConnectionFactory connectionFactory;

    @Inject
    private UserStatsDAO(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public int getNumKilledMutantsByUser(int userId) {
        return getNumMutantsByUser(userId, false);
    }

    public int getNumAliveMutantsByUser(int userId) {
        return getNumMutantsByUser(userId, true);
    }

    private int getNumMutantsByUser(int userId, boolean alive) {
        final String query = "SELECT count(Mutant_ID) AS mutants "
                + "FROM view_valid_mutants "
                + "WHERE User_ID = ? "
                + "AND Alive = ?;";
        try {
            return connectionFactory.getQueryRunner().query(
                    query,
                    resultSet -> DatabaseUtils.nextFromRS(
                            resultSet, rs -> rs.getInt("mutants")
                    ),
                    userId,
                    alive ? 1 : 0
            ).orElse(0);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public int getNumKillingTestsByUser(int userId) {
        return getNumTestsByUser(userId, true);
    }

    public int getNumNonKillingTestsByUser(int userId) {
        return getNumTestsByUser(userId, false);
    }

    private int getNumTestsByUser(int userId, boolean killingTest) {
        final String query = "SELECT count(Test_ID) AS tests "
                + "FROM view_valid_tests "
                + "WHERE Player_ID IN (SELECT ID as Player_ID FROM view_players WHERE User_ID = ?) "
                + "AND MutantsKilled " + (killingTest ? ">" : "=") + " 0;";
        try {
            return connectionFactory.getQueryRunner().query(
                    query,
                    resultSet -> DatabaseUtils.nextFromRS(
                            resultSet, rs -> rs.getInt("tests")
                    ),
                    userId
            ).orElse(0);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public double getAveragePointsTestByUser(int userId) {
        return getPointsTestByUser(userId, true);
    }

    public int getTotalPointsTestsByUser(int userId) {
        return (int) getPointsTestByUser(userId, false);
    }

    private double getPointsTestByUser(int userId, boolean avg) {
        final String acc = avg ? "avg" : "sum";
        final String query = "SELECT " + acc + "(Points) AS points "
                + "FROM view_valid_tests "
                + "WHERE Player_ID IN (SELECT ID as Player_ID FROM view_players WHERE User_ID = ?);";
        try {
            return connectionFactory.getQueryRunner().query(
                    query,
                    resultSet -> DatabaseUtils.nextFromRS(
                            resultSet, rs -> rs.getDouble("points")
                    ),
                    userId
            ).orElse(0d);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public double getAveragePointsMutantByUser(int userId) {
        return getPointsMutantByUser(userId, true);
    }

    public int getTotalPointsMutantByUser(int userId) {
        return (int) getPointsMutantByUser(userId, false);
    }

    private double getPointsMutantByUser(int userId, boolean avg) {
        final String acc = avg ? "avg" : "sum";
        final String query = "SELECT " + acc + "(Points) AS points "
                + "FROM view_valid_mutants "
                + "WHERE User_ID = ?;";
        try {
            return connectionFactory.getQueryRunner().query(
                    query,
                    resultSet -> DatabaseUtils.nextFromRS(
                            resultSet, rs -> rs.getDouble("points")
                    ),
                    userId
            ).orElse(0d);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public int getAttackerGamesByUser(int userId) {
        return getGamesOfRoleByUser(userId, false);
    }

    public int getDefenderGamesByUser(int userId) {
        return getGamesOfRoleByUser(userId, true);
    }

    private int getGamesOfRoleByUser(int userId, boolean defender) {
        final Role role = defender ? Role.DEFENDER : Role.ATTACKER;
        final String query = "SELECT count(ID) AS games "
                + "FROM view_players "
                + "WHERE User_ID = ? "
                + "AND Role = ?";
        try {
            return connectionFactory.getQueryRunner().query(
                    query,
                    resultSet -> DatabaseUtils.nextFromRS(
                            resultSet, rs -> rs.getInt("games")
                    ),
                    userId,
                    role.toString()
            ).orElse(0);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }
}
