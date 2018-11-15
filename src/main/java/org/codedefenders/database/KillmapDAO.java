package org.codedefenders.database;

import org.codedefenders.execution.KillMap;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class handles the database logic for killmaps.
 * @see KillMap
 */
public class KillmapDAO {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseAccess.class);

    /**
     * Returns if the given game already has a computed killmap.
     */
    public static Boolean hasKillMap(int gameId) {
        String query = String.join("\n",
                "SELECT HasKillMap",
                "FROM games",
                "WHERE games.ID = ?"
        );

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(gameId));
        try {
            ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
            if (rs.next()) {
                return rs.getBoolean("HasKillMap");
            }
        } catch (SQLException e) {
            logger.error("SQL exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }

        return null;
    }

    /**
     * Sets a flag that indicates if the given game has a computed killmap.
     */
    public static boolean setHasKillMap(int gameId, boolean hasKillMap) {
        String query = String.join("\n",
                "UPDATE games",
                "SET HasKillMap = ?",
                "WHERE ID = ?;"
        );

        Connection conn = DB.getConnection();

        DatabaseValue[] values = new DatabaseValue[]{
                DB.getDBV(hasKillMap),
                DB.getDBV(gameId)
        };

        PreparedStatement stmt = DB.createPreparedStatement(conn, query, values);

        try {
            return DB.executeUpdate(stmt, conn);
        } finally {
            DB.cleanup(conn, stmt);
        }
    }

    /**
     * Helper method to retrieve killmap entries from the database.
     */
    public static List<KillMap.KillMapEntry> getKillMapEntries(PreparedStatement stmt, Connection conn, List<Test> tests, List<Mutant> mutants) {
        try {
            stmt.setFetchSize(Integer.MIN_VALUE);
        } catch (SQLException e) {
            logger.error("SQL Exception caught", e);
            return null;
        }

        List<KillMap.KillMapEntry> entries = new LinkedList<>();

        /* Set up mapping from test id to test / mutant id to mutant. */
        Map<Integer, Test> testMap = new HashMap<>();
        Map<Integer, Mutant> mutantMap = new HashMap<>();
        for (Test test : tests) { testMap.put(test.getId(), test); }
        for (Mutant mutant : mutants) { mutantMap.put(mutant.getId(), mutant); }

        try {
            ResultSet rs = DB.executeQueryReturnRS(conn, stmt);

            while (rs.next()) {
                int testId = rs.getInt("Test_ID");
                int mutantId = rs.getInt("Mutant_ID");
                String status = rs.getString("Status");
                entries.add(new KillMap.KillMapEntry(testMap.get(testId), mutantMap.get(mutantId), KillMap.KillMapEntry.Status.valueOf(status)));
            }
        } catch (SQLException e) {
            logger.error("SQL exception caught", e);
            return null;
        } finally {
            DB.cleanup(conn, stmt);
        }

        return entries;
    }

    /**
     * Returns the killmap entries for the given game.
     */
    public static List<KillMap.KillMapEntry> getKillMapEntriesForGame(int gameId) {
        String query = String.join("\n",
                "SELECT killmap.*",
                "FROM killmap",
                "WHERE killmap.Game_ID = ?"
        );

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(gameId));

        List<Test> tests = TestDAO.getValidTestsForGame(gameId, false);
        List<Mutant> mutants = MutantDAO.getValidMutantsForGame(gameId);

        return getKillMapEntries(stmt, conn, tests, mutants);
    }

    /**
     * Returns the killmap entries for the given class.
     */
    public static List<KillMap.KillMapEntry> getKillMapEntriesForClass(int classId) {
        String query = String.join("\n",
                "SELECT killmap.*",
                "FROM killmap",
                "WHERE killmap.Class_ID = ?"
        );

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(classId));

        List<Test> tests = TestDAO.getValidTestsForClass(classId);
        List<Mutant> mutants = MutantDAO.getValidMutantsForClass(classId);

        return getKillMapEntries(stmt, conn, tests, mutants);
    }

    /**
     * Inserts a killmap entry into the database.
     */
    public static boolean insertKillMapEntry(KillMap.KillMapEntry entry, int classId) {
        String query = String.join("\n",
                "INSERT INTO killmap (Class_ID,Game_ID,Test_ID,Mutant_ID,Status) VALUES (?,?,?,?,?)",
                "ON DUPLICATE KEY UPDATE Status = VALUES(Status);"
        );

        Connection conn = DB.getConnection();

        try {
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, classId);

            if (entry.test.getGameId() == entry.mutant.getGameId()) {
                stmt.setInt(2, entry.test.getGameId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }

            stmt.setInt(3, entry.test.getId());
            stmt.setInt(4, entry.mutant.getId());
            stmt.setString(5, entry.status.toString());


            return DB.executeUpdate(stmt, conn);
        } catch (SQLException e) {
            logger.error("SQL exception caught", e);
            return false;
        }
    }
}
