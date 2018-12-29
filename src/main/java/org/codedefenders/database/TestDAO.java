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

import org.codedefenders.database.DB.RSMapper;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.game.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.codedefenders.util.Constants.DUMMY_DEFENDER_USER_ID;

/**
 * This class handles the database logic for tests.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 * @see Test
 */
public class TestDAO {
    private static final Logger logger = LoggerFactory.getLogger(TestDAO.class);

    /**
     * Constructs a test from a {@link ResultSet} entry.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed test.
     * @see RSMapper
     */
    static Test testFromRS(ResultSet rs) throws SQLException {
        int testId = rs.getInt("Test_ID");
        int gameId = rs.getInt("Game_ID");
        int classId = rs.getInt("Class_ID");
        // before 1.3.2, mutants didn't have a mandatory classId attribute
        if (rs.wasNull()) {
            classId = -1;
        }
        String javaFile = rs.getString("JavaFile");
        String classFile = rs.getString("ClassFile");
        int roundCreated = rs.getInt("RoundCreated");
        int mutantsKilled = rs.getInt("MutantsKilled");
        int playerId = rs.getInt("Player_ID");
        int points = rs.getInt("Points");
        String linesCoveredString = rs.getString("Lines_Covered");
        String linesUncoveredString = rs.getString("Lines_Uncovered");

        List<Integer> linesCovered = new ArrayList<>();
        if (linesCoveredString != null && !linesCoveredString.isEmpty()) {
            linesCovered.addAll(Arrays.stream(linesCoveredString.split(","))
                            .map(Integer::parseInt)
                            .collect(Collectors.toList()));
        }

        List<Integer> linesUncovered = new ArrayList<>();
        if (linesUncoveredString != null && !linesUncoveredString.isEmpty()) {
            linesUncovered.addAll(Arrays.stream(linesUncoveredString.split(","))
                            .map(Integer::parseInt)
                            .collect(Collectors.toList()));
        }

        return new Test(testId, classId, gameId, javaFile, classFile, roundCreated, mutantsKilled, playerId,
                linesCovered, linesUncovered, points);
    }

    /**
     * Returns the {@link Test} for the given test id.
     */
    public static Test getTestById(int testId) throws UncheckedSQLException, SQLMappingException {
        String query = "SELECT * FROM tests WHERE Test_ID = ?;";
        return DB.executeQueryReturnValue(query, TestDAO::testFromRS, DB.getDBV(testId));
    }

    /**
     * Returns the {@link Test Tests} from the given game.
     */
    public static List<Test> getTestsForGame(int gameId) throws UncheckedSQLException, SQLMappingException {
        String query = "SELECT * FROM tests WHERE Game_ID = ?;";
        return DB.executeQueryReturnList(query, TestDAO::testFromRS, DB.getDBV(gameId));
    }

    /**
     * Returns the valid {@link Test Tests} from the given game.
     * Valid tests are compilable and do not fail when executed against the original class.
     *
     * @param gameId the identifier of the given game.
     * @param defendersOnly If {@code true}, only return tests that were written by defenders.
     *                      <p>
     *                      Include also the tests uploaded by the System Defender
     * @return a {@link List} of valid tests for the given game.
     */
    public static List<Test> getValidTestsForGame(int gameId, boolean defendersOnly)
            throws UncheckedSQLException, SQLMappingException {
        List<Test> result = new ArrayList<>();

        String query = String.join("\n",
                "SELECT tests.* FROM tests",
                (defendersOnly ? "INNER JOIN players pl on tests.Player_ID = pl.ID" : ""),
                "WHERE tests.Game_ID=? AND tests.ClassFile IS NOT NULL",
                (defendersOnly ? "AND pl.Role='DEFENDER'" : ""),
                "  AND EXISTS (",
                "    SELECT * FROM targetexecutions ex",
                "    WHERE ex.Test_ID = tests.Test_ID",
                "      AND ex.Target='TEST_ORIGINAL'",
                "      AND ex.Status='SUCCESS'",
                "  );"
        );
        result.addAll(DB.executeQueryReturnList(query, TestDAO::testFromRS, DB.getDBV(gameId)));

        String systemDefenderQuery = String.join("\n",
                "SELECT tests.*",
                "FROM tests",
                "INNER JOIN players pl on tests.Player_ID = pl.ID",
                "INNER JOIN users u on u.User_ID = pl.User_ID",
                "WHERE tests.Game_ID = ?",
                "  AND tests.ClassFile IS NOT NULL",
                "  AND u.User_ID = ?;"
        );

        DatabaseValue[] values = new DatabaseValue[]{
                DB.getDBV(gameId),
                DB.getDBV(DUMMY_DEFENDER_USER_ID)
        };

        result.addAll(DB.executeQueryReturnList(systemDefenderQuery, TestDAO::testFromRS, values));

        return result;
    }

    /**
     * Returns the valid {@link Test Tests} from the games played on the given class.
     * Valid tests are compilable and do not fail when executed against the original class.
     * <p>
     * Include also the tests from the System Defender
     *
     * @param classId the identifier of the given class.
     * @return a {@link List} of valid tests for the given class.
     */
    public static List<Test> getValidTestsForClass(int classId) throws UncheckedSQLException, SQLMappingException {
        List<Test> result = new ArrayList<>();

        String query = String.join("\n",
                "SELECT tests.*",
                "FROM tests, games",
                "WHERE tests.Game_ID = games.ID",
                "  AND games.Class_ID = ?",
                "  AND tests.ClassFile IS NOT NULL",
                "  AND EXISTS (",
                "    SELECT * FROM targetexecutions ex",
                "    WHERE ex.Test_ID = tests.Test_ID",
                "      AND ex.Target='TEST_ORIGINAL'",
                "      AND ex.Status='SUCCESS'",
                "  );"
        );
        result.addAll(DB.executeQueryReturnList(query, TestDAO::testFromRS, DB.getDBV(classId)));

        // Include also those tests uploaded, i.e, player_id = -1
        String systemDefenderQuery = String.join("\n",
                "SELECT tests.*",
                "FROM tests, test_uploaded_with_class up",
                "WHERE tests.Test_ID = up.Test_ID",
                "  AND up.Class_ID = ?",
                "  AND tests.ClassFile IS NOT NULL;"
        );

        result.addAll(DB.executeQueryReturnList(systemDefenderQuery, TestDAO::testFromRS, DB.getDBV(classId)));

        return result;
    }

    /**
     * Stores a given {@link Test} in the database.
     * <p>
     * This method does not update the given test object.
     * Use {@link Test#insert()} instead.
     *
     * @param test the given test as a {@link Test}.
     * @return the generated identifier of the test as an {@code int}.
     * @throws UncheckedSQLException If storing the test was not successful.
     */
    public static int storeTest(Test test) throws UncheckedSQLException {
        String javaFile = DatabaseAccess.addSlashes(test.getJavaFile());
        String classFile = DatabaseAccess.addSlashes(test.getClassFile());
        int gameId = test.getGameId();
        int roundCreated = test.getRoundCreated();
        int mutantsKilled = test.getMutantsKilled();
        int playerId = test.getPlayerId();
        int score = test.getScore();
        int classId = test.getClassId();
        LineCoverage lineCoverage = test.getLineCoverage();

        String linesCovered = "";
        String linesUncovered = "";

        if (lineCoverage != null) {
            linesCovered = lineCoverage.getLinesCovered().stream().map(Object::toString).collect(Collectors.joining(","));
            linesUncovered = lineCoverage.getLinesUncovered().stream().map(Object::toString).collect(Collectors.joining(","));
        }

        String query = "INSERT INTO tests (JavaFile, ClassFile, Game_ID, RoundCreated, MutantsKilled, Player_ID, Points, Class_ID, Lines_Covered, Lines_Uncovered) VALUES (?,?,?,?,?,?,?,?,?,?);";
        DatabaseValue[] values = new DatabaseValue[]{
                DB.getDBV(javaFile),
                DB.getDBV(classFile),
                DB.getDBV(gameId),
                DB.getDBV(roundCreated),
                DB.getDBV(mutantsKilled),
                DB.getDBV(playerId),
                DB.getDBV(score),
                DB.getDBV(classId),
                DB.getDBV(linesCovered),
                DB.getDBV(linesUncovered)
        };

        final int result = DB.executeUpdateQueryGetKeys(query, values);
        if (result != -1) {
            return result;
        } else {
            throw new UncheckedSQLException("Could not store test to database.");
        }
    }

    /**
     * Stores a mapping between a {@link Test} and a {@link GameClass} in the database.
     *
     * @param testId  the identifier of the test.
     * @param classId the identifier of the class.
     * @return {@code true} whether storing the mapping was successful, {@code false} otherwise.
     */
    public static boolean mapTestToClass(int testId, int classId) {
        String query = String.join("\n",
                "INSERT INTO test_uploaded_with_class (Test_ID, Class_ID)",
                "VALUES (?, ?);"
        );
        DatabaseValue[] values = new DatabaseValue[]{
                DB.getDBV(testId),
                DB.getDBV(classId)
        };

        return DB.executeUpdateQuery(query, values);
    }

    /**
     * Removes a test for a given identifier.
     *
     * @param id the identifier of the test to be removed.
     * @return {@code true} for successful removal, {@code false} otherwise.
     */
    public static boolean removeTestForId(Integer id) {
        String query = String.join("\n",
                "DELETE FROM tests WHERE Test_ID = ?;",
                "DELETE FROM test_uploaded_with_class WHERE Test_ID = ?;"
        );

        return DB.executeUpdateQuery(query, DB.getDBV(id));
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
        String query = String.join("\n",
                "DELETE FROM tests",
                " WHERE Test_ID in ",
                range,
                "DELETE FROM test_uploaded_with_class",
                "WHERE Test_ID in ",
                range
        );

        // Hack to make sure all values are listed in both 'ranges'.
        tests.addAll(new LinkedList<>(tests));
        DatabaseValue[] values = tests.stream().map(DB::getDBV).toArray(DatabaseValue[]::new);

        return DB.executeUpdateQuery(query, values);
    }

    /**
     * Returns the number of killed AI mutants for a given test.
     *
     * @param testId the identifier of the test.
     * @return number of killed AI mutants, or {@code 0} if none found.
     */
    public static int getNumAiMutantsKilledByTest(int testId) {
        String query = "SELECT * FROM tests WHERE Test_ID=?;";
        final Integer kills = DB.executeQueryReturnValue(query, rs -> rs.getInt("NumberAiMutantsKilled"), DB.getDBV(testId));
        return Optional.ofNullable(kills).orElse(0);
    }
}
