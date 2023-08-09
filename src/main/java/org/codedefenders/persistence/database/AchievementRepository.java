package org.codedefenders.persistence.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import javax.inject.Inject;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.model.Achievement;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.ResultSetUtils.listFromRS;

public class AchievementRepository {
    private static final Logger logger = LoggerFactory.getLogger(AchievementRepository.class);

    private final QueryRunner queryRunner;

    @Inject
    public AchievementRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    public Achievement achievementFromRS(ResultSet rs) throws SQLException {
        return new Achievement(
                Achievement.Id.fromInt(rs.getInt("ID")),
                rs.getInt("Level"),
                rs.getString("Name"),
                rs.getInt("Metric")
        );
    }

    public Collection<Achievement> getAchievementsForUser(int userId) {
        @Language("SQL")
        String query = String.join("\n",
                "SELECT Achievement_ID, Achievement_Level",
                "FROM has_achievement",
                "WHERE User_ID = ?",
                "LEFT JOIN achievements",
                "ON has_achievement.Achievement_ID = achievement.ID",
                "AND has_achievement.Achievement_Level = achievement.Level"
        );

        try {
            return queryRunner.query(
                    query,
                    resultSet -> listFromRS(resultSet, this::achievementFromRS),
                    userId
            );
        } catch (SQLException e) {
            logger.error("Failed to get achievements for user {}", userId, e);
            throw new UncheckedSQLException(e);
        }
    }
    public int updateAchievementForUser(int userId, Achievement.Id achievementId, int metricChange) {
        int updated = updateAchievementMetricForUser(userId, achievementId, metricChange);
        return updated > 0 ? updateAchievementLevelForUser(userId, achievementId) : updated;
    }

    private int updateAchievementMetricForUser(int userId, Achievement.Id achievementId, int metricChange) {
        @Language("SQL")
        String query = String.join("\n",
                "INSERT INTO has_achievement(`Achievement_ID`, `User_ID`, `Metric`)",
                "VALUES (?, ?, ?)",
                "ON DUPLICATE KEY UPDATE",
                "Metric = Metric + ?"
        );

        try {
            return queryRunner.update(query, achievementId.getAsInt(), userId, metricChange, metricChange);
        } catch (SQLException e) {
            logger.error("Failed to update achievement metric for user {} and achievement {}", userId, achievementId,
                    e);
            throw new UncheckedSQLException(e);
        }
    }

    private int updateAchievementLevelForUser(int userId, Achievement.Id achievementId) {
        @Language("SQL")
        String query = String.join("\n",
                "UPDATE has_achievement",
                "SET Achievement_Level = Achievement_Level + 1",
                "WHERE User_ID = ?",
                "AND Achievement_ID = ?",
                "AND Metric >= (SELECT Metric ",
                "FROM achievements",
                "WHERE ID = ? ",
                "AND Level = Achievement_Level + 1",
                ")"
        );

        try {
            return queryRunner.update(query, userId, achievementId.getAsInt(), achievementId.getAsInt());
        } catch (SQLException e) {
            logger.error("Failed to update achievement level for user {} and achievement {}", userId, achievementId, e);
            throw new UncheckedSQLException(e);
        }
    }
}
