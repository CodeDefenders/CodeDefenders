package org.codedefenders.util;

import org.codedefenders.*;
import org.codedefenders.duel.DuelGame;
import org.codedefenders.events.Event;
import org.codedefenders.events.EventStatus;
import org.codedefenders.leaderboard.Entry;
import org.codedefenders.multiplayer.LineCoverage;
import org.codedefenders.multiplayer.MultiplayerGame;
import org.codedefenders.singleplayer.NoDummyGameException;
import org.codedefenders.singleplayer.SinglePlayerGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ALL")
public class DatabaseAccess {


    private static final Logger logger = LoggerFactory.getLogger(DatabaseAccess.class);

    /**
     * Sanitises user input. If a whole SQL query is entered, syntax
     * errors may occur.
     *
     * @param s user input String
     * @return sanitised String s
     */
    public static String sanitise(String s) {
        s = s.replaceAll("\\<", "&lt;");
        s = s.replaceAll("\\>", "&gt;");
        s = s.replaceAll("\\\"", "&quot;");
        s = s.replaceAll("\\'", "&apos;");
        return s;
    }

	public static Connection getConnection() throws SQLException, NamingException {
		return DatabaseConnection.getConnection();
	}


    /**
     * Execute an insert statement and returns primary key.
     *
     * @param sql PreparedStatement to be executed
     */
    public static int executeInsert(PreparedStatement stmt, Connection conn) {
        try {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return -1;
    }

    public static boolean execute(String sql) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt.execute(sql);
            return true;
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return false;
    }

    public static int getInt(PreparedStatement stmt, String att, Connection conn) {
        int n = -1;
        try {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                n = rs.getInt(att);
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return n;
    }

    public static void cleanup(Connection c, Statement s) {
        try {
            if (s != null) {
                s.close();
            }
        } catch (SQLException se2) {
            logger.error("SQL exception caught", se2);
        }
        try {
            if (c != null) {
                c.close();
            }
        } catch (SQLException se3) {
            logger.error("SQL exception caught", se3);
        }
    }

    public static void insertMessage(int uid, String ipAddress) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("INSERT INTO events (User_ID, IP_Address) VALUES (?, ?);");
            stmt.setInt(1, uid);
            stmt.setString(2, ipAddress);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
    }

    public static ArrayList<Event> getEventsForGame(int gameId) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM events " + "LEFT JOIN event_messages AS em ON events.Event_Type = em" + ".Event_Type " + "LEFT JOIN event_chat AS ec ON events.Event_Id = ec.Event_Id " + "WHERE Game_ID=? " + "AND Event_Status=?");
            stmt.setInt(1, gameId);
            stmt.setString(2, EventStatus.GAME.toString());
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getEvents(stmt, conn);
    }

    public static void removePlayerEventsForGame(int gameId, int
            playerId) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM events WHERE Game_ID=? " + "AND Player_ID=?");
            stmt.setInt(1, gameId);
            stmt.setInt(2, playerId);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        for (Event e : getEvents(stmt, conn)) {
            e.setStatus(EventStatus.DELETED);
            e.update();
        }
    }

    public static ArrayList<Event> getNewEventsForGame(int gameId, long time, Role role) {

        String sql = "SELECT * FROM events " + "LEFT JOIN event_messages AS em ON events.Event_Type = em.Event_Type " + "LEFT JOIN event_chat AS ec ON events.Event_Id = ec" + ".Event_Id " + "WHERE Game_ID=? " + "AND Event_Status=? " + "AND Timestamp >= FROM_UNIXTIME(?)";
        if (role.equals(Role.ATTACKER)) {
            sql += " AND events.Event_Type!='DEFENDER_MESSAGE'";
        } else if (role.equals(Role.DEFENDER)) {
            sql += " AND events.Event_Type!='ATTACKER_MESSAGE'";
        }

        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, gameId);
            stmt.setString(2, EventStatus.GAME.toString());
            stmt.setLong(3, time);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }

        return getEvents(stmt, conn);
    }

    public static ArrayList<Event> getEventsForUser(int userId) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM events " + "LEFT JOIN event_messages AS em ON events.Event_Type = em.Event_Type " + "LEFT JOIN event_chat AS ec ON events.Event_Id = ec.Event_Id " + "WHERE " + "Event_Status!='DELETED' " + "AND Player_ID=?;");
            stmt.setInt(1, userId);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getEvents(stmt, conn);
    }

    public static ArrayList<Event> getNewEventsForUser(int userId, long time) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM events " + "LEFT JOIN event_messages AS em ON events.Event_Type = em.Event_Type " + "LEFT JOIN event_chat AS ec ON events.Event_Id = ec.Event_Id " + "WHERE Player_ID=? AND Event_Status<>? AND Event_Status<>? " + "AND Timestamp >= FROM_UNIXTIME(?)");
            stmt.setInt(1, userId);
            stmt.setString(2, EventStatus.DELETED.toString());
            stmt.setString(3, EventStatus.GAME.toString());
            stmt.setLong(4, time);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getEvents(stmt, conn);
    }

    public static void setGameAsAIDummy(int gId) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("UPDATE games SET IsAIDummyGame = 1 WHERE ID = ?;");
            stmt.setInt(1, gId);
            stmt.executeUpdate();

        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
    }

    public static DuelGame getAiDummyGameForClass(int cId) throws NoDummyGameException {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM games WHERE Class_ID=? AND IsAIDummyGame=1");
            stmt.setInt(1, cId);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        int gID = getInt(stmt, "ID", conn);
        if (gID == 0) {
            NoDummyGameException e = new NoDummyGameException("No dummy game found.");
            throw e;
        }
        return getGameForKey("ID", gID);
    }

    public static String addSlashes(String s) {
        return s.replaceAll("\\\\", "\\\\\\\\");
    }

    public static int getNumAiMutantsKilledByTest(int tId) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM tests WHERE Test_ID=?;");
            stmt.setInt(1, tId);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getInt(stmt, "NumberAiMutantsKilled", conn);
    }

    public static int getNumTestsKillMutant(int mId) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM mutants WHERE Mutant_ID=?;");
            stmt.setInt(1, mId);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getInt(stmt, "NumberAiKillingTests", conn);
    }

    public static boolean gameWithUserExistsForClass(int uId, int cId) {
        List<DuelGame> games = getGamesForUser(uId);
        for (DuelGame g : games) {
            if (g.getClassId() == cId) {
                return true;
            }
        }
        return false;
    }

    public static GameClass getClassForGame(int gameId) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT classes.* FROM classes INNER JOIN games ON classes.Class_ID = games.Class_ID WHERE games.ID=?;");
            stmt.setInt(1, gameId);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getClass(stmt, conn);
    }

    public static GameClass getClassForKey(String keyName, int id) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM classes WHERE " + keyName + "=?;");
            stmt.setInt(1, id);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getClass(stmt, conn);
    }

    public static boolean isAiPrepared(GameClass c) {
        boolean prepared = false;
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM classes WHERE AiPrepared = 1 AND Class_ID = ?");
            stmt.setInt(1, c.getId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                //Class is prepared
                prepared = true;
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return prepared;
    }

    public static void setAiPrepared(GameClass c) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("UPDATE classes SET AiPrepared = 1 WHERE Class_ID = ?;");
            stmt.setInt(1, c.getId());
            stmt.executeUpdate();
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
    }

    private static ArrayList<Event> getEvents(PreparedStatement stmt, Connection conn) {
        ArrayList<Event> events = new ArrayList<Event>();
        try {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Event event = new Event(rs.getInt("events.Event_ID"),
                        rs.getInt("Game_ID"),
                        rs.getInt("Player_ID"),
                        rs.getString("em.Message"),
                        rs.getString("events.Event_Type"),
                        rs.getString("Event_Status"),
                        rs.getTimestamp("Timestamp"));
                String chatMessage = rs.getString("ec.Message");
                event.setChatMessage(chatMessage);
                events.add(event);
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return events;
    }

    private static GameClass getClass(PreparedStatement stmt, Connection conn) {
        try {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                GameClass classRecord = new GameClass(rs.getInt("Class_ID"), rs.getString("Name"), rs.getString("Alias"), rs.getString("JavaFile"), rs.getString("ClassFile"));
                return classRecord;
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return null;
    }

    public static List<GameClass> getAllClasses() {
        List<GameClass> classList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM classes;");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                classList.add(new GameClass(rs.getInt("Class_ID"), rs.getString("Name"), rs.getString("Alias"), rs.getString("JavaFile"), rs.getString("ClassFile")));
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return classList;
    }

    public static List<User> getAllUsers() {
        Connection conn = null;
        PreparedStatement stmt = null;
        List<User> uList = new ArrayList<>();
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM users");

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                User userRecord = new User(rs.getInt("User_ID"), rs.getString("Username"), rs.getString("Password"), rs.getString("Email"), rs.getBoolean("Validated"));
                uList.add(userRecord);
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return uList;
    }

    public static User getUser(int uid) {
        return getUserForKey("User_ID", uid);
    }

    public static User getUserForNameOrEmail(String usernameOrEmail) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM users WHERE Username=? OR Email=?;");
            stmt.setString(1, usernameOrEmail);
            stmt.setString(2, usernameOrEmail);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getUserFromDB(stmt, conn);
    }

    public static User getUserFromPlayer(int playerId) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM users AS u " + "LEFT JOIN players AS p ON p.User_ID=u.User_ID " + "WHERE p.ID=?;");
            stmt.setInt(1, playerId);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getUserFromDB(stmt, conn);
    }

    public static User getUserForKey(String keyName, int id) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM users WHERE " + keyName + "=?;");
            stmt.setInt(1, id);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getUserFromDB(stmt, conn);
    }

    private static User getUserFromDB(PreparedStatement stmt, Connection conn) {
        try {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User userRecord = new User(rs.getInt("User_ID"), rs.getString("Username"), rs.getString("Password"), rs.getString("Email"), rs.getBoolean("Validated"));
                return userRecord;
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return null;
    }

    public static DuelGame getGameForKey(String keyName, int id) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT g.ID, g.Class_ID, g.Level, g.Creator_ID, g.State," + "g.CurrentRound, g.FinalRound, g.ActiveRole, g.Mode, g.Creator_ID,\n" + "IFNULL(att.User_ID,0) AS Attacker_ID, IFNULL(def.User_ID,0) AS Defender_ID\n" + "FROM games AS g\n" + "LEFT JOIN players AS att ON g.ID=att.Game_ID AND att.Role='ATTACKER' AND att.Active=TRUE\n" + "LEFT JOIN players AS def ON g.ID=def.Game_ID AND def.Role='DEFENDER' AND def.Active=TRUE\n" + "WHERE g." + keyName + "=?;\n");
            stmt.setInt(1, id);
            // Load the MultiplayerGame Data with the provided ID.
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                DuelGame gameRecord;
                if (rs.getString("Mode").equals(GameMode.SINGLE.name()))
                    gameRecord = new SinglePlayerGame(rs.getInt("ID"), rs.getInt("Attacker_ID"), rs.getInt("Defender_ID"), rs.getInt("Class_ID"),
                            rs.getInt("CurrentRound"), rs.getInt("FinalRound"), Role.valueOf(rs.getString("ActiveRole")), GameState.valueOf(rs.getString("State")),
                            GameLevel.valueOf(rs.getString("Level")), GameMode.valueOf(rs.getString("Mode")));
                else
                    gameRecord = new DuelGame(rs.getInt("ID"), rs.getInt("Attacker_ID"), rs.getInt("Defender_ID"), rs.getInt("Class_ID"),
                            rs.getInt("CurrentRound"), rs.getInt("FinalRound"), Role.valueOf(rs.getString("ActiveRole")), GameState.valueOf(rs.getString("State")),
                            GameLevel.valueOf(rs.getString("Level")), GameMode.valueOf(rs.getString("Mode")));
                stmt.close();
                conn.close();
                return gameRecord;
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return null;
    }

    /**
     * Returns list of <b>active</b> games for a user
     *
     * @param userId
     * @return
     */
    public static List<DuelGame> getGamesForUser(int userId) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT g.ID, g.Class_ID, g.Level, g.Creator_ID, g.State, g.CurrentRound, g.FinalRound, g.ActiveRole, g.Mode, g.Creator_ID,\n" + "IFNULL(att.User_ID,0) AS Attacker_ID, IFNULL(def.User_ID,0) AS Defender_ID FROM games AS g LEFT JOIN players AS att ON g.ID=att.Game_ID  AND att.Role='ATTACKER' AND att.Active=TRUE\n" + "LEFT JOIN players AS def ON g.ID=def.Game_ID AND def.Role='DEFENDER' AND def.Active=TRUE WHERE g.Mode != 'PARTY' AND g.State!='FINISHED' AND (g.Creator_ID=? OR IFNULL(att.User_ID,0)=? OR IFNULL(def.User_ID,0)=?);");
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getGames(stmt, conn);
    }

    public static List<MultiplayerGame> getJoinedMultiplayerGamesForUser(int userId) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM games AS m " + "LEFT JOIN players AS p ON p.Game_ID=m.ID \n" + "WHERE m.Mode = 'PARTY' AND (p.User_ID=?)" + "GROUP BY m.ID;");
            stmt.setInt(1, userId);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getMultiplayerGames(stmt, conn);
    }

    public static List<MultiplayerGame> getMultiplayerGamesForUser(int userId) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM games AS m LEFT JOIN players AS p ON p.Game_ID=m.ID  AND p.Active=TRUE" +
                    " WHERE m.Mode = 'PARTY' AND (p.User_ID=? OR m.Creator_ID=?) AND m.State != 'FINISHED' GROUP BY m.ID;");
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getMultiplayerGames(stmt, conn);
    }

    public static List<MultiplayerGame> getFinishedMultiplayerGamesForUser(int userId) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM games AS m " + "LEFT JOIN players AS p ON p.Game_ID=m.ID  AND p.Active=TRUE \n" + "WHERE (p.User_ID=? OR m.Creator_ID=?) AND m.State = 'FINISHED' AND m.Mode='PARTY'" + "GROUP BY m.ID;");
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getMultiplayerGames(stmt, conn);
    }

    public static List<MultiplayerGame> getOpenMultiplayerGamesForUser(int userId) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM games AS g\n" + "INNER JOIN (SELECT gatt.ID, sum(CASE WHEN Role = 'ATTACKER' THEN 1 ELSE 0 END) nAttackers, sum(CASE WHEN Role = 'DEFENDER' THEN 1 ELSE 0 END) nDefenders\n" + "              FROM games AS gatt LEFT JOIN players ON gatt.ID=players.Game_ID AND players.Active=TRUE GROUP BY gatt.ID) AS nplayers\n" + "ON g.ID=nplayers.ID\n" + "WHERE g.Mode='PARTY' AND g.Creator_ID!=? AND (g.State='CREATED' OR g.State='ACTIVE')\n" + "\tAND (g.RequiresValidation=FALSE OR (? IN (SELECT User_ID FROM users WHERE Validated=TRUE)))\n" + "\tAND g.ID NOT IN (SELECT g.ID FROM games g INNER JOIN players p ON g.ID=p.Game_ID WHERE p.User_ID=? AND p.Active=TRUE)\n" + "\tAND nplayers.nAttackers < g.Attackers_Limit AND nplayers.nDefenders < g.Defenders_Limit;");
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getMultiplayerGames(stmt, conn);
    }

    public static Role getRole(int userId, int gameId) {
        PreparedStatement stmt = null;
        Connection conn = null;
        Role role = Role.NONE;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM games AS m " + "LEFT JOIN players AS p ON p.Game_ID = m.ID AND p.Active=TRUE " + "WHERE m.ID = ? AND (m.Creator_ID=? OR (p.User_ID=? AND p.Game_ID=?))");
            stmt.setInt(1, gameId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, gameId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                if (rs.getInt("Creator_ID") == userId) {
                    role = Role.CREATOR;
                } else {
                    try {
                        role = Role.valueOf(rs.getString("Role"));
                    } catch (NullPointerException | SQLException e) {
                    }
                }
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return role;
    }

    /**
     * Returns list of <b>finished</b> games for a user
     *
     * @param userId
     * @return
     */
    public static List<DuelGame> getHistoryForUser(int userId) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT g.ID, g.Class_ID, g.Level, g.Creator_ID, g.State," + "g.CurrentRound, g.FinalRound, g.ActiveRole, g.Mode, g.Creator_ID,\n" + "IFNULL(att.User_ID,0) AS Attacker_ID, IFNULL(def.User_ID,0) AS Defender_ID\n" + "FROM games AS g\n" + "LEFT JOIN players AS att ON g.ID=att.Game_ID  AND att.Role='ATTACKER'\n" + "LEFT JOIN players AS def ON g.ID=def.Game_ID AND def.Role='DEFENDER'\n" + "WHERE g.Mode != 'PARTY' AND g.State='FINISHED' AND (g.Creator_ID=? OR IFNULL(att.User_ID,0)=? OR IFNULL(def.User_ID,0)=?);\n");
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getGames(stmt, conn);
    }

    public static List<DuelGame> getOpenGames() {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT g.ID, g.Class_ID, g.Level, g.Creator_ID, g.State," + "g.CurrentRound, g.FinalRound, g.ActiveRole, g.Mode, g.Creator_ID,\n" + "IFNULL(att.User_ID,0) AS Attacker_ID, IFNULL(def.User_ID,0) AS Defender_ID\n" + "FROM games AS g\n" + "LEFT JOIN players AS att ON g.ID=att.Game_ID  AND att.Role='ATTACKER' AND att.Active=TRUE\n" + "LEFT JOIN players AS def ON g.ID=def.Game_ID AND def.Role='DEFENDER' AND def.Active=TRUE\n" + "WHERE g.Mode = 'DUEL' AND g.State = 'CREATED';");

        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getGames(stmt, conn);
    }

    public static DuelGame getActiveUnitTestingSession(int userId) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM games WHERE Defender_ID=? AND Mode='UTESTING' AND State='ACTIVE';");
            stmt.setInt(1, userId);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        List<DuelGame> games = getGames(stmt, conn);
        if (games.isEmpty())
            return null;
        else
            return games.get(0);
    }

    public static List<DuelGame> getGames(PreparedStatement stmt, Connection conn) {
        List<DuelGame> gameList = new ArrayList<>();
        try {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                gameList.add(new DuelGame(rs.getInt("ID"), rs.getInt("Attacker_ID"), rs.getInt("Defender_ID"),
                        rs.getInt("Class_ID"), rs.getInt("CurrentRound"), rs.getInt("FinalRound"),
                        Role.valueOf(rs.getString("ActiveRole")), GameState.valueOf(rs.getString("State")),
                        GameLevel.valueOf(rs.getString("Level")), GameMode.valueOf(rs.getString("Mode"))));
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return gameList;
    }

    public static MultiplayerGame getMultiplayerGame(int id) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM games AS m WHERE ID=? AND m.Mode='PARTY'");
            stmt.setInt(1, id);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        List<MultiplayerGame> mgs = getMultiplayerGames(stmt, conn);
        if (mgs.size() > 0) {
            return mgs.get(0);
        }
        return null;
    }

    public static List<MultiplayerGame> getMultiplayerGames(PreparedStatement stmt, Connection conn) {
        List<MultiplayerGame> gameList = new ArrayList<>();
        try {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                MultiplayerGame mg = new MultiplayerGame(rs.getInt("Class_ID"), rs.getInt("Creator_ID"),
                        GameLevel.valueOf(rs.getString("Level")), (float) rs.getDouble("Coverage_Goal"),
                        (float) rs.getDouble("Mutant_Goal"), rs.getInt("Prize"), rs.getInt("Defender_Value"),
                        rs.getInt("Attacker_Value"), rs.getInt("Defenders_Limit"), rs.getInt("Attackers_Limit"),
                        rs.getInt("Defenders_Needed"), rs.getInt("Attackers_Needed"), rs.getTimestamp("Start_Time").getTime(),
                        rs.getTimestamp("Finish_Time").getTime(), rs.getString("State"), rs.getBoolean("RequiresValidation"));
                mg.setId(rs.getInt("ID"));
                gameList.add(mg);
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return gameList;
    }

    public static List<Mutant> getMutantsForGame(int gid) {
        List<Mutant> mutList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM mutants WHERE Game_ID=? AND ClassFile IS NOT NULL;");
            stmt.setInt(1, gid);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Mutant newMutant = new Mutant(rs.getInt("Mutant_ID"), rs.getInt("Game_ID"),
                        rs.getString("JavaFile"), rs.getString("ClassFile"),
                        rs.getBoolean("Alive"), Mutant.Equivalence.valueOf(rs.getString("Equivalent")),
                        rs.getInt("RoundCreated"), rs.getInt("RoundKilled"), rs.getInt("Player_ID"));
                newMutant.setScore(rs.getInt("Points"));
                mutList.add(newMutant);
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return mutList;
    }

    public static List<Mutant> getMutantsForPlayer(int pid) {
        List<Mutant> mutList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM mutants WHERE Player_ID=? AND ClassFile IS NOT NULL;");
            stmt.setInt(1, pid);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Mutant newMutant = new Mutant(rs.getInt("Mutant_ID"), rs.getInt("Game_ID"),
                        rs.getString("JavaFile"), rs.getString("ClassFile"),
                        rs.getBoolean("Alive"), Mutant.Equivalence.valueOf(rs.getString("Equivalent")),
                        rs.getInt("RoundCreated"), rs.getInt("RoundKilled"), rs.getInt("Player_ID"));
                newMutant.setScore(rs.getInt("Points"));
                mutList.add(newMutant);
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return mutList;
    }

    public static Mutant getMutantFromDB(PreparedStatement stmt, Connection conn) {
        Mutant newMutant = null;
        try {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                newMutant = new Mutant(rs.getInt("Mutant_ID"), rs.getInt("Game_ID"),
                        rs.getString("JavaFile"), rs.getString("ClassFile"),
                        rs.getBoolean("Alive"), Mutant.Equivalence.valueOf(rs.getString("Equivalent")),
                        rs.getInt("RoundCreated"), rs.getInt("RoundKilled"), rs.getInt("Player_ID"));
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return newMutant;
    }

    public static Mutant getMutant(DuelGame game, int mutantID) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM mutants WHERE Mutant_ID=? AND Game_ID=?;");
            stmt.setInt(1, mutantID);
            stmt.setInt(2, game.getId());
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getMutantFromDB(stmt, conn);
    }

    public static Mutant getMutant(int gameId, String md5) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM mutants WHERE Game_ID=? AND MD5=?;");
            stmt.setInt(1, gameId);
            stmt.setString(2, md5);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getMutantFromDB(stmt, conn);
    }

    public static List<Integer> getUsedAiTestsForGame(DuelGame g) {
        List<Integer> testList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM usedaitests WHERE Game_ID=?;");
            stmt.setInt(1, g.getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                testList.add(rs.getInt("Value"));
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return testList;
    }

    public static void increasePlayerPoints(int points, int player) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("UPDATE players SET Points=Points+? WHERE ID=?");
            stmt.setInt(1, points);
            stmt.setInt(2, player);
            stmt.executeUpdate();
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
    }

    public static int getEquivalentDefenderId(Mutant m) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM equivalences WHERE Mutant_ID=?;");
            stmt.setInt(1, m.getId());
            ResultSet rs = stmt.executeQuery();
            int id = -1;
            while (rs.next()) {
                id = rs.getInt("Defender_ID");
            }
            return id;
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return -1;
    }

    public static int getPlayerPoints(int playerId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM players WHERE ID=?;");
            stmt.setInt(1, playerId);
            ResultSet rs = stmt.executeQuery();
            int points = 0;
            while (rs.next()) {
                points = rs.getInt("Points");
            }
            return points;
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return 0;
    }

    public static boolean insertEquivalence(Mutant mutant, int defender) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("INSERT INTO equivalences " +
                            "(Mutant_ID, Defender_ID, Mutant_Points) VALUES " + "(?, ?, ?)");
            stmt.setInt(1, mutant.getId());
            stmt.setInt(2, defender);
            stmt.setInt(3, mutant.getScore());
            return stmt.executeUpdate() > 0;
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return false;
    }

    public static boolean setAiTestAsUsed(int testNumber, DuelGame g) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("INSERT INTO usedaitests (Value, Game_ID) VALUES (?, ?);");
            stmt.setInt(1, testNumber);
            stmt.setInt(2, g.getId());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                stmt.close();
                conn.close();
                return true;
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return false;
    }

    public static List<Integer> getUsedAiMutantsForGame(DuelGame g) {
        List<Integer> mutantList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM usedaimutants WHERE Game_ID=?;");
            stmt.setInt(1, g.getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                mutantList.add(rs.getInt("Value"));
            }
            stmt.close();
            conn.close();
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return mutantList;
    }

    /**
     * @param mutantNumber the number of the mutant
     * @param g            the game the mutant belongs to
     * @return
     */
    public static boolean setAiMutantAsUsed(int mutantNumber, DuelGame g) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("INSERT INTO usedaimutants (Value, Game_ID) VALUES (?, ?);");
            stmt.setInt(1, mutantNumber);
            stmt.setInt(2, g.getId());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return true;
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return false;
    }

    public static List<Test> getTestsForGame(int gid) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM tests WHERE Game_ID=?;");
            stmt.setInt(1, gid);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getTests(stmt, conn);
    }

    public static Test getTestForId(int tid) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM tests WHERE Test_ID=?;");
            stmt.setInt(1, tid);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getTests(stmt, conn).get(0);
    }

    /**
     * @param gameId
     * @param defendersOnly
     * @return Tests submitted by defenders which compiled and passed on CUT
     */
    public static List<Test> getExecutableTests(int gameId, boolean defendersOnly) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT tests.* FROM tests\n" + "INNER JOIN targetexecutions ex on tests.Test_ID = ex.Test_ID\n" + (defendersOnly ? "INNER JOIN players pl on tests.Player_ID = pl.ID\n" : "") + "WHERE tests.Game_ID=? AND tests.ClassFile IS NOT NULL\n" + (defendersOnly ? "AND pl.Role='DEFENDER'\n" : "") + "AND ex.Target='TEST_ORIGINAL' AND ex.Status='SUCCESS';");
            stmt.setInt(1, gameId);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getTests(stmt, conn);
    }

    public static int getPlayerIdForMultiplayerGame(int userId, int gameId) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM players AS p " + "WHERE p.User_ID = ? AND p.Game_ID = ?");
            stmt.setInt(1, userId);
            stmt.setInt(2, gameId);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getInt(stmt, "ID", conn);
    }

    public static int[] getPlayersForMultiplayerGame(int gameId, Role role) {
        Connection conn = null;
        PreparedStatement stmt = null;
        int[] players = new int[0];
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM players WHERE Game_ID = ? AND Role=? AND Active=TRUE;");
            stmt.setInt(1, gameId);
            stmt.setString(2, role.toString());
            // Load the MultiplayerGame Data with the provided ID.
            ResultSet rs = stmt.executeQuery();
            List<Integer> atks = new ArrayList<>();
            while (rs.next()) {
                atks.add(rs.getInt("ID"));
            }
            players = new int[atks.size()];
            for (int i = 0; i < atks.size(); i++) {
                players[i] = atks.get(i);
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return players;
    }

    private static List<Test> getTests(PreparedStatement stmt, Connection conn) {
        List<Test> testList = new ArrayList<>();
        try {
            // Load the MultiplayerGame Data with the provided ID.
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Test newTest = new Test(rs.getInt("Test_ID"), rs.getInt("Game_ID"),
                        rs.getString("JavaFile"), rs.getString("ClassFile"),
                        rs.getInt("RoundCreated"), rs.getInt("MutantsKilled"), rs.getInt("Player_ID"));
                String lcs = rs.getString("Lines_Covered");
                String lucs = rs.getString("Lines_Uncovered");
                LineCoverage lc = new LineCoverage();
                if (lcs != null && lucs != null && lcs.length() > 0 && lucs.length() > 0) {
                    ArrayList<Integer> covered = new ArrayList<>();
                    ArrayList<Integer> uncovered = new ArrayList<>();
                    for (String s : lcs.split(",")) {
                        covered.add(Integer.parseInt(s));
                    }
                    for (String s : lucs.split(",")) {
                        uncovered.add(Integer.parseInt(s));
                    }
                    lc.setLinesUncovered(uncovered);
                    lc.setLinesCovered(covered);
                }
                newTest.setLineCoverage(lc);
                newTest.setScore(rs.getInt("Points"));
                testList.add(newTest);
            }
            stmt.close();
            conn.close();
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return testList;
    }

    public static List<Entry> getLeaderboard() {
        Connection conn = null;
        PreparedStatement stmt = null;
        String sql = "SELECT U.username AS username, IFNULL(NMutants,0) AS NMutants, IFNULL(AScore,0) AS AScore, IFNULL(NTests,0) AS NTests, IFNULL(DScore,0) AS DScore, IFNULL(NKilled,0) AS NKilled, IFNULL(AScore,0)+IFNULL(DScore,0) AS TotalScore\n" +
                "FROM users U LEFT JOIN\n" +
                "  (SELECT PA.user_id, count(M.Mutant_ID) AS NMutants, sum(M.Points) AS AScore FROM players PA LEFT JOIN mutants M ON PA.id = M.Player_ID GROUP BY PA.user_id)\n" +
                "    AS Attacker ON U.user_id = Attacker.user_id\n" +
                "  LEFT JOIN\n" +
                "  (SELECT PD.user_id, count(T.Test_ID) AS NTests, sum(T.Points) AS DScore, sum(T.MutantsKilled) AS NKilled FROM players PD LEFT JOIN tests T ON PD.id = T.Player_ID GROUP BY PD.user_id)\n" +
                "    AS Defender ON U.user_id = Defender.user_id\n" +
                "WHERE U.user_id > 2;";
        List<Entry> leaderboard = new ArrayList<>();
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Entry p = new Entry();
                p.setUsername(rs.getString("username"));
                p.setMutantsSubmitted(rs.getInt("NMutants"));
                p.setAttackerScore(rs.getInt("AScore"));
                p.setTestsSubmitted(rs.getInt("NTests"));
                p.setDefenderScore(rs.getInt("DScore"));
                p.setMutantsKilled(rs.getInt("NKilled"));
                p.setTotalPoints(rs.getInt("TotalScore"));
                leaderboard.add(p);
            }
            stmt.close();
            conn.close();
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return leaderboard;
    }

    public static int getKillingTestIdForMutant(int mid) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM targetexecutions WHERE Target='TEST_MUTANT' AND Status!='SUCCESS' AND Mutant_ID=?;");
            stmt.setInt(1, mid);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        TargetExecution targ = getTargetExecutionSQL(stmt, conn);
        return (targ != null) ? targ.testId : -1; // TODO: We shouldn't give away that we don't know which test killed the mutant?
    }

    public static TargetExecution getTargetExecutionForPair(int tid, int mid) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM targetexecutions WHERE Test_ID=? AND Mutant_ID=?;");
            stmt.setInt(1, tid);
            stmt.setInt(2, mid);
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getTargetExecutionSQL(stmt, conn);
    }

    public static TargetExecution getTargetExecutionForTest(Test test, TargetExecution.Target target) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM targetexecutions WHERE Test_ID=? AND Target=?;");
            stmt.setInt(1, test.getId());
            stmt.setString(2, target.name());
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getTargetExecutionSQL(stmt, conn);
    }

    public static TargetExecution getTargetExecutionForMutant(Mutant mutant, TargetExecution.Target target) {
        PreparedStatement stmt = null;
        Connection conn = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("SELECT * FROM targetexecutions WHERE Mutant_ID=? AND Target=?;");
            stmt.setInt(1, mutant.getId());
            stmt.setString(2, target.name());
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        }
        return getTargetExecutionSQL(stmt, conn);
    }

    public static TargetExecution getTargetExecutionSQL(PreparedStatement stmt, Connection conn) {
        try {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                TargetExecution targetExecution = new TargetExecution(rs.getInt("TargetExecution_ID"), rs.getInt("Test_ID"),
                        rs.getInt("Mutant_ID"), TargetExecution.Target.valueOf(rs.getString("Target")),
                        rs.getString("Status"), rs.getString("Message"), rs.getString("Timestamp"));
                return targetExecution;
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
        return null;
    }

    public static void logSession(int uid, String ipAddress) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.prepareStatement("INSERT INTO sessions (User_ID, IP_Address) VALUES (?, ?);");
            stmt.setInt(1, uid);
            stmt.setString(2, ipAddress);
            stmt.executeUpdate();
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            cleanup(conn, stmt);
        }
    }
}
