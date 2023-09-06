/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
package org.codedefenders.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codedefenders.model.Feedback;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.model.Feedback.MAX_RATING;
import static org.codedefenders.model.Feedback.MIN_RATING;
import static org.codedefenders.model.Feedback.Type;

public class FeedbackDAO {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackDAO.class);

    public static boolean storeFeedback(int gameId, int userId, Map<Feedback.Type, Integer> ratings) {
        if (ratings.isEmpty()) {
            return true;
        }

        List<DatabaseValue<?>> values = new ArrayList<>();

        for (Map.Entry<Feedback.Type, Integer> entry : ratings.entrySet()) {
            int clampedRating = Math.min(Math.max(MIN_RATING, entry.getValue()), MAX_RATING);
            values.add(DatabaseValue.of(userId));
            values.add(DatabaseValue.of(gameId));
            values.add(DatabaseValue.of(entry.getKey().name()));
            values.add(DatabaseValue.of(clampedRating));
        }

        String placeholders = Stream.generate(() -> "(?, ?, ?, ?)")
                .limit(ratings.size())
                .collect(Collectors.joining(","));

        @Language("SQL") String query = """
                INSERT INTO ratings (User_ID, Game_ID, type, value)
                VALUES %s
                ON DUPLICATE KEY UPDATE value = VALUES(value);
        """.formatted(placeholders);

        return DB.executeUpdateQuery(query, values.toArray(new DatabaseValue[0]));
    }

    public static Map<Feedback.Type, Integer> getFeedbackValues(int gameId, int userId) {
        Map<Feedback.Type, Integer> ratings = new HashMap<>();

        @Language("SQL") final String query = """
                SELECT value, type
                FROM ratings
                WHERE Game_ID = ?
                  AND User_ID = ?;
        """;

        DB.RSMapper<Void> mapper = rs -> {
            Feedback.Type type = Feedback.Type.valueOf(rs.getString("type"));
            int rating = rs.getInt("value");
            ratings.put(type, rating);
            return null;
        };

        DB.executeQueryReturnList(query, mapper,
                DatabaseValue.of(gameId), DatabaseValue.of(userId));

        return ratings;
    }

    public static Map<Feedback.Type, Double> getAverageGameRatings(int gameId) throws UncheckedSQLException, SQLMappingException {
        Map<Feedback.Type, Double> avgRatings = new HashMap<>();

        @Language("SQL") String query = """
                SELECT AVG(value) AS 'average', type
                FROM ratings
                WHERE Game_ID = ?
                  AND value > 0
                GROUP BY type;
        """;

        DB.RSMapper<Void> mapper = rs -> {
            Feedback.Type type = Feedback.Type.valueOf(rs.getString("type"));
            double avgRating = rs.getDouble("average");
            avgRatings.put(type, avgRating);
            return null;
        };

        DB.executeQueryReturnList(query, mapper,
                DatabaseValue.of(gameId));

        return avgRatings;
    }

    private static Map<Integer, Double> getAverageClassDifficultyRatings(Type feedbackType)
            throws UncheckedSQLException, SQLMappingException {
        Map<Integer, Double> avgClassRatings = new HashMap<>();

        @Language("SQL") String query = """
                SELECT AVG(ratings.value) AS 'average',
                       games.Class_ID AS classId,
                       COUNT(ratings.value) AS 'votes'
                FROM ratings, games
                WHERE ratings.Game_ID = games.ID
                  AND ratings.type = ?
                  AND ratings.value > 0
                GROUP BY games.Class_ID;
        """;

        DB.executeQueryReturnList(query,
                rs -> avgClassRatings.put(
                        rs.getInt("classId"),
                        rs.getDouble("average")),
                DatabaseValue.of(feedbackType.name()));

        return avgClassRatings;
    }

    public static Map<Integer, Double> getAverageMutationDifficulties() {
        return getAverageClassDifficultyRatings(Type.CUT_MUTATION_DIFFICULTY);
    }

    public static Map<Integer, Double> getAverageTestDifficulties() {
        return getAverageClassDifficultyRatings(Type.CUT_TEST_DIFFICULTY);
    }
}
