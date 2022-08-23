package org.codedefenders.persistence.database;

import java.sql.SQLException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.database.ConnectionFactory;
import org.codedefenders.game.GameState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class GameRepository {
    private static final Logger logger = LoggerFactory.getLogger(GameRepository.class);
    private final ConnectionFactory connectionFactory;

    @Inject
    public GameRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public int closeExpiredGames() {
        final String sql = (
                "UPDATE games " +
                "SET State = '" + GameState.FINISHED + "' " +
                "WHERE State = '" + GameState.ACTIVE + "' " +
                "AND Finish_Time <= NOW()" +
                "AND Finish_Time <> 0"
        );

        try {
            final int rowsAffected = connectionFactory.getQueryRunner().update(sql);
            logger.info("Closed {} expired games", rowsAffected);
            return rowsAffected;
        } catch (SQLException e) {
            logger.error("Error closing expired games", e);
            return 0;
        }
    }
}
