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
package org.codedefenders.service;

import java.util.HashMap;
import java.util.Map;

import org.codedefenders.dto.UserStats;
import org.codedefenders.game.GameType;
import org.codedefenders.persistence.database.PuzzleRepository;
import org.codedefenders.persistence.database.UserStatsDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Provides an API for fetching statistics of users.
 */
@ApplicationScoped
public class UserStatsService {
    private static final Logger logger = LoggerFactory.getLogger(UserStatsService.class);

    private final UserStatsDAO userStatsDAO;
    private final PuzzleRepository puzzleRepo;

    @Inject
    private UserStatsService(UserStatsDAO userStatsDAO, PuzzleRepository puzzleRepo) {
        this.userStatsDAO = userStatsDAO;
        this.puzzleRepo = puzzleRepo;
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
                userStatsDAO.getNumKilledMutantsByUser(userId, gameType),
                userStatsDAO.getNumAliveMutantsByUser(userId, gameType),
                userStatsDAO.getNumKillingTestsByUser(userId, gameType),
                userStatsDAO.getNumNonKillingTestsByUser(userId, gameType),
                userStatsDAO.getAveragePointsTestByUser(userId, gameType),
                userStatsDAO.getTotalPointsTestsByUser(userId, gameType),
                userStatsDAO.getAveragePointsMutantByUser(userId, gameType),
                userStatsDAO.getTotalPointsMutantByUser(userId, gameType)
        );

        if (gameType == GameType.MULTIPLAYER) {
            us.setAttackerDefenderGames(
                    userStatsDAO.getAttackerGamesByUser(userId, gameType),
                    userStatsDAO.getDefenderGamesByUser(userId, gameType)
            );
        } else {
            us.setTotalGames(userStatsDAO.getTotalGamesByUser(userId, gameType));
        }

        return us;
    }

    public UserStats.PuzzleStats getPuzzleStatsByUserId(int userId) {
        final UserStats.PuzzleStats puzzleStats = userStatsDAO.getPuzzleStatsByUser(userId);
        puzzleStats.setChapters(puzzleRepo.getPuzzleChapters());
        return puzzleStats;
    }
}
