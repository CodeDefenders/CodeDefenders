package org.codedefenders;

import org.codedefenders.multiplayer.LineCoverage;
import org.codedefenders.multiplayer.MultiplayerGame;
import org.codedefenders.singleplayer.SinglePlayerGame;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;

public class DatabaseAccess {

	public static Connection getConnection() throws ClassNotFoundException, SQLException, NamingException {
		Context initialContext = new InitialContext();
		Context environmentContext = (Context) initialContext.lookup("java:comp/env");
		String dataResourceName = "jdbc/codedefenders";
		DataSource dataSource = (DataSource) environmentContext.lookup(dataResourceName);
		return dataSource.getConnection();
	}

	public static String addSlashes(String s) {
		return s.replaceAll("\\\\", "\\\\\\\\");
	}

	public static int getNumAiMutantsKilledByTest(int tId) {
		String sql = String.format("SELECT * FROM tests WHERE Test_ID='%d';", tId);
		return getInt(sql, "NumberAiMutantsKilled");
	}

	public static int getNumTestsKillMutant(int mId) {
		String sql = String.format("SELECT * FROM mutants WHERE Mutant_ID='%d';", mId);
		return getInt(sql, "NumberAiKillingTests");
	}

	public static boolean gameWithUserExistsForClass(int uId, int cId) {
		ArrayList<Game> games = getGamesForUser(uId);
		for (Game g : games) {
			if(g.getClassId() == cId) {
				return true;
			}
		}
		return false;
	}

	public static int getInt(String sql, String att) {
		Connection conn = null;
		Statement stmt = null;

		int numMutants = 0;

		try {

			// Load the Game Data with the provided ID.
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				numMutants = rs.getInt(att);
			}

			stmt.close();
			conn.close();
		} catch (SQLException se) {
			System.out.println(se);
		} // Handle errors for JDBC
		catch (Exception e) {
			System.out.println(e);
		} // Handle errors for Class.forName
		finally {
			cleanup(conn, stmt);
		}
		return numMutants;
	}

	public static GameClass getClassForGame(int gameId) {
		String sql = String.format("SELECT classes.* from classes INNER JOIN games ON classes.Class_ID = games.Class_ID WHERE games.ID=%d;", gameId);
		return getClass(sql);
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

			stmt.close();
			conn.close();
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
		Connection conn = null;
		Statement stmt = null;
		String sql = String.format("UPDATE classes SET AiPrepared = 1 WHERE Class_ID = %d;", c.getId());

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);

			stmt.close();
			conn.close();
		} catch (SQLException se) {
			System.out.println(se);
			//Handle errors for JDBC
		} catch (Exception e) {
			System.out.println(e);
			//Handle errors for Class.forName
		} finally {
			cleanup(conn, stmt);
		} //end try
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
				stmt.close();
				conn.close();
				return classRecord;
			}

			stmt.close();
			conn.close();


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

	public static ArrayList<GameClass> getAllClasses() {
		Connection conn = null;
		Statement stmt = null;
		String sql = null;
		ArrayList<GameClass> classList = new ArrayList<GameClass>();

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			sql = "SELECT * FROM classes;";
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				classList.add(new GameClass(rs.getInt("Class_ID"), rs.getString("Name"), rs.getString("Alias"), rs.getString("JavaFile"), rs.getString("ClassFile")));
			}

			stmt.close();
			conn.close();


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

	public static ArrayList<User> getAllUsers() {
		String sql = String.format("SELECT * FROM users");

		Connection conn = null;
		Statement stmt = null;

		ArrayList<User> uList = new ArrayList<User>();

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				User userRecord = new User(rs.getInt("User_ID"), rs.getString("Username"), rs.getString("Password"), rs.getString("Email"), rs.getBoolean("Validated"));
				uList.add(userRecord);
			}
			stmt.close();
			conn.close();
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
				stmt.close();
				conn.close();
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

	public static Game getGameForKey(String keyName, int id) {

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
				Game gameRecord;
				if (rs.getString("Mode").equals(Game.Mode.SINGLE.name()))
					gameRecord = new SinglePlayerGame(rs.getInt("ID"), rs.getInt("Attacker_ID"), rs.getInt("Defender_ID"), rs.getInt("Class_ID"),
							rs.getInt("CurrentRound"), rs.getInt("FinalRound"), Role.valueOf(rs.getString("ActiveRole")), AbstractGame.State.valueOf(rs.getString("State")),
							Game.Level.valueOf(rs.getString("Level")), Game.Mode.valueOf(rs.getString("Mode")));
				else
					gameRecord = new Game(rs.getInt("ID"), rs.getInt("Attacker_ID"), rs.getInt("Defender_ID"), rs.getInt("Class_ID"),
							rs.getInt("CurrentRound"), rs.getInt("FinalRound"), Role.valueOf(rs.getString("ActiveRole")), AbstractGame.State.valueOf(rs.getString("State")),
							Game.Level.valueOf(rs.getString("Level")), Game.Mode.valueOf(rs.getString("Mode")));

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

	/**
	 * Returns list of <b>active</b> games for a user
	 * @param userId
	 * @return
	 */
	public static ArrayList<Game> getGamesForUser(int userId) {

		String sql = String.format("SELECT g.ID, g.Class_ID, g.Level, g.Creator_ID, g.State," +
				"g.CurrentRound, g.FinalRound, g.ActiveRole, g.Mode, g.Creator_ID,\n" +
				"IFNULL(att.User_ID,0) AS Attacker_ID, IFNULL(def.User_ID,0) AS Defender_ID\n" +
				"FROM games as g\n" +
				"LEFT JOIN players as att ON g.ID=att.Game_ID  AND att.Role='ATTACKER' AND att.Active=TRUE\n" +
				"LEFT JOIN players AS def ON g.ID=def.Game_ID AND def.Role='DEFENDER' AND def.Active=TRUE\n" +
				"WHERE g.Mode != 'PARTY' AND g.State!='FINISHED' AND (g.Creator_ID=%d OR IFNULL(att.User_ID,0)=%d OR IFNULL(def.User_ID,0)=%d);\n", userId, userId, userId);

		return getGames(sql);
	}

	public static ArrayList<MultiplayerGame> getJoinedMultiplayerGamesForUser(int userId) {
		String sql = String.format("SELECT * FROM games AS m " +
				"LEFT JOIN players as p ON p.Game_ID=m.ID \n" +
				"WHERE m.Mode = 'PARTY' AND (p.User_ID=%d)" +
				"GROUP BY m.ID;", userId);
		return getMultiplayerGames(sql);
	}

	public static ArrayList<MultiplayerGame> getMultiplayerGamesForUser(int userId) {
		String sql = String.format("SELECT * FROM games AS m " +
				"LEFT JOIN players as p ON p.Game_ID=m.ID  AND p.Active=TRUE \n" +
				"WHERE m.Mode = 'PARTY' AND (p.User_ID=%d OR m.Creator_ID=%d) AND m.State != 'FINISHED' " +
				"GROUP BY m.ID;", userId, userId, userId);
		return getMultiplayerGames(sql);
	}

	public static ArrayList<MultiplayerGame> getFinishedMultiplayerGamesForUser(int userId) {
		String sql = String.format("SELECT * FROM games AS m " +
				"LEFT JOIN players as p ON p.Game_ID=m.ID  AND p.Active=TRUE \n" +
				"WHERE (p.User_ID=%d OR m.Creator_ID=%d) AND m.State = 'FINISHED' AND m.Mode='PARTY'" +
				"GROUP BY m.ID;", userId, userId, userId);
		return getMultiplayerGames(sql);
	}

	public static ArrayList<MultiplayerGame> getOpenMultiplayerGamesForUser(int userId) {
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
			stmt.close();
			conn.close();
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
	public static ArrayList<Game> getHistoryForUser(int userId) {
		String sql = String.format("SELECT g.ID, g.Class_ID, g.Level, g.Creator_ID, g.State," +
				"g.CurrentRound, g.FinalRound, g.ActiveRole, g.Mode, g.Creator_ID,\n" +
				"IFNULL(att.User_ID,0) AS Attacker_ID, IFNULL(def.User_ID,0) AS Defender_ID\n" +
				"FROM games as g\n" +
				"LEFT JOIN players as att ON g.ID=att.Game_ID  AND att.Role='ATTACKER'\n" +
				"LEFT JOIN players AS def ON g.ID=def.Game_ID AND def.Role='DEFENDER'\n" +
				"WHERE g.Mode != 'PARTY' AND g.State='FINISHED' AND (g.Creator_ID=%d OR IFNULL(att.User_ID,0)=%d OR IFNULL(def.User_ID,0)=%d);\n", userId, userId, userId);

		return getGames(sql);
	}

	public static ArrayList<Game> getOpenGames() {

		String sql = String.format("SELECT g.ID, g.Class_ID, g.Level, g.Creator_ID, g.State," +
				"g.CurrentRound, g.FinalRound, g.ActiveRole, g.Mode, g.Creator_ID,\n" +
				"IFNULL(att.User_ID,0) AS Attacker_ID, IFNULL(def.User_ID,0) AS Defender_ID\n" +
				"FROM games as g\n" +
				"LEFT JOIN players as att ON g.ID=att.Game_ID  AND att.Role='ATTACKER' AND att.Active=TRUE\n" +
				"LEFT JOIN players AS def ON g.ID=def.Game_ID AND def.Role='DEFENDER' AND def.Active=TRUE\n" +
				"WHERE g.Mode = 'DUEL' AND g.State = 'CREATED';");

		return getGames(sql);
	}

	public static Game getActiveUnitTestingSession(int userId) {
		String sql = String.format("SELECT * FROM games WHERE Defender_ID='%d' AND Mode='UTESTING' AND State='ACTIVE';", userId);
		ArrayList<Game> games = getGames(sql);
		if (games.isEmpty())
			return null;
		else
			return games.get(0);
	}

	public static ArrayList<Game> getGames(String sql) {
		Connection conn = null;
		Statement stmt = null;
		ArrayList<Game> gameList = new ArrayList<>();

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				gameList.add(new Game(rs.getInt("ID"), rs.getInt("Attacker_ID"), rs.getInt("Defender_ID"),
						rs.getInt("Class_ID"), rs.getInt("CurrentRound"), rs.getInt("FinalRound"),
						Role.valueOf(rs.getString("ActiveRole")), AbstractGame.State.valueOf(rs.getString("State")),
						Game.Level.valueOf(rs.getString("Level")), Game.Mode.valueOf(rs.getString("Mode"))));
			}
			stmt.close();
			conn.close();
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

	public static MultiplayerGame getMultiplayerGame(int id){
		String sql = String.format("SELECT * FROM games AS m " +
				"WHERE ID=%d AND m.Mode='PARTY'", id);

		ArrayList<MultiplayerGame> mgs = getMultiplayerGames(sql);
		if (mgs.size() > 0){
			return mgs.get(0);
		}
		return null;
	}

	public static ArrayList<MultiplayerGame> getMultiplayerGames(String sql) {
		Connection conn = null;
		Statement stmt = null;
		ArrayList<MultiplayerGame> gameList = new ArrayList<>();

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				MultiplayerGame mg = new MultiplayerGame(rs.getInt("Class_ID"), rs.getInt("Creator_ID"),
						Game.Level.valueOf(rs.getString("Level")), (float)rs.getDouble("Coverage_Goal"),
						(float)rs.getDouble("Mutant_Goal"), rs.getInt("Prize"), rs.getInt("Defender_Value"),
						rs.getInt("Attacker_Value"), rs.getInt("Defenders_Limit"), rs.getInt("Attackers_Limit"),
						rs.getInt("Defenders_Needed"), rs.getInt("Attackers_Needed"), rs.getTimestamp("Start_Time").getTime(),
						rs.getTimestamp("Finish_Time").getTime(), rs.getString("State"), rs.getBoolean("RequiresValidation"));
				mg.setId(rs.getInt("ID"));
				gameList.add(mg);
			}
			stmt.close();
			conn.close();
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

	public static ArrayList<Mutant> getMutantsForGame(int gid) {

		ArrayList<Mutant> mutList = new ArrayList<>();
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
			stmt.close();
			conn.close();
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

	public static ArrayList<Mutant> getMutantsForPlayer(int pid) {

		ArrayList<Mutant> mutList = new ArrayList<>();
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
			stmt.close();
			conn.close();
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

			stmt.close();
			conn.close();
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

	public static Mutant getMutant(Game game, int mutantID) {
		String sql = String.format("SELECT * FROM mutants WHERE Mutant_ID='%d' AND Game_ID='%d';", mutantID, game.getId());
		return getMutantFromDB(sql);
	}

	public static Mutant getMutant(int gameId, String md5) {
		String sql = String.format("SELECT * FROM mutants WHERE Game_ID='%d' AND MD5='%s';", gameId, md5);
		return getMutantFromDB(sql);
	}

	public static ArrayList<Integer> getUsedAiTestsForGame(Game g) {
		ArrayList<Integer> testList = new ArrayList<Integer>();

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

		return testList;
	}

	public static void increasePlayerPoints(int points, int player){
		Connection conn =  null;
		Statement stmt = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			String sql = String.format(
					"UPDATE players SET Points=Points+%d WHERE ID=%d",
					points, player
			);

			stmt.execute(sql);
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
			stmt.close();
			conn.close();

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
			stmt.close();
			conn.close();

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

	public static boolean setAiTestAsUsed(int testNumber, Game g) {
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

	public static ArrayList<Integer> getUsedAiMutantsForGame(Game g) {
		ArrayList<Integer> mutantList = new ArrayList<Integer>();

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
	public static boolean setAiMutantAsUsed(int mutantNumber, Game g) {
		Connection conn =  null;
		Statement stmt = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			String sql = String.format("INSERT INTO usedaimutants (Value, Game_ID) VALUES ('%d', '%d');", mutantNumber, g.getId());

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

	public static ArrayList<Test> getTestsForGame(int gid) {
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
	public static ArrayList<Test> getExecutableTests(int gameId, boolean defendersOnly) {
		String stmt = "SELECT tests.* FROM tests\n"
				+ "INNER JOIN targetexecutions ex on tests.Test_ID = ex.Test_ID\n"
				+ (defendersOnly ? "INNER JOIN players pl on tests.Player_ID = pl.ID\n" : "")
				+ "WHERE tests.Game_ID='%d' AND tests.ClassFile IS NOT NULL\n"  // only compilable tests for a game
				+ (defendersOnly ? "AND pl.Role='DEFENDER'\n" : "")             // [optional] submitted by defenders only
				+ "AND ex.Target='TEST_ORIGINAL' AND ex.Status='SUCCESS';";     // that pass on original CUT

		String sql = String.format(stmt, gameId);
		return getTests(sql);
	}

	public static int getPlayerIdForMultiplayerGame(int userId, int gameId) {
		String sql = String.format("SELECT * FROM players AS p " +
				"WHERE p.User_ID = %d AND p.Game_ID = %d", userId, gameId); // that pass on original CUT

		Connection conn = null;
		Statement stmt = null;
		try {

			// Load the MultiplayerGame Data with the provided ID.
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				return rs.getInt("ID");
			}

			stmt.close();
			conn.close();
		} catch (SQLException se) {
			System.out.println(se);
		} // Handle errors for JDBC
		catch (Exception e) {
			System.out.println(e);
		} // Handle errors for Class.forName
		finally {
			cleanup(conn, stmt);
		}
		return 0;
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

			ArrayList<Integer> atks = new ArrayList<>();

			while (rs.next()) {
				atks.add(rs.getInt("ID"));
			}

			players = new int[atks.size()];

			for (int i = 0; i < atks.size(); i++){
				players[i] = atks.get(i);
			}

			stmt.close();
			conn.close();
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

	private static ArrayList<Test> getTests(String sql) {

		ArrayList<Test> testList = new ArrayList<>();

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

	public static int getKillingTestIdForMutant(int mid) {
		String sql = String.format("SELECT * FROM targetexecutions WHERE Target='TEST_MUTANT' AND Status!='SUCCESS' AND Mutant_ID='%d';", mid);
		TargetExecution targ = getTargetExecutionSQL(sql);
		return (targ != null) ? targ.testId : -1; // TODO: We shouldn't give away that we don't know which test killed the mutant?
	}

	public static TargetExecution getTargetExecutionForPair(int tid, int mid) {
		String sql = String.format("SELECT * FROM targetexecutions WHERE Test_ID='%d' AND Mutant_ID='%d';", tid, mid);
		return getTargetExecutionSQL(sql);
	}

	public static TargetExecution getTargetExecutionForTest(Test test, TargetExecution.Target target) {
		String sql = String.format("SELECT * FROM targetexecutions WHERE Test_ID='%d' AND Target='%s';", test.getId(), target.name());
		return getTargetExecutionSQL(sql);
	}

	public static TargetExecution getTargetExecutionForMutant(Mutant mutant, TargetExecution.Target target) {
		String sql = String.format("SELECT * FROM targetexecutions WHERE Mutant_ID='%d' AND Target='%s';", mutant.getId(), target.name());
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
				stmt.close();
				conn.close();
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
