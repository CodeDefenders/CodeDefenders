/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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

import org.codedefenders.game.GameClass;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.game.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class handles the database logic for tests.
 *
 * @author <a href=https://github.com/werli>Phil Werli<a/>
 * @see Test
 */
public class TestDAO {
    private static final Logger logger = LoggerFactory.getLogger(TestDAO.class);

    /**
     * Stores a given {@link Test} in the database.
     *
     * @param test the given test as a {@link Test}.
     * @throws Exception If storing the test was not successful.
     * @return the generated identifier of the test as an {@code int}.
     */
    public static int storeTest(Test test) throws Exception {
        String javaFile = DatabaseAccess.addSlashes(test.getJavaFile());
        String classFile = DatabaseAccess.addSlashes(test.getClassFile());
        int gameId = test.getGameId();
        int roundCreated = test.getRoundCreated();
        int playerId = test.getPlayerId();
        int score = test.getScore();
        Integer classId = test.getClassId();
        LineCoverage lineCoverage = test.getLineCoverage();

        String linesCovered = "";
        String linesUncovered = "";

        if (lineCoverage != null) {
            linesCovered = lineCoverage.getLinesCovered().stream().map(Object::toString).collect(Collectors.joining(","));
            linesUncovered = lineCoverage.getLinesUncovered().stream().map(Object::toString).collect(Collectors.joining(","));
        }

        String query = "INSERT INTO tests (JavaFile, ClassFile, Game_ID, RoundCreated, Player_ID, Points, Class_ID, Lines_Covered, Lines_Uncovered) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(javaFile),
                DB.getDBV(classFile),
                DB.getDBV(gameId),
                DB.getDBV(roundCreated),
                DB.getDBV(playerId),
                DB.getDBV(score),
                (classId == null) ? null : DB.getDBV(classId),
                DB.getDBV(linesCovered),
                DB.getDBV(linesUncovered)
        };
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        final int result = DB.executeUpdateGetKeys(stmt, conn);
        if (result != -1) {
            return result;
        } else {
            throw new Exception("Could not store test to database.");
        }
    }

    /**
     * Stores a mapping between a {@link Test} and a {@link GameClass} in the database.
     *
     * @param testId the identifier of the test.
     * @param classId the identifier of the class.
     * @return {@code true} whether storing the mapping was successful, {@code false} otherwise.
     */
    public static boolean mapTestToClass(Integer testId, Integer classId) {
        String query = "UPDATE tests SET Class_ID = ? WHERE Test_ID = ?";
        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(classId),
                DB.getDBV(testId)
        };
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        return DB.executeUpdate(stmt, conn);
    }

    /**
     * Removes a test for a given identifier.
     *
     * @param id the identifier of the test to be removed.
     * @return {@code true} for successful removal, {@code false} otherwise.
     */
    public static boolean removeTestForId(Integer id) {
        String query = "DELETE FROM tests WHERE Test_ID = ?;";
        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(id),
        };

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        return DB.executeUpdate(stmt, conn);
    }

    /**
     * Removes multiple tests for a given list of identifiers.
     *
     * @param tests the identifiers of the tests to be removed.
     * @return {@code true} for successful removal, {@code false} otherwise.
     */
    public static boolean removeTestsForIds(List<Integer> tests) {
        if (tests.isEmpty()) {
            return false;
        }

        final StringBuilder bob = new StringBuilder("(");
        for (int i = 0; i < tests.size() - 1; i++) {
            bob.append("?,");
        }
        bob.append("?);");

        final String range = bob.toString();
        String query = "DELETE FROM tests WHERE Test_ID in " + range;

        DatabaseValue[] valueList = tests.stream().map(DB::getDBV).toArray(DatabaseValue[]::new);

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        return DB.executeUpdate(stmt, conn);
    }
}
