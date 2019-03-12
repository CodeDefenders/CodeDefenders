/**
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
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class handles the database logic for killmaps.
 *
 * @see KillMap
 */
public class KillmapDAO {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseAccess.class);

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
                logger.warn("Unknown type of Killmap Job !");
                return false;
        }
        return DB.executeUpdateQuery(query, DatabaseValue.of(theJob.getReference()));
    }
}
