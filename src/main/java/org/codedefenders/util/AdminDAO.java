package org.codedefenders.util;

import org.codedefenders.GameLevel;
import org.codedefenders.Role;
import org.codedefenders.User;
import org.codedefenders.AdminSystemSettings;
import org.codedefenders.leaderboard.Entry;
import org.codedefenders.multiplayer.MultiplayerGame;
import org.codedefenders.validation.CodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {

    public static final String TIMETSTAMP_NEVER = "never";
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
                    "  WHERE (State = 'ACTIVE' OR State = 'CREATED') AND Finish_Time > NOW() AND Role IN ('ATTACKER', 'DEFENDER') AND Active = TRUE\n" +
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
                    "WHERE State != 'FINISHED' AND Mode = 'PARTY' AND Finish_Time > NOW();";

    private static final String UNFINISHED_MULTIPLAYER_GAMES_QUERY =
            "SELECT *\n" +
                    "FROM games\n" +
                    "WHERE Mode = 'PARTY' AND (State = 'ACTIVE' OR State = 'CREATED');";
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

    public final static String UNASSIGNED_USERS_INFO_QUERY =
            "SELECT DISTINCT\n" +
                    "  users.User_ID,\n" +
                    "  users.Username,\n" +
                    "  users.Email,\n" +
                    "  lastLogin.ts AS lastLogin,\n" +
                    "  Role         AS lastRole,\n" +
                    "  totalScore\n" +
                    "FROM\n" +
                    "  users\n" +
                    "  LEFT JOIN (SELECT\n" +
                    "          MAX(Timestamp) AS ts,\n" +
                    "          user_id\n" +
                    "        FROM sessions\n" +
                    "        GROUP BY User_ID) AS lastLogin ON lastLogin.User_ID = users.User_ID\n" +
                    "  LEFT JOIN\n" +
                    "  (SELECT\n" +
                    "     players.User_ID,\n" +
                    "     Role\n" +
                    "   FROM users\n" +
                    "     INNER JOIN players ON users.User_ID = players.User_ID\n" +
                    "     INNER JOIN games ON players.Game_ID = games.ID\n" +
                    "     INNER JOIN\n" +
                    "     (SELECT\n" +
                    "        players.User_ID,\n" +
                    "        max(players.Game_ID) AS latestGame\n" +
                    "      FROM players\n" +
                    "      GROUP BY players.User_ID) AS lg ON lg.User_ID = players.User_ID AND lg.latestGame = games.ID) AS lastRole\n" +
                    "    ON lastRole.User_ID = users.User_ID\n" +
                    "  JOIN\n" +
                    "  (SELECT\n" +
                    "     U.User_ID,\n" +
                    "     IFNULL(AScore, 0) + IFNULL(DScore, 0) AS TotalScore\n" +
                    "   FROM users U LEFT JOIN\n" +
                    "     (SELECT\n" +
                    "        PA.user_id,\n" +
                    "        sum(M.Points) AS AScore\n" +
                    "      FROM players PA LEFT JOIN mutants M ON PA.id = M.Player_ID\n" +
                    "      GROUP BY PA.user_id)\n" +
                    "       AS Attacker ON U.user_id = Attacker.user_id\n" +
                    "     LEFT JOIN\n" +
                    "     (SELECT\n" +
                    "        PD.user_id,\n" +
                    "        sum(T.Points) AS DScore\n" +
                    "      FROM players PD LEFT JOIN tests T ON PD.id = T.Player_ID\n" +
                    "      GROUP BY PD.user_id)\n" +
                    "       AS Defender ON U.user_id = Defender.user_id) AS totalScore ON totalScore.User_ID = users.User_ID\n" +
                    "WHERE users.User_ID > 2 AND users.User_ID NOT IN (\n" +
                    "  SELECT DISTINCT players.User_ID\n" +
                    "  FROM (players\n" +
                    "    INNER JOIN games ON players.Game_ID = games.ID)\n" +
                    "  WHERE\n" +
                    "    (State = 'ACTIVE' OR State = 'CREATED') AND Finish_Time > NOW() AND Role IN ('ATTACKER', 'DEFENDER') AND Active = 1\n" +
                    ")\n" +
                    "ORDER BY lastLogin DESC, User_ID;";


    public final static String USERS_INFO_QUERY =
            "SELECT DISTINCT\n" +
                    "  users.User_ID,\n" +
                    "  users.Username,\n" +
                    "  users.Email,\n" +
                    "  lastLogin.ts AS lastLogin,\n" +
                    "  Role         AS lastRole,\n" +
                    "  totalScore\n" +
                    "FROM\n" +
                    "  users\n" +
                    "  LEFT JOIN (SELECT\n" +
                    "          MAX(Timestamp) AS ts,\n" +
                    "          user_id\n" +
                    "        FROM sessions\n" +
                    "        GROUP BY User_ID) AS lastLogin ON lastLogin.User_ID = users.User_ID\n" +
                    "  LEFT JOIN\n" +
                    "  (SELECT\n" +
                    "     players.User_ID,\n" +
                    "     Role\n" +
                    "   FROM users\n" +
                    "     INNER JOIN players ON users.User_ID = players.User_ID\n" +
                    "     INNER JOIN games ON players.Game_ID = games.ID\n" +
                    "     INNER JOIN\n" +
                    "     (SELECT\n" +
                    "        players.User_ID,\n" +
                    "        max(players.Game_ID) AS latestGame\n" +
                    "      FROM players\n" +
                    "      GROUP BY players.User_ID) AS lg ON lg.User_ID = players.User_ID AND lg.latestGame = games.ID) AS lastRole\n" +
                    "    ON lastRole.User_ID = users.User_ID\n" +
                    "  JOIN\n" +
                    "  (SELECT\n" +
                    "     U.User_ID,\n" +
                    "     IFNULL(AScore, 0) + IFNULL(DScore, 0) AS TotalScore\n" +
                    "   FROM users U LEFT JOIN\n" +
                    "     (SELECT\n" +
                    "        PA.user_id,\n" +
                    "        sum(M.Points) AS AScore\n" +
                    "      FROM players PA LEFT JOIN mutants M ON PA.id = M.Player_ID\n" +
                    "      GROUP BY PA.user_id)\n" +
                    "       AS Attacker ON U.user_id = Attacker.user_id\n" +
                    "     LEFT JOIN\n" +
                    "     (SELECT\n" +
                    "        PD.user_id,\n" +
                    "        sum(T.Points) AS DScore\n" +
                    "      FROM players PD LEFT JOIN tests T ON PD.id = T.Player_ID\n" +
                    "      GROUP BY PD.user_id)\n" +
                    "       AS Defender ON U.user_id = Defender.user_id) AS totalScore ON totalScore.User_ID = users.User_ID\n" +
                    "ORDER BY lastLogin DESC, User_ID;";

    public static final String PLAYERS_INFO_QUERY =
            "SELECT\n" +
                    "  ID,\n" +
                    "  Username,\n" +
                    "  Role,\n" +
                    "  lastSubmission,\n" +
                    "  TotalScore,\n" +
                    "  IFNULL(nbSubmissions, 0) as nbSubmissions\n" +
                    "FROM (SELECT\n" +
                    "        User_ID,\n" +
                    "        Role,\n" +
                    "        ID\n" +
                    "      FROM players\n" +
                    "      WHERE Game_ID = ? AND Active = TRUE) AS activePlayers\n" +
                    "  JOIN users ON activePlayers.User_ID = users.User_ID\n" +
                    "  JOIN\n" +
                    "  (SELECT\n" +
                    "     U.User_ID,\n" +
                    "     IFNULL(AScore, 0) + IFNULL(DScore, 0) AS TotalScore\n" +
                    "   FROM users U LEFT JOIN\n" +
                    "     (SELECT\n" +
                    "        PA.user_id,\n" +
                    "        sum(M.Points) AS AScore\n" +
                    "      FROM players PA LEFT JOIN mutants M ON PA.id = M.Player_ID\n" +
                    "      GROUP BY PA.user_id)\n" +
                    "       AS Attacker ON U.user_id = Attacker.user_id\n" +
                    "     LEFT JOIN\n" +
                    "     (SELECT\n" +
                    "        PD.user_id,\n" +
                    "        sum(T.Points) AS DScore\n" +
                    "      FROM players PD LEFT JOIN tests T ON PD.id = T.Player_ID\n" +
                    "      GROUP BY PD.user_id)\n" +
                    "       AS Defender ON U.user_id = Defender.user_id) AS totalScore ON totalScore.User_ID = users.User_ID\n" +
                    "  LEFT JOIN (SELECT\n" +
                    "               MAX(ts) AS lastSubmission,\n" +
                    "               Player_ID\n" +
                    "             FROM (SELECT\n" +
                    "                     MAX(mutants.Timestamp) AS ts,\n" +
                    "                     Player_ID\n" +
                    "                   FROM mutants\n" +
                    "                   GROUP BY Player_ID\n" +
                    "                   UNION\n" +
                    "                   SELECT\n" +
                    "                     MAX(tests.Timestamp) AS ts,\n" +
                    "                     Player_ID\n" +
                    "                   FROM tests\n" +
                    "                   GROUP BY Player_ID\n" +
                    "                  ) AS t\n" +
                    "             GROUP BY Player_ID) AS lastAction\n" +
                    "    ON Player_ID = activePlayers.ID\n" +
                    "  LEFT JOIN (SELECT\n" +
                    "               COUNT(*) AS nbSubmissions,\n" +
                    "               Player_ID\n" +
                    "             FROM (SELECT\n" +
                    "                     Player_ID,\n" +
                    "                     tests.Test_ID\n" +
                    "                   FROM (tests\n" +
                    "                     JOIN targetexecutions t2 ON tests.Test_ID = t2.Test_ID)\n" +
                    "                   WHERE t2.Target = 'COMPILE_TEST' AND t2.Status = 'SUCCESS'\n" +
                    "                   UNION SELECT\n" +
                    "                           Player_ID,\n" +
                    "                           mutants.Mutant_ID\n" +
                    "                         FROM (mutants\n" +
                    "                           JOIN targetexecutions t2 ON mutants.Mutant_ID = t2.Mutant_ID)\n" +
                    "                         WHERE t2.Target = 'COMPILE_MUTANT' AND t2.Status = 'SUCCESS') AS TestsAndMutants\n" +
                    "             GROUP BY Player_ID) AS submissions ON submissions.Player_ID = ID\n" +
                    "ORDER BY Role, nbSubmissions;";

    public final static String DELETE_PLAYER = "DELETE FROM players WHERE ID =?;";
    public final static String DELETE_TESTS = "DELETE FROM tests WHERE Player_ID =?;";
    public final static String DELETE_MUTANTS = "DELETE FROM mutants WHERE Player_ID =?;";
    public final static String DELETE_DEFENDER_EQUIVALENCES = "DELETE FROM equivalences WHERE Defender_ID =?;";
    public final static String DELETE_ATTACKER_EQUIVALENCES = "DELETE FROM equivalences\n" +
			"WHERE Mutant_ID IN (SELECT Mutant_ID\n" +
			"                    FROM mutants\n" +
			"                    WHERE Player_ID = ?);";
    public final static String DELETE_TEST_TARGETEXECUTIONS = "DELETE FROM targetexecutions WHERE Test_ID =?;";
    public final static String DELETE_MUTANT_TARGETEXECUTIONS = "DELETE FROM targetexecutions WHERE Mutant_ID = ?;";
    public final static String SET_USER_PW = "UPDATE users SET Password = ? WHERE User_ID = ?;";
    public final static String DELETE_USER = "DELETE FROM users WHERE User_ID = ?;";
	public final static String GET_ALL_SETTINGS = "SELECT * FROM settings;";
	public  final static String UPDATE_SETTING_1 = "UPDATE settings\n" +
            "SET ";
    public  final static String UPDATE_SETTING_2 = " = ?\n" +
            "WHERE name = ?;";
    private static final String GET_SETTING = "SELECT *\n" +
            "FROM settings WHERE settings.name = ?;";

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
                        rs.getTimestamp("Finish_Time").getTime(), rs.getString("State"), rs.getBoolean("RequiresValidation"),
                        rs.getInt("MaxAssertionsPerTest"),rs.getBoolean("ChatEnabled"),
                        CodeValidator.CodeValidatorLevel.valueOf(rs.getString("MutantValidator")), rs.getBoolean("MarkUncovered"));
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


    public static Role getLastRole(int uid) {
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, LAST_ROLE_QUERY, DB.getDBV(uid));
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        try {
            if (rs.next()) {
                return Role.valueOf(rs.getString(3));
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
        return deleteFromTable(DELETE_TESTS, pid);
    }

    public static boolean deletePlayerMutants(int pid) {
        return deleteFromTable(DELETE_MUTANTS, pid);
    }

    public static boolean deleteDefenderEquivalences(int pid) {
        return deleteFromTable(DELETE_DEFENDER_EQUIVALENCES, pid);
    }

    public static boolean deleteAttackerEquivalences(int pid) {
        return deleteFromTable(DELETE_ATTACKER_EQUIVALENCES, pid);
    }

    public static boolean deletePlayer(int pid) {
        return deleteFromTable(DELETE_PLAYER, pid);
    }

    public static boolean deleteTestTargetExecutions(int tid) {
        return deleteFromTable(DELETE_TEST_TARGETEXECUTIONS, tid);
    }

    public static boolean deleteMutantTargetExecutions(int mid) {
        return deleteFromTable(DELETE_MUTANT_TARGETEXECUTIONS, mid);
    }

    private static boolean deleteFromTable(String query, int id){
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(id));
        return DB.executeUpdate(stmt, conn);
    }

    public static List<List<String>> getUnassignedUsersInfo() {
        return getUsersInfo(UNASSIGNED_USERS_INFO_QUERY);
    }

    public static List<List<String>> getAllUsersInfo() {
        return getUsersInfo(USERS_INFO_QUERY);
    }


    private static List<List<String>> getUsersInfo(String query) {
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query);
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);

        List<List<String>> unassignedUsers = new ArrayList<>();
        try {
            while (rs.next()) {
                List<String> userInfo = new ArrayList<>();
                userInfo.add(String.valueOf(rs.getInt("User_ID")));
                userInfo.add(rs.getString("Username"));
                userInfo.add(rs.getString("Email"));
                Timestamp ts = rs.getTimestamp("lastLogin");
                userInfo.add(ts == null ? "-- never --" : ts.toString().substring(0, ts.toString().length() - 5));
                userInfo.add(rs.getString("lastRole"));
                userInfo.add(String.valueOf(rs.getInt("TotalScore")));
                unassignedUsers.add(userInfo);
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return unassignedUsers;
    }

    public static List<List<String>> getPlayersInfo(int gid) {
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, PLAYERS_INFO_QUERY, DB.getDBV(gid));
        long start = System.currentTimeMillis();
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        List<List<String>> players = new ArrayList<>();
        try {
            while (rs.next()) {
                List<String> playerInfo = new ArrayList<>();
                playerInfo.add(String.valueOf(rs.getInt("ID")));
                playerInfo.add(rs.getString("Username"));
                playerInfo.add(rs.getString("Role"));
                Timestamp ts = rs.getTimestamp("lastSubmission");
                playerInfo.add(ts == null ? TIMETSTAMP_NEVER: "" + ts.getTime());
                playerInfo.add(String.valueOf(rs.getInt("TotalScore")));
                playerInfo.add(String.valueOf(rs.getInt("nbSubmissions")));
                players.add(playerInfo);
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return players;
    }

    public static boolean setUserPassword(int uid, String password) {
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(password), DB.getDBV(uid)};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, SET_USER_PW, valueList);
        return DB.executeUpdate(stmt, conn);
    }

    public static boolean deleteUser(int uid) {
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, DELETE_USER, DB.getDBV(uid));
        return DB.executeUpdate(stmt, conn);
        // this does not work as foreign keys are not deleted (recommended: update w/ ON DELETE CASCADE)
    }

    public static List<AdminSystemSettings.SettingsDTO> getSystemSettings(){
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, GET_ALL_SETTINGS);
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        return getSettings(rs, conn, stmt);
	}

    public static boolean updateSystemSetting(AdminSystemSettings.SettingsDTO setting) {
        Connection conn = DB.getConnection();
        String valueToSet = setting.getType().name();
        DatabaseValue[] valueList = new DatabaseValue[]{null, DB.getDBV(setting.getName().name())};
        switch (setting.getType()) {
            case STRING_VALUE:
                valueList[0] = DB.getDBV(setting.getStringValue());
                break;
            case INT_VALUE:
                valueList[0] = DB.getDBV(setting.getIntValue());
                break;
            case BOOL_VALUE:
                valueList[0] = DB.getDBV(setting.getBoolValue());
                break;
        }
        PreparedStatement stmt = DB.createPreparedStatement(conn, UPDATE_SETTING_1 + valueToSet + UPDATE_SETTING_2, valueList);
        return DB.executeUpdate(stmt, conn);
    }

    private static List<AdminSystemSettings.SettingsDTO> getSettings(ResultSet rs, Connection conn, PreparedStatement stmt) {
        List<AdminSystemSettings.SettingsDTO> settings = new ArrayList<>();
        try {
            while (rs.next()) {
                AdminSystemSettings.SettingsDTO setting = null;
                AdminSystemSettings.SETTING_NAME name = AdminSystemSettings.SETTING_NAME.valueOf(rs.getString("name"));
                AdminSystemSettings.SETTING_TYPE settingType = AdminSystemSettings.SETTING_TYPE.valueOf(rs.getString("type"));
                switch (settingType) {
                    case STRING_VALUE:
                        setting = new AdminSystemSettings.SettingsDTO(name, rs.getString(settingType.name()));
                        break;
                    case INT_VALUE:
                        setting = new AdminSystemSettings.SettingsDTO(name,rs.getInt(settingType.name()));
                        break;
                    case BOOL_VALUE:
                        setting = new AdminSystemSettings.SettingsDTO(name,rs.getBoolean(settingType.name()));
                        break;
                }
                settings.add(setting);
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return settings;
    }

    public static AdminSystemSettings.SettingsDTO getSystemSetting(AdminSystemSettings.SETTING_NAME name){
        Connection conn = DB.getConnection();
		PreparedStatement stmt = DB.createPreparedStatement(conn, GET_SETTING, DB.getDBV(name.name()));
		ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
		return getSettings(rs, conn, stmt).get(0);
    }


	public static AdminSystemSettings.SettingsDTO getSystemSettingInt(AdminSystemSettings.SETTING_NAME name, Connection conn) throws SQLException {
		PreparedStatement stmt = conn.prepareStatement(GET_SETTING);
		stmt.setString(1, name.name());
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			AdminSystemSettings.SETTING_TYPE settingType = AdminSystemSettings.SETTING_TYPE.valueOf(rs.getString("type"));
			return new AdminSystemSettings.SettingsDTO(name, rs.getInt(settingType.name()));
		}
		return null;
	}
}
