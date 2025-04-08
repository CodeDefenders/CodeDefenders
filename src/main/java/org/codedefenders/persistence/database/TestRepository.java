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
package org.codedefenders.persistence.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.database.SQLMappingException;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.transaction.Transactional;
import org.codedefenders.util.FileUtils;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import static org.codedefenders.persistence.database.util.QueryUtils.batchParamsFromList;
import static org.codedefenders.persistence.database.util.ResultSetUtils.generatedKeyFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.listFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.nextFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.oneFromRS;

@Transactional
@ApplicationScoped
public class TestRepository {
    private static final Logger logger = LoggerFactory.getLogger(TestRepository.class);

    private final QueryRunner queryRunner;

    @Inject
    public TestRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    /**
     * Constructs a test from a {@link ResultSet} row.
     */
    public static Test testFromRS(ResultSet rs) throws SQLException {
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
            Arrays.stream(linesCoveredString.split(","))
                    .map(Integer::parseInt)
                    .forEach(linesCovered::add);
        }

        List<Integer> linesUncovered = new ArrayList<>();
        if (linesUncoveredString != null && !linesUncoveredString.isEmpty()) {
            Arrays.stream(linesUncoveredString.split(","))
                    .map(Integer::parseInt)
                    .forEach(linesUncovered::add);
        }

        return new Test(testId, classId, gameId, absoluteJavaFile, absoluteClassFile, roundCreated, mutantsKilled,
                playerId, linesCovered, linesUncovered, points);
    }

    /**
     * Returns the {@link Test} for the given test id.
     */
    public Test getTestById(int testId) throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = "SELECT * FROM tests WHERE Test_ID = ?;";
        return queryRunner.query(query, oneFromRS(TestRepository::testFromRS), testId).orElse(null);
    }

    /**
     * Returns the {@link Test Tests} from the given game.
     */
    @Deprecated
    public List<Test> getTestsForGame(int gameId) throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = "SELECT * FROM tests WHERE Game_ID = ?;";
        return queryRunner.query(query, listFromRS(TestRepository::testFromRS), gameId);
    }

    public boolean gameHasPredefinedTests(int gameId) {
        @Language("SQL") String query = """
                SELECT * FROM view_system_test_instances tests
                WHERE tests.Game_ID = ?
                LIMIT 1;
        """;
        return queryRunner.query(query, nextFromRS(rs -> true), gameId).orElse(false);
    }

    public Test getLatestTestForGameAndUser(int gameId, int userId)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                        SELECT * FROM tests
                        LEFT JOIN players ON players.ID = tests.Player_ID
                        LEFT JOIN users ON players.User_ID = users.User_ID
                        WHERE tests.Game_ID = ?
                          AND players.User_ID = ?
                        ORDER BY tests.Timestamp DESC
                        LIMIT 1;
                """;
        return queryRunner.query(query, nextFromRS(TestRepository::testFromRS), gameId, userId).orElse(null);
    }

    /**
     * Returns the {@link Test Tests} from the given game for the given player.
     */
    public List<Test> getTestsForGameAndPlayer(int gameId, int playerId)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT * FROM view_valid_game_tests tests
                WHERE tests.Game_ID = ?
                  AND tests.Player_ID = ?;
        """;
        return queryRunner.query(query, listFromRS(TestRepository::testFromRS), gameId, playerId);
    }

    /**
     * Returns the {@link Test Tests} from the given game for the given user.
     */
    public List<Test> getTestsForGameAndUser(int gameId, int userId)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT * FROM view_valid_game_tests tests
                WHERE tests.Game_ID = ?
                  AND tests.User_ID = ?;
        """;
        return queryRunner.query(query, listFromRS(TestRepository::testFromRS), gameId, userId);
    }

    /**
     * Returns the valid {@link Test Tests} from the given game.
     * Valid tests are compilable and do not fail when executed against the original class.
     *
     * <p>This includes valid user-submitted tests as well as instances of predefined tests in the game.
     */
    public List<Test> getValidTestsForGame(int gameId) {
        @Language("SQL") String query = """
                SELECT tests.*
                FROM view_valid_game_tests tests
                WHERE tests.Game_ID = ?;
        """;
        return queryRunner.query(query, listFromRS(TestRepository::testFromRS), gameId);
    }

    /**
     * Returns the defender-written valid {@link Test Tests} from the given game.
     * Valid tests are compilable and do not fail when executed against the original class.
     *
     * <p>This includes valid user-submitted tests as well as instances of predefined tests in the game.
     *
     * <p>Compared to {@link #getValidTestsForGame(int)}, this method does not return tests created by attackers for
     * equivalence duels in battleground games.
     * It does return all tests created by players in melee games, though.
     */
    public List<Test> getValidDefenderTestsForGame(int gameId) {
        @Language("SQL") String query = """
                SELECT tests.*
                FROM view_valid_game_tests tests
                INNER JOIN players on tests.Player_ID = players.ID
                WHERE tests.Game_ID = ?
                  AND players.Role IN (?, ?);
        """;
        return queryRunner.query(query, listFromRS(TestRepository::testFromRS),
                gameId,
                Role.DEFENDER.name(),
                Role.PLAYER.name());
    }

    public List<Test> getValidTestsForGameSubmittedAfterMutant(int gameId, Mutant aliveMutant) {
        // ATM, we do not consider system generated tests.
        @Language("SQL") String query = """
                SELECT tests.*
                FROM view_valid_game_tests tests
                WHERE tests.Timestamp >= (SELECT mutants.Timestamp FROM mutants WHERE mutants.Mutant_ID = ?)
                  AND tests.Game_ID = ?;
        """;
        return queryRunner.query(query, listFromRS(TestRepository::testFromRS), aliveMutant.getId(), gameId);
    }

    /**
     * Returns the valid {@link Test Tests} from the games played on the given class.
     * Valid tests are compilable and do not fail when executed against the original class.
     *
     * <p>This includes valid user-submitted mutants as well as templates of predefined mutants
     * (not the instances that are copied into games).
     */
    public List<Test> getValidTestsForClass(int classId) throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                WITH tests_for_class AS
                   (SELECT * FROM view_valid_user_tests UNION ALL SELECT * FROM view_system_test_templates)

                SELECT *
                FROM tests_for_class tests
                WHERE tests.Class_ID = ?;
        """;
        return queryRunner.query(query, listFromRS(TestRepository::testFromRS), classId);
    }

    /**
     * Returns the valid {@link Test Tests} from the games played in the given classroom.
     *
     * <p>This includes valid user-submitted tests from classroom games as well as templates of predefined tests
     * for classes used in the classroom.
     *
     * @return The tests, partitioned by Class ID.
     */
    public Multimap<Integer, Test> getValidTestsForClassroom(int classroomId)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                WITH relevant_classes AS (
                    SELECT DISTINCT games.Class_ID
                    FROM games
                    WHERE games.Classroom_ID = ?
                ),

                classroom_system_tests AS (
                    SELECT tests.*
                    FROM view_system_test_templates tests
                    WHERE tests.Class_ID IN (SELECT * FROM relevant_classes)
                ),

                classroom_user_tests AS (
                    SELECT tests.*
                    FROM view_valid_user_tests tests, games
                    WHERE tests.Game_ID = games.ID
                    AND games.Classroom_ID = ?
                )

                SELECT * FROM classroom_system_tests
                UNION ALL
                SELECT * FROM classroom_user_tests;
        """;

        var tests = queryRunner.query(query, listFromRS(TestRepository::testFromRS), classroomId, classroomId);

        Multimap<Integer, Test> testsMap = ArrayListMultimap.create();
        for (Test test : tests) {
            testsMap.put(test.getClassId(), test);
        }
        return testsMap;
    }

    /**
     * Stores a given {@link Test} in the database.
     *
     * @param test The test to store.
     * @return The generated test ID.
     */
    public int storeTest(Test test) throws UncheckedSQLException {
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

        @Language("SQL") String query = """
                INSERT INTO tests (
                    JavaFile,
                    ClassFile,
                    Game_ID,
                    RoundCreated,
                    MutantsKilled,
                    Player_ID,
                    Points,
                    Class_ID,
                    Lines_Covered,
                    Lines_Uncovered
                )
                VALUES (?,?,?,?,?,?,?,?,?,?);
        """;

        return queryRunner.insert(query,
                generatedKeyFromRS(),
                relativeJavaFile,
                relativeClassFile,
                gameId,
                roundCreated,
                mutantsKilled,
                playerId,
                score,
                classId,
                linesCovered,
                linesUncovered
        ).orElseThrow(() -> new UncheckedSQLException("Couldn't store test."));
    }

    /**
     * Updates a given {@link Test} in the database.
     *
     * @param test The test to update.
     */
    public void updateTest(Test test) throws UncheckedSQLException {
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


        @Language("SQL") String query = """
            UPDATE tests
            SET mutantsKilled = ?,
                Lines_Covered = ?,
                Lines_Uncovered = ?,
                Points = ?
            WHERE Test_ID = ?;
        """;
        int updatedRows = queryRunner.update(query,
                mutantsKilled,
                linesCoveredString,
                linesUncoveredString,
                score,
                testId
        );
        if (updatedRows != 1) {
            throw new UncheckedSQLException("Couldn't update test.");
        }
    }

    /**
     * Stores a mapping between a {@link Test} and a {@link GameClass} in the database.
     *
     * @param testId The test ID.
     * @param classId The class ID.
     */
    public void mapTestToClass(int testId, int classId) {
        @Language("SQL") String query = """
                INSERT INTO test_uploaded_with_class (Test_ID, Class_ID)
                VALUES (?, ?);
        """;

        queryRunner.insert(query, rs -> null,
                testId,
                classId
        );
    }

    /**
     * Removes a test for a given ID.
     */
    public void removeTestForId(int testId) {
        @Language("SQL") String query1 = "DELETE FROM tests WHERE Test_ID = ?;";
        @Language("SQL") String query2 = "DELETE FROM test_uploaded_with_class WHERE Test_ID = ?;";

        queryRunner.update(query1, testId);
        queryRunner.update(query2, testId);
    }

    /**
     * Removes multiple tests for a given list of identifiers.
     *
     * @param tests the identifiers of the tests to be removed.
     */
    public void removeTestsForIds(List<Integer> tests) {
        if (tests.isEmpty()) {
            return;
        }

        @Language("SQL") String query1 = "DELETE FROM test_uploaded_with_class WHERE Test_ID = ?;";
        @Language("SQL") String query2 = "DELETE FROM tests WHERE Test_ID = ?;";

        var params = batchParamsFromList(tests);

        queryRunner.batch(query1, params);
        queryRunner.batch(query2, params);
    }

    /**
     * Returns the ID of the first Test (from the same game) that killed the mutant with the provided ID.
     */
    public int getKillingTestIdForMutant(int mutantId) {
        @Language("SQL") String query = """
                SELECT te.*
                FROM targetexecutions te
                JOIN mutants m on m.Mutant_ID = te.Mutant_ID
                JOIN tests t on te.Test_ID = t.Test_ID
                WHERE te.Target = ?
                  AND te.Status != ?
                  AND t.Game_ID = m.Game_ID
                  AND te.Mutant_ID = ?
                ORDER BY te.TargetExecution_ID LIMIT 1;
        """;

        var targ = queryRunner.query(query, nextFromRS(TargetExecutionDAO::targetExecutionFromRS),
                TargetExecution.Target.TEST_MUTANT.name(),
                TargetExecution.Status.SUCCESS.name(),
                mutantId
        );

        // TODO: We shouldn't give away that we don't know which test killed the mutant?
        return targ.map(t -> t.testId).orElse(-1);
    }

    public Optional<String> findKillMessageForMutant(int mutantId) {
        @Language("SQL") String query = """
                SELECT te.*
                FROM targetexecutions te
                JOIN mutants m on m.Mutant_ID = te.Mutant_ID
                JOIN tests t on te.Test_ID = t.Test_ID
                WHERE te.Target = ?
                  AND te.Status != ?
                  AND te.Mutant_ID = ?
                ORDER BY te.TargetExecution_ID LIMIT 1;
        """;

        var targetExecution = queryRunner.query(query, nextFromRS(TargetExecutionDAO::targetExecutionFromRS),
                TargetExecution.Target.TEST_MUTANT.name(),
                TargetExecution.Status.SUCCESS.name(),
                mutantId
        );

        return targetExecution.map(t -> t.message);
    }

    /**
     * Returns the first Test (from the same game) that killed the mutant with the provided ID.
     */
    public Test getKillingTestForMutantId(int mutantId) {
        int testId = getKillingTestIdForMutant(mutantId);
        if (testId == -1) {
            return null;
        } else {
            return getTestById(testId);
        }
    }

    /**
     * Returns the Mutants (from the same game) that got killed by the Test with the provided ID.
     */
    public Set<Mutant> getKilledMutantsForTestId(int testId) {
        @Language("SQL") String query = """
                SELECT * FROM (
                    SELECT TargetExecution_ID,
                           te.Test_ID,
                           m.*,
                           RANK() over (PARTITION BY te.Mutant_ID ORDER BY TargetExecution_ID) AS ranks
                    FROM targetexecutions te
                    JOIN mutants m on m.Mutant_ID = te.Mutant_ID
                    JOIN tests t on te.Test_ID = t.Test_ID
                    WHERE te.Target = ?
                      AND te.Status != ?
                      AND t.Game_ID = m.Game_ID
                      AND t.Game_ID = (
                          SELECT Game_ID
                          FROM tests
                          WHERE Test_ID = ?
                      )
                    ORDER BY Mutant_ID, TargetExecution_ID
                ) tmp
                WHERE Test_ID = ? AND ranks = 1;
        """;

        List<Mutant> mutants = queryRunner.query(query, listFromRS(MutantRepository::mutantFromRS),
                TargetExecution.Target.TEST_MUTANT.name(),
                TargetExecution.Status.SUCCESS.name(),
                testId,
                testId
        );
        return new HashSet<>(mutants);
    }

    public void incrementTestScore(int testId, int score) {
        if (score == 0) {
            logger.warn("Do not increment score for test {} when score is zero", testId);
            return;
        }

        @Language("SQL") String query = """
            UPDATE tests
            SET Points = Points + ?
            WHERE Test_ID = ?;
        """;

        int updatedRows = queryRunner.update(query,
                score,
                testId);
        if (updatedRows != 1) {
            throw new UncheckedSQLException("Couldn't update test.");
        }
        logger.info("Increment score for {} by {}.", testId, score);
    }

    public void incrementMutantsKilled(Test test) {
        logger.info("Test {} killed a new mutant", test.getId());

        @Language("SQL") String query = """
            UPDATE tests
            SET MutantsKilled = MutantsKilled + ?
            WHERE Test_ID = ?;
        """;

        int updatedRows = queryRunner.update(query, 1, test.getId());
        if (updatedRows != 1) {
            throw new UncheckedSQLException("Couldn't update test.");
        }

        // Eventually update the kill count from the DB
        int mutantsKilled = getTestById(test.getId()).getMutantsKilled();
        test.setMutantsKilled(mutantsKilled);

        logger.info("Test {} new kill count is {}.", test, mutantsKilled);
    }

    // TODO: Move this into a service
    public Set<Test> getCoveringTestsForMutant(Mutant mutant) {
        List<Test> tests = getValidTestsForGame(mutant.getGameId());
        return mutant.getCoveringTests(tests);
    }
}
