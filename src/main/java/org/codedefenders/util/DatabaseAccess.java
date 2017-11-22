package org.codedefenders.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.codedefenders.GameClass;
import org.codedefenders.GameLevel;
import org.codedefenders.GameMode;
import org.codedefenders.GameState;
import org.codedefenders.Mutant;
import org.codedefenders.Role;
import org.codedefenders.TargetExecution;
import org.codedefenders.Test;
import org.codedefenders.User;
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


    public static int getInt(PreparedStatement stmt, String att, Connection conn) {
        int n = -1;
        try {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                n = rs.getInt(att);
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            DB.cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return n;
    }

    public static ArrayList<Event> getEventsForGame(int gameId) {
        String query = "SELECT * FROM events " + "LEFT JOIN event_messages AS em ON events.Event_Type = em" + ".Event_Type " + "LEFT JOIN event_chat AS ec ON events.Event_Id = ec.Event_Id " + "WHERE Game_ID=? " + "AND Event_Status=?";
        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(gameId),
                DB.getDBV(EventStatus.GAME.toString())
        };
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return getEvents(stmt, conn);
    }

    public static void removePlayerEventsForGame(int gameId, int playerId) {
        String query = "SELECT * FROM events WHERE Game_ID=? " + "AND Player_ID=?";
        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(gameId),
                DB.getDBV(playerId)};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        for (Event e : getEvents(stmt, conn)) {
            e.setStatus(EventStatus.DELETED);
            e.update();
        }
    }

    public static ArrayList<Event> getNewEventsForGame(int gameId, long time, Role role) {
        String query = "SELECT * FROM events LEFT JOIN event_messages AS em ON events.Event_Type = em.Event_Type " +
                "LEFT JOIN event_chat AS ec ON events.Event_Id = ec.Event_Id WHERE Game_ID=? AND Event_Status=? " +
                "AND Timestamp >= FROM_UNIXTIME(?)";
        if (role.equals(Role.ATTACKER)) {
            query += " AND events.Event_Type!='DEFENDER_MESSAGE'";
        } else if (role.equals(Role.DEFENDER)) {
            query += " AND events.Event_Type!='ATTACKER_MESSAGE'";
        }

        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(gameId),
                DB.getDBV(EventStatus.GAME.toString()),
                DB.getDBV((Long) time)};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return getEvents(stmt, conn);
    }

    public static ArrayList<Event> getEventsForUser(int userId) {
        String query = "SELECT * FROM events " + "LEFT JOIN event_messages AS em ON events.Event_Type = em.Event_Type " + "LEFT JOIN event_chat AS ec ON events.Event_Id = ec.Event_Id " + "WHERE " + "Event_Status!='DELETED' " + "AND Player_ID=?;";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(userId));
        return getEvents(stmt, conn);
    }

    public static ArrayList<Event> getNewEventsForUser(int userId, long time) {
        String query = "SELECT * FROM events " + "LEFT JOIN event_messages AS em ON events.Event_Type = em.Event_Type " + "LEFT JOIN event_chat AS ec ON events.Event_Id = ec.Event_Id " + "WHERE Player_ID=? AND Event_Status<>? AND Event_Status<>? " + "AND Timestamp >= FROM_UNIXTIME(?)";
        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(userId),
                DB.getDBV(EventStatus.DELETED.toString()),
                DB.getDBV(EventStatus.GAME.toString()),
                DB.getDBV((Long) time)};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return getEvents(stmt, conn);
    }

    public static void setGameAsAIDummy(int gId) {
        String query = "UPDATE games SET IsAIDummyGame = 1 WHERE ID = ?;";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(gId));
        DB.executeUpdate(stmt, conn);
    }

    public static DuelGame getAiDummyGameForClass(int cId) throws NoDummyGameException {
        String query = "SELECT * FROM games WHERE Class_ID=? AND IsAIDummyGame=1";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(cId));
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
        String query = "SELECT * FROM tests WHERE Test_ID=?;";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(tId));
        return getInt(stmt, "NumberAiMutantsKilled", conn);
    }

    public static int getNumTestsKillMutant(int mId) {
        String query = "SELECT * FROM mutants WHERE Mutant_ID=?;";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(mId));
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
        String query = "SELECT classes.* FROM classes INNER JOIN games ON classes.Class_ID = games.Class_ID WHERE games.ID=?;";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(gameId));
        return getClass(stmt, conn);
    }

    public static GameClass getClassForKey(String keyName, int id) {
        String query = "SELECT * FROM classes WHERE " + keyName + "=?;";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(id));
        return getClass(stmt, conn);
    }

    public static boolean isAiPrepared(GameClass c) {
        String query = "SELECT * FROM classes WHERE AiPrepared = 1 AND Class_ID = ?";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(c.getId()));
        return DB.executeUpdateGetKeys(stmt, conn) > -1;
    }

    public static void setAiPrepared(GameClass c) {
        String query = "UPDATE classes SET AiPrepared = 1 WHERE Class_ID = ?;";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(c.getId()));
        DB.executeUpdate(stmt, conn);
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
            DB.cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
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
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return null;
    }

    public static List<GameClass> getAllClasses() {
        List<GameClass> classList = new ArrayList<>();

        String query = "SELECT * FROM classes;";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query);
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        try {
            while (rs.next()) {
                classList.add(new GameClass(rs.getInt("Class_ID"), rs.getString("Name"), rs.getString("Alias"), rs.getString("JavaFile"), rs.getString("ClassFile")));
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return classList;
    }

    public static List<User> getAllUsers() {
        List<User> uList = new ArrayList<>();
        String query = "SELECT * FROM users";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query);
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        try {
            while (rs.next()) {
                User userRecord = new User(rs.getInt("User_ID"), rs.getString("Username"), rs.getString("Password"), rs.getString("Email"), rs.getBoolean("Validated"));
                uList.add(userRecord);
            }
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            DB.cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return uList;
    }

    public static User getUser(int uid) {
        return getUserForKey("User_ID", uid);
    }

    public static User getUserForNameOrEmail(String usernameOrEmail) {
        String query = "SELECT * FROM users WHERE Username=? OR Email=?;";
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(usernameOrEmail),
                DB.getDBV(usernameOrEmail)};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return getUserFromDB(stmt, conn);
    }

    public static User getUserFromPlayer(int playerId) {
        String query = "SELECT * FROM users AS u " + "LEFT JOIN players AS p ON p.User_ID=u.User_ID " + "WHERE p.ID=?;";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(playerId));
        return getUserFromDB(stmt, conn);
    }

    public static User getUserForKey(String keyName, int id) {
        String query = "SELECT * FROM users WHERE " + keyName + "=?;";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(id));
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
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return null;
    }

    public static DuelGame getGameForKey(String keyName, int id) {
        String query = "SELECT g.ID, g.Class_ID, g.Level, g.Creator_ID, g.State," + "g.CurrentRound, g.FinalRound, g.ActiveRole, g.Mode, g.Creator_ID,\n" + "IFNULL(att.User_ID,0) AS Attacker_ID, IFNULL(def.User_ID,0) AS Defender_ID\n" + "FROM games AS g\n" + "LEFT JOIN players AS att ON g.ID=att.Game_ID AND att.Role='ATTACKER' AND att.Active=TRUE\n" + "LEFT JOIN players AS def ON g.ID=def.Game_ID AND def.Role='DEFENDER' AND def.Active=TRUE\n" + "WHERE g." + keyName + "=?;\n";
        // Load the MultiplayerGame Data with the provided ID.
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(id));
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        try {
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
                return gameRecord;
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

    /**
     * Returns list of <b>active</b> games for a user
     *
     * @param userId
     * @return
     */
    public static List<DuelGame> getGamesForUser(int userId) {
        String query = "SELECT g.ID, g.Class_ID, g.Level, g.Creator_ID, g.State, g.CurrentRound, g.FinalRound, g.ActiveRole, g.Mode, g.Creator_ID,\n" + "IFNULL(att.User_ID,0) AS Attacker_ID, IFNULL(def.User_ID,0) AS Defender_ID FROM games AS g LEFT JOIN players AS att ON g.ID=att.Game_ID  AND att.Role='ATTACKER' AND att.Active=TRUE\n" + "LEFT JOIN players AS def ON g.ID=def.Game_ID AND def.Role='DEFENDER' AND def.Active=TRUE WHERE g.Mode != 'PARTY' AND g.State!='FINISHED' AND (g.Creator_ID=? OR IFNULL(att.User_ID,0)=? OR IFNULL(def.User_ID,0)=?);";
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(userId),
                DB.getDBV(userId),
                DB.getDBV(userId)};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return getGames(stmt, conn);
    }

    public static List<MultiplayerGame> getJoinedMultiplayerGamesForUser(int userId) {
        String query = "SELECT * FROM games AS m " + "LEFT JOIN players AS p ON p.Game_ID=m.ID \n" + "WHERE m.Mode = 'PARTY' AND (p.User_ID=?)" + "GROUP BY m.ID;";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(userId));
        return getMultiplayerGames(stmt, conn);
    }

    public static List<MultiplayerGame> getMultiplayerGamesForUser(int userId) {
        String query = "SELECT * FROM games AS m LEFT JOIN players AS p ON p.Game_ID=m.ID  AND p.Active=TRUE" +
                " WHERE m.Mode = 'PARTY' AND (p.User_ID=? OR m.Creator_ID=?) AND m.State != 'FINISHED' GROUP BY m.ID;";
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(userId),
                DB.getDBV(userId)};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return getMultiplayerGames(stmt, conn);
    }

    public static List<MultiplayerGame> getFinishedMultiplayerGamesForUser(int userId) {
        String query = "SELECT * FROM games AS m " + "LEFT JOIN players AS p ON p.Game_ID=m.ID  AND p.Active=TRUE \n" + "WHERE (p.User_ID=? OR m.Creator_ID=?) AND m.State = 'FINISHED' AND m.Mode='PARTY'" + "GROUP BY m.ID;";
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(userId),
                DB.getDBV(userId)};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return getMultiplayerGames(stmt, conn);
    }

    public static List<MultiplayerGame> getOpenMultiplayerGamesForUser(int userId) {
        String query = "SELECT * FROM games AS g\n" + "INNER JOIN (SELECT gatt.ID, sum(CASE WHEN Role = 'ATTACKER' THEN 1 ELSE 0 END) nAttackers, sum(CASE WHEN Role = 'DEFENDER' THEN 1 ELSE 0 END) nDefenders\n" + "              FROM games AS gatt LEFT JOIN players ON gatt.ID=players.Game_ID AND players.Active=TRUE GROUP BY gatt.ID) AS nplayers\n" + "ON g.ID=nplayers.ID\n" + "WHERE g.Mode='PARTY' AND g.Creator_ID!=? AND (g.State='CREATED' OR g.State='ACTIVE')\n" + "\tAND (g.RequiresValidation=FALSE OR (? IN (SELECT User_ID FROM users WHERE Validated=TRUE)))\n" + "\tAND g.ID NOT IN (SELECT g.ID FROM games g INNER JOIN players p ON g.ID=p.Game_ID WHERE p.User_ID=? AND p.Active=TRUE)\n" + "\tAND nplayers.nAttackers < g.Attackers_Limit AND nplayers.nDefenders < g.Defenders_Limit;";
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(userId),
                DB.getDBV(userId),
                DB.getDBV(userId)};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return getMultiplayerGames(stmt, conn);
    }

    public static Role getRole(int userId, int gameId) {
        Role role = Role.NONE;
        String query = "SELECT * FROM games AS m " + "LEFT JOIN players AS p ON p.Game_ID = m.ID AND p.Active=TRUE " + "WHERE m.ID = ? AND (m.Creator_ID=? OR (p.User_ID=? AND p.Game_ID=?))";
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(gameId),
                DB.getDBV(userId),
                DB.getDBV(userId),
                DB.getDBV(gameId)};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);

        try {
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
        } catch (SQLException e) {
            logger.error("SQLException while parsing result set for statement\n\t" + query, stmt);
        } finally {
            DB.cleanup(conn, stmt);
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
        String query = "SELECT g.ID, g.Class_ID, g.Level, g.Creator_ID, g.State," + "g.CurrentRound, g.FinalRound, g.ActiveRole, g.Mode, g.Creator_ID,\n" + "IFNULL(att.User_ID,0) AS Attacker_ID, IFNULL(def.User_ID,0) AS Defender_ID\n" + "FROM games AS g\n" + "LEFT JOIN players AS att ON g.ID=att.Game_ID  AND att.Role='ATTACKER'\n" + "LEFT JOIN players AS def ON g.ID=def.Game_ID AND def.Role='DEFENDER'\n" + "WHERE g.Mode != 'PARTY' AND g.State='FINISHED' AND (g.Creator_ID=? OR IFNULL(att.User_ID,0)=? OR IFNULL(def.User_ID,0)=?);\n";
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(userId),
                DB.getDBV(userId),
                DB.getDBV(userId)};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return getGames(stmt, conn);
    }

    public static List<DuelGame> getOpenGames() {
        String query = "SELECT g.ID, g.Class_ID, g.Level, g.Creator_ID, g.State," + "g.CurrentRound, g.FinalRound, g.ActiveRole, g.Mode, g.Creator_ID,\n" + "IFNULL(att.User_ID,0) AS Attacker_ID, IFNULL(def.User_ID,0) AS Defender_ID\n" + "FROM games AS g\n" + "LEFT JOIN players AS att ON g.ID=att.Game_ID  AND att.Role='ATTACKER' AND att.Active=TRUE\n" + "LEFT JOIN players AS def ON g.ID=def.Game_ID AND def.Role='DEFENDER' AND def.Active=TRUE\n" + "WHERE g.Mode = 'DUEL' AND g.State = 'CREATED';";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query);
        return getGames(stmt, conn);
    }

    public static DuelGame getActiveUnitTestingSession(int userId) {
        String query = "SELECT * FROM games WHERE Defender_ID=? AND Mode='UTESTING' AND State='ACTIVE';";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(userId));
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
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return gameList;
    }

    public static MultiplayerGame getMultiplayerGame(int id) {
        String query = "SELECT * FROM games AS m WHERE ID=? AND m.Mode='PARTY'";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(id));
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
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return gameList;
    }

    public static List<Mutant> getMutantsForGame(int gid) {
        List<Mutant> mutList = new ArrayList<>();

        String query = "SELECT * FROM mutants WHERE Game_ID=? AND ClassFile IS NOT NULL;";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(gid));
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        try {
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
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return mutList;
    }

    public static List<Mutant> getMutantsForPlayer(int pid) {
        List<Mutant> mutList = new ArrayList<>();

        String query = "SELECT * FROM mutants WHERE Player_ID=? AND ClassFile IS NOT NULL;";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(pid));
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        try {
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
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
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
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return newMutant;
    }

    public static Mutant getMutant(DuelGame game, int mutantID) {
        String query = "SELECT * FROM mutants WHERE Mutant_ID=? AND Game_ID=?;";
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(mutantID),
                DB.getDBV(game.getId())};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return getMutantFromDB(stmt, conn);
    }

    public static Mutant getMutant(int gameId, String md5) {
        String query = "SELECT * FROM mutants WHERE Game_ID=? AND MD5=?;";
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(gameId),
                DB.getDBV(md5)};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return getMutantFromDB(stmt, conn);
    }

    public static List<Integer> getUsedAiTestsForGame(DuelGame g) {
        List<Integer> testList = new ArrayList<>();

        String query = "SELECT * FROM usedaitests WHERE Game_ID=?;";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(g.getId()));
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        try {
            while (rs.next()) {
                testList.add(rs.getInt("Value"));
            }
        } catch (SQLException e) {
            logger.error("SQLException while parsing result set for statement\n\t" + query, stmt);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return testList;
    }

    public static void increasePlayerPoints(int points, int player) {
        String query = "UPDATE players SET Points=Points+? WHERE ID=?";
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(points),
                DB.getDBV(player)};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        DB.executeUpdate(stmt, conn);
    }

    public static int getEquivalentDefenderId(Mutant m) {
        String query = "SELECT * FROM equivalences WHERE Mutant_ID=?;";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(m.getId()));
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        int id = -1;
        try {
            while (rs.next()) {
                id = rs.getInt("Defender_ID");
            }
        } catch (SQLException e) {
            logger.error("SQLException while parsing result set for statement\n\t" + query, stmt);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return id;
    }

    public static int getPlayerPoints(int playerId) {
        String query = "SELECT * FROM players WHERE ID=?;";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(playerId));
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        int points = 0;
        try {
            while (rs.next()) {
                points = rs.getInt("Points");
            }
        } catch (SQLException e) {
            logger.error("SQLException while parsing result set for statement\n\t" + query, stmt);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return points;
    }

    public static boolean insertEquivalence(Mutant mutant, int defender) {
        String query = "INSERT INTO equivalences " +
                "(Mutant_ID, Defender_ID, Mutant_Points) VALUES " + "(?, ?, ?)";
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(mutant.getId()),
                DB.getDBV(defender),
                DB.getDBV(mutant.getScore())};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return DB.executeUpdate(stmt, conn);
    }

    public static boolean setAiTestAsUsed(int testNumber, DuelGame g) {
        String query = "INSERT INTO usedaitests (Value, Game_ID) VALUES (?, ?);";
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(testNumber),
                DB.getDBV(g.getId())};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return DB.executeUpdateGetKeys(stmt, conn) > -1;
    }

    public static List<Integer> getUsedAiMutantsForGame(DuelGame g) {
        List<Integer> mutantList = new ArrayList<>();

        String query = "SELECT * FROM usedaimutants WHERE Game_ID=?;";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(g.getId()));
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        try {
            while (rs.next()) {
                mutantList.add(rs.getInt("Value"));
            }
        } catch (SQLException e) {
            logger.error("SQLException while parsing result set for statement\n\t" + query, stmt);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return mutantList;
    }

    /**
     * @param mutantNumber the number of the mutant
     * @param g            the game the mutant belongs to
     * @return
     */
    public static boolean setAiMutantAsUsed(int mutantNumber, DuelGame g) {
        String query = "INSERT INTO usedaimutants (Value, Game_ID) VALUES (?, ?);";
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(mutantNumber),
                DB.getDBV(g.getId())};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return DB.executeUpdateGetKeys(stmt, conn) > -1;
    }

    public static List<Test> getTestsForGame(int gid) {
        String query = "SELECT * FROM tests WHERE Game_ID=?;";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(gid));
        return getTests(stmt, conn);
    }

    public static Test getTestForId(int tid) {
        String query = "SELECT * FROM tests WHERE Test_ID=?;";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(tid));
        return getTests(stmt, conn).get(0);
    }

    /**
     * @param gameId
     * @param defendersOnly
     * @return Tests submitted by defenders which compiled and passed on CUT
     */
    public static List<Test> getExecutableTests(int gameId, boolean defendersOnly) {
        String query = "SELECT tests.* FROM tests\n" + "INNER JOIN targetexecutions ex on tests.Test_ID = ex.Test_ID\n" + (defendersOnly ? "INNER JOIN players pl on tests.Player_ID = pl.ID\n" : "") + "WHERE tests.Game_ID=? AND tests.ClassFile IS NOT NULL\n" + (defendersOnly ? "AND pl.Role='DEFENDER'\n" : "") + "AND ex.Target='TEST_ORIGINAL' AND ex.Status='SUCCESS';";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(gameId));
        return getTests(stmt, conn);
    }

    public static int getPlayerIdForMultiplayerGame(int userId, int gameId) {
        String query = "SELECT * FROM players AS p " + "WHERE p.User_ID = ? AND p.Game_ID = ?";
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(userId),
                DB.getDBV(gameId)};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return getInt(stmt, "ID", conn);
    }

    public static int[] getPlayersForMultiplayerGame(int gameId, Role role) {
        int[] players = new int[0];
        String query = "SELECT * FROM players WHERE Game_ID = ? AND Role=? AND Active=TRUE;";
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(gameId),
                DB.getDBV(role.toString())};
        // Load the MultiplayerGame Data with the provided ID.
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
        try {
            List<Integer> atks = new ArrayList<>();
            while (rs.next()) {
                atks.add(rs.getInt("ID"));
                players = new int[atks.size()];
                for (int i = 0; i < atks.size(); i++) {
                    players[i] = atks.get(i);
                }
            }
        } catch (SQLException e) {
            logger.error("SQLException while parsing result set for statement\n\t" + query, stmt);
        } finally {
            DB.cleanup(conn, stmt);
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
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return testList;
    }

    public static List<Entry> getLeaderboard() {
        Connection conn = DB.getConnection();
        PreparedStatement stmt = null;
        String query = "SELECT U.username AS username, IFNULL(NMutants,0) AS NMutants, IFNULL(AScore,0) AS AScore, IFNULL(NTests,0) AS NTests, IFNULL(DScore,0) AS DScore, IFNULL(NKilled,0) AS NKilled, IFNULL(AScore,0)+IFNULL(DScore,0) AS TotalScore\n" +
                "FROM users U LEFT JOIN\n" +
                "  (SELECT PA.user_id, count(M.Mutant_ID) AS NMutants, sum(M.Points) AS AScore FROM players PA LEFT JOIN mutants M ON PA.id = M.Player_ID GROUP BY PA.user_id)\n" +
                "    AS Attacker ON U.user_id = Attacker.user_id\n" +
                "  LEFT JOIN\n" +
                "  (SELECT PD.user_id, count(T.Test_ID) AS NTests, sum(T.Points) AS DScore, sum(T.MutantsKilled) AS NKilled FROM players PD LEFT JOIN tests T ON PD.id = T.Player_ID GROUP BY PD.user_id)\n" +
                "    AS Defender ON U.user_id = Defender.user_id\n" +
                "WHERE U.user_id > 2;";
        List<Entry> leaderboard = new ArrayList<>();
        try {
            stmt = DB.createPreparedStatement(conn, query);
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
        } catch (SQLException se) {
            logger.error("SQL exception caught", se);
            DB.cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return leaderboard;
    }

    public static int getKillingTestIdForMutant(int mid) {
        String query = "SELECT * FROM targetexecutions WHERE Target='TEST_MUTANT' AND Status!='SUCCESS' AND Mutant_ID=?;";
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, DB.getDBV(mid));
        TargetExecution targ = getTargetExecutionSQL(stmt, conn);
        return (targ != null) ? targ.testId : -1; // TODO: We shouldn't give away that we don't know which test killed the mutant?
    }

    public static TargetExecution getTargetExecutionForPair(int tid, int mid) {
        String query = "SELECT * FROM targetexecutions WHERE Test_ID=? AND Mutant_ID=?;";
        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(tid),
                DB.getDBV(mid)};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return getTargetExecutionSQL(stmt, conn);
    }

    public static TargetExecution getTargetExecutionForTest(Test test, TargetExecution.Target target) {
        String query = "SELECT * FROM targetexecutions WHERE Test_ID=? AND Target=?;";
        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(test.getId()),
                DB.getDBV(target.name())};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return getTargetExecutionSQL(stmt, conn);
    }

    public static TargetExecution getTargetExecutionForMutant(Mutant mutant, TargetExecution.Target target) {
        String query = "SELECT * FROM targetexecutions WHERE Mutant_ID=? AND Target=?;";
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(mutant.getId()),
                DB.getDBV(target.name())};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
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
            DB.cleanup(conn, stmt);
        } catch (Exception e) {
            logger.error("Exception caught", e);
        } finally {
            DB.cleanup(conn, stmt);
        }
        return null;
    }

    public static void logSession(int uid, String ipAddress) {
        String query = "INSERT INTO sessions (User_ID, IP_Address) VALUES (?, ?);";
        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(uid),
                DB.getDBV(ipAddress)};
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        DB.executeUpdate(stmt, conn);
    }
}
