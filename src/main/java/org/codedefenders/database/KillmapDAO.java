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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.codedefenders.database.DB.RSMapper;
import org.codedefenders.execution.KillMap;
import org.codedefenders.execution.KillMap.KillMapEntry;
import org.codedefenders.execution.KillMap.KillMapJob;
import org.codedefenders.execution.KillMap.KillMapType;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.model.Classroom;
import org.codedefenders.service.ClassroomService;
import org.codedefenders.util.CDIUtil;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;

/**
 * This class handles the database logic for killmaps.
 *
 * @see KillMap
 */
public class KillmapDAO {
    private static final Logger logger = LoggerFactory.getLogger(KillmapDAO.class);

    /**
     * Helper method to retrieve killmap entries from the database.
     */
    private static List<KillMapEntry> getKillMapEntries(List<Test> tests, List<Mutant> mutants,
                                                        @Language("SQL") String query, DatabaseValue<?>... values)
            throws UncheckedSQLException, SQLMappingException {
        /* Set up mapping from test id to test / mutant id to mutant. */
        Map<Integer, Test> testMap = tests.stream().collect(Collectors.toMap(Test::getId, t -> t));
        Map<Integer, Mutant> mutantMap = mutants.stream().collect(Collectors.toMap(Mutant::getId, m -> m));

        final RSMapper<KillMapEntry> mapper = rs -> {
            int testId = rs.getInt("Test_ID");
            int mutantId = rs.getInt("Mutant_ID");
            String status = rs.getString("Status");
            return new KillMapEntry(testMap.get(testId), mutantMap.get(mutantId), KillMapEntry.Status.valueOf(status));
        };
        return DB.executeQueryReturnListWithFetchSize(query, Integer.MIN_VALUE, mapper, values);
    }

    /**
     * Returns the killmap entries for the given game.
     *
     * <p>The killmap encompasses all mutants and tests as they appear in game.
     * This means the instances of predefined mutants and tests are used iff they are used in game.
     */
    public static List<KillMapEntry> getKillMapEntriesForGame(int gameId) {
        @Language("SQL") String query = """
                SELECT killmap.*
                FROM killmap
                WHERE killmap.Game_ID = ?
        """;

        List<Test> tests = TestDAO.getValidTestsForGame(gameId, false);
        List<Mutant> mutants = MutantDAO.getValidMutantsForGame(gameId);

        return getKillMapEntries(tests, mutants, query, DatabaseValue.of(gameId));
    }

    /**
     * Returns the killmap entries for the given class.
     *
     * <p>The killmap encompasses all valid user-submitted mutants and tests,
     * as well as the templates of predefined mutants and tests (not the instances that are copied into games).
     */
    public static List<KillMapEntry> getKillMapEntriesForClass(int classId) {
        @Language("SQL") String query = """
                WITH tests_for_class AS
                   (SELECT * FROM view_valid_user_tests UNION ALL SELECT * FROM view_system_test_templates),
                mutants_for_class AS
                   (SELECT * FROM view_valid_user_mutants UNION ALL SELECT * FROM view_system_mutant_templates)

                SELECT killmap.*
                FROM killmap,
                     tests_for_class tests,
                     mutants_for_class mutants
                WHERE killmap.Class_ID = ?
                  AND killmap.Test_ID = tests.Test_ID
                  AND killmap.Mutant_ID = mutants.Mutant_ID;
        """;

        List<Test> tests = TestDAO.getValidTestsForClass(classId);
        List<Mutant> mutants = MutantDAO.getValidMutantsForClass(classId);

        return getKillMapEntries(tests, mutants, query, DatabaseValue.of(classId));
    }

    /**
     * Returns the killmap entries for the given classroom.
     *
     * <p>The killmap encompasses all valid user-submitted mutants and tests from the classroom,
     * as well as the templates of predefined mutants and tests of used classes.
     */
    public static List<KillMapEntry> getKillMapEntriesForClassroom(int classroomId) {
        @Language("SQL") String query = """
                WITH relevant_classes AS (
                    SELECT DISTINCT games.Class_ID
                    FROM games
                    WHERE games.Classroom_ID = ?
                ),
                classroom_system_mutants AS (
                    SELECT mutants.*
                    FROM view_system_mutant_templates mutants
                    WHERE mutants.Class_ID IN (SELECT * FROM relevant_classes)
                ),
                classroom_user_mutants AS (
                    SELECT mutants.*
                    FROM view_valid_user_mutants mutants, games
                    WHERE mutants.Game_ID = games.ID
                      AND games.Classroom_ID = ?
                ),
                mutants_for_classroom AS (
                    SELECT * FROM classroom_system_mutants
                    UNION ALL
                    SELECT * FROM classroom_user_mutants
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
                ),
                tests_for_classroom AS (
                    SELECT * FROM classroom_system_tests
                    UNION ALL
                    SELECT * FROM classroom_user_tests
                )

                SELECT killmap.*
                FROM killmap,
                   tests_for_classroom tests,
                   mutants_for_classroom mutants
                WHERE killmap.Test_ID = tests.Test_ID
                  AND killmap.Mutant_ID = mutants.Mutant_ID
        """;

        List<Test> tests = new ArrayList<>(TestDAO.getValidTestsForClassroom(classroomId).values());
        List<Mutant> mutants = new ArrayList<>(MutantDAO.getValidMutantsForClassroom(classroomId).values());

        return getKillMapEntries(tests, mutants, query,
                DatabaseValue.of(classroomId), DatabaseValue.of(classroomId), DatabaseValue.of(classroomId));
    }

    /**
     * Inserts a killmap entry into the database.
     */
    public static boolean insertKillMapEntry(KillMapEntry entry, int classId) {
        @Language("SQL") String query = """
                INSERT INTO killmap (Class_ID,Game_ID,Test_ID,Mutant_ID,Status)
                VALUES (?,?,?,?,?)
                ON DUPLICATE KEY UPDATE Status = VALUES(Status);
        """;
        int testGameId = entry.test.getGameId();
        int mutantGameId = entry.mutant.getGameId();

        DatabaseValue<?>[] values = new DatabaseValue[]{
                DatabaseValue.of(classId),
                DatabaseValue.of(testGameId == mutantGameId ? testGameId : null),
                DatabaseValue.of(entry.test.getId()),
                DatabaseValue.of(entry.mutant.getId()),
                DatabaseValue.of(entry.status.name()),
        };
        return DB.executeUpdateQuery(query, values);
    }

    /**
     * Inserts many killmap entries into the database.
     */
    public static void insertManyKillMapEntries(List<KillMapEntry> entries, int classId) {
        @Language("SQL") String query = """
                INSERT INTO killmap (Class_ID,Game_ID,Test_ID,Mutant_ID,Status)
                VALUES (?,?,?,?,?)
                ON DUPLICATE KEY UPDATE Status = VALUES(Status);
        """;

        final DB.DBVExtractor<KillMapEntry> dbvExtractor = entry -> {
            int testGameId = entry.test.getGameId();
            int mutantGameId = entry.mutant.getGameId();
            return new DatabaseValue[]{
                DatabaseValue.of(classId),
                DatabaseValue.of(testGameId == mutantGameId ? testGameId : null),
                DatabaseValue.of(entry.test.getId()),
                DatabaseValue.of(entry.mutant.getId()),
                DatabaseValue.of(entry.status.name()),
            };
        };
        DB.executeBatchQueryReturnKeys(query, entries, dbvExtractor);
    }

    public static boolean removeGameKillmap(int gameId) {
        @Language("SQL") String query = """
                DELETE FROM killmap
                WHERE killmap.Game_ID = ?
        """;
        return DB.executeUpdateQuery(query, DatabaseValue.of(gameId));
    }

    public static boolean removeClassKillmap(int classId) {
        List<Test> testsForClass = TestDAO.getValidTestsForClass(classId);
        List<Mutant> mutantsForClass = MutantDAO.getValidMutantsForClass(classId);

        String testIds = testsForClass.stream()
                .map(Test::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String mutantIds = mutantsForClass.stream()
                .map(Mutant::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        @Language("SQL") String query = """
                DELETE FROM killmap
                WHERE killmap.Test_ID IN (%s)
                  AND killmap.Mutant_ID IN (%s);
        """.formatted(
                testIds,
                mutantIds
        );
        return DB.executeUpdateQuery(query);
    }

    public static boolean removeClassroomKillmap(int classroomId) {
        Collection<Test> testsForClass = TestDAO.getValidTestsForClassroom(classroomId).values();
        Collection<Mutant> mutantsForClass = MutantDAO.getValidMutantsForClassroom(classroomId).values();

        String testIds = testsForClass.stream()
                .map(Test::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String mutantIds = mutantsForClass.stream()
                .map(Mutant::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        @Language("SQL") String query = """
                DELETE FROM killmap
                WHERE killmap.Test_ID IN (%s)
                  AND killmap.Mutant_ID IN (%s);
        """.formatted(
                testIds,
                mutantIds
        );
        return DB.executeUpdateQuery(query);
    }

    public static void removeKillmapsByIds(KillMapType killmapType, List<Integer> ids) {
        switch (killmapType) {
            case CLASS:
                ids.forEach(KillmapDAO::removeClassKillmap);
                break;
            case GAME:
                ids.forEach(KillmapDAO::removeGameKillmap);
                break;
            case CLASSROOM:
                ids.forEach(KillmapDAO::removeClassroomKillmap);
                break;
            default:
                logger.warn("Unknown type of Killmap Job!");
        }
    }

    /**
     * Return a list of pending killmap jobs ordered by timestamp.
     */
    public static List<KillMapJob> getPendingJobs() {
        @Language("SQL") String query = """
                SELECT *
                FROM killmapjob
                ORDER BY Timestamp ASC;
        """;

        return DB.executeQueryReturnList(query, rs -> {
            KillMapType type = null;
            Integer id = null;

            int gameId = rs.getInt("Game_ID");
            if (!rs.wasNull()) {
                type = KillMapType.GAME;
                id = gameId;
            }

            int classId = rs.getInt("Class_ID");
            if (!rs.wasNull()) {
                type = KillMapType.CLASS;
                id = classId;
            }

            int classroomId = rs.getInt("Classroom_ID");
            if (!rs.wasNull()) {
                type = KillMapType.CLASSROOM;
                id = classroomId;
            }

            return new KillMapJob(type, id);
        });
    }

    public static boolean enqueueJob(KillMapJob theJob) {
        switch (theJob.getType()) {
            case CLASS:
                return DB.executeUpdateQuery(
                        "REPLACE INTO killmapjob (Class_ID) VALUES (?);",
                        DatabaseValue.of(theJob.getId()));
            case GAME:
                return DB.executeUpdateQuery(
                        "REPLACE INTO killmapjob (Game_ID) VALUES (?);",
                        DatabaseValue.of(theJob.getId()));
            case CLASSROOM:
                return DB.executeUpdateQuery(
                        "REPLACE INTO killmapjob (Classroom_ID) VALUES (?);",
                        DatabaseValue.of(theJob.getId()));
            default:
                logger.warn("Unknown type of Killmap Job!");
                return false;
        }
    }

    public static boolean removeJob(KillMapType type, int id) {
        switch (type) {
            case CLASS:
                return DB.executeUpdateQuery(
                        "DELETE FROM killmapjob WHERE Class_ID = ?;",
                        DatabaseValue.of(id));
            case GAME:
                return DB.executeUpdateQuery(
                        "DELETE FROM killmapjob WHERE Game_ID = ?;",
                        DatabaseValue.of(id));
            case CLASSROOM:
                return DB.executeUpdateQuery(
                        "DELETE FROM killmapjob WHERE Classroom_ID = ?;",
                        DatabaseValue.of(id));
            default:
                logger.warn("Unknown type of Killmap Job!");
                return false;
        }
    }

    public static boolean removeJob(KillMapJob theJob) {
        return removeJob(theJob.getType(), theJob.getId());
    }

    public static boolean removeKillmapJobsByIds(KillMapType jobType, List<Integer> ids) {
        boolean success = true;
        for (int id : ids) {
            success &= removeJob(jobType, id);
        }
        return success;
    }


    /** Returns the killmaps progress for all class killmaps not queued for processing. */
    public static List<KillMapClassProgress> getNonQueuedKillMapClassProgress() {
        /* Use queries that GROUP the entire killmap, tests and mutants tables, under the assumption that
         * most killmaps are not queued for computation and most of the data will be used. */

        @Language("SQL") String classesQuery = """
                SELECT Class_ID, Name, Alias
                FROM view_playable_classes classes
                WHERE NOT EXISTS (
                   SELECT *
                   FROM killmapjob
                   WHERE killmapjob.Class_ID = classes.Class_ID
                )
                ORDER BY Class_ID;
        """;

        @Language("SQL") String nrTestsQuery = """
                WITH tests_for_class AS
                   (SELECT * FROM view_valid_user_tests UNION ALL SELECT * FROM view_system_test_templates)

                SELECT Class_ID, COUNT(Test_ID)
                FROM tests_for_class
                GROUP BY Class_ID;
        """;

        @Language("SQL") String nrMutantsQuery = """
                WITH mutants_for_class AS
                   (SELECT * FROM view_valid_user_mutants UNION ALL SELECT * FROM view_system_mutant_templates)

                SELECT Class_ID, COUNT(Mutant_ID)
                FROM mutants_for_class
                GROUP BY Class_ID;
        """;

        @Language("SQL") String nrEntriesQuery = """
                WITH mutants_for_class AS
                   (SELECT * FROM view_valid_user_mutants UNION ALL SELECT * FROM view_system_mutant_templates),
                tests_for_class AS
                   (SELECT * FROM view_valid_user_tests UNION ALL SELECT * FROM view_system_test_templates)

                SELECT killmap.Class_ID, COUNT(*)
                FROM killmap,
                     tests_for_class tests,
                     mutants_for_class mutants
                WHERE killmap.Test_ID = tests.Test_ID
                  AND killmap.Mutant_ID = mutants.Mutant_ID
                GROUP BY killmap.Class_ID;
        """;

        Map<Integer, Integer> classIdToNrTests = new TreeMap<>();
        Map<Integer, Integer> classIdToNrMutants = new TreeMap<>();
        Map<Integer, Integer> classIdToNrEntries = new TreeMap<>();
        DB.executeQueryReturnList(nrTestsQuery, rs -> classIdToNrTests.put(rs.getInt(1), rs.getInt(2)));
        DB.executeQueryReturnList(nrMutantsQuery, rs -> classIdToNrMutants.put(rs.getInt(1), rs.getInt(2)));
        DB.executeQueryReturnList(nrEntriesQuery, rs -> classIdToNrEntries.put(rs.getInt(1), rs.getInt(2)));

        return DB.executeQueryReturnList(classesQuery, rs -> {
            int classId = rs.getInt("Class_ID");
            String className = rs.getString("Name");
            String classAlias = rs.getString("Alias");
            int nrTests = classIdToNrTests.getOrDefault(classId, 0);
            int nrMutants = classIdToNrMutants.getOrDefault(classId, 0);
            int nrEntries = classIdToNrEntries.getOrDefault(classId, 0);
            int nrExpectedEntries = nrTests * nrMutants;
            return new KillMapClassProgress(
                    nrTests, nrMutants, nrEntries, nrExpectedEntries,
                    classId, className, classAlias
            );
        });
    }

    /** Returns the killmaps progress for all game killmaps not queued for processing. */
    public static List<KillMapGameProgress> getNonQueuedKillMapGameProgress() {
        /* Use queries that GROUP the entire killmap, tests and mutants tables, under the assumption that
         * most killmaps are not queued for computation and most of the data will be used. */

        @Language("SQL") String gamesQuery = """
                SELECT ID, Mode
                FROM games
                WHERE ID >= 0
                  AND (Mode = 'DUEL' OR MODE = 'PARTY' OR MODE = 'MELEE')
                  AND NOT EXISTS (
                     SELECT *
                     FROM killmapjob
                     WHERE killmapjob.Game_ID = games.ID
                  )
                ORDER BY ID;
        """;

        @Language("SQL") String nrTestsQuery = """
                SELECT Game_ID, COUNT(Test_ID)
                FROM view_valid_game_tests
                GROUP BY Game_ID;
        """;

        @Language("SQL") String nrMutantsQuery = """
                SELECT Game_ID, COUNT(Mutant_ID)
                FROM view_valid_game_mutants
                GROUP BY Game_ID;
        """;

        @Language("SQL") String nrEntriesQuery = """
                SELECT Game_ID, COUNT(*)
                FROM killmap
                GROUP BY Game_ID;
        """;

        Map<Integer, Integer> gameIdToNrTests   = new TreeMap<>();
        Map<Integer, Integer> gameIdToNrMutants = new TreeMap<>();
        Map<Integer, Integer> gameIdToNrEntries = new TreeMap<>();
        DB.executeQueryReturnList(nrTestsQuery,   rs -> gameIdToNrTests.put(rs.getInt(1), rs.getInt(2)));
        DB.executeQueryReturnList(nrMutantsQuery, rs -> gameIdToNrMutants.put(rs.getInt(1), rs.getInt(2)));
        DB.executeQueryReturnList(nrEntriesQuery, rs -> gameIdToNrEntries.put(rs.getInt(1), rs.getInt(2)));

        return DB.executeQueryReturnList(gamesQuery, rs -> {
            int gameId = rs.getInt("ID");
            GameMode gameMode = GameMode.valueOf(rs.getString("Mode"));
            int nrTests = gameIdToNrTests.getOrDefault(gameId, 0);
            int nrMutants = gameIdToNrMutants.getOrDefault(gameId, 0);
            int nrEntries = gameIdToNrEntries.getOrDefault(gameId, 0);
            int nrExpectedEntries = nrTests * nrMutants;
            return new KillMapGameProgress(
                    nrTests, nrMutants, nrEntries, nrExpectedEntries,
                    gameId, gameMode
            );
        });
    }

    public static List<KillMapClassroomProgress> getNonQueuedKillMapClassroomProgress() {
        @Language("SQL") String classroomsQuery = """
                SELECT ID
                FROM classrooms
                WHERE NOT EXISTS (
                     SELECT *
                     FROM killmapjob
                     WHERE killmapjob.Classroom_ID = classrooms.ID
                  )
                ORDER BY ID;
        """;

        List<Integer> classroomIds =  DB.executeQueryReturnList(classroomsQuery, rs -> rs.getInt(1));

        return classroomIds.stream()
                .map(KillmapDAO::getKillMapProgressForClassroom)
                .collect(Collectors.toList());
    }

    /** Returns the killmaps progress for all class killmaps queued for processing. */
    public static List<KillMapClassProgress> getQueuedKillMapClassProgress() {
        /* First query the classes that are queued, then query the rest for each class,
           under the assumption that only a small fraction of classes are queued for computation. */

        @Language("SQL") String classesQuery = """
                SELECT classes.Class_ID
                FROM killmapjob, classes
                WHERE killmapjob.Class_ID = classes.Class_ID
                ORDER BY Class_ID;
        """;

        List<Integer> classIds = DB.executeQueryReturnList(classesQuery, rs -> rs.getInt(1));

        return classIds.stream()
                .map(KillmapDAO::getKillMapProgressForClass)
                .collect(Collectors.toList());
    }

    /** Returns the killmaps progress for all game killmaps queued for processing. */
    public static List<KillMapGameProgress> getQueuedKillMapGameProgress() {
        /* First query the games that are queued, then query the rest for each game,
           under the assumption that only a small fraction of classes are queued for computation. */

        @Language("SQL") String gamesQuery = """
                SELECT games.ID
                FROM killmapjob, games
                WHERE killmapjob.Game_ID = games.ID
                ORDER BY Game_ID;
        """;

        List<Integer> gameIds = DB.executeQueryReturnList(gamesQuery, rs -> rs.getInt(1));

        return gameIds.stream()
                .map(KillmapDAO::getKillMapProgressForGame)
                .collect(Collectors.toList());
    }

    public static List<KillMapClassroomProgress> getQueuedKillMapClassroomProgress() {
        @Language("SQL") String classroomsQuery = """
                SELECT classrooms.ID
                FROM killmapjob, classrooms
                WHERE killmapjob.Classroom_ID = classrooms.ID
                ORDER BY ID;
        """;

        List<Integer> classroomIds =  DB.executeQueryReturnList(classroomsQuery, rs -> rs.getInt(1));

        return classroomIds.stream()
                .map(KillmapDAO::getKillMapProgressForClassroom)
                .collect(Collectors.toList());
    }

    /** Returns the number of queued jobs for class killmaps. */
    public static int getNumClassKillmapJobsQueued() {
        @Language("SQL") String query = "SELECT COUNT(DISTINCT Class_ID) from killmapjob WHERE Class_ID IS NOT NULL;";
        return DB.executeQueryReturnValue(query, rs -> rs.getInt(1));
    }


    /** Returns the number of queued jobs for game killmaps. */
    public static int getNumGameKillmapJobsQueued() {
        @Language("SQL") String query = "SELECT COUNT(DISTINCT Game_ID) from killmapjob WHERE Game_ID IS NOT NULL;";
        return DB.executeQueryReturnValue(query, rs -> rs.getInt(1));
    }

    /**
     * Returns the killmap progress for the class with the given id.
     * @param classId The class id.
     * @return The killmap progress for the class.
     */
    public static KillMapClassProgress getKillMapProgressForClass(int classId) {
        @Language("SQL") String nrTestsQuery = """
                WITH tests_for_class AS
                   (SELECT * FROM view_valid_user_tests UNION ALL SELECT * FROM view_system_test_templates)

                SELECT COUNT(Test_ID)
                FROM tests_for_class
                WHERE Class_ID = ?;
        """;


        @Language("SQL") String nrMutantsQuery = """
                WITH mutants_for_class AS
                   (SELECT * FROM view_valid_user_mutants UNION ALL SELECT * FROM view_system_mutant_templates)

                SELECT COUNT(Mutant_ID)
                FROM mutants_for_class
                WHERE Class_ID = ?;
        """;

        @Language("SQL") String nrEntriesQuery = """
                WITH mutants_for_class AS
                   (SELECT * FROM view_valid_user_mutants UNION ALL SELECT * FROM view_system_mutant_templates),
                tests_for_class AS
                   (SELECT * FROM view_valid_user_tests UNION ALL SELECT * FROM view_system_test_templates)

                SELECT COUNT(*)
                FROM killmap,
                     tests_for_class tests,
                     mutants_for_class mutants
                WHERE killmap.Test_ID = tests.Test_ID
                  AND killmap.Mutant_ID = mutants.Mutant_ID
                  AND killmap.Class_ID = ?;
        """;

        int nrTests = DB.executeQueryReturnValue(nrTestsQuery, rs -> rs.getInt(1), DatabaseValue.of(classId));
        int nrMutants = DB.executeQueryReturnValue(nrMutantsQuery, rs -> rs.getInt(1), DatabaseValue.of(classId));
        int nrEntries = DB.executeQueryReturnValue(nrEntriesQuery, rs -> rs.getInt(1), DatabaseValue.of(classId));

        int nrExpectedEntries = nrMutants * nrTests;
        GameClass cut = GameClassDAO.getClassForId(classId);

        return new KillMapClassProgress(
                nrTests, nrMutants, nrEntries, nrExpectedEntries,
                classId, cut.getName(), cut.getAlias()
        );
    }

    /**
     * Returns the killmap progress for the game with the given id.
     * @param gameId The game id.
     * @return The killmap progress for the game.
     */
    public static KillMapGameProgress getKillMapProgressForGame(int gameId) {
        @Language("SQL") String nrTestsQuery = """
                SELECT COUNT(Test_ID)
                FROM view_valid_game_tests
                WHERE Game_ID = ?;
        """;

        @Language("SQL") String nrMutantsQuery = """
                SELECT COUNT(Mutant_ID)
                FROM view_valid_game_mutants
                WHERE Game_ID = ?;
        """;

        @Language("SQL") String nrEntriesQuery = """
                SELECT COUNT(*)
                FROM killmap
                WHERE Game_ID = ?;
        """;

        int nrTests = DB.executeQueryReturnValue(nrTestsQuery, rs -> rs.getInt(1), DatabaseValue.of(gameId));
        int nrMutants = DB.executeQueryReturnValue(nrMutantsQuery, rs -> rs.getInt(1), DatabaseValue.of(gameId));
        int nrEntries = DB.executeQueryReturnValue(nrEntriesQuery, rs -> rs.getInt(1), DatabaseValue.of(gameId));

        int nrExpectedEntries = nrMutants * nrTests;
        GameMode gameMode = GameDAO.getGameMode(gameId);

        return new KillMapGameProgress(
                nrTests, nrMutants, nrEntries, nrExpectedEntries,
                gameId, gameMode
        );
    }

    public static KillMapClassroomProgress getKillMapProgressForClassroom(int classroomId) {
        @Language("SQL") String nrEntriesQuery = """
                WITH relevant_classes AS (
                    SELECT DISTINCT games.Class_ID
                    FROM games
                    WHERE games.Classroom_ID = ?
                ),
                classroom_system_mutants AS (
                    SELECT mutants.*
                    FROM view_system_mutant_templates mutants
                    WHERE mutants.Class_ID IN (SELECT * FROM relevant_classes)
                ),
                classroom_user_mutants AS (
                    SELECT mutants.*
                    FROM view_valid_user_mutants mutants, games
                    WHERE mutants.Game_ID = games.ID
                      AND games.Classroom_ID = ?
                ),
                mutants_for_classroom AS (
                    SELECT * FROM classroom_system_mutants
                    UNION ALL
                    SELECT * FROM classroom_user_mutants
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
                ),
                tests_for_classroom AS (
                    SELECT * FROM classroom_system_tests
                    UNION ALL
                    SELECT * FROM classroom_user_tests
                )

                SELECT COUNT(*)
                FROM killmap,
                   tests_for_classroom tests,
                   mutants_for_classroom mutants
                WHERE killmap.Test_ID = tests.Test_ID
                  AND killmap.Mutant_ID = mutants.Mutant_ID
        """;


        Multimap<Integer, Test> tests = TestDAO.getValidTestsForClassroom(classroomId);
        Multimap<Integer, Mutant> mutants = MutantDAO.getValidMutantsForClassroom(classroomId);

        int nrEntries = DB.executeQueryReturnValue(nrEntriesQuery, rs -> rs.getInt(1),
                DatabaseValue.of(classroomId), DatabaseValue.of(classroomId), DatabaseValue.of(classroomId));

        int expectedNrEntries = 0;
        for (int classId : tests.keySet()) {
            expectedNrEntries += tests.get(classId).size() * mutants.get(classId).size();
        }

        ClassroomService classroomService = CDIUtil.getBeanFromCDI(ClassroomService.class);
        Classroom classroom = classroomService.getClassroomById(classroomId)
                .orElseThrow(IllegalArgumentException::new);

        return new KillMapClassroomProgress(
                tests.size(), mutants.size(), nrEntries, expectedNrEntries,
                classroomId, classroom.getName()
        );
    }

    /**
     * Represents the computation progress for a killmap through the number of test, mutants,
     * and previously computed test vs. mutant execution outcomes.
     *
     * <p>Used to display the progress on the killmap management page.
     */
    public static class KillMapProgress {
        private final int nrTests;
        private final int nrMutants;
        private final int nrEntries;
        private final int nrExpectedEntries;

        public KillMapProgress(int nrTests, int nrMutants, int nrEntries, int nrExpectedEntries) {
            this.nrTests = nrTests;
            this.nrMutants = nrMutants;
            this.nrEntries = nrEntries;
            this.nrExpectedEntries = nrExpectedEntries;
        }

        public int getNrTests() {
            return nrTests;
        }

        public int getNrMutants() {
            return nrMutants;
        }

        public int getNrEntries() {
            return nrEntries;
        }

        public int getNrExpectedEntries() {
            return nrExpectedEntries;
        }
    }

    /** Represents the progress of a game killmap. */
    public static class KillMapGameProgress extends KillMapProgress {
        private final int gameId;
        private final GameMode gameMode;

        public KillMapGameProgress(int nrTests, int nrMutants, int nrEntries, int nrExpectedEntries,
                                   int gameId, GameMode gameMode) {
            super(nrTests, nrMutants, nrEntries, nrExpectedEntries);
            this.gameId = gameId;
            this.gameMode = gameMode;
        }

        public int getGameId() {
            return gameId;
        }

        public GameMode getGameMode() {
            return gameMode;
        }
    }

    /** Represents the progress of a class killmap. */
    public static class KillMapClassProgress extends KillMapProgress {
        private final int classId;
        private final String className;
        private final String classAlias;

        public KillMapClassProgress(int nrTests, int nrMutants, int nrEntries, int nrExpectedEntries,
                                    int classId, String className, String classAlias) {
            super(nrTests, nrMutants, nrEntries, nrExpectedEntries);
            this.classId = classId;
            this.className = className;
            this.classAlias = classAlias;
        }

        public int getClassId() {
            return classId;
        }

        public String getClassName() {
            return className;
        }

        public String getClassAlias() {
            return classAlias;
        }
    }

    /** Represents the progress of a class killmap. */
    public static class KillMapClassroomProgress extends KillMapProgress {
        private final int classroomId;
        private final String classroomName;

        public KillMapClassroomProgress(int nrTests, int nrMutants, int nrEntries, int nrExpectedEntries,
                                        int classroomId, String classroomName) {
            super(nrTests, nrMutants, nrEntries, nrExpectedEntries);
            this.classroomId = classroomId;
            this.classroomName = classroomName;
        }

        public int getClassroomId() {
            return classroomId;
        }

        public String getClassroomName() {
            return classroomName;
        }
    }
}
