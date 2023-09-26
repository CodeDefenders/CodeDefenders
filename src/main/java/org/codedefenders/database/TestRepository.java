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
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.database.DB.RSMapper;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.transaction.Transactional;
import org.codedefenders.util.FileUtils;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import static org.codedefenders.persistence.database.util.ResultSetUtils.listFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.nextFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.oneFromRS;

/**
 * This class handles the database logic for tests.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 * @see Test
 */
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
     * Constructs a test from a {@link ResultSet} entry.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed test.
     * @see RSMapper
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
        try {
            return queryRunner.query(query, oneFromRS(TestRepository::testFromRS), testId).orElse(null);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Returns the {@link Test Tests} from the given game.
     */
    public List<Test> getTestsForGame(int gameId) throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = "SELECT * FROM tests WHERE Game_ID = ?;";
        try {
            return queryRunner.query(query, listFromRS(TestRepository::testFromRS), gameId);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public List<Test> getTestsForGameAndUser(int gameId, int userId)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT * FROM tests
                LEFT JOIN players ON players.ID = tests.Player_ID
                LEFT JOIN users ON players.User_ID = users.User_ID
                WHERE tests.Game_ID = ?
                  AND players.User_ID = ?;
        """;
        try {
            return queryRunner.query(query, listFromRS(TestRepository::testFromRS), gameId, userId);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }


    /**
     * Returns the {@link Test Tests} from the given game for the given player.
     */
    public List<Test> getTestsForGameAndPlayer(int gameId, int playerId)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT * FROM view_valid_game_tests tests
                LEFT JOIN players ON players.ID = tests.Player_ID
                LEFT JOIN users ON players.User_ID = users.User_ID
                WHERE tests.Game_ID = ?
                  AND tests.Player_ID = ?;
        """;
        try {
            return queryRunner.query(query, listFromRS(TestRepository::testFromRS), gameId, playerId);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Returns the valid {@link Test Tests} from the given game.
     * Valid tests are compilable and do not fail when executed against the original class.
     *
     * <p>This includes valid user-submitted mutants as well as instances of predefined mutants in the game.
     *
     * @param gameId        the identifier of the given game.
     * @param defendersOnly If {@code true}, only return tests that were written by defenders.
     *                      Include also the tests uploaded by the System Defender
     * @return a {@link List} of valid tests for the given game.
     */
    public List<Test> getValidTestsForGame(int gameId, boolean defendersOnly)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT t.*
                FROM view_valid_game_tests t
                %s
                WHERE t.Game_ID = ?
                %s
        """.formatted(
                defendersOnly ? "INNER JOIN players pl on t.Player_ID = pl.ID" : "",
                defendersOnly ? "AND (pl.Role = 'DEFENDER' OR pl.Role = 'PLAYER');" : ";"
        );
        try {
            return queryRunner.query(query, listFromRS(TestRepository::testFromRS), gameId);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public List<Test> getValidTestsForGameSubmittedAfterMutant(int gameId, boolean defendersOnly,
            Mutant aliveMutant) {
        /*
         * ATM, we do not consider system generated tests, as they will be
         * automatically ruled out by the timestamp Unless, we allow new system
         * tests to be submitted also after the game started (#102)
         */
        // TODO Not sure if using table 'mutants' here is correct or we
        // need to use some view instead...
        @Language("SQL") String query = """
                SELECT t.*
                FROM view_valid_game_tests t
                %s,
                WHERE t.Timestamp >= (select mutants.Timestamp from mutants where mutants.Mutant_ID = ? )
                  AND t.Game_ID=?
                %s
        """.formatted(
                defendersOnly ? "INNER JOIN players pl on t.Player_ID = pl.ID" : "",
                defendersOnly ? "AND pl.Role='DEFENDER';" : ";"
        );
        try {
            return queryRunner.query(query, listFromRS(TestRepository::testFromRS), aliveMutant.getId(), gameId);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Returns the valid {@link Test Tests} from the games played on the given class.
     * Valid tests are compilable and do not fail when executed against the original class.
     *
     * <p>This includes valid user-submitted mutants as well as templates of predefined mutants
     * (not the instances that are copied into games).
     *
     * @param classId the identifier of the given class.
     * @return a {@link List} of valid tests for the given class.
     */
    public List<Test> getValidTestsForClass(int classId) throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                WITH tests_for_class AS
                   (SELECT * FROM view_valid_user_tests UNION ALL SELECT * FROM view_system_test_templates)

                SELECT *
                FROM tests_for_class tests
                WHERE tests.Class_ID = ?;
        """;
        try {
            return queryRunner.query(query, listFromRS(TestRepository::testFromRS), classId);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Returns the valid {@link Test Tests} from the games played on the given class.
     *
     * <p>This includes valid user-submitted tests from classroom games as well as templates of predefined tests
     * for classes used in the classroom.
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

        List<Test> tests;
        try {
            tests = queryRunner.query(query, listFromRS(TestRepository::testFromRS), classroomId, classroomId);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }

        Multimap<Integer, Test> testsMap = ArrayListMultimap.create();
        for (Test test : tests) {
            testsMap.put(test.getClassId(), test);
        }
        return testsMap;
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

        try {
            return queryRunner.insert(query,
                    oneFromRS(rs -> rs.getInt(1)),
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
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Updates a given {@link Test} in the database and returns whether
     * updating was successful or not.
     *
     * @param test the given test as a {@link Test}.
     * @throws UncheckedSQLException If storing the test was not successful.
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
        try {
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
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Stores a mapping between a {@link Test} and a {@link GameClass} in the database.
     *
     * @param testId  the identifier of the test.
     * @param classId the identifier of the class.
     */
    public void mapTestToClass(int testId, int classId) {
        @Language("SQL") String query = """
                INSERT INTO test_uploaded_with_class (Test_ID, Class_ID)
                VALUES (?, ?);
        """;

        try {
            queryRunner.insert(query, rs -> null,
                    testId,
                    classId
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Removes a test for a given identifier.
     *
     * @param id the identifier of the test to be removed.
     */
    public void removeTestForId(Integer id) {
        @Language("SQL") String query = """
                DELETE FROM tests WHERE Test_ID = ?;
                DELETE FROM test_uploaded_with_class WHERE Test_ID = ?;
        """;

        try {
            queryRunner.update(query, id, id);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
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

        String range = Stream.generate(() -> "?")
                .limit(tests.size())
                .collect(Collectors.joining(","));

        @Language("SQL") String query = """
                DELETE FROM tests
                WHERE Test_ID in (%s);

                DELETE FROM test_uploaded_with_class
                WHERE Test_ID in (%s);
        """.formatted(
                range,
                range
        );

        // Hack to make sure all values are listed in both 'ranges'.
        tests.addAll(new LinkedList<>(tests));

        try {
            queryRunner.update(query, tests.toArray());
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Returns the id of the first Test (from the same game) that killed the mutant with the provided ID.
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

        Optional<TargetExecution> targ;
        try {
            targ = queryRunner.query(query, nextFromRS(TargetExecutionDAO::targetExecutionFromRS),
                    TargetExecution.Target.TEST_MUTANT.name(),
                    TargetExecution.Status.SUCCESS.name(),
                    mutantId
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }

        // TODO: We shouldn't give away that we don't know which test killed the mutant?
        return targ.map(t -> t.testId).orElse(-1);
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
        try {
            List<Mutant> mutants = queryRunner.query(query, listFromRS(MutantRepository::mutantFromRS),
                    TargetExecution.Target.TEST_MUTANT.name(),
                    TargetExecution.Status.SUCCESS.name(),
                    testId,
                    testId
            );
            return new HashSet<>(mutants);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public void incrementTestScore(int testId, int score) {
        if (score == 0) {
            // Why this is happening?
            // Phil: ^ because the calculated score for this test so far is zero (e.g. no mutants in a game yet)
            logger.warn("Do not increment score for test {} when score is zero", testId);
            return;
        }

        @Language("SQL") String query = """
            UPDATE tests
            SET Points = Points + ?
            WHERE Test_ID = ?;
        """;

        try {
            int updatedRows = queryRunner.update(query,
                    score,
                    testId);
            if (updatedRows != 1) {
                throw new UncheckedSQLException("Couldn't update test.");
            }
            logger.info("Increment score for {} by {}.", testId, score);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void killMutant(Test test) {
        // TODO Phil 06/08/19: Why isn't the out-commented code called?
        // mutantsKilled++;
        // update();
        logger.info("Test {} killed a new mutant", test.getId());

        @Language("SQL") String query = """
            UPDATE tests
            SET MutantsKilled = MutantsKilled + ?
            WHERE Test_ID = ?;
        """;

        try {
            int updatedRows = queryRunner.update(query, 1, test.getId());
            if (updatedRows != 1) {
                throw new UncheckedSQLException("Couldn't update test.");
            }

            // Eventually update the kill count from the DB
            int mutantsKilled = getTestById(test.getId()).getMutantsKilled();
            test.setMutantsKilled(mutantsKilled);

            logger.info("Test {} new killcount is {}.", test, mutantsKilled);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
