package org.codedefenders.util;

import org.codedefenders.GameLevel;
import org.codedefenders.Role;
import org.codedefenders.User;
import org.codedefenders.leaderboard.Entry;
import org.codedefenders.multiplayer.MultiplayerGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {

    private static final Logger logger = LoggerFactory.getLogger(AdminDAO.class);
    private static final String ACTIVE_USERS_QUERY =
            "SELECT DISTINCT\n" +
                    "  Username,\n" +
                    "  users.User_ID,\n" +
                    "  MAX(players.Game_ID) AS newestGame\n" +
                    "FROM (users\n" +
                    "  INNER JOIN players ON users.User_ID = players.User_ID) INNER JOIN (games\n" +
                    "  INNER JOIN classes ON games.Class_ID = classes.Class_ID) ON players.Game_ID = games.ID\n" +
                    "WHERE State = 'ACTIVE' AND Mode = 'PARTY' AND Finish_Time > NOW()\n" +
                    "GROUP BY Username, users.User_ID\n" +
                    "ORDER BY newestGame DESC;";
    private static final String INACTIVE_USERS_QUERY =
            "SELECT DISTINCT\n" +
                    "  users.User_ID,\n" +
                    "  users.Username,\n" +
                    "  MAX(sessions.Timestamp) AS 'Last Login'\n" +
                    "FROM\n" +
                    "  users LEFT JOIN sessions on users.User_ID = sessions.User_ID\n" +
                    "WHERE users.User_ID > 2 AND users.User_ID NOT IN (\n" +
                    "  SELECT DISTINCT players.User_ID\n" +
                    "  FROM (players\n" +
                    "    INNER JOIN games ON players.Game_ID = games.ID)\n" +
                    "  WHERE State = 'ACTIVE' AND Finish_Time > NOW() AND Role IN ('ATTACKER', 'DEFENDER')\n" +
                    ") GROUP BY Username, User_ID\n" +
                    "ORDER BY `Last Login` DESC;";
    private static final String UNASSIGNED_USERS_QUERY =
            "SELECT DISTINCT\n" +
                    "  users.User_ID,\n" +
                    "  users.Username\n" +
                    "FROM\n" +
                    "  users\n" +
                    "WHERE users.User_ID > 2 AND users.User_ID NOT IN (\n" +
                    "  SELECT DISTINCT players.User_ID\n" +
                    "  FROM (players\n" +
                    "    INNER JOIN games ON players.Game_ID = games.ID)\n" +
                    "  WHERE (State = 'ACTIVE' OR State = 'CREATED') AND Finish_Time > NOW() AND Role IN ('ATTACKER', 'DEFENDER')\n" +
                    ")\n" +
                    "GROUP BY Username, User_ID;";
    private static final String GAMES_FOR_USER_QUERY =
            "SELECT games.*\n" +
                    "FROM (users\n" +
                    "  INNER JOIN players ON users.User_ID = players.User_ID) INNER JOIN (games\n" +
                    "  INNER JOIN classes ON games.Class_ID = classes.Class_ID) ON players.Game_ID = games.ID\n" +
                    "WHERE State = 'ACTIVE' AND Mode = 'PARTY' AND (role = 'ATTACKER' OR role = 'DEFENDER')\n" +
                    "      AND Finish_Time > NOW() AND Username = ?\n" +
                    "ORDER BY games.Timestamp DESC;";
    private static final String AVAILABLE_GAMES_QUERY =
            "SELECT *\n" +
                    "FROM\n" +
                    "  games\n" +
                    "WHERE State = 'ACTIVE' AND Mode = 'PARTY' AND Finish_Time > NOW();";

    private static final String UNFINISHED_MULTIPLAYER_GAMES_QUERY =
            "SELECT *\n" +
                    "FROM games\n" +
                    "WHERE Mode = 'PARTY' AND (State = 'ACTIVE' OR State = 'CREATED');";

    private static final String LAST_LOGIN_QUERY =
            "SELECT MAX(Timestamp)\n" +
                    "FROM sessions\n" +
                    "WHERE User_ID = ?;";
    private static final String LAST_SUBMISSION_TS_QUERY =
            "SELECT MAX(ts)\n" +
                    "FROM (SELECT MAX(mutants.Timestamp) AS ts\n" +
                    "      FROM players\n" +
                    "        LEFT JOIN mutants ON mutants.Player_ID = players.ID\n" +
                    "      WHERE players.ID = ?\n" +
                    "      UNION\n" +
                    "      SELECT MAX(tests.Timestamp) AS ts\n" +
                    "      FROM players\n" +
                    "        LEFT JOIN tests ON tests.Player_ID = players.ID\n" +
                    "      WHERE players.ID = ?) AS t;";
    private static final String LAST_ROLE_QUERY =
            "SELECT\n" +
                    "  players.User_ID,\n" +
                    "  Game_ID,\n" +
                    "  Role\n" +
                    "FROM users\n" +
                    "  INNER JOIN players ON users.User_ID = players.User_ID\n" +
                    "  INNER JOIN games ON players.Game_ID = games.ID\n" +
                    "  INNER JOIN\n" +
                    "  (SELECT\n" +
                    "     players.User_ID,\n" +
                    "     max(players.Game_ID) AS latestGame\n" +
                    "   FROM players\n" +
                    "   GROUP BY players.User_ID) AS lg ON lg.User_ID = players.User_ID AND lg.latestGame = games.ID\n" +
                    "WHERE lg.User_ID=?;";

    public static final String USER_SCORE_QUERY =
            "SELECT\n" +
                    "  U.username                            AS username,\n" +
                    "  IFNULL(NMutants, 0)                   AS NMutants,\n" +
                    "  IFNULL(AScore, 0)                     AS AScore,\n" +
                    "  IFNULL(NTests, 0)                     AS NTests,\n" +
                    "  IFNULL(DScore, 0)                     AS DScore,\n" +
                    "  IFNULL(NKilled, 0)                    AS NKilled,\n" +
                    "  IFNULL(AScore, 0) + IFNULL(DScore, 0) AS TotalScore\n" +
                    "FROM users U LEFT JOIN\n" +
                    "  (SELECT\n" +
                    "     PA.user_id,\n" +
                    "     count(M.Mutant_ID) AS NMutants,\n" +
                    "     sum(M.Points)      AS AScore\n" +
                    "   FROM players PA LEFT JOIN mutants M ON PA.id = M.Player_ID\n" +
                    "   GROUP BY PA.user_id)\n" +
                    "    AS Attacker ON U.user_id = Attacker.user_id\n" +
                    "  LEFT JOIN\n" +
                    "  (SELECT\n" +
                    "     PD.user_id,\n" +
                    "     count(T.Test_ID)     AS NTests,\n" +
                    "     sum(T.Points)        AS DScore,\n" +
                    "     sum(T.MutantsKilled) AS NKilled\n" +
                    "   FROM players PD LEFT JOIN tests T ON PD.id = T.Player_ID\n" +
                    "   GROUP BY PD.user_id)\n" +
                    "    AS Defender ON U.user_id = Defender.user_id\n" +
                    "WHERE U.user_id = ?;";

    public final static String DELETE_PLAYER = "DELETE FROM players WHERE ID =?;";
    public final static String DELETE_TESTS = "DELETE FROM tests WHERE Player_ID =?;";
    public final static String DELETE_MUTANTS = "DELETE FROM mutants WHERE Player_ID =?;";
    public final static String DELETE_EQUIVALENCES = "DELETE FROM equivalences WHERE Defender_ID =?;";
    public final static String DELETE_TEST_TARGETEXECUTIONS = "DELETE FROM targetexecutions WHERE Test_ID =?;";
    public final static String DELETE_MUTANT_TARGETEXECUTIONS = "DELETE FROM targetexecutions WHERE Mutant_ID = ?;";

    public static List<User> getUsers(String query) {
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query);
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);

        List<User> activeUsers = new ArrayList<User>();
        try {
            while (rs.next()) {
                User userRecord = new User(rs.getString("Username"), "");
                userRecord.setId(rs.getInt("User_ID"));
                activeUsers.add(userRecord);
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return activeUsers;
    }

    public static List<User> getActiveUsers() {
        return getUsers(ACTIVE_USERS_QUERY);
    }

    public static List<User> getInactiveUsers() {
        return getUsers(INACTIVE_USERS_QUERY);
    }

    public static List<User> getUnassignedUsers() {
        return getUsers(UNASSIGNED_USERS_QUERY);
    }

    public static List<MultiplayerGame> getGamesFromRS(ResultSet rs, Connection conn, PreparedStatement stmt) {
        List<MultiplayerGame> gamesList = new ArrayList<MultiplayerGame>();
        try {
            while (rs.next()) {
                MultiplayerGame mg = new MultiplayerGame(rs.getInt("Class_ID"), rs.getInt("Creator_ID"),
                        GameLevel.valueOf(rs.getString("Level")), (float) rs.getDouble("Coverage_Goal"),
                        (float) rs.getDouble("Mutant_Goal"), rs.getInt("Prize"), rs.getInt("Defender_Value"),
                        rs.getInt("Attacker_Value"), rs.getInt("Defenders_Limit"), rs.getInt("Attackers_Limit"),
                        rs.getInt("Defenders_Needed"), rs.getInt("Attackers_Needed"), rs.getTimestamp("Start_Time").getTime(),
                        rs.getTimestamp("Finish_Time").getTime(), rs.getString("State"), rs.getBoolean("RequiresValidation"));
                mg.setId(rs.getInt("ID"));
                gamesList.add(mg);
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return gamesList;
    }

    public static MultiplayerGame getActiveGameForUser(String userName) {
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, GAMES_FOR_USER_QUERY, DB.getDBV(userName));
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        return getGamesFromRS(rs, conn, stmt).get(0);
    }

    public static List<MultiplayerGame> getAvailableGames() {
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, AVAILABLE_GAMES_QUERY);
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        return getGamesFromRS(rs, conn, stmt);
    }

    public static List<MultiplayerGame> getUnfinishedMultiplayerGames() {
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, UNFINISHED_MULTIPLAYER_GAMES_QUERY);
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        return getGamesFromRS(rs, conn, stmt);
    }

    public static Timestamp getLast(Connection conn, PreparedStatement stmt) {
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        try {
            if (rs.next()) {
                return rs.getTimestamp(1);
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return null;
    }

    public static Timestamp getLastLogin(int uid) {
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, LAST_LOGIN_QUERY, DB.getDBV(uid));
        return getLast(conn, stmt);
    }

    public static Timestamp getLastSubmissionTS(int pid) {
        Connection conn = DB.getConnection();
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(pid), DB.getDBV(pid)};
        PreparedStatement stmt = DB.createPreparedStatement(conn, LAST_SUBMISSION_TS_QUERY, valueList);
        return getLast(conn, stmt);
    }

    public static Role getLastRole(int uid) {
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, LAST_ROLE_QUERY, DB.getDBV(uid));
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        try {
            if (rs.next()) {
                switch (rs.getString(3)) {
                    case "ATTACKER":
                        return Role.ATTACKER;
                    case "DEFENDER":
                        return Role.DEFENDER;
                    default:
                        return Role.NONE;
                }
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return null;
    }

    public static Entry getScore(int userID) {
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, USER_SCORE_QUERY, DB.getDBV(userID));
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        try {
            if (rs.next()) {
                Entry p = new Entry();
                p.setUsername(rs.getString("username"));
                p.setMutantsSubmitted(rs.getInt("NMutants"));
                p.setAttackerScore(rs.getInt("AScore"));
                p.setTestsSubmitted(rs.getInt("NTests"));
                p.setDefenderScore(rs.getInt("DScore"));
                p.setMutantsKilled(rs.getInt("NKilled"));
                p.setTotalPoints(rs.getInt("TotalScore"));
                return p;
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            DB.cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return null;
    }

    public static boolean deletePlayerTest(int pid) {

        Connection conn = DB.getConnection();
        PreparedStatement tests_stmt = DB.createPreparedStatement(conn, DELETE_TESTS, DB.getDBV(pid));
        return DB.executeUpdate(tests_stmt, conn);
    }

    public static boolean deletePlayerMutants(int pid) {
        Connection conn = DB.getConnection();
        PreparedStatement mutants_stmt = DB.createPreparedStatement(conn, DELETE_MUTANTS, DB.getDBV(pid));
        return DB.executeUpdate(mutants_stmt, conn);
    }

    public static boolean deletePlayerEquivalences(int pid) {
        Connection conn = DB.getConnection();
        PreparedStatement equivalences_stmt = DB.createPreparedStatement(conn, DELETE_EQUIVALENCES, DB.getDBV(pid));
        return DB.executeUpdate(equivalences_stmt, conn);
    }

    public static boolean deletePlayer(int pid) {
        Connection conn = DB.getConnection();
        PreparedStatement player_stmt = DB.createPreparedStatement(conn, DELETE_PLAYER, DB.getDBV(pid));
        return DB.executeUpdate(player_stmt, conn);
    }

    public static boolean deleteTestTargetExecutions(int tid) {
        Connection conn = DB.getConnection();
        PreparedStatement player_stmt = DB.createPreparedStatement(conn, DELETE_TEST_TARGETEXECUTIONS, DB.getDBV(tid));
        return DB.executeUpdate(player_stmt, conn);
    }

    public static boolean deleteMutantTargetExecutions(int mid) {
        Connection conn = DB.getConnection();
        PreparedStatement player_stmt = DB.createPreparedStatement(conn, DELETE_MUTANT_TARGETEXECUTIONS, DB.getDBV(mid));
        return DB.executeUpdate(player_stmt, conn);
    }
}
