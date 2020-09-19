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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.codedefenders.servlets.FeedbackManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.model.Feedback.MAX_RATING;
import static org.codedefenders.model.Feedback.MIN_RATING;
import static org.codedefenders.model.Feedback.Type;
import static org.codedefenders.model.Feedback.types;

public class FeedbackDAO {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackManager.class);

    public static boolean storeFeedback(int gameId, int userId, List<Integer> ratingsList) {
        StringBuilder bob = new StringBuilder("INSERT INTO ratings (User_ID, Game_ID, type, value) VALUES ");

        if (ratingsList.size() > types.size() || ratingsList.size() < 1) {
            return false;
        }

        String queryValues = "(?, ?, ?, ?),";
        List<DatabaseValue> allValuesList = new ArrayList<>();
        for (int i = 0; i < ratingsList.size(); i++) {
            int boundedValue = Math.min(Math.max(MIN_RATING, ratingsList.get(i)), MAX_RATING);
            List<DatabaseValue> valueList = Arrays.asList(
                    DatabaseValue.of(userId),
                    DatabaseValue.of(gameId),
                    DatabaseValue.of(types.get(i).name()),
                    DatabaseValue.of(boundedValue)
            );
            allValuesList.addAll(valueList);
            bob.append(queryValues);
        }
        bob.deleteCharAt(bob.length() - 1).append(" ON DUPLICATE KEY UPDATE value = VALUES(value)");

        String query = bob.toString();
        DatabaseValue[] values = allValuesList.toArray(new DatabaseValue[0]);
        return DB.executeUpdateQuery(query, values);
    }

    public static List<Integer> getFeedbackValues(int gameId, int userId) {
        List<Integer> values = Stream.generate(() -> -1).limit(types.size()).collect(Collectors.toList());

        final String query = String.join("\n",
                "SELECT value, type",
                "FROM ratings",
                "WHERE Game_ID = ?",
                "  AND User_ID = ?;");

        DB.RSMapper<Boolean> mapper = rs -> {
            String typeString = rs.getString("type");
            if (types.stream().anyMatch(feedbackType -> typeString.equals(feedbackType.name()))) {
                int index = Type.valueOf(typeString).ordinal();
                values.set(index, rs.getInt("value"));
            } else {
                logger.warn("No such feedback type: " + typeString);
            }
            return true;
        };

        DatabaseValue[] dbvalues = {DatabaseValue.of(gameId), DatabaseValue.of(userId)};
        List<Boolean> result = DB.executeQueryReturnList(query, mapper, dbvalues);
        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        return values;
    }

    public static List<Double> getAverageGameRatings(int gameId) throws UncheckedSQLException, SQLMappingException {
        List<Double> values = DoubleStream.generate(() -> -1.0).limit(types.size())
                .boxed()
                .collect(Collectors.toList());

        String query = String.join("\n",
                "SELECT AVG(value) AS 'average', type",
                "FROM ratings",
                "WHERE Game_ID = ?",
                "  AND value > 0",
                "GROUP BY type;");

        DB.RSMapper<Boolean> mapper = rs -> {
            String typeString = rs.getString("type");
            if (types.stream().anyMatch(feedbackType -> typeString.equals(feedbackType.name()))) {
                int index = Type.valueOf(typeString).ordinal();
                values.set(index, rs.getDouble("average"));
            } else {
                logger.warn("No such feedback type: " + typeString);
            }
            return true;
        };

        List<Boolean> result = DB.executeQueryReturnList(query, mapper, DatabaseValue.of(gameId));

        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        return values;
    }

    private static List<Double> getAverageClassDifficultyRatings(Type feedbackType)
            throws UncheckedSQLException, SQLMappingException {
        String query = String.join("\n",
                "SELECT",
                "  IFNULL(AVG(value), -1)   AS 'average',",
                "  c.Class_ID,",
                "  COUNT(value) AS 'votes'",
                "FROM (SELECT * FROM ratings WHERE type = ? AND value > 0) as filteredRatings",
                "RIGHT JOIN games g ON filteredRatings.Game_ID = g.ID",
                "RIGHT JOIN classes c ON g.Class_ID = c.Class_ID",
                "GROUP BY c.Class_ID ORDER BY c.Class_ID;");

        return DB.executeQueryReturnList(query, rs -> rs.getDouble(1), DatabaseValue.of(feedbackType.name()));
    }

    // TODO Phil 28/12/18: pretty sure this doesn't result in the wanted behavior.
    //  This is ordered, but when trying to map to classes, the classes aren't ordered so the mapping just disappears.
    public static List<Double> getAverageMutationDifficulties() {
        return getAverageClassDifficultyRatings(Type.CUT_MUTATION_DIFFICULTY);
    }

    public static List<Double> getAverageTestDifficulties() {
        return getAverageClassDifficultyRatings(Type.CUT_TEST_DIFFICULTY);
    }
}
