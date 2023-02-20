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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.codedefenders.database.DB.RSMapper;
import org.codedefenders.execution.KillMap;
import org.codedefenders.execution.KillMap.KillMapEntry;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                                                        String query, DatabaseValue<?>... values)
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
     */
    public static List<KillMapEntry> getKillMapEntriesForGame(int gameId) {
        String query = String.join("\n",
                "SELECT killmap.*",
                "FROM killmap",
                "WHERE killmap.Game_ID = ?");

        List<Test> tests = TestDAO.getValidTestsForGame(gameId, false);
        List<Mutant> mutants = MutantDAO.getValidMutantsForGame(gameId);

        return getKillMapEntries(tests, mutants, query, DatabaseValue.of(gameId));
    }

    /**
     * Returns the killmap entries for the given class.
     */
    public static List<KillMapEntry> getKillMapEntriesForClass(int classId) {
        String query = String.join("\n",
                "SELECT killmap.*",
                "FROM killmap",
                "WHERE killmap.Class_ID = ?");

        List<Test> tests = TestDAO.getValidTestsForClass(classId);
        List<Mutant> mutants = MutantDAO.getValidMutantsForClass(classId);

        return getKillMapEntries(tests, mutants, query, DatabaseValue.of(classId));
    }

    /**
     * Inserts a killmap entry into the database.
     */
    public static boolean insertKillMapEntry(KillMapEntry entry, int classId) {
        String query = String.join("\n",
                "INSERT INTO killmap (Class_ID,Game_ID,Test_ID,Mutant_ID,Status)",
                "VALUES (?,?,?,?,?)",
                "ON DUPLICATE KEY UPDATE Status = VALUES(Status);");
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
        String query = String.join("\n",
                "INSERT INTO killmap (Class_ID,Game_ID,Test_ID,Mutant_ID,Status)",
                "VALUES (?,?,?,?,?)",
                "ON DUPLICATE KEY UPDATE Status = VALUES(Status);");

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

    public static boolean removeKillmapsByIds(KillMap.KillMapType killmapType, List<Integer> ids) {
        if (ids.isEmpty()) {
            return true;
        }

        String idName;
        switch (killmapType) {
            case CLASS:
                idName = "Class_ID";
                break;
            case GAME:
                idName = "Game_ID";
                break;
            default:
                logger.warn("Unknown type of Killmap Job!");
                return false;
        }

        String idsString = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        String query = "DELETE FROM killmap WHERE " + idName + " in (" + idsString + ")";
        return DB.executeUpdateQuery(query);
    }

    /**
     * Return a list of pending killmap jobs ordered by timestamp.
     */
    public static List<KillMap.KillMapJob> getPendingJobs() {
        String query = String.join("\n",
                "SELECT *",
                "FROM killmapjob",
                "ORDER BY Timestamp ASC;");

        return DB.executeQueryReturnList(query, rs -> {
            int gameId = rs.getInt("Game_ID");
            int classId = rs.getInt("Class_ID");
            // if SQL NULL then int is 0
            KillMap.KillMapType type = (classId != 0) ? KillMap.KillMapType.CLASS : KillMap.KillMapType.GAME;
            int reference = (classId != 0) ? classId : gameId;
            return new KillMap.KillMapJob(type, reference);
        });
    }

    public static boolean enqueueJob(KillMap.KillMapJob theJob) {
        String query;
        switch (theJob.getType()) {
            case CLASS:
                query = "INSERT INTO killmapjob (Class_ID) VALUES (?)";
                break;
            case GAME:
                query = "INSERT INTO killmapjob (Game_ID) VALUES (?)";
                break;
            default:
                logger.warn("Unknown type of Killmap Job !");
                return false;
        }

        return DB.executeUpdateQuery(query, DatabaseValue.of(theJob.getId()));
    }

    public static boolean removeJob(KillMap.KillMapJob theJob) {
        String query;
        switch (theJob.getType()) {
            case CLASS:
                query = "DELETE FROM killmapjob WHERE Class_ID = ?";
                break;
            case GAME:
                query = "DELETE FROM killmapjob WHERE Game_ID = ?";
                break;
            default:
                logger.warn("Unknown type of Killmap Job!");
                return false;
        }
        return DB.executeUpdateQuery(query, DatabaseValue.of(theJob.getId()));
    }

    public static boolean removeKillmapJobsByIds(KillMap.KillMapType jobType, List<Integer> ids) {
        if (ids.isEmpty()) {
            return true;
        }

        String idName;
        switch (jobType) {
            case CLASS:
                idName = "Class_ID";
                break;
            case GAME:
                idName = "Game_ID";
                break;
            default:
                logger.warn("Unknown type of Killmap Job!");
                return false;
        }

        String idsString = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        String query = "DELETE FROM killmapjob WHERE " + idName + " in (" + idsString + ")";
        return DB.executeUpdateQuery(query);
    }


    /** Returns the killmaps progress for all class killmaps not queued for processing. */
    public static List<KillMapClassProgress> getNonQueuedKillMapClassProgress() {
        /* Use queries that GROUP the entire killmap, tests and mutants tables, under the assumption that
         * most most killmaps are not queued for computation and most of the data will be used. */

        String classesQuery = String.join("\n",
                "SELECT Class_ID, Name, Alias",
                "FROM view_playable_classes classes",
                "WHERE NOT EXISTS (",
                "   SELECT *",
                "   FROM killmapjob",
                "   WHERE killmapjob.Class_ID = classes.Class_ID",
                ")",
                "ORDER BY Class_ID;");

        String nrTestsQuery = String.join("\n",
                "SELECT Class_ID, COUNT(Test_ID)",
                "FROM view_valid_tests",
                "WHERE Game_ID >= 0",
                "GROUP BY Class_ID;");

        String nrMutantsQuery = String.join("\n",
                "SELECT Class_ID, COUNT(Mutant_ID)",
                "FROM view_valid_mutants",
                "WHERE Game_ID >= 0",
                "GROUP BY Class_ID;");

        String nrEntriesQuery = String.join("\n",
                "SELECT killmap.Class_ID, COUNT(*)",
                "FROM killmap, tests, mutants",
                "WHERE killmap.Test_ID = tests.Test_ID",
                "  AND killmap.Mutant_ID = mutants.Mutant_ID",
                "  AND tests.Game_ID >= 0",
                "  AND mutants.Game_ID >= 0",
                "GROUP BY killmap.Class_ID;");

        List<KillMapClassProgress> progresses = DB.executeQueryReturnList(classesQuery, rs -> {
            KillMapClassProgress progress = new KillMapClassProgress();
            progress.setClassId(rs.getInt("Class_ID"));
            progress.setClassName(rs.getString("Name"));
            progress.setClassAlias(rs.getString("Alias"));
            return progress;
        });

        Map<Integer, Integer> classIdToNrTests = new TreeMap<>();
        Map<Integer, Integer> classIdToNrMutants = new TreeMap<>();
        Map<Integer, Integer> classIdToNrEntries = new TreeMap<>();

        DB.executeQueryReturnList(nrTestsQuery, rs -> classIdToNrTests.put(rs.getInt(1), rs.getInt(2)));
        DB.executeQueryReturnList(nrMutantsQuery, rs -> classIdToNrMutants.put(rs.getInt(1), rs.getInt(2)));
        DB.executeQueryReturnList(nrEntriesQuery, rs -> classIdToNrEntries.put(rs.getInt(1), rs.getInt(2)));

        for (KillMapClassProgress progress : progresses) {
            progress.setNrTests(classIdToNrTests.getOrDefault(progress.classId, 0));
            progress.setNrMutants(classIdToNrMutants.getOrDefault(progress.classId, 0));
            progress.setNrEntries(classIdToNrEntries.getOrDefault(progress.classId, 0));
        }

        return progresses;
    }

    /** Returns the killmaps progress for all game killmaps not queued for processing. */
    public static List<KillMapGameProgress> getNonQueuedKillMapGameProgress() {
        /* Use queries that GROUP the entire killmap, tests and mutants tables, under the assumption that
         * most most killmaps are not queued for computation and most of the data will be used. */

        String gamesQuery = String.join("\n",
                "SELECT ID, Mode",
                "FROM games",
                "WHERE ID >= 0",
                "  AND (Mode = 'DUEL' OR MODE = 'PARTY')",
                "  AND NOT EXISTS (",
                "     SELECT *",
                "     FROM killmapjob",
                "     WHERE killmapjob.Game_ID = games.ID",
                "  )",
                "ORDER BY ID;");

        String nrTestsQuery = String.join("\n",
                "SELECT Game_ID, COUNT(Test_ID)",
                "FROM view_valid_tests",
                "GROUP BY Game_ID;");

        String nrMutantsQuery = String.join("\n",
                "SELECT Game_ID, COUNT(Mutant_ID)",
                "FROM view_valid_mutants",
                "GROUP BY Game_ID;");

        String nrEntriesQuery = String.join("\n",
                "SELECT Game_ID, COUNT(*)",
                "FROM killmap",
                "GROUP BY Game_ID;");

        List<KillMapGameProgress> progresses = DB.executeQueryReturnList(gamesQuery, rs -> {
            KillMapGameProgress progress = new KillMapGameProgress();
            progress.gameId = rs.getInt("ID");
            progress.gameMode = GameMode.valueOf(rs.getString("Mode"));
            return progress;
        });

        Map<Integer, Integer> gameIdToNrTests   = new TreeMap<>();
        Map<Integer, Integer> gameIdToNrMutants = new TreeMap<>();
        Map<Integer, Integer> gameIdToNrEntries = new TreeMap<>();

        DB.executeQueryReturnList(nrTestsQuery,   rs -> gameIdToNrTests.put(rs.getInt(1), rs.getInt(2)));
        DB.executeQueryReturnList(nrMutantsQuery, rs -> gameIdToNrMutants.put(rs.getInt(1), rs.getInt(2)));
        DB.executeQueryReturnList(nrEntriesQuery, rs -> gameIdToNrEntries.put(rs.getInt(1), rs.getInt(2)));

        for (KillMapGameProgress progress : progresses) {
            progress.setNrTests(gameIdToNrTests.getOrDefault(progress.gameId, 0));
            progress.setNrMutants(gameIdToNrMutants.getOrDefault(progress.gameId, 0));
            progress.setNrEntries(gameIdToNrEntries.getOrDefault(progress.gameId, 0));
        }

        return progresses;
    }

    /** Returns the killmaps progress for all class killmaps queued for processing. */
    public static List<KillMapClassProgress> getQueuedKillMapClassProgress() {
        /* First query the classes that are queued, then query the rest for each class,
           under the assumption that only a small fraction of classes are queued for computation. */

        String classesQuery = String.join("\n",
                "SELECT classes.Class_ID, classes.Name, classes.Alias",
                "FROM killmapjob, classes",
                "WHERE killmapjob.Class_ID = classes.Class_ID",
                "ORDER BY Class_ID;");

        List<KillMapClassProgress> progresses = DB.executeQueryReturnList(classesQuery, rs -> {
            KillMapClassProgress progress = new KillMapClassProgress();
            progress.setClassId(rs.getInt("Class_ID"));
            progress.setClassName(rs.getString("Name"));
            progress.setClassAlias(rs.getString("Alias"));
            return progress;
        });

        for (KillMapClassProgress progress : progresses) {
            KillMapProgress values = getKillMapProgressForClass(progress.getClassId());
            progress.setNrTests(values.getNrTests());
            progress.setNrMutants(values.getNrMutants());
            progress.setNrEntries(values.getNrEntries());
        }

        return progresses;
    }

    /** Returns the killmaps progress for all game killmaps queued for processing. */
    public static List<KillMapGameProgress> getQueuedKillMapGameProgress() {
        /* First query the games that are queued, then query the rest for each game,
           under the assumption that only a small fraction of classes are queued for computation. */

        String gamesQuery = String.join("\n",
                "SELECT games.ID, games.Mode",
                "FROM killmapjob, games",
                "WHERE killmapjob.Game_ID = games.ID",
                "ORDER BY Game_ID;");

        List<KillMapGameProgress> progresses = DB.executeQueryReturnList(gamesQuery, rs -> {
            KillMapGameProgress progress = new KillMapGameProgress();
            progress.setGameId(rs.getInt("ID"));
            progress.setGameMode(GameMode.valueOf(rs.getString("Mode")));
            return progress;
        });

        for (KillMapGameProgress progress : progresses) {
            KillMapProgress values = getKillMapProgressForGame(progress.gameId);
            progress.setNrTests(values.getNrTests());
            progress.setNrMutants(values.getNrMutants());
            progress.setNrEntries(values.getNrEntries());
        }

        return progresses;
    }

    /** Returns the number of queued jobs for class killmaps. */
    public static int getNumClassKillmapJobsQueued() {
        String query = "SELECT COUNT(DISTINCT Class_ID) from killmapjob WHERE Class_ID IS NOT NULL;";
        return DB.executeQueryReturnValue(query, rs -> rs.getInt(1));
    }


    /** Returns the number of queued jobs for game killmaps. */
    public static int getNumGameKillmapJobsQueued() {
        String query = "SELECT COUNT(DISTINCT Game_ID) from killmapjob WHERE Game_ID IS NOT NULL;";
        return DB.executeQueryReturnValue(query, rs -> rs.getInt(1));
    }

    /**
     * Returns the killmap progress for the class with the given id.
     * @param classId The class id.
     * @return The killmap progress for the class.
     */
    public static KillMapProgress getKillMapProgressForClass(int classId) {
        String nrTestsQuery = String.join("\n",
                "SELECT COUNT(Test_ID)",
                "FROM view_valid_tests",
                "WHERE Game_ID >= 0",
                "  AND Class_ID = ?;");

        String nrMutantsQuery = String.join("\n",
                "SELECT COUNT(Mutant_ID)",
                "FROM view_valid_mutants",
                "WHERE Game_ID >= 0",
                "  AND Class_ID = ?;");

        String nrEntriesQuery = String.join("\n",
                "SELECT COUNT(*)",
                "FROM killmap, tests, mutants",
                "WHERE killmap.Test_ID = tests.Test_ID",
                "  AND killmap.Mutant_ID = mutants.Mutant_ID",
                "  AND tests.Game_ID >= 0",
                "  AND mutants.Game_ID >= 0",
                "  AND killmap.Class_ID = ?;");

        int nrTests = DB.executeQueryReturnValue(nrTestsQuery, rs -> rs.getInt(1), DatabaseValue.of(classId));
        int nrMutants = DB.executeQueryReturnValue(nrMutantsQuery, rs -> rs.getInt(1), DatabaseValue.of(classId));
        int nrEntries = DB.executeQueryReturnValue(nrEntriesQuery, rs -> rs.getInt(1), DatabaseValue.of(classId));

        KillMapProgress progress = new KillMapProgress();
        progress.setNrTests(nrTests);
        progress.setNrMutants(nrMutants);
        progress.setNrEntries(nrEntries);
        return progress;
    }

    /**
     * Returns the killmap progress for the game with the given id.
     * @param gameId The game id.
     * @return The killmap progress for the game.
     */
    public static KillMapProgress getKillMapProgressForGame(int gameId) {
        String nrTestsQuery = String.join("\n",
                "SELECT COUNT(Test_ID)",
                "FROM view_valid_tests",
                "WHERE Game_ID = ?;");

        String nrMutantsQuery = String.join("\n",
                "SELECT COUNT(Mutant_ID)",
                "FROM view_valid_mutants",
                "WHERE Game_ID = ?;");

        String nrEntriesQuery = String.join("\n",
                "SELECT COUNT(*)",
                "FROM killmap",
                "WHERE Game_ID = ?;");

        int nrTests = DB.executeQueryReturnValue(nrTestsQuery, rs -> rs.getInt(1), DatabaseValue.of(gameId));
        int nrMutants = DB.executeQueryReturnValue(nrMutantsQuery, rs -> rs.getInt(1), DatabaseValue.of(gameId));
        int nrEntries = DB.executeQueryReturnValue(nrEntriesQuery, rs -> rs.getInt(1), DatabaseValue.of(gameId));

        KillMapProgress progress = new KillMapProgress();
        progress.setNrTests(nrTests);
        progress.setNrMutants(nrMutants);
        progress.setNrEntries(nrEntries);
        return progress;
    }

    /**
     * Represents the computation progress for a killmap through the number of test, mutants,
     * and previously computed test vs. mutant execution outcomes.
     *
     * <p>Used to display the progress on the killmap management page.
     */
    public static class KillMapProgress {
        private int nrTests;
        private int nrMutants;
        private int nrEntries;

        public int getNrTests() {
            return nrTests;
        }

        public void setNrTests(int nrTests) {
            this.nrTests = nrTests;
        }

        public int getNrMutants() {
            return nrMutants;
        }

        public void setNrMutants(int nrMutants) {
            this.nrMutants = nrMutants;
        }

        public int getNrEntries() {
            return nrEntries;
        }

        public void setNrEntries(int nrEntries) {
            this.nrEntries = nrEntries;
        }
    }

    /** Represents the progress of a game killmap. */
    public static class KillMapGameProgress extends KillMapProgress {
        private int gameId;
        private GameMode gameMode;

        public int getGameId() {
            return gameId;
        }

        public void setGameId(int gameId) {
            this.gameId = gameId;
        }

        public GameMode getGameMode() {
            return gameMode;
        }

        public void setGameMode(GameMode gameMode) {
            this.gameMode = gameMode;
        }
    }

    /** Represents the progress of a class killmap. */
    public static class KillMapClassProgress extends KillMapProgress {
        private int classId;
        private String className;
        private String classAlias;

        public int getClassId() {
            return classId;
        }

        public void setClassId(int classId) {
            this.classId = classId;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getClassAlias() {
            return classAlias;
        }

        public void setClassAlias(String classAlias) {
            this.classAlias = classAlias;
        }
    }
}
