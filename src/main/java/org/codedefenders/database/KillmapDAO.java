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

import org.codedefenders.execution.KillMap;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * This class handles the database logic for killmaps.
 *
 * @see KillMap
 */
public class KillmapDAO {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseAccess.class);

    // TODO: remove (hasKillMap will be replaced with progress)
    /**
     * Returns if the given game already has a computed killmap.
     */
    public static Boolean hasKillMap(int gameId) throws UncheckedSQLException, SQLMappingException {
        String query = String.join("\n",
                "SELECT HasKillMap",
                "FROM games",
                "WHERE games.ID = ?");
        return DB.executeQueryReturnValue(query, rs -> rs.getBoolean("HasKillMap"), DatabaseValue.of(gameId));
    }

    // TODO: remove (hasKillMap will be replaced with progress)
    /**
     * Sets a flag that indicates if the given game has a computed killmap.
     */
    public static boolean setHasKillMap(int gameId, boolean hasKillMap) {
        String query = String.join("\n",
                "UPDATE games",
                "SET HasKillMap = ?",
                "WHERE ID = ?;");

        return DB.executeUpdateQuery(query, DatabaseValue.of(hasKillMap), DatabaseValue.of(gameId));
    }

    /**
     * Helper method to retrieve killmap entries from the database.
     */
    private static List<KillMap.KillMapEntry> getKillMapEntries(List<Test> tests, List<Mutant> mutants,
                                                                String query, DatabaseValue... values) throws UncheckedSQLException, SQLMappingException {
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, values);
        try {
            stmt.setFetchSize(Integer.MIN_VALUE);
        } catch (SQLException e) {
            logger.error("Caught SQL exception while trying to set fetch size.", e);
            throw new UncheckedSQLException("Caught SQL exception while trying to set fetch size.", e);
        }

        /* Set up mapping from test id to test / mutant id to mutant. */
        Map<Integer, Test> testMap = tests.stream().collect(Collectors.toMap(Test::getId, t -> t));
        Map<Integer, Mutant> mutantMap = mutants.stream().collect(Collectors.toMap(Mutant::getId, m -> m));

        return DB.executeQueryReturnList(conn, stmt, rs -> {
            int testId = rs.getInt("Test_ID");
            int mutantId = rs.getInt("Mutant_ID");
            String status = rs.getString("Status");
            return new KillMap.KillMapEntry(testMap.get(testId), mutantMap.get(mutantId), KillMap.KillMapEntry.Status.valueOf(status));
        });
    }

    /**
     * Returns the killmap entries for the given game.
     */
    public static List<KillMap.KillMapEntry> getKillMapEntriesForGame(int gameId) {
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
    public static List<KillMap.KillMapEntry> getKillMapEntriesForClass(int classId) {
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
    public static boolean insertKillMapEntry(KillMap.KillMapEntry entry, int classId) {
        String query = String.join("\n",
                "INSERT INTO killmap (Class_ID,Game_ID,Test_ID,Mutant_ID,Status)",
                "VALUES (?,?,?,?,?)",
                "ON DUPLICATE KEY UPDATE Status = VALUES(Status);");
        int testGameId = entry.test.getGameId();
        int mutantGameId = entry.mutant.getGameId();

        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(classId),
                DatabaseValue.of(testGameId == mutantGameId ? testGameId : null),
                DatabaseValue.of(entry.test.getId()),
                DatabaseValue.of(entry.mutant.getId()),
                DatabaseValue.of(entry.status.name()),
        };
        return DB.executeUpdateQuery(query, values);
    }

    public static boolean deleteKillMapForGame(int gameId) {
        String query = "DELETE FROM killmap WHERE Game_ID = ?;";
        return DB.executeUpdateQuery(query, DatabaseValue.of(gameId));
    }

    public static boolean deleteKillMapForClass(int classId) {
        String query = "DELETE FROM killmap WHERE Class_ID = ?;";
        return DB.executeUpdateQuery(query, DatabaseValue.of(classId));
    }





    /**
     * Return a list of pending killmap jobs ordered by timestamp
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
            KillMap.KillMapJob.Type type = (classId != 0) ? KillMap.KillMapJob.Type.CLASS : KillMap.KillMapJob.Type.GAME;
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

        return DB.executeUpdateQuery(query, DatabaseValue.of(theJob.getReference()));
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
        return DB.executeUpdateQuery(query, DatabaseValue.of(theJob.getReference()));
    }

    public static boolean removeJobsByIds(KillMap.KillMapJob.Type jobType, List<Integer> ids) {
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

    public static List<KillMapClassProgress> getNonQueuedKillMapClassProgress() {
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

    //TODO: javadoc: "assumption: few killmaps are queued, many are available"
    public static List<KillMapGameProgress> getNonQueuedKillMapGameProgress() {
        // TODO: view_playable_games?
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

    //TODO: javadoc: "assumption: few killmaps are queued, many are available"
    public static List<KillMapClassProgress> getQueuedKillMapClassProgress() {
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

    public static List<KillMapGameProgress> getQueuedKillMapGameProgress() {
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

    public static int getNumClassKillmapJobsQueued() {
        String query = "SELECT COUNT(DISTINCT Class_ID) from killmapjob WHERE Class_ID IS NOT NULL;";
        return DB.executeQueryReturnValue(query, rs -> rs.getInt(1));
    }


    public static int getNumGameKillmapJobsQueued() {
        String query = "SELECT COUNT(DISTINCT Game_ID) from killmapjob WHERE Game_ID IS NOT NULL;";
        return DB.executeQueryReturnValue(query, rs -> rs.getInt(1));
    }

    // TODO: move into KillMap or into own class file?
    // TODO: make members private and use getters and setters
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
