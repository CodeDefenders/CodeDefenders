package org.codedefenders.util;

import com.mysql.jdbc.exceptions.MySQLDataException;
import org.codedefenders.*;
import org.codedefenders.duel.DuelGame;
import org.codedefenders.events.Event;
import org.codedefenders.events.EventStatus;
import org.codedefenders.story.StoryGame;
import org.codedefenders.story.StoryPuzzle;
import org.codedefenders.multiplayer.LineCoverage;
import org.codedefenders.multiplayer.MultiplayerGame;
import org.codedefenders.singleplayer.SinglePlayerGame;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseAccess {

	public static Connection getConnection() throws SQLException, NamingException {
		Context initialContext = new InitialContext();
		Context environmentContext = (Context) initialContext.lookup("java:comp/env");
		String dataResourceName = "jdbc/codedefenders";
		DataSource dataSource = (DataSource) environmentContext.lookup(dataResourceName);
		return dataSource.getConnection();
	}


	/**
	 * Execute an update statement.
	 * @param sql Statement to be executed
     */
	public static boolean executeUpdate(String sql) {
		Connection conn = null;
		Statement stmt = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();

			return stmt.executeUpdate(sql) > 0;
		} catch (SQLException se) {
			System.out.println(se);
			//Handle errors for JDBC
		} catch (Exception e) {
			System.out.println(e);
			//Handle errors for Class.forName
		} finally {
			cleanup(conn, stmt);
		} //end try
		return false;
	}


	public static boolean execute(String sql){
		Connection conn =  null;
		Statement stmt = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			stmt.execute(sql);
			return true;
		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			cleanup(conn, stmt);
		}
		return false;
	}

	public static int getInt(String sql, String att) {
		Connection conn = null;
		Statement stmt = null;

		int n = 0;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				n = rs.getInt(att);
			}

		} catch (SQLException se) {
			System.out.println(se);
		} // Handle errors for JDBC
		catch (Exception e) {
			System.out.println(e);
		} // Handle errors for Class.forName
		finally {
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
			se2.printStackTrace();
		}
		try {
			if (c != null) {
				c.close();
			}
		} catch (SQLException se3) {
			se3.printStackTrace();
		}
	}


	public static ArrayList<Event> getEventsForGame(int gameId) {
		String sql = String.format("SELECT * FROM events WHERE Game_ID='%d' " +
						"AND Event_Status='%s'",
				gameId, EventStatus.GAME);
		return getEvents(sql);
	}

	public static void removePlayerEventsForGame(int gameId, int
			playerId) {
		String sql = String.format("SELECT * FROM events WHERE Game_ID=%d " +
						"AND Player_ID=%d",
				gameId, EventStatus.GAME);
		for (Event e : getEvents(sql)){
			e.setStatus(EventStatus.DELETED);
			e.update();
		}
	}

	public static ArrayList<Event> getNewEventsForGame(int gameId, long time) {
		String sql = String.format("SELECT * FROM events WHERE Game_ID='%d' " +
						"AND Event_Status='%s' " +
						"AND Timestamp >= FROM_UNIXTIME(%d)",
				gameId, EventStatus.GAME, time);
		return getEvents(sql);
	}

	public static ArrayList<Event> getEventsForUser(int userId) {
		String sql = String.format("SELECT * FROM events WHERE " +
						"Event_Status!='DELETED' " +
						"AND Player_ID='%d';",
				userId);
		return getEvents(sql);
	}

	public static ArrayList<Event> getNewEventsForUser(int userId, long time) {
		String sql = String.format("SELECT * FROM events WHERE Player_ID='%d'" +
						" " +
						"AND Event_Status<>'%s' " +
						"AND Event_Status<>'%s' " +
						"AND Timestamp >= FROM_UNIXTIME(%d)",
				userId, EventStatus.DELETED, EventStatus.GAME, time);
		return getEvents(sql);
	}

	public static void setGameAsAIDummy(int gId) {
		String sql = String.format("UPDATE games SET IsAIDummyGame = 1 WHERE ID = %d;", gId);
		executeUpdate(sql);
	}

	public static DuelGame getAiDummyGameForClass(int cId) throws Exception {
		String sql = String.format("SELECT * FROM games WHERE Class_ID='%d' AND IsAIDummyGame=1", cId);
		int gID = getInt(sql, "ID");
		if (gID == 0) {
			Exception e = new Exception("No dummy game found.");
			throw e;
		}
		return getGameForKey("ID", gID);
	}

	public static String addSlashes(String s) {
		return s.replaceAll("\\\\", "\\\\\\\\");
	}

	public static int getNumAiMutantsKilledByTest(int tId) {
		String sql = String.format("SELECT * FROM tests WHERE Test_ID='%d';", tId);
		return getInt(sql, "NumberAiMutantsKilled");
	}

	public static int getNumAiMutantsKilledByPuzzleTest(int tid) {
		String sql = String.format("SELECT * FROM puzzleTests WHERE Test_ID='%d';", tid);
		return getInt(sql, "NumberAiMutantsKilled");
	}

	public static int getNumTestsKillMutant(int mId) {
		String sql = String.format("SELECT * FROM mutants WHERE Mutant_ID='%d';", mId);
		return getInt(sql, "NumberAiKillingTests");
	}

	public static int getNumTestsKillPuzzleMutant(int mid) {
		String sql = String.format("SELECT * FROM puzzleMutants WHERE Mutant_ID='%d';", mid);
		return getInt(sql, "NumberAiKillingTests");
	}

	public static boolean gameWithUserExistsForClass(int uId, int cId) {
		List<DuelGame> games = getGamesForUser(uId);
		for (DuelGame g : games) {
			if(g.getClassId() == cId) {
				return true;
			}
		}
		return false;
	}

	public static GameClass getClassForGame(int gameId) {
		String sql = String.format("SELECT classes.* from classes INNER JOIN games ON classes.Class_ID = games.Class_ID WHERE games.ID=%d;", gameId);
		return getClass(sql);
	}

	public static StoryClass getClassForPuzzle(int pid) {

		Connection conn = null;
		Statement stmt = null;

		String sql = String.format("SELECT classes.* from classes INNER JOIN puzzles ON classes.Class_ID = puzzles.Class_ID WHERE puzzles.Puzzle_ID=%d", pid);

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				StoryClass classRecord = new StoryClass(rs.getInt("Class_ID"), rs.getString("Name"), rs.getString("Alias"), rs.getString("JavaFile"), rs.getString("ClassFile"));
				return classRecord;
			}
		} catch (SQLException se) {
			System.out.println(se);
			//Handle errors for JDBC
		} catch (Exception e) {
			System.out.println(e);
			//Handle errors for Class.forName
		} finally {
			cleanup(conn, stmt);
		} //end try
		return null;

	}

	public static List<PuzzleClass> getDetailsForPuzzle(int puzzleId) {

		Connection conn = null;
		Statement stmt = null;
		List<PuzzleClass> puzzleDetails = new ArrayList<PuzzleClass>();

		String sql = String.format("SELECT * FROM puzzles AS p " +
				"INNER JOIN classes AS c ON p.Class_ID=c.Class_ID " +
				"WHERE Puzzle_ID=%d", puzzleId);

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				puzzleDetails.add(new PuzzleClass(rs.getInt("Class_ID"),
						rs.getString("Hint"), rs.getString("Description")));
			}
		} catch (SQLException se) {
			System.out.println(se);
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			cleanup(conn,stmt);
		}

		return puzzleDetails;

	}

	public static GameClass getClassForKey(String keyName, int id) {
		String sql = String.format("SELECT * FROM classes WHERE %s=%d;", keyName, id);
		return getClass(sql);
	}

	public static boolean isAiPrepared(GameClass c) {
		boolean prepared = false;

		Connection conn = null;
		Statement stmt = null;
		String sql = String.format("SELECT * from classes WHERE AiPrepared = 1 AND Class_ID = %d", c.getId());

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				//Class is prepared
				prepared = true;
			}
		} catch (SQLException se) {
			System.out.println(se);
			//Handle errors for JDBC
		} catch (Exception e) {
			System.out.println(e);
			//Handle errors for Class.forName
		} finally {
			cleanup(conn, stmt);
		} //end try

		return prepared;
	}

	public static void setAiPrepared(GameClass c) {
		String sql = String.format("UPDATE classes SET AiPrepared = 1 WHERE Class_ID = %d;", c.getId());
		executeUpdate(sql);
	}

	private static ArrayList<Event> getEvents(String sql) {
		Connection conn = null;
		Statement stmt = null;

		ArrayList<Event> events = new ArrayList<Event>();

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql + " ORDER BY Timestamp ASC");

			while (rs.next()) {
				Event event = new Event(rs.getInt("Event_ID"),
						rs.getInt("Game_ID"),
						rs.getInt("Player_ID"),
						rs.getString("Event_Message"),
						rs.getString("Event_Type"),
						rs.getString("Event_Status"),
						rs.getTimestamp("Timestamp"));

				events.add(event);
			}
		} catch (SQLException se) {
			System.out.println(se);
			//Handle errors for JDBC
		} catch (Exception e) {
			System.out.println(e);
			//Handle errors for Class.forName
		} finally {
			cleanup(conn, stmt);
		} //end try
		return events;
	}

	private static GameClass getClass(String sql) {
		Connection conn = null;
		Statement stmt = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				GameClass classRecord = new GameClass(rs.getInt("Class_ID"), rs.getString("Name"), rs.getString("Alias"), rs.getString("JavaFile"), rs.getString("ClassFile"));
				return classRecord;
			}
		} catch (SQLException se) {
			System.out.println(se);
			//Handle errors for JDBC
		} catch (Exception e) {
			System.out.println(e);
			//Handle errors for Class.forName
		} finally {
			cleanup(conn, stmt);
		} //end try
		return null;
	}

	// getClassForKey equivalent
	public static StoryClass getStoryForKey(int classId) {

		Connection conn = null;
		Statement stmt = null;
		String sql = String.format("SELECT * FROM classes AS c " +
				"INNER JOIN puzzles AS p ON c.Class_ID=p.Class_ID " +
				"WHERE c.Class_ID=%d", classId);

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				StoryClass classRecord = new StoryClass(rs.getInt("Puzzle_ID"), rs.getInt("Class_ID"), rs.getInt("Creator_ID"), rs.getString("Name"),
						rs.getString("Alias"), rs.getString("JavaFile"), rs.getString("ClassFile"));
				return classRecord;
			}

		} catch (SQLException se) {
			System.out.println(se);
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			cleanup(conn, stmt);
		}

		return null;

	}

	public static StoryClass getTestForPuzzleId(int puzzleId) {

		Connection conn = null;
		Statement stmt = null;
		String sql = String.format("SELECT * FROM puzzleTests WHERE Puzzle_ID = '%d'", puzzleId);

		try {

			conn = getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				StoryClass result = new StoryClass(rs.getString("JavaFile"));
				return result;
			}

		} catch (SQLException se) {
			System.out.println(se);
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			cleanup(conn, stmt);
		}

		return null;

	}

	// gets user submitted test
	public static StoryClass getUserPuzzleTest(int pid, int uid) {

		Connection conn = null;
		Statement stmt = null;
		String sql = String.format("SELECT * FROM puzzleTests " +
				"WHERE User_ID=%d AND Puzzle_ID=%d " +
				"ORDER BY Timestamp DESC " +
				"LIMIT 1", uid, pid); // get the latest record the user has submitted

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				StoryClass sc = new StoryClass(rs.getString("JavaFile"));
				return sc;
			}

		} catch (SQLException se) {
			System.out.println(se);
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			cleanup(conn, stmt);
		}

		return null;

	}

	// get user-submitted mutants
	public static StoryClass getUserPuzzleMutant(int pid, int uid) {

		Connection conn = null;
		Statement stmt = null;
		String sql = String.format("SELECT * FROM puzzleMutants " +
				"WHERE User_ID=%d AND Puzzle_ID=%d " +
				"ORDER BY Timestamp DESC " +
				"LIMIT 1", uid, pid);

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				StoryClass sc = new StoryClass(rs.getString("JavaFile"));
				return sc;
			}

		} catch (SQLException se) {
			System.out.println(se);
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			cleanup(conn, stmt);
		}

		return null;

	}

	public static List<GameClass> getAllClasses() {
		List<GameClass> classList = new ArrayList<>();

		Connection conn = null;
		Statement stmt = null;

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM classes;");
			while (rs.next()) {
				classList.add(new GameClass(rs.getInt("Class_ID"), rs.getString("Name"), rs.getString("Alias"), rs.getString("JavaFile"), rs.getString("ClassFile")));
			}

		} catch (SQLException se) {
			System.out.println(se);
			//Handle errors for JDBC
			se.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			//Handle errors for Class.forName
			e.printStackTrace();
		} finally {
			cleanup(conn, stmt);
		} //end try

		return classList;
	}

	public static List<User> getAllUsers() {
		String sql = String.format("SELECT * FROM users");

		Connection conn = null;
		Statement stmt = null;

		List<User> uList = new ArrayList<>();

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				User userRecord = new User(rs.getInt("User_ID"), rs.getString("Username"), rs.getString("Password"), rs.getString("Email"), rs.getBoolean("Validated"));
				uList.add(userRecord);
			}
		} catch (SQLException se) {
			System.out.println(se);
			//Handle errors for JDBC
			se.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			//Handle errors for Class.forName
			e.printStackTrace();
		} finally {
			cleanup(conn, stmt);
		} //end try

		return uList;
	}

	public static User getUser(int uid) {
		return getUserForKey("User_ID", uid);
	}

	public static User getUserForNameOrEmail(String usernameOrEmail) {
		String sql = String.format("SELECT * FROM users WHERE Username='%1$s' OR Email='%1$s';", usernameOrEmail);
		return getUserFromDB(sql);
	}

	public static User getUserFromPlayer(int playerId) {
		String sql = String.format("SELECT * FROM users AS u " +
				"LEFT JOIN players AS p ON p.User_ID=u.User_ID " +
				"WHERE p.ID='%d';", playerId);
		return getUserFromDB(sql);
	}

	public static User getUserForKey(String keyName, int id) {
		String sql = String.format("SELECT * FROM users WHERE %s=%d;", keyName, id);
		return getUserFromDB(sql);
	}

	private static User getUserFromDB(String sql) {
		Connection conn = null;
		Statement stmt = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				User userRecord = new User(rs.getInt("User_ID"), rs.getString("Username"), rs.getString("Password"), rs.getString("Email"), rs.getBoolean("Validated"));
				return userRecord;
			}

		} catch (SQLException se) {
			System.out.println(se);
			//Handle errors for JDBC
		} catch (Exception e) {
			System.out.println(e);
			//Handle errors for Class.forName
		} finally {
			cleanup(conn, stmt);
		} //end try

		return null;
	}

	public static DuelGame getGameForKey(String keyName, int id) {

		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {

			// Load the MultiplayerGame Data with the provided ID.
			conn = getConnection();

			stmt = conn.createStatement();
			sql = String.format("SELECT g.ID, g.Class_ID, g.Level, g.Creator_ID, g.State," +
					"g.CurrentRound, g.FinalRound, g.ActiveRole, g.Mode, g.Creator_ID,\n" +
					"IFNULL(att.User_ID,0) AS Attacker_ID, IFNULL(def.User_ID,0) AS Defender_ID\n" +
					"FROM games as g\n" +
					"LEFT JOIN players as att ON g.ID=att.Game_ID AND att.Role='ATTACKER' AND att.Active=TRUE\n" +
					"LEFT JOIN players AS def ON g.ID=def.Game_ID AND def.Role='DEFENDER' AND def.Active=TRUE\n" +
					"WHERE g.%s='%d';\n", keyName, id);

			ResultSet rs = stmt.executeQuery(sql);

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
			System.out.println(se);
		} // Handle errors for JDBC
		catch (Exception e) {
			System.out.println(e);
		} // Handle errors for Class.forName
		finally {
			cleanup(conn, stmt);
		}
		return null;
	}

	// getGameForKey equivalent for puzzles
	public static StoryGame getPuzzleForUser(int puzzleId) {

		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {

			conn = getConnection();

			stmt = conn.createStatement();
			sql = String.format("SELECT * FROM puzzles AS p " +
					"INNER JOIN levels AS l ON p.Level_ID = l.Level_ID " +
					"INNER JOIN story AS s ON p.Puzzle_ID = s.Puzzle_ID " +
					"WHERE p.Puzzle_ID=%d", puzzleId);

			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				StoryGame puzzleRecord = new StoryGame(rs.getInt("User_ID"), rs.getInt("Puzzle_ID"), rs.getInt("LevelNumber"),
						rs.getInt("Puzzle"), rs.getInt("Class_ID"), rs.getString("PuzzleName"), rs.getString("Hint"),
						rs.getString("Description"), PuzzleMode.valueOf(rs.getString("Mode")), rs.getInt("Points"), StoryState.valueOf(rs.getString("State")));

				stmt.close();
				conn.close();
				return puzzleRecord;
			}
		} catch (SQLException se) {
			System.out.println(se);
			se.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		} finally {
			cleanup(conn,stmt);
		}

		return null;

	}

	/**
	 * Returns list of <b>active</b> games for a user
	 * @param userId
	 * @return
	 */
	public static List<DuelGame> getGamesForUser(int userId) {

		String sql = String.format("SELECT g.ID, g.Class_ID, g.Level, g.Creator_ID, g.State," +
				"g.CurrentRound, g.FinalRound, g.ActiveRole, g.Mode, g.Creator_ID,\n" +
				"IFNULL(att.User_ID,0) AS Attacker_ID, IFNULL(def.User_ID,0) AS Defender_ID\n" +
				"FROM games as g\n" +
				"LEFT JOIN players as att ON g.ID=att.Game_ID  AND att.Role='ATTACKER' AND att.Active=TRUE\n" +
				"LEFT JOIN players AS def ON g.ID=def.Game_ID AND def.Role='DEFENDER' AND def.Active=TRUE\n" +
				"WHERE g.Mode != 'PARTY' AND g.State!='FINISHED' AND (g.Creator_ID=%d OR IFNULL(att.User_ID,0)=%d OR IFNULL(def.User_ID,0)=%d);\n", userId, userId, userId);

		return getGames(sql);
	}

	public static List<MultiplayerGame> getJoinedMultiplayerGamesForUser(int userId) {
		String sql = String.format("SELECT * FROM games AS m " +
				"LEFT JOIN players as p ON p.Game_ID=m.ID \n" +
				"WHERE m.Mode = 'PARTY' AND (p.User_ID=%d)" +
				"GROUP BY m.ID;", userId);
		return getMultiplayerGames(sql);
	}

	public static List<MultiplayerGame> getMultiplayerGamesForUser(int userId) {
		String sql = String.format("SELECT * FROM games AS m " +
				"LEFT JOIN players as p ON p.Game_ID=m.ID  AND p.Active=TRUE \n" +
				"WHERE m.Mode = 'PARTY' AND (p.User_ID=%d OR m.Creator_ID=%d) AND m.State != 'FINISHED' " +
				"GROUP BY m.ID;", userId, userId, userId);
		return getMultiplayerGames(sql);
	}

	public static List<MultiplayerGame> getFinishedMultiplayerGamesForUser(int userId) {
		String sql = String.format("SELECT * FROM games AS m " +
				"LEFT JOIN players as p ON p.Game_ID=m.ID  AND p.Active=TRUE \n" +
				"WHERE (p.User_ID=%d OR m.Creator_ID=%d) AND m.State = 'FINISHED' AND m.Mode='PARTY'" +
				"GROUP BY m.ID;", userId, userId, userId);
		return getMultiplayerGames(sql);
	}

	public static List<MultiplayerGame> getOpenMultiplayerGamesForUser(int userId) {
		String sql = String.format("SELECT * from games as g\n" +
				"INNER JOIN (SELECT gatt.ID, sum(case when Role = 'ATTACKER' then 1 else 0 end) nAttackers, sum(case when Role = 'DEFENDER' then 1 else 0 end) nDefenders\n" +
				"              FROM games as gatt LEFT JOIN players ON gatt.ID=players.Game_ID AND players.Active=TRUE GROUP BY gatt.ID) as nplayers\n" +
				"ON g.ID=nplayers.ID\n" +
				"WHERE g.Mode='PARTY' AND g.Creator_ID!=%1$d AND (g.State='CREATED' OR g.State='ACTIVE')\n" +
				"\tAND (g.RequiresValidation=FALSE OR (%1$d IN (SELECT User_ID from users where Validated=TRUE)))\n" +
				"\tAND g.ID NOT IN (SELECT g.ID from games g INNER JOIN players p ON g.ID=p.Game_ID WHERE p.User_ID=%1$d AND p.Active=TRUE)\n" +
				"\tAND nplayers.nAttackers < g.Attackers_Limit AND nplayers.nDefenders < g.Defenders_Limit;", userId);
		return getMultiplayerGames(sql);
	}

	public static Role getRole(int userId, int gameId){
		String sql = String.format("SELECT * FROM games AS m " +
				"LEFT JOIN players AS p ON p.Game_ID = m.ID AND p.Active=TRUE " +
				"WHERE m.ID = %d AND (m.Creator_ID=%d OR (p.User_ID=%d AND p.Game_ID=%d))",
				gameId, userId, userId, gameId);

		Connection conn = null;
		Statement stmt = null;
		Role role = Role.NONE;

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				if (rs.getInt("Creator_ID") == userId) {
					role = Role.CREATOR;
				} else {
					try {
						role = Role.valueOf(rs.getString("Role"));
					} catch (NullPointerException | SQLException e){}

				}

			}
		} catch (SQLException se) {
			System.out.println(se);
			//Handle errors for JDBC
			se.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			//Handle errors for Class.forName
			e.printStackTrace();
		} finally {
			cleanup(conn, stmt);
		} //end try

		return role;
	}

	/**
	 * Returns list of <b>finished</b> games for a user
	 * @param userId
	 * @return
	 */
	public static List<DuelGame> getHistoryForUser(int userId) {
		String sql = String.format("SELECT g.ID, g.Class_ID, g.Level, g.Creator_ID, g.State," +
				"g.CurrentRound, g.FinalRound, g.ActiveRole, g.Mode, g.Creator_ID,\n" +
				"IFNULL(att.User_ID,0) AS Attacker_ID, IFNULL(def.User_ID,0) AS Defender_ID\n" +
				"FROM games as g\n" +
				"LEFT JOIN players as att ON g.ID=att.Game_ID  AND att.Role='ATTACKER'\n" +
				"LEFT JOIN players AS def ON g.ID=def.Game_ID AND def.Role='DEFENDER'\n" +
				"WHERE g.Mode != 'PARTY' AND g.State='FINISHED' AND (g.Creator_ID=%d OR IFNULL(att.User_ID,0)=%d OR IFNULL(def.User_ID,0)=%d);\n", userId, userId, userId);

		return getGames(sql);
	}

	public static List<DuelGame> getOpenGames() {

		String sql = String.format("SELECT g.ID, g.Class_ID, g.Level, g.Creator_ID, g.State," +
				"g.CurrentRound, g.FinalRound, g.ActiveRole, g.Mode, g.Creator_ID,\n" +
				"IFNULL(att.User_ID,0) AS Attacker_ID, IFNULL(def.User_ID,0) AS Defender_ID\n" +
				"FROM games as g\n" +
				"LEFT JOIN players as att ON g.ID=att.Game_ID  AND att.Role='ATTACKER' AND att.Active=TRUE\n" +
				"LEFT JOIN players AS def ON g.ID=def.Game_ID AND def.Role='DEFENDER' AND def.Active=TRUE\n" +
				"WHERE g.Mode = 'DUEL' AND g.State = 'CREATED';");

		return getGames(sql);
	}

	public static DuelGame getActiveUnitTestingSession(int userId) {
		String sql = String.format("SELECT * FROM games WHERE Defender_ID='%d' AND Mode='UTESTING' AND State='ACTIVE';", userId);
		List<DuelGame> games = getGames(sql);
		if (games.isEmpty())
			return null;
		else
			return games.get(0);
	}

	public static List<DuelGame> getGames(String sql) {
		Connection conn = null;
		Statement stmt = null;
		List<DuelGame> gameList = new ArrayList<>();

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				gameList.add(new DuelGame(rs.getInt("ID"), rs.getInt("Attacker_ID"), rs.getInt("Defender_ID"),
						rs.getInt("Class_ID"), rs.getInt("CurrentRound"), rs.getInt("FinalRound"),
						Role.valueOf(rs.getString("ActiveRole")), GameState.valueOf(rs.getString("State")),
						GameLevel.valueOf(rs.getString("Level")), GameMode.valueOf(rs.getString("Mode"))));
			}
		} catch (SQLException se) {
			System.out.println(se);
			//Handle errors for JDBC
			se.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			//Handle errors for Class.forName
			e.printStackTrace();
		} finally {
			cleanup(conn, stmt);
		} //end try

		return gameList;
	}

	public static List<Achievements> getAchievements() {

		Connection conn = null;
		Statement stmt = null;
		List<Achievements> achv = new ArrayList<Achievements>();
		String sql = String.format("SELECT * FROM achievements AS a " +
				"LEFT JOIN user_achievements AS ua ON a.Achievement_ID = ua.Achievement_ID");

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				achv.add(new Achievements(rs.getInt("User_ID"), rs.getInt("Achievement_ID"), rs.getInt("Achieved"),
						rs.getString("Achievement_Name"), rs.getString("Achievement_Desc")));
			}
		} catch (SQLException se) {
			System.out.println(se);
			se.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		} finally {
			cleanup(conn, stmt);
		}

		return achv;

	}

	public static List<StoryGame> getStoryClassesForUser(int userId) {

		String sql = String.format("SELECT c.Class_ID, c.Name, c.Alias, p.PuzzleName, p.Description, p.Level_ID, p.Puzzle FROM classes AS c " +
				"INNER JOIN puzzles AS p ON c.Class_ID = p.Class_ID " +
				"WHERE c.Creator_ID=%d " +
				"ORDER BY Level_ID, Puzzle", userId);

		return getStoryClasses(sql);

	}

	public static boolean checkPuzzle(int levelNum, int puzzleNum) {

		boolean check = false; // available is false

		Connection conn = null;
		Statement stmt = null;
		String sql = String.format("SELECT * FROM puzzles AS p " +
				"INNER JOIN levels AS l ON p.Level_ID = l.Level_ID " +
				"WHERE l.Level_ID = %s AND Puzzle = %d",levelNum, puzzleNum);

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				check = true; // taken is true
			}
		} catch (SQLException se) {
			System.out.println(se);
			se.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		} finally {
			cleanup(conn,stmt);
		}

		return check;

	}

	// setAttribute("puzzle")
	public static StoryGame getActivePuzzle(int puzzleId) {

		Connection conn = null;
		Statement stmt = null;

		String sql = String.format("SELECT * FROM puzzles AS p " +
				"INNER JOIN classes AS c ON p.Class_ID = c.Class_ID " +
				"INNER JOIN levels AS l ON p.Level_ID = l.Level_ID " +
				"INNER JOIN story AS s ON p.Puzzle_ID = s.Puzzle_ID " +
				"WHERE p.Puzzle_ID=%s", puzzleId);

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				StoryGame puzzles;
				puzzles = new StoryGame(rs.getInt("User_ID"), rs.getInt("Puzzle_ID"), rs.getInt("LevelNumber"),
						rs.getInt("Puzzle"), rs.getInt("Class_ID"), rs.getString("PuzzleName"),
						rs.getString("Hint"), rs.getString("Description"), PuzzleMode.valueOf(rs.getString("Mode")),
						rs.getInt("Points"), StoryState.valueOf(rs.getString("State")));
				return puzzles;
			}
		} catch (SQLException se) {
			System.out.println(se);
			se.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		} finally {
			cleanup(conn, stmt);
		}

		return null;

	}

	public static List<StoryGame> getCurrentLevel(int userId) {

		Connection conn = null;
		Statement stmt = null;
		List<StoryGame> levels = new ArrayList<StoryGame>();

		String sql = String.format("SELECT * FROM puzzles AS p " +
				"INNER JOIN levels AS l ON p.Level_ID = l.Level_ID " +
				"INNER JOIN story AS s ON p.Puzzle_ID = s.Puzzle_ID " +
				"WHERE s.User_ID = %d " +
				"ORDER BY LevelNumber, Puzzle ", userId);

		try {

			conn = getConnection();
			stmt = conn.createStatement();

			ResultSet rs = stmt.executeQuery(sql);

			while(rs.next()) {
				levels.add(new StoryGame(rs.getInt("LevelNumber"), rs.getInt("Puzzle"),
						rs.getString("PuzzleName"), StoryState.valueOf(rs.getString("State"))));
			}

		} catch (SQLException se) {
			System.out.println(se);
			se.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		} finally {
			cleanup(conn, stmt);
		}

		return levels;

	}

	public static int getAllPuzzleCount(int userId) {

		String sql = String.format("SELECT * FROM puzzles " +
				"WHERE Puzzle IS NOT NULL AND PuzzleName IS NOT NULL");
		return getStoryProgress(sql, userId);

	}

	public static int getCompletedPuzzleCount(int userId) {

		String sql = String.format("SELECT * FROM puzzles AS p " +
				"INNER JOIN story AS s ON p.Puzzle_ID = s.Puzzle_ID " +
				"WHERE s.User_ID = %d AND s.State='COMPLETED'", userId);
		return getStoryProgress(sql, userId);

	}

	public static int getStoryProgress(String sql, int userId) {

		Connection conn = null;
		Statement stmt = null;
		List<StoryPuzzle> list = new ArrayList<StoryPuzzle>();

		try {

			conn = getConnection();
			stmt = conn.createStatement();

			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				list.add(new StoryPuzzle(rs.getInt("Puzzle_ID")));
			}

		} catch (SQLException se) {
			System.out.println(se);
			se.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		} finally {
			cleanup(conn, stmt);
		}

		return list.size();

	}

	public static List<PuzzleClass> getTakenPuzzles(int levelNum) {

		Connection conn = null;
		Statement stmt = null;
		List<PuzzleClass> takenPuzzles = new ArrayList<PuzzleClass>();

		String sql = String.format("SELECT Puzzle, l.LevelNumber FROM puzzles AS p " +
				"INNER JOIN levels AS l ON p.Level_ID = l.Level_ID " +
				"WHERE l.Level_ID=%s",levelNum);

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				takenPuzzles.add(new PuzzleClass(rs.getInt("LevelNumber"), rs.getInt("Puzzle")));
			}

		} catch (SQLException se) {
			System.out.println(se);
			se.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		} finally {
			cleanup(conn,stmt);
		}

		return takenPuzzles;

	}

	public static List<StoryGame> getStoryClasses(String sql) {

		Connection conn = null;
		Statement stmt = null;
		List<StoryGame> classesList = new ArrayList<>();

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			while(rs.next()) {
				classesList.add(new StoryGame(rs.getInt("Class_ID"), rs.getString("Name"),
						rs.getString("Alias"), rs.getString("PuzzleName"), rs.getString("Description"),
						rs.getInt("Level_ID"), rs.getInt("Puzzle")));
			}

		} catch (SQLException se) {
			System.out.println(se);
			se.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		} finally {
			cleanup(conn, stmt);
		}

		return classesList;

	}

	public static StoryPuzzle getPuzzleId(int classId) {

		Connection conn = null;
		Statement stmt = null;
		String sql = String.format("SELECT p.Puzzle_ID FROM puzzles AS p " +
				"WHERE p.Class_ID=%d", classId);

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				StoryPuzzle pid;
				pid = new StoryPuzzle(rs.getInt("Puzzle_ID"));
				return pid;
			}
		} catch (SQLException se) {
			System.out.println(se);
			se.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		} finally {
			cleanup(conn, stmt);
		}

		return null;

	}

	public static List<StoryGame> getPuzzles() {

		Connection conn = null;
		Statement stmt = null;
		List<StoryGame> puzzleList = new ArrayList<>();

		String sql = String.format("SELECT * FROM puzzles AS p " +
				"INNER JOIN story AS s ON p.Puzzle_ID = s.Puzzle_ID " +
				"INNER JOIN levels AS l ON p.Level_ID = l.Level_ID " +
				"ORDER BY LevelNumber, Puzzle");

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				puzzleList.add(new StoryGame(rs.getInt("User_ID"), rs.getInt("Puzzle"), rs.getInt("LevelNumber"),
						rs.getInt("Points"), rs.getInt("Puzzle_ID"), StoryState.valueOf(rs.getString("State")),
						rs.getString("PuzzleName"), rs.getString("Description"), PuzzleMode.valueOf(rs.getString("Mode"))));
			}
		} catch (SQLException se) {
			System.out.println(se);
			se.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		} finally {
			cleanup(conn, stmt);
		}

		return puzzleList;

	}

	public static List<StoryGame> getAllPuzzles() {

		Connection conn = null;
		Statement stmt = null;
		List<StoryGame> puzzleList = new ArrayList<>();

		String sql = String.format("SELECT * FROM puzzles");

		try {

			conn = getConnection();
			stmt = conn.createStatement();

			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				puzzleList.add(new StoryGame(rs.getInt("Puzzle_ID")));
			}

		} catch (SQLException se) {
			System.out.println(se);
			se.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		} finally {
			cleanup(conn, stmt);
		}

		return puzzleList;

	}

	public static List<PuzzleClass> getLevels() {

		Connection conn = null;
		Statement stmt = null;
		List<PuzzleClass> levelList = new ArrayList<>();

		try {
			conn = getConnection();
			stmt = conn.createStatement();

			String sql = String.format("SELECT DISTINCT LevelNumber from levels");

			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				levelList.add(new PuzzleClass(rs.getInt("LevelNumber")));
			}
		} catch (SQLException se) {
			System.out.println(se);
			se.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		} finally {
			cleanup(conn, stmt);
		}

		return levelList;

	}

	public static StoryPuzzle getLevelDetails(int classId) {

		Connection conn = null;
		Statement stmt = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();

			String sql = String.format("SELECT * FROM puzzles AS p " +
					"INNER JOIN levels AS l ON p.Level_ID = l.Level_ID " +
					"WHERE p.Class_ID = %d", classId);

			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				StoryPuzzle details;
				details = new StoryPuzzle(rs.getInt("LevelNumber"), rs.getInt("Puzzle"),
						rs.getString("PuzzleName"), rs.getString("Hint"), rs.getString("Description"),
						PuzzleMode.valueOf(rs.getString("Mode")));
				return details;
			}
		} catch (SQLException se) {
			System.out.println(se);
			se.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		} finally {
			cleanup(conn, stmt);
		}

		return null;

	}

	public static Achievements getAchvNameById(int aid) {

		Connection conn = null;
		Statement stmt = null;

		String sql = String.format("SELECT * FROM achievements " +
				"WHERE Achievement_ID = %d", aid);

		try {

			conn = getConnection();
			stmt = conn.createStatement();

			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				Achievements achv;
				achv = new Achievements(rs.getString("Achievement_Name"));
				return achv;
			}

		} catch (SQLException se) {
			System.out.println(se);
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			cleanup(conn, stmt);
		}

		return null;

	}

	public static List<Achievements> firstLevelAchv(int userId) {

		String sql = String.format("SELECT * FROM user_achievements AS ua " +
				"INNER JOIN achievements AS a ON ua.Achievement_ID = a.Achievement_ID " +
				"WHERE ua.User_ID = %d AND ua.Achievement_ID = 1", userId);
		return checkAchievements(sql);

	}

	public static List<Achievements> fullLevelAchv(int userId, int aid) {

		String sql = String.format("SELECT * FROM user_achivements AS ua " +
				"INNER JOIN achievements AS a ON ua.Achievement_ID = a.Achivement_ID " +
				"INNER JOIN puzzles AS p ON " +
				"WHERE ua.User_ID = %d AND ua.Achievement_ID = %d", userId, aid);
		return checkAchievements(sql);

	}

	public static List<Achievements> checkAchievements(String sql) {

		Connection conn = null;
		Statement stmt = null;
		List<Achievements> achv = new ArrayList<Achievements>();

		try {

			conn = getConnection();
			stmt = conn.createStatement();

			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				achv.add(new Achievements(rs.getInt("User_ID"), rs.getInt("Achievement_ID"),
						rs.getInt("Achieved"), rs.getString("Achievement_Name")));

				return achv;
			}

		} catch (SQLException se) {
			System.out.println(se);
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			cleanup(conn, stmt);
		}

		return null;

	}

	public static boolean alreadyAchieved(int uid, int aid) {

		Connection conn = null;
		Statement stmt = null;

		String sql = String.format("SELECT * FROM user_achievements " +
				"WHERE User_ID = %d AND Achievement_ID = %d", uid, aid);

		try {

			conn = getConnection();
			stmt = conn.createStatement();

			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				return true;
			}
		} catch (SQLException se) {
			System.out.println(se);
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			cleanup(conn, stmt);
		}

		return false;

	}

	public static List<StoryGame> getPuzzlesForLevel(int lid) {

		Connection conn = null;
		Statement stmt = null;
		List<StoryGame> list = new ArrayList<StoryGame>();

		String sql = String.format("SELECT * FROM puzzles AS p " +
				"INNER JOIN story AS s ON p.Puzzle_ID = s.Puzzle_ID " +
				"WHERE p.Level_ID = %d", lid);

		try {

			conn = getConnection();
			stmt = conn.createStatement();

			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				list.add(new StoryGame(rs.getInt("Level_ID"), StoryState.valueOf(rs.getString("State")),
						rs.getInt("Puzzle")));
			}

		} catch (SQLException se) {
			System.out.println(se);
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			cleanup(conn, stmt);
		}

		return list;

	}

	public static int getOverallPoints(int userId) {

		String sql = String.format("SELECT * FROM story WHERE User_ID = %d", userId);

		Connection conn = null;
		Statement stmt = null;
		int points = 0;

		try {

			conn = getConnection();
			stmt = conn.createStatement();

			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				points += rs.getInt("Points");
			}

		} catch (SQLException se) {
			System.out.println(se);
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			cleanup(conn, stmt);
		}

		return points;

	}

	public static MultiplayerGame getMultiplayerGame(int id){
		String sql = String.format("SELECT * FROM games AS m " +
				"WHERE ID=%d AND m.Mode='PARTY'", id);

		List<MultiplayerGame> mgs = getMultiplayerGames(sql);
		if (mgs.size() > 0){
			return mgs.get(0);
		}
		return null;
	}

	public static List<MultiplayerGame> getMultiplayerGames(String sql) {
		List<MultiplayerGame> gameList = new ArrayList<>();
		Connection conn = null;
		Statement stmt = null;

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				MultiplayerGame mg = new MultiplayerGame(rs.getInt("Class_ID"), rs.getInt("Creator_ID"),
						GameLevel.valueOf(rs.getString("Level")), (float)rs.getDouble("Coverage_Goal"),
						(float)rs.getDouble("Mutant_Goal"), rs.getInt("Prize"), rs.getInt("Defender_Value"),
						rs.getInt("Attacker_Value"), rs.getInt("Defenders_Limit"), rs.getInt("Attackers_Limit"),
						rs.getInt("Defenders_Needed"), rs.getInt("Attackers_Needed"), rs.getTimestamp("Start_Time").getTime(),
						rs.getTimestamp("Finish_Time").getTime(), rs.getString("State"), rs.getBoolean("RequiresValidation"));
				mg.setId(rs.getInt("ID"));
				gameList.add(mg);
			}
		} catch (SQLException se) {
			System.out.println(se);
			//Handle errors for JDBC
			se.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			//Handle errors for Class.forName
			e.printStackTrace();
		} finally {
			cleanup(conn, stmt);
		} //end try


		return gameList;
	}

	public static List<Mutant> getMutantsForGame(int gid) {

		List<Mutant> mutList = new ArrayList<>();
		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			sql = String.format("SELECT * FROM mutants WHERE Game_ID='%d' AND ClassFile IS NOT NULL;", gid);
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				Mutant newMutant = new Mutant(rs.getInt("Mutant_ID"), rs.getInt("Game_ID"),
						rs.getString("JavaFile"), rs.getString("ClassFile"),
						rs.getBoolean("Alive"), Mutant.Equivalence.valueOf(rs.getString("Equivalent")),
						rs.getInt("RoundCreated"), rs.getInt("RoundKilled"), rs.getInt("Player_ID"));
				newMutant.setScore(rs.getInt("Points"));
				mutList.add(newMutant);
			}
		} catch (SQLException se) {
			System.out.println(se);
		} // Handle errors for JDBC
		catch (Exception e) {
			System.out.println(e);
		} // Handle errors for Class.forName
		finally {
			cleanup(conn, stmt);
		}
		return mutList;
	}

	public static List<PuzzleMutant> getMutantsforPuzzle(int pid) {

		List<PuzzleMutant> mutList = new ArrayList<>();
		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {

			conn = getConnection();
			stmt = conn.createStatement();
			sql = String.format("SELECT * FROM puzzleMutants WHERE Puzzle_ID='%d' AND ClassFile IS NOT NULL;", pid);
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				PuzzleMutant newMutant = new PuzzleMutant(rs.getInt("Mutant_ID"), rs.getInt("Puzzle_ID"),
						rs.getString("JavaFile"), rs.getString("ClassFile"),
						rs.getBoolean("Alive"), PuzzleMutant.Equivalence.valueOf(rs.getString("Equivalent")),
						rs.getInt("User_ID"));
				newMutant.setScore(rs.getInt("Points"));
				mutList.add(newMutant);
			}

		} catch (SQLException se) {
			System.out.println(se);
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			cleanup(conn, stmt);
		}

		return mutList;

	}

	public static List<Mutant> getMutantsForPlayer(int pid) {

		List<Mutant> mutList = new ArrayList<>();
		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			sql = String.format("SELECT * FROM mutants WHERE Player_ID='%d' AND ClassFile IS NOT NULL;", pid);
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				Mutant newMutant = new Mutant(rs.getInt("Mutant_ID"), rs.getInt("Game_ID"),
						rs.getString("JavaFile"), rs.getString("ClassFile"),
						rs.getBoolean("Alive"), Mutant.Equivalence.valueOf(rs.getString("Equivalent")),
						rs.getInt("RoundCreated"), rs.getInt("RoundKilled"), rs.getInt("Player_ID"));
				newMutant.setScore(rs.getInt("Points"));
				mutList.add(newMutant);
			}
		} catch (SQLException se) {
			System.out.println(se);
		} // Handle errors for JDBC
		catch (Exception e) {
			System.out.println(e);
		} // Handle errors for Class.forName
		finally {
			cleanup(conn, stmt);
		}
		return mutList;
	}

	public static Mutant getMutantFromDB(String sql) {

		Mutant newMutant = null;

		Connection conn = null;
		Statement stmt = null;

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				newMutant = new Mutant(rs.getInt("Mutant_ID"), rs.getInt("Game_ID"),
						rs.getString("JavaFile"), rs.getString("ClassFile"),
						rs.getBoolean("Alive"), Mutant.Equivalence.valueOf(rs.getString("Equivalent")),
						rs.getInt("RoundCreated"), rs.getInt("RoundKilled"), rs.getInt("Player_ID"));
			}
		} catch (SQLException se) {
			System.out.println(se);
		} // Handle errors for JDBC
		catch (Exception e) {
			System.out.println(e);
		} // Handle errors for Class.forName
		finally {
			cleanup(conn, stmt);
		}

		return newMutant;
	}

	public static PuzzleMutant getPMutantFromDB(String sql) {

		PuzzleMutant newMutant = null;

		Connection conn = null;
		Statement stmt = null;

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				newMutant = new PuzzleMutant(rs.getInt("Mutant_ID"), rs.getInt("Puzzle_ID"),
						rs.getString("JavaFile"), rs.getString("ClassFile"),
						rs.getBoolean("Alive"), PuzzleMutant.Equivalence.valueOf(rs.getString("Equivalent")),
						rs.getInt("User_ID"));
			}
		} catch (SQLException se) {
			System.out.println(se);
			se.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		} finally {
			cleanup(conn, stmt);
		}

		return newMutant;

	}

	public static Mutant getMutant(DuelGame game, int mutantID) {
		String sql = String.format("SELECT * FROM mutants WHERE Mutant_ID='%d' AND Game_ID='%d';", mutantID, game.getId());
		return getMutantFromDB(sql);
	}

	public static Mutant getMutant(int gameId, String md5) {
		String sql = String.format("SELECT * FROM mutants WHERE Game_ID='%d' AND MD5='%s';", gameId, md5);
		return getMutantFromDB(sql);
	}

	public static PuzzleMutant getPMutant(int puzzleId, String md5) {

		String sql = String.format("SELECT * FROM puzzleMutants WHERE Puzzle_ID='%d' AND MD5='%s';", puzzleId, md5);
		return getPMutantFromDB(sql);

	}



	public static List<Integer> getUsedAiTestsForGame(DuelGame g) {
		List<Integer> testList = new ArrayList<>();

		Connection conn =  null;
		Statement stmt = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			String sql = String.format("SELECT * FROM usedaitests WHERE Game_ID='%d';", g.getId());
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				testList.add(rs.getInt("Value"));
			}

		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			cleanup(conn, stmt);
		}

		return testList;
	}

	public static void increasePlayerPoints(int points, int player){
		String sql = String.format(
				"UPDATE players SET Points=Points+%d WHERE ID=%d",
				points, player
		);
		execute(sql);
	}

	public static int getEquivalentDefenderId(Mutant m){

		Connection conn =  null;
		Statement stmt = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			String sql = String.format("SELECT * FROM equivalences WHERE Mutant_ID='%d';", m.getId());
			ResultSet rs = stmt.executeQuery(sql);
			int id = -1;
			while (rs.next()) {
				id = rs.getInt("Defender_ID");
			}
			return id;

		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			cleanup(conn, stmt);
		}
		return -1;
	}

	public static int getPlayerPoints(int playerId){

		Connection conn =  null;
		Statement stmt = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			String sql = String.format("SELECT * FROM players WHERE ID='%d';", playerId);
			ResultSet rs = stmt.executeQuery(sql);
			int points = 0;
			while (rs.next()) {
				points = rs.getInt("Points");
			}

			return points;

		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			cleanup(conn, stmt);
		}
		return 0;
	}

	public static boolean insertEquivalence(Mutant mutant, int defender) {
		Connection conn =  null;
		Statement stmt = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			String sql = String.format("INSERT INTO equivalences " +
					"(Mutant_ID, Defender_ID, Mutant_Points) VALUES " +
					"('%d', '%d', '%d')",
					mutant.getId(), defender, mutant.getScore());

			stmt.execute(sql, Statement.RETURN_GENERATED_KEYS);

			ResultSet rs = stmt.getGeneratedKeys();

			if (rs.next()) {
				stmt.close();
				conn.close();
				return true;
			}

		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			cleanup(conn, stmt);
		}
		return false;
	}

	public static boolean setAiTestAsUsed(int testNumber, DuelGame g) {
		Connection conn =  null;
		Statement stmt = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			String sql = String.format("INSERT INTO usedaitests (Value, Game_ID) VALUES ('%d', '%d');", testNumber, g.getId());

			stmt.execute(sql, Statement.RETURN_GENERATED_KEYS);

			ResultSet rs = stmt.getGeneratedKeys();

			if (rs.next()) {
				stmt.close();
				conn.close();
				return true;
			}

		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			cleanup(conn, stmt);
		}
		return false;
	}

	public static List<Integer> getUsedAiMutantsForGame(DuelGame g) {
		List<Integer> mutantList = new ArrayList<>();

		Connection conn =  null;
		Statement stmt = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			String sql = String.format("SELECT * FROM usedaimutants WHERE Game_ID='%d';", g.getId());
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				mutantList.add(rs.getInt("Value"));
			}
			stmt.close();
			conn.close();

		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			cleanup(conn, stmt);
		}

		return mutantList;
	}

	/**
	 *
	 * @param mutantNumber the number of the mutant
	 * @param g the game the mutant belongs to
	 * @return
	 */
	public static boolean setAiMutantAsUsed(int mutantNumber, DuelGame g) {
		Connection conn =  null;
		Statement stmt = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			String sql = String.format("INSERT INTO usedaimutants (Value, Game_ID) VALUES ('%d', '%d');", mutantNumber, g.getId());

			stmt.execute(sql, Statement.RETURN_GENERATED_KEYS);

			ResultSet rs = stmt.getGeneratedKeys();

			if (rs.next()) {
				return true;
			}

		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			cleanup(conn, stmt);
		}
		return false;
	}

	public static List<Test> getTestsForGame(int gid) {
		String sql = String.format("SELECT * FROM tests WHERE Game_ID='%d';", gid);
		return getTests(sql);
	}

	public static Test getTestForId(int tid) {
		String sql = String.format("SELECT * FROM tests WHERE Test_ID='%d';", tid);
		return getTests(sql).get(0);
	}

	/**
	 *
	 * @param gameId
	 * @param defendersOnly
	 * @return Tests submitted by defenders which compiled and passed on CUT
	 */
	public static List<Test> getExecutableTests(int gameId, boolean defendersOnly) {
		String stmt = "SELECT tests.* FROM tests\n"
				+ "INNER JOIN targetexecutions ex on tests.Test_ID = ex.Test_ID\n"
				+ (defendersOnly ? "INNER JOIN players pl on tests.Player_ID = pl.ID\n" : "")
				+ "WHERE tests.Game_ID='%d' AND tests.ClassFile IS NOT NULL\n"  // only compilable tests for a game
				+ (defendersOnly ? "AND pl.Role='DEFENDER'\n" : "")             // [optional] submitted by defenders only
				+ "AND ex.Target='TEST_ORIGINAL' AND ex.Status='SUCCESS';";     // that pass on original CUT

		String sql = String.format(stmt, gameId);
		return getTests(sql);
	}

	public static List<PuzzleTest> getTestsforPuzzle(int pid) {

		String sql = String.format("SELECT * FROM puzzleTests WHERE Puzzle_ID='%d' AND ClassFile IS NOT NULL;", pid);
		return getPuzzleTests(sql);

	}

	public static PuzzleTest getPTestForId(int tid) {

		String sql = String.format("SELECT * FROM puzzleTests WHERE Test_ID='%d'", tid);
		return getPuzzleTests(sql).get(0);

	}

	public static List<PuzzleTest> getExecutablePTests(int pid, boolean dOnly) {

		String stmt = "SELECT puzzleTests.* FROM puzzleTests\n"
				+ "INNER JOIN storytargetexecutions ex ON puzzleTests.Test_ID = ex.Test_ID\n"
				+ (dOnly ? "INNER JOIN story s ON puzzleTests.User_ID = s.ID\n"
				+ "INNER JOIN puzzles p ON puzzleTests.Puzzle_ID = p.Puzzle_ID\n" : "")
				+ "WHERE puzzleTests.Puzzle_ID='%d' AND puzzleTests.ClassFile IS NOT NULL\n"
				+ (dOnly ? "AND p.Mode='DEFENDER'\n" : "")
				+ "AND ex.Target='TEST_ORIGINAL' AND ex.Status='SUCCESS';";
		String sql = String.format(stmt, pid);
		return getPuzzleTests(sql);
	}

	public static int getPlayerIdForMultiplayerGame(int userId, int gameId) {
		String sql = String.format("SELECT * FROM players AS p " +
				"WHERE p.User_ID = %d AND p.Game_ID = %d", userId, gameId); // that pass on original CUT

		return getInt(sql, "ID");
	}

	public static int getPlayerForStory(int userId, int puzzleId) {

		String sql = String.format("SELECT * FROM story AS s " +
				"WHERE s.User_ID = %d AND s.Puzzle_ID = %d", userId, puzzleId);

		return getInt(sql, "ID");
	}

	public static int[] getPlayersForMultiplayerGame(int gameId, Role role) {
		Connection conn = null;
		Statement stmt = null;

		String sql = String.format("SELECT * FROM players \n" +
				"WHERE Game_ID = %d AND Role='%s' AND Active=TRUE;", gameId, role);

		int[] players = new int[0];

		try {

			// Load the MultiplayerGame Data with the provided ID.
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			List<Integer> atks = new ArrayList<>();

			while (rs.next()) {
				atks.add(rs.getInt("ID"));
			}

			players = new int[atks.size()];

			for (int i = 0; i < atks.size(); i++){
				players[i] = atks.get(i);
			}
		} catch (SQLException se) {
			System.out.println(se);
		} // Handle errors for JDBC
		catch (Exception e) {
			System.out.println(e);
		} // Handle errors for Class.forName
		finally {
			cleanup(conn, stmt);
		}
		return players;
	}

	public static int getNumTestsForPlayer(int pid) {
		int n = 0;
		String sql = String.format("SELECT * FROM tests WHERE Player_ID = '%d'", pid);
		n += getTests(sql).size();
		return n;
	}

	public static int getNumPuzzleTestsForUser(int pid) {

		int n = 0;
		String sql = String.format("SELECT * FROM puzzleTests WHERE User_ID = '%d'", pid);
		n += getPuzzleTests(sql).size();
		return n;

	}

	private static List<Test> getTests(String sql) {

		List<Test> testList = new ArrayList<>();

		Connection conn = null;
		Statement stmt = null;
		try {
			// Load the MultiplayerGame Data with the provided ID.
			conn = getConnection();
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

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
			System.out.println(se);
		} // Handle errors for JDBC
		catch (Exception e) {
			e.printStackTrace();
		} // Handle errors for Class.forName
		finally {
			cleanup(conn, stmt);
		}

		return testList;
	}

	private static List<PuzzleTest> getPuzzleTests(String sql) {

		List<PuzzleTest> testList = new ArrayList<>();

		Connection conn = null;
		Statement stmt = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();

			ResultSet rs = stmt.executeQuery(sql);

			while(rs.next()) {
				PuzzleTest newTest = new PuzzleTest(rs.getInt("Test_ID"), rs.getInt("Puzzle_ID"),
						rs.getString("JavaFile"), rs.getString("ClassFile"),
						rs.getInt("MutantsKilled"), rs.getInt("User_ID"));
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
					lc.setLinesCovered(covered);
					lc.setLinesUncovered(uncovered);
				}
				newTest.setLineCoverage(lc);
				newTest.setScore(rs.getInt("Points"));
				testList.add(newTest);
			}
			stmt.close();
			conn.close();
		} catch (SQLException se) {
			System.out.println(se);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			cleanup(conn, stmt);
		}

		return testList;

	}

	public static PuzzleTest getTestIdByPuzzle(int pid) {

		Connection conn = null;
		Statement stmt = null;

		String sql = String.format("SELECT Test_ID, JavaFile FROM puzzleTests WHERE Puzzle_ID = '%d'", pid);

		try {

			conn = getConnection();
			stmt = conn.createStatement();

			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				PuzzleTest pt = new PuzzleTest(rs.getInt("Test_ID"), rs.getString("JavaFile"));
				return pt;
			}

		} catch (SQLException se) {
			System.out.println(se);
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			cleanup(conn, stmt);
		}

		return null;

	}

	public static PuzzleMutant getMutantIdByPuzzle(int pid) {

		Connection conn = null;
		Statement stmt = null;

		String sql = String.format("SELECT Mutant_ID, JavaFile FROM puzzleMutants WHERE Puzzle_ID = '%d'", pid);

		try {

			conn = getConnection();
			stmt = conn.createStatement();

			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				PuzzleMutant pm = new PuzzleMutant(rs.getInt("Mutant_ID"), rs.getString("JavaFile"));
				return pm;
			}

		} catch (SQLException se) {
			System.out.println(se);
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			cleanup(conn, stmt);
		}

		return null;

	}

	public static List<Test> getPartyTestsForUser(int uid) {
		String sql = String.format("SELECT * FROM vw_mp_tests WHERE User_ID='%d';", uid);
		return getTests(sql);
	}

	public static int getNumPartyTestsForUser(int uid) {
		String sql = String.format("SELECT * FROM vw_mp_num_tests WHERE User_ID='%d';", uid);
		return getInt(sql, "TestCount");
	}

	public static int getNumPartyTestKillsForUser(int uid) {
		String sql = String.format("SELECT * FROM vw_mp_num_tests_kill WHERE User_ID='%d';", uid);
		return getInt(sql, "TestKillCount");
	}

	public static int getNumPartyMutantsForUser(int uid) {
		String sql = String.format("SELECT * FROM vw_mp_num_mutants WHERE User_ID='%d';", uid);
		return getInt(sql, "MutantCount");
	}

	public static int getUserPartyPointsMutants(int uid) {
		String sql = String.format("SELECT * FROM vw_mp_user_points WHERE User_ID='%d';", uid);
		return getInt(sql, "MutantPoints");
	}
	public static int getUserPartyPointsTests(int uid) {
		String sql = String.format("SELECT * FROM vw_mp_user_points WHERE User_ID='%d';", uid);
		return getInt(sql, "TestPoints");
	}
	public static int getUserPartyPointsTotal(int uid) {
		String sql = String.format("SELECT * FROM vw_mp_user_points WHERE User_ID='%d';", uid);
		return getInt(sql, "TotalPoints");
	}

	public static int getKillingTestIdForMutant(int mid) {
		String sql = String.format("SELECT * FROM targetexecutions WHERE Target='TEST_MUTANT' AND Status!='SUCCESS' AND Mutant_ID='%d';", mid);
		TargetExecution targ = getTargetExecutionSQL(sql);
		return (targ != null) ? targ.testId : -1; // TODO: We shouldn't give away that we don't know which test killed the mutant?
	}

	public static TargetExecution getTargetExecutionForPair(int tid, int mid) {
		String sql = String.format("SELECT * FROM targetexecutions WHERE Test_ID='%d' AND Mutant_ID='%d';", tid, mid);
		return getTargetExecutionSQL(sql);
	}

	public static TargetExecution getTargetExecutionForStoryPair(int tid, int mid) {
		String sql = String.format("SELECT * FROM storytargetexecutions WHERE Test_ID='%d' AND Mutant_ID='%d';", tid, mid);
		return getTargetExecutionSQL(sql);
	}

	public static TargetExecution getTargetExecutionForTest(Test test, TargetExecution.Target target) {
		String sql = String.format("SELECT * FROM targetexecutions WHERE Test_ID='%d' AND Target='%s';", test.getId(), target.name());
		return getTargetExecutionSQL(sql);
	}

	public static TargetExecution getTargetExecutionForPTest(PuzzleTest test, TargetExecution.Target target) {
		String sql = String.format("SELECT * FROM storytargetexecutions WHERE Test_ID='%d' AND Target='%s';", test.getTestId(), target.name());
		return getTargetExecutionSQL(sql);
	}

	public static TargetExecution getTargetExecutionForMutant(Mutant mutant, TargetExecution.Target target) {
		String sql = String.format("SELECT * FROM targetexecutions WHERE Mutant_ID='%d' AND Target='%s';", mutant.getId(), target.name());
		return getTargetExecutionSQL(sql);
	}

	public static TargetExecution getTargetExecutionForPMutant(PuzzleMutant mutant, TargetExecution.Target target) {
		String sql = String.format("SELECT * FROM storytargetexecutions WHERE Mutant_ID='%d' AND Target='%s';", mutant.getMutantId(), target.name());
		return getTargetExecutionSQL(sql);
	}

	public static TargetExecution getTargetExecutionSQL(String sql) {
		Connection conn = null;
		Statement stmt = null;

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				TargetExecution targetExecution = new TargetExecution(rs.getInt("TargetExecution_ID"), rs.getInt("Test_ID"),
						rs.getInt("Mutant_ID"), TargetExecution.Target.valueOf(rs.getString("Target")),
						rs.getString("Status"), rs.getString("Message"), rs.getString("Timestamp"));
				return targetExecution;
			}

		} catch (SQLException se) {
			System.out.println(se);
		} // Handle errors for JDBC
		catch (Exception e) {
			System.out.println(e);
		} // Handle errors for Class.forName
		finally {
			cleanup(conn, stmt);
		}
		return null;
	}

	public static void logSession(int uid, String ipAddress) {
		Connection conn =  null;
		Statement stmt = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			String sql = String.format("INSERT INTO sessions (User_ID, IP_Address) VALUES ('%d', '%s');", uid, ipAddress);

			stmt.execute(sql, Statement.RETURN_GENERATED_KEYS);

			ResultSet rs = stmt.getGeneratedKeys();

			if (rs.next()) {
				stmt.close();
				conn.close();
			}

		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			cleanup(conn, stmt);
		}
	}
}
