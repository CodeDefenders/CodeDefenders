package org.codedefenders.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.dto.UserStats;
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
    public UserStatsService(UserStatsDAO userStatsDAO) {
        dao = userStatsDAO;
    }

    public UserStats getStatsByUserId(int userId) {
        return new UserStats(userId,
                dao.getNumKilledMutantsByUser(userId),
                dao.getNumAliveMutantsByUser(userId),
                dao.getNumKillingTestsByUser(userId),
                dao.getNumNonKillingTestsByUser(userId),
                dao.getAveragePointsTestByUser(userId),
                dao.getTotalPointsTestsByUser(userId),
                dao.getAveragePointsMutantByUser(userId),
                dao.getTotalPointsMutantByUser(userId),
                dao.getAttackerGamesByUser(userId),
                dao.getDefenderGamesByUser(userId)
        );
    }
}
