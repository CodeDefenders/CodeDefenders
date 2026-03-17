/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.model.Achievement;
import org.codedefenders.model.AchievementLevel;
import org.codedefenders.model.AchievementType;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.ResultSetUtils.listFromRS;

@ApplicationScoped
public class AchievementRepository {
    private static final Logger logger = LoggerFactory.getLogger(AchievementRepository.class);

    private final QueryRunner queryRunner;

    @Inject
    public AchievementRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    private record UserProgress(AchievementType type, int level, int metric) {
    }

    /**
     * Returns all achievements for a user with the given id. It always returns Achievement objects with the current
     * level and the current metric, even for achievements the user has not yet unlocked.
     *
     * @param userId The id of the user to get the achievements for.
     * @return The achievements for the user.
     */
    public Collection<Achievement> getAchievementsForUser(int userId) {
        return getAchievementsForUser(userId, null);
    }

    /**
     * Returns the achievement for a user with the given id and the given achievement type. It always returns an
     * Achievement object with the current level and the current metric, even when the user has not yet unlocked it.
     *
     * @param userId         The id of the user to get the achievement for.
     * @param achievementType The type of the achievement to get.
     * @return The achievement for the user.
     */
    public Optional<Achievement> getAchievementForUser(int userId, AchievementType achievementType) {
        return getAchievementsForUser(userId, achievementType).stream().findFirst();
    }

    private Collection<Achievement> getAchievementsForUser(int userId, AchievementType singleType) {
        @Language("SQL")
        String query = """
                SELECT Achievement_ID, Achievement_Level, Metric
                FROM has_achievement
                WHERE User_ID = ?
                %s;
        """.formatted(singleType != null ? "AND Achievement_ID = ?" : "");

        Collection<UserProgress> progressList = queryRunner.query(
                query,
                listFromRS(rs -> new UserProgress(
                        AchievementType.fromInt(rs.getInt("Achievement_ID")),
                        rs.getInt("Achievement_Level"),
                        rs.getInt("Metric")
                )),
                singleType == null ? new Object[]{userId} : new Object[]{userId, singleType.getId()}
        );

        Map<AchievementType, UserProgress> progressMap = progressList.stream()
                .filter(p -> p.type() != null)
                .collect(Collectors.toMap(UserProgress::type, p -> p));

        AchievementType[] types = singleType != null
                ? new AchievementType[]{singleType}
                : AchievementType.values();

        return Arrays.stream(types)
                .map(type -> {
                    UserProgress progress = progressMap.getOrDefault(type, new UserProgress(type, 0, 0));
                    return new Achievement(type, progress.level(), progress.metric());
                })
                .toList();
    }

    /**
     * Updates the achievement for a user by a given amount. It increments the metric first and then updates the level
     * if necessary.
     * The return value indicates whether the level was updated or not.
     *
     * @param userId         The user to update the achievement for.
     * @param achievementType The achievement type to update.
     * @param metricChange   The amount to change the metric by.
     * @return The number of rows for which the achievement level was updated (should be either 0 or 1).
     */
    public int updateAchievementForUser(int userId, AchievementType achievementType, int metricChange) {
        int updated = updateAchievementMetricForUser(userId, achievementType, metricChange);
        return updated > 0 ? updateAchievementLevelForUser(userId, achievementType) : 0;
    }

    private int updateAchievementMetricForUser(int userId, AchievementType achievementType, int metricChange) {
        @Language("SQL")
        String query = """
                INSERT INTO has_achievement(`Achievement_ID`, `User_ID`, `Metric`)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                Metric = Metric + ?;
        """;

        return queryRunner.update(query, achievementType.getId(), userId, metricChange, metricChange);
    }


    /**
     * Sets the achievements metric for a user to the given amount if it's greater than the current amount.
     * It sets the metric first and then updates the level if necessary.
     * The return value indicates whether the level was updated or not.
     *
     * @param userId          The user to update the achievement for.
     * @param achievementType The achievement type to update.
     * @param metricAbsolute  The amount to set the metric to if it's greater than the current value.
     * @return The number of rows for which the achievement level was updated (should be either 0 or 1).
     */
    public int setAchievementForUser(int userId, AchievementType achievementType, int metricAbsolute) {
        int updated = setAchievementMetricForUser(userId, achievementType, metricAbsolute);
        return updated > 0 ? updateAchievementLevelForUser(userId, achievementType) : 0;
    }

    private int setAchievementMetricForUser(int userId, AchievementType achievementType, int metricAbsolute) {
        @Language("SQL")
        String query = """
                        INSERT INTO has_achievement(`Achievement_ID`, `User_ID`, `Metric`)
                        VALUES (?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                        Metric = GREATEST(Metric, ?);
                """;

        return queryRunner.update(query, achievementType.getId(), userId, metricAbsolute, metricAbsolute);
    }

    /**
     * Updates the achievement level for a user if the metric threshold for the next level is met.
     * Reads the current level and metric from the DB, checks the threshold from {@link AchievementType},
     * and atomically updates the level with optimistic locking.
     */
    private int updateAchievementLevelForUser(int userId, AchievementType type) {
        @Language("SQL")
        String selectQuery = """
                SELECT Achievement_Level, Metric
                FROM has_achievement
                WHERE User_ID = ? AND Achievement_ID = ?;
        """;

        Collection<UserProgress> results = queryRunner.query(
                selectQuery,
                listFromRS(rs -> new UserProgress(
                        type,
                        rs.getInt("Achievement_Level"),
                        rs.getInt("Metric")
                )),
                userId, type.getId()
        );

        Optional<UserProgress> progressOpt = results.stream().findFirst();
        if (progressOpt.isEmpty()) {
            return 0;
        }

        UserProgress progress = progressOpt.get();
        AchievementLevel nextLevel = type.getLevel(progress.level() + 1);
        if (nextLevel == null || progress.metric() < nextLevel.metric()) {
            return 0;
        }

        @Language("SQL")
        String updateQuery = """
                UPDATE has_achievement
                SET Achievement_Level = ?
                WHERE User_ID = ? AND Achievement_ID = ?
                AND Achievement_Level = ?;
        """;

        return queryRunner.update(updateQuery, progress.level() + 1, userId, type.getId(), progress.level());
    }
}
