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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.model.Achievement;
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

    public Achievement achievementFromRS(ResultSet rs) throws SQLException {
        int nextLevelMetric = rs.getInt("NextLevelMetric");
        Optional<Integer> nextLevelMetricOptional = rs.wasNull() ? Optional.empty() : Optional.of(nextLevelMetric);
        return new Achievement(
                Achievement.Id.fromInt(rs.getInt("achievements.ID")),
                rs.getInt("achievements.Level"),
                rs.getInt("achievements.Index"),
                rs.getString("achievements.Name"),
                rs.getString("achievements.Description"),
                rs.getString("achievements.ProgressText"),
                rs.getInt("achievements.Metric"),
                nextLevelMetricOptional,
                rs.getInt("CurrentUserMetric")
        );
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
     * Returns the achievement for a user with the given id and the given achievement id. It always returns an Achievement
     * object with the current level and the current metric, even when the user has not yet unlocked the achievement.
     *
     * @param userId        The id of the user to get the achievement for.
     * @param achievementId The id of the achievement to get.
     * @return The achievement for the user.
     */
    public Optional<Achievement> getAchievementForUser(int userId, Achievement.Id achievementId) {
        return getAchievementsForUser(userId, achievementId).stream().findFirst();
    }

    private Collection<Achievement> getAchievementsForUser(int userId, Achievement.Id achievementId) {
        @Language("SQL")
        String query = """
                SELECT achievements.*, COALESCE(has_achievement.Metric, 0) AS CurrentUserMetric, (
                    SELECT a.Metric FROM achievements a
                    WHERE a.ID = achievements.ID
                    AND a.Level = achievements.Level + 1
                ) AS NextLevelMetric
                FROM achievements LEFT OUTER JOIN has_achievement
                ON has_achievement.User_ID = ?
                AND has_achievement.Achievement_ID = achievements.ID
                WHERE (
                    has_achievement.Achievement_Level = achievements.Level
                    -- get achievements with no progress tracked as well -> left outer join
                    OR has_achievement.Achievement_Level IS NULL AND achievements.Level = 0
                )
                %s;

        """.formatted(achievementId != null ? "AND achievements.ID = ?" : "");

        return queryRunner.query(
                query,
                listFromRS(this::achievementFromRS),
                achievementId == null ? new Object[] {userId} : new Object[] {userId, achievementId.getAsInt()}
        );
    }

    /**
     * Updates the achievement for a user by a given amount. It increments the metric first and then updates the level
     * if necessary.
     * The return value indicates whether the level was updated or not.
     *
     * @param userId The user to update the achievement for.
     * @param achievementId The achievement type to update.
     * @param metricChange The amount to change the metric by.
     * @return The number of rows for which the achievement level was updated (should be either 0 or 1).
     */
    public int updateAchievementForUser(int userId, Achievement.Id achievementId, int metricChange) {
        int updated = updateAchievementMetricForUser(userId, achievementId, metricChange);
        return updated > 0 ? updateAchievementLevelForUser(userId, achievementId) : 0;
    }

    private int updateAchievementMetricForUser(int userId, Achievement.Id achievementId, int metricChange) {
        @Language("SQL")
        String query = """
                INSERT INTO has_achievement(`Achievement_ID`, `User_ID`, `Metric`)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                Metric = Metric + ?;
        """;

        return queryRunner.update(query, achievementId.getAsInt(), userId, metricChange, metricChange);
    }


    /**
     * Sets the achievements metric for a user to the given amount if it's greater than the current amount.
     * It sets the metric first and then updates the level if necessary.
     * The return value indicates whether the level was updated or not.
     *
     * @param userId         The user to update the achievement for.
     * @param achievementId  The achievement type to update.
     * @param metricAbsolute The amount to set the metric to if it's greater than the current value.
     * @return The number of rows for which the achievement level was updated (should be either 0 or 1).
     */
    public int setAchievementForUser(int userId, Achievement.Id achievementId, int metricAbsolute) {
        int updated = setAchievementMetricForUser(userId, achievementId, metricAbsolute);
        return updated > 0 ? updateAchievementLevelForUser(userId, achievementId) : 0;
    }

    private int setAchievementMetricForUser(int userId, Achievement.Id achievementId, int metricAbsolute) {
        @Language("SQL")
        String query = """
                        INSERT INTO has_achievement(`Achievement_ID`, `User_ID`, `Metric`)
                        VALUES (?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                        Metric = GREATEST(Metric, ?);
                """;

        return queryRunner.update(query, achievementId.getAsInt(), userId, metricAbsolute, metricAbsolute);
    }

    private int updateAchievementLevelForUser(int userId, Achievement.Id achievementId) {
        @Language("SQL")
        String query = """
                UPDATE has_achievement
                SET Achievement_Level = Achievement_Level + 1
                WHERE User_ID = ?
                AND Achievement_ID = ?
                AND Metric >= (
                    SELECT Metric
                    FROM achievements
                    WHERE ID = ?
                    AND Level = Achievement_Level + 1
                );
        """;

        return queryRunner.update(query, userId, achievementId.getAsInt(), achievementId.getAsInt());
    }
}
