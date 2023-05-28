package org.codedefenders.service;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.database.PuzzleDAO;
import org.codedefenders.dto.UserStats;
import org.codedefenders.game.GameType;
import org.codedefenders.persistence.database.UserStatsDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides an API for fetching statistics of users.
 */
@ApplicationScoped
public class UserStatsService {
    private static final Logger logger = LoggerFactory.getLogger(UserStatsService.class);

    private final UserStatsDAO dao;

    @Inject
    private UserStatsService(UserStatsDAO userStatsDAO) {
        dao = userStatsDAO;
    }

    /**
     * Fetches the statistics of a user for all game types (MELEE, MULTIPLAYER).
     *
     * @param userId The id of the user.
     * @return The statistics of the user, grouped by game type.
     */
    public Map<GameType, UserStats> getStatsByUserId(int userId) {
        Map<GameType, UserStats> stats = new HashMap<>();
        for (GameType gameType : GameType.values()) {
            stats.put(gameType, getStatsByUserId(userId, gameType));
        }
        return stats;
    }

    private UserStats getStatsByUserId(int userId, GameType gameType) {
        UserStats us = new UserStats(userId,
                dao.getNumKilledMutantsByUser(userId, gameType),
                dao.getNumAliveMutantsByUser(userId, gameType),
                dao.getNumKillingTestsByUser(userId, gameType),
                dao.getNumNonKillingTestsByUser(userId, gameType),
                dao.getAveragePointsTestByUser(userId, gameType),
                dao.getTotalPointsTestsByUser(userId, gameType),
                dao.getAveragePointsMutantByUser(userId, gameType),
                dao.getTotalPointsMutantByUser(userId, gameType)
        );

        if (gameType == GameType.MULTIPLAYER) {
            us.setAttackerDefenderGames(
                    dao.getAttackerGamesByUser(userId, gameType),
                    dao.getDefenderGamesByUser(userId, gameType)
            );
        } else {
            us.setTotalGames(dao.getTotalGamesByUser(userId, gameType));
        }

        return us;
    }

    public UserStats.PuzzleStats getPuzzleStatsByUserId(int userId) {
        final UserStats.PuzzleStats puzzleStats = dao.getPuzzleStatsByUser(userId);
        puzzleStats.setChapters(PuzzleDAO.getPuzzleChapters());
        return puzzleStats;
    }
}
