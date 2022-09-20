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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.codedefenders.database.DB.RSMapper;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.util.Constants.DUMMY_DEFENDER_USER_ID;

/**
 * This class handles the database logic for tests.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
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
        String absoluteJavaFile = FileUtils.getAbsoluteDataPath(javaFile).toString();
        String absoluteClassFile = classFile == null ? null : FileUtils.getAbsoluteDataPath(classFile).toString();
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

        return new Test(testId, classId, gameId, absoluteJavaFile, absoluteClassFile, roundCreated, mutantsKilled,
                playerId, linesCovered, linesUncovered, points);
    }

    /**
     * Returns the {@link Test} for the given test id.
     */
    public static Test getTestById(int testId) throws UncheckedSQLException, SQLMappingException {
        String query = "SELECT * FROM tests WHERE Test_ID = ?;";
        return DB.executeQueryReturnValue(query, TestDAO::testFromRS, DatabaseValue.of(testId));
    }

    /**
     * Returns the {@link Test Tests} from the given game.
     */
    public static List<Test> getTestsForGame(int gameId) throws UncheckedSQLException, SQLMappingException {
        String query = "SELECT * FROM tests WHERE Game_ID = ?;";
        return DB.executeQueryReturnList(query, TestDAO::testFromRS, DatabaseValue.of(gameId));
    }

    public static List<Test> getTestsForGameAndUser(int gameId, int userId)
            throws UncheckedSQLException, SQLMappingException {
        String query = String.join("\n",
                "SELECT * FROM tests ",
                "LEFT JOIN players ON players.ID = tests.Player_ID ",
                "LEFT JOIN users ON players.User_ID = users.User_ID ",
                "WHERE tests.Game_ID = ?",
                "  AND players.User_ID = ?",
                ";");
        return DB.executeQueryReturnList(query,
                TestDAO::testFromRS,
                DatabaseValue.of(gameId),
                DatabaseValue.of(userId));
    }


    /**
     * Returns the {@link Test Tests} from the given game for the given player.
     */
    public static List<Test> getTestsForGameAndPlayer(int gameId, int playerId)
            throws UncheckedSQLException, SQLMappingException {
        String query = String.join("\n",
                "SELECT * FROM tests ",
                "LEFT JOIN players ON players.ID = tests.Player_ID ",
                "LEFT JOIN users ON players.User_ID = users.User_ID ",
                "WHERE tests.Game_ID = ?",
                "  AND tests.Player_ID = ?",
                ";");
        return DB.executeQueryReturnList(query, TestDAO::testFromRS,
                DatabaseValue.of(gameId), DatabaseValue.of(playerId));
    }

    /**
     * Returns the valid {@link Test Tests} from the given game.
     * Valid tests are compilable and do not fail when executed against the original class.
     *
     * @param gameId        the identifier of the given game.
     * @param defendersOnly If {@code true}, only return tests that were written by defenders.
     *                      Include also the tests uploaded by the System Defender
     * @return a {@link List} of valid tests for the given game.
     */
    public static List<Test> getValidTestsForGame(int gameId, boolean defendersOnly)
            throws UncheckedSQLException, SQLMappingException {
        List<Test> result = new ArrayList<>();

        String query = String.join("\n",
                "SELECT t.*",
                "FROM view_valid_tests t",
                (defendersOnly ? "INNER JOIN players pl on t.Player_ID = pl.ID" : ""),
                "WHERE t.Game_ID=?",
                (defendersOnly ? "AND (pl.Role='DEFENDER' OR pl.Role='PLAYER');" : ";")
        );
        result.addAll(DB.executeQueryReturnList(query, TestDAO::testFromRS, DatabaseValue.of(gameId)));

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
                DatabaseValue.of(gameId),
                DatabaseValue.of(DUMMY_DEFENDER_USER_ID)
        };

        result.addAll(DB.executeQueryReturnList(systemDefenderQuery, TestDAO::testFromRS, values));

        return result;
    }

    public static List<Test> getValidTestsForGameSubmittedAfterMutant(int gameId, boolean defendersOnly,
            Mutant aliveMutant) {
        /*
         * ATM, we do not consider system generated tests, as they will be
         * automatically ruled out by the timestamp Unless, we allow new system
         * tests to be submitted also after the game started (#102)
         */
        // TODO Not sure if using table 'mutants' here is correct or we
        // need to use some view instead...
        String query = String.join("\n",
                "SELECT t.*",
                "FROM view_valid_tests t",
                (defendersOnly ? "INNER JOIN players pl on t.Player_ID = pl.ID" : ""),
                "WHERE t.Timestamp >= (select mutants.Timestamp from mutants where mutants.Mutant_ID = ? )",
                "  AND t.Game_ID=? ",
                (defendersOnly ? "AND pl.Role='DEFENDER';" : ";"));
        return DB.executeQueryReturnList(query, TestDAO::testFromRS,
                DatabaseValue.of(aliveMutant.getId()),
                DatabaseValue.of(gameId));
    }

    /**
     * Returns the valid {@link Test Tests} from the games played on the given class.
     * Valid tests are compilable and do not fail when executed against the original class.
     *
     * <p>Include also the tests from the System Defender
     *
     * @param classId the identifier of the given class.
     * @return a {@link List} of valid tests for the given class.
     */
    public static List<Test> getValidTestsForClass(int classId) throws UncheckedSQLException, SQLMappingException {
        List<Test> result = new ArrayList<>();

        String query = String.join("\n",
                "SELECT t.*",
                "FROM view_valid_tests t, games",
                "WHERE t.Game_ID = games.ID",
                "  AND games.Class_ID = ?;"
        );
        result.addAll(DB.executeQueryReturnList(query, TestDAO::testFromRS, DatabaseValue.of(classId)));

        // Include also those tests uploaded, i.e, player_id = -1
        String systemDefenderQuery = String.join("\n",
                "SELECT tests.*",
                "FROM tests, test_uploaded_with_class up",
                "WHERE tests.Test_ID = up.Test_ID",
                "  AND up.Class_ID = ?",
                "  AND tests.ClassFile IS NOT NULL;"
        );

        result.addAll(DB.executeQueryReturnList(systemDefenderQuery, TestDAO::testFromRS, DatabaseValue.of(classId)));

        return result;
    }

    /**
     * Stores a given {@link Test} in the database.
     *
     * <p>This method does not update the given test object.
     * Use {@link Test#insert()} instead.
     *
     * @param test the given test as a {@link Test}.
     * @return the generated identifier of the test as an {@code int}.
     * @throws UncheckedSQLException If storing the test was not successful.
     */
    public static int storeTest(Test test) throws UncheckedSQLException {
        String relativeJavaFile = FileUtils.getRelativeDataPath(test.getJavaFile()).toString();
        String relativeClassFile =
                test.getClassFile() == null ? null : FileUtils.getRelativeDataPath(test.getClassFile()).toString();
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
            linesCovered = lineCoverage.getLinesCovered()
                    .stream()
                    .sorted()
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
            linesUncovered = lineCoverage.getLinesUncovered()
                    .stream()
                    .sorted()
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
        }

        String query = String.join("\n",
                "INSERT INTO tests (JavaFile, ClassFile, Game_ID, RoundCreated, MutantsKilled, Player_ID,",
                "Points, Class_ID, Lines_Covered, Lines_Uncovered)",
                "VALUES (?,?,?,?,?,?,?,?,?,?);");
        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(relativeJavaFile),
                DatabaseValue.of(relativeClassFile),
                DatabaseValue.of(gameId),
                DatabaseValue.of(roundCreated),
                DatabaseValue.of(mutantsKilled),
                DatabaseValue.of(playerId),
                DatabaseValue.of(score),
                DatabaseValue.of(classId),
                DatabaseValue.of(linesCovered),
                DatabaseValue.of(linesUncovered)
        };

        final int result = DB.executeUpdateQueryGetKeys(query, values);
        if (result != -1) {
            return result;
        } else {
            throw new UncheckedSQLException("Could not store test to database.");
        }
    }

    /**
     * Updates a given {@link Test} in the database and returns whether
     * updating was successful or not.
     *
     * @param test the given test as a {@link Test}.
     * @return whether updating was successful or not
     * @throws UncheckedSQLException If storing the test was not successful.
     */
    public static boolean updateTest(Test test) throws UncheckedSQLException {
        final int testId = test.getId();
        final int mutantsKilled = test.getMutantsKilled();
        final int score = test.getScore();

        String linesCoveredString = "";
        String linesUncoveredString = "";

        LineCoverage lineCoverage = test.getLineCoverage();
        if (lineCoverage != null) {
            linesCoveredString = lineCoverage.getLinesCovered()
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
            linesUncoveredString = lineCoverage.getLinesUncovered()
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
        }


        String query = "UPDATE tests SET mutantsKilled=?,Lines_Covered=?,Lines_Uncovered=?,Points=? WHERE Test_ID=?;";
        DatabaseValue[] values = new DatabaseValue[]{
            DatabaseValue.of(mutantsKilled),
            DatabaseValue.of(linesCoveredString),
            DatabaseValue.of(linesUncoveredString),
            DatabaseValue.of(score),
            DatabaseValue.of(testId)
        };

        return DB.executeUpdateQuery(query, values);
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
                DatabaseValue.of(testId),
                DatabaseValue.of(classId)
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

        return DB.executeUpdateQuery(query, DatabaseValue.of(id));
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
        DatabaseValue[] values = tests.stream().map(DatabaseValue::of).toArray(DatabaseValue[]::new);

        return DB.executeUpdateQuery(query, values);
    }

    public static int getKillingTestIdForMutant(int mutantId) {
        String query = String.join("\n",
                "SELECT *",
                "FROM targetexecutions",
                "WHERE Target = ?",
                "  AND Status != ?",
                "  AND Mutant_ID = ?;"
        );
        DatabaseValue<?>[] values = new DatabaseValue[]{
                DatabaseValue.of(TargetExecution.Target.TEST_MUTANT.name()),
                DatabaseValue.of(TargetExecution.Status.SUCCESS.name()),
                DatabaseValue.of(mutantId)
        };
        TargetExecution targ = DB.executeQueryReturnValue(query, TargetExecutionDAO::targetExecutionFromRS, values);
        // TODO: We shouldn't give away that we don't know which test killed the mutant?
        return Optional.ofNullable(targ).map(t -> t.testId).orElse(-1);
    }

    public static Test getKillingTestForMutantId(int mutantId) {
        int testId = getKillingTestIdForMutant(mutantId);
        if (testId == -1) {
            return null;
        } else {
            return getTestById(testId);
        }
    }

    public static Set<Mutant> getKilledMutantsForTestId(int testId) {
        String query = String.join("\n",
                "SELECT DISTINCT m.*",
                "FROM targetexecutions te, mutants m",
                "WHERE te.Target = ?",
                "  AND te.Status != ?",
                "  AND te.Test_ID = ?",
                "  AND te.Mutant_ID = m.Mutant_ID",
                "ORDER BY m.Mutant_ID ASC");
        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(TargetExecution.Target.TEST_MUTANT.name()),
                DatabaseValue.of(TargetExecution.Status.SUCCESS.name()),
                DatabaseValue.of(testId)
        };
        final List<Mutant> mutants = DB.executeQueryReturnList(query, MutantDAO::mutantFromRS, values);
        return new HashSet<>(mutants);
    }
}
