package org.codedefenders;

import org.codedefenders.multiplayer.LineCoverage;
import org.codedefenders.multiplayer.MultiplayerGame;
import org.codedefenders.multiplayer.MultiplayerMutant;
import org.codedefenders.singleplayer.SinglePlayerGame;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException se2) {
			} // Nothing we can do
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException se) {
				System.out.println(se);
			}
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
			//finally block used to close resources
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
			}// nothing we can do

			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				System.out.println(se);
			}//end finally try
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
			//finally block used to close resources
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
			}// nothing we can do

			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			}//end finally try
		} //end try

		return classList;
	}

	public static User getUserFromPlayer(int playerId) {

		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			sql = String.format("SELECT * FROM users AS u " +
					"LEFT JOIN players AS p ON p.User_ID=u.User_ID " +
					"WHERE p.ID='%d';", playerId);
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				User userRecord = new User(rs.getInt("User_ID"), rs.getString("Username"), rs.getString("Password"), rs.getString("Email"));

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
			//finally block used to close resources
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
			}// nothing we can do

			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				System.out.println(se);
			}//end finally try
		} //end try

		return null;
	}

	public static User getUserForKey(String keyName, int id) {

		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			sql = String.format("SELECT * FROM users WHERE %s=%d;", keyName, id);
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				User userRecord = new User(rs.getInt("User_ID"), rs.getString("Username"), rs.getString("Password"), rs.getString("Email"));

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
			//finally block used to close resources
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
			}// nothing we can do

			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				System.out.println(se);
			}//end finally try
		} //end try

		return null;
	}

	public static User getUserForName(String username) {

		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			sql = String.format("SELECT * FROM users WHERE Username='%s';", username);
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				User newUser = new User(rs.getInt("User_ID"), rs.getString("Username"), rs.getString("Password"), rs.getString("Email"));
				stmt.close();
				conn.close();
				return newUser;
			} else {
				stmt.close();
				conn.close();
				return null;
			}

		} catch (SQLException se) {
			System.out.println(se);
			//Handle errors for JDBC
		} catch (Exception e) {
			System.out.println(e);
			//Handle errors for Class.forName
		} finally {
			//finally block used to close resources
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
			}// nothing we can do

			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				System.out.println(se);
			}//end finally try
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
					"LEFT JOIN players as att ON g.ID=att.Game_ID  AND att.Role='ATTACKER'\n" +
					"LEFT JOIN players AS def ON g.ID=def.Game_ID AND def.Role='DEFENDER'\n" +
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
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException se2) {
			} // Nothing we can do
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException se) {
				System.out.println(se);
			}
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
				"LEFT JOIN players as att ON g.ID=att.Game_ID  AND att.Role='ATTACKER'\n" +
				"LEFT JOIN players AS def ON g.ID=def.Game_ID AND def.Role='DEFENDER'\n" +
				"WHERE g.Mode != 'PARTY' AND (g.Creator_ID=%d OR IFNULL(att.User_ID,0)=%d OR IFNULL(def.User_ID,0)=%d);\n", userId, userId, userId);

		return getGames(sql);
	}

	public static ArrayList<MultiplayerGame> getMultiplayerGamesForUser(int userId) {
		String sql = String.format("SELECT * FROM games AS m " +
				"LEFT JOIN players as p ON p.Game_ID=m.ID " +
				"WHERE m.Mode = 'PARTY' AND (p.User_ID=%d OR m.Creator_ID=%d) AND m.State != 'FINISHED' " +
				"GROUP BY m.ID;", userId, userId, userId);
		return getMultiplayerGames(sql);
	}

	public static ArrayList<MultiplayerGame> getFinishedMultiplayerGamesForUser(int userId) {
		String sql = String.format("SELECT * FROM games AS m " +
				"LEFT JOIN players as p ON p.Game_ID=m.ID " +
				"WHERE (p.User_ID=%d OR m.Creator_ID=%d) AND m.State = 'FINISHED' AND m.Mode='PARTY'" +
				"GROUP BY m.ID;", userId, userId, userId);
		return getMultiplayerGames(sql);
	}

	public static ArrayList<MultiplayerGame> getMultiplayerGamesExcludingUser(int userId) {
		String sql = String.format("SELECT * FROM games AS m " +
				"WHERE m.Mode='PARTY' AND m.Creator_ID!=%d AND NOT EXISTS " +
				"(SELECT * FROM players AS p WHERE p.User_ID=%d AND p.Game_ID=m.ID);", userId, userId, userId);
		return getMultiplayerGames(sql);
	}

	public static Role getRole(int userId, int gameId){
		String sql = String.format("SELECT * FROM games AS m " +
				"LEFT JOIN players AS p ON p.Game_ID = m.ID " +
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
			//finally block used to close resources
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
			}// nothing we can do

			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			}//end finally try
		} //end try

		return role;
	}

	/**
	 * Returns list of <b>finished</b> games for a user
	 * @param userId
	 * @return
	 */
	public static ArrayList<Game> getHistoryForUser(int userId) {
		String sql = String.format("SELECT * FROM games WHERE (Attacker_ID=%d OR Defender_ID=%d) AND State='FINISHED';", userId, userId);
		return getGames(sql);
	}

	public static ArrayList<Game> getAllGames() {
		String sql = "SELECT * FROM games;";
		return getGames(sql);
	}

	public static ArrayList<Game> getOpenGames() {

		String sql = String.format("SELECT g.ID, g.Class_ID, g.Level, g.Creator_ID, g.State," +
				"g.CurrentRound, g.FinalRound, g.ActiveRole, g.Mode, g.Creator_ID,\n" +
				"IFNULL(att.User_ID,0) AS Attacker_ID, IFNULL(def.User_ID,0) AS Defender_ID\n" +
				"FROM games as g\n" +
				"LEFT JOIN players as att ON g.ID=att.Game_ID  AND att.Role='ATTACKER'\n" +
				"LEFT JOIN players AS def ON g.ID=def.Game_ID AND def.Role='DEFENDER'\n" +
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
			//finally block used to close resources
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
			}// nothing we can do

			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			}//end finally try
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
						(float)rs.getDouble("Mutant_Goal"), rs.getInt("Price"), rs.getInt("Defender_Value"),
						rs.getInt("Attacker_Value"), rs.getInt("Defenders_Limit"), rs.getInt("Attackers_Limit"),
						rs.getInt("Defenders_Needed"), rs.getInt("Attackers_Needed"), rs.getLong("Finish_Time"),
								rs.getString("State"));
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
			//finally block used to close resources
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
			}// nothing we can do

			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			}//end finally try
		} //end try

		return gameList;
	}

	public static ArrayList<MultiplayerMutant> getMutantsForAttackers(int[] attackers) {

		ArrayList<MultiplayerMutant> mutList = new ArrayList<>();

		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {

			// Load the MultiplayerGame Data with the provided ID.
			conn = getConnection();

			stmt = conn.createStatement();
			for (int i : attackers) {
				sql = String.format("SELECT * FROM mutants WHERE Player_ID=%d;", i);
				ResultSet rs = stmt.executeQuery(sql);

				while (rs.next()) {
					MultiplayerMutant newMutant = new MultiplayerMutant(rs.getInt("Mutant_ID"), rs.getInt("Game_ID"),
							rs.getString("JavaFile"), rs.getString("ClassFile"), rs.getString("Equivalent"),
							rs.getBoolean("Alive"), rs.getInt("Player_ID"));
					newMutant.setScore(rs.getInt("Points"));
					mutList.add(newMutant);
				}
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
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException se2) {
			} // Nothing we can do
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException se) {
				System.out.println(se);
			}
		}

		return mutList;
	}

	public static ArrayList<Mutant> getMutantsForGame(int gid) {

		ArrayList<Mutant> mutList = new ArrayList<>();

		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {

			// Load the MultiplayerGame Data with the provided ID.
			conn = getConnection();

			stmt = conn.createStatement();
			sql = String.format("SELECT * FROM mutants WHERE Game_ID='%d' AND RoundCreated >= 0;", gid);
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				Mutant newMutant = new Mutant(rs.getInt("Mutant_ID"), rs.getInt("Game_ID"),
						rs.getString("JavaFile"), rs.getString("ClassFile"),
						rs.getBoolean("Alive"), Mutant.Equivalence.valueOf(rs.getString("Equivalent")),
						rs.getInt("RoundCreated"), rs.getInt("RoundKilled"), rs.getInt("Player_ID"));
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
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException se2) {
			} // Nothing we can do
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException se) {
				System.out.println(se);
			}
		}

		return mutList;
	}

	public static Mutant getMutant(Game game, int mutantID) {

		Mutant newMutant = null;

		Connection conn = null;
		Statement stmt = null;

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			String sql = String.format("SELECT * FROM mutants WHERE Mutant_ID='%d' AND Game_ID='%d' AND RoundCreated >= 0;", mutantID, game.getId());
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
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException se2) {
			} // Nothing we can do
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException se) {
				System.out.println(se);
			}
		}

		return newMutant;
	}

	public static MultiplayerMutant getMultiplayerMutant(int mutantID) {

		MultiplayerMutant newMutant = null;

		Connection conn = null;
		Statement stmt = null;

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			String sql = String.format("SELECT * FROM mutants WHERE Mutant_ID='%d';", mutantID);
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				newMutant = new MultiplayerMutant(rs.getInt("Mutant_ID"), rs.getInt("Game_ID"),
						rs.getString("JavaFile"), rs.getString("ClassFile"), rs.getString("Equivalent"),
						rs.getBoolean("Alive"), rs.getInt("Attacker_ID"));
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
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException se2) {
			} // Nothing we can do
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException se) {
				System.out.println(se);
			}
		}

		return newMutant;
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
			//se2.printStackTrace();
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

	public static ArrayList<Test> getExecutableTestsForGame(int gid) {
		String stmt = "SELECT tests.* FROM tests "
				+ "INNER JOIN targetexecutions ex on tests.Test_ID = ex.Test_ID "
				+ "WHERE tests.Game_ID='%d' AND tests.ClassFile IS NOT NULL " // only compilable tests
				+ "AND ex.Target='TEST_ORIGINAL' AND ex.Status='SUCCESS';"; // that pass on original CUT

		//String sql = String.format("SELECT * FROM tests WHERE Game_ID='%d' AND ClassFile IS NOT NULL;", gid);
		String sql = String.format(stmt, gid);
		return getTests(sql);
	}

	public static ArrayList<Test> getExecutableTestsForMultiplayerGame(int defenderId) {
		String stmt = "SELECT tests.* FROM tests "
				+ "INNER JOIN targetexecutions ex on tests.Test_ID = ex.Test_ID "
				+ "WHERE tests.Player_ID='%d' AND tests.ClassFile IS NOT NULL " // only compilable tests
				+ "AND ex.Target='TEST_ORIGINAL' AND ex.Status='SUCCESS';"; // that pass on original CUT

		//String sql = String.format("SELECT * FROM tests WHERE Game_ID='%d' AND ClassFile IS NOT NULL;", gid);
		String sql = String.format(stmt, defenderId);
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
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException se2) {
			} // Nothing we can do
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException se) {
				System.out.println(se);
			}
		}
		return 0;
	}

	public static int[] getDefendersForMultiplayerGame(int gameId) {
		String sql = String.format("SELECT * FROM players " +
				"WHERE Game_ID = %d AND Role='%s'", gameId, Role.DEFENDER); // that pass on original CUT

		Connection conn = null;
		Statement stmt = null;

		int[] defenders = new int[0];

		try {

			// Load the MultiplayerGame Data with the provided ID.
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			ArrayList<Integer> defs = new ArrayList<>();

			while (rs.next()) {
				defs.add(rs.getInt("ID"));
			}

			defenders = new int[defs.size()];

			for (int i = 0; i < defs.size(); i++){
				defenders[i] = defs.get(i);
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
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException se2) {
			} // Nothing we can do
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException se) {
				System.out.println(se);
			}
		}
		return defenders;
	}

	public static int[] getAttackersForMultiplayerGame(int gameId) {
		String sql = String.format("SELECT * FROM players " +
				"WHERE Game_ID = %d AND Role='%s'", gameId, Role.ATTACKER); // that pass on original CUT

		Connection conn = null;
		Statement stmt = null;

		int[] attackers = new int[0];

		try {

			// Load the MultiplayerGame Data with the provided ID.
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			ArrayList<Integer> atks = new ArrayList<>();

			while (rs.next()) {
				atks.add(rs.getInt("ID"));
			}

			attackers = new int[atks.size()];

			for (int i = 0; i < atks.size(); i++){
				attackers[i] = atks.get(i);
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
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException se2) {
			} // Nothing we can do
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException se) {
				System.out.println(se);
			}
		}
		return attackers;
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
				if (lcs != null && lucs != null && lcs.length() > 0 && lucs.length() > 0) {
					String[] covered = lcs.split(",");
					String[] uncovered = lucs.split(",");
					Integer[] cov = new Integer[covered.length];
					Integer[] uncov = new Integer[uncovered.length];
					for (int i = 0; i < covered.length; i++) {
						cov[i] = Integer.parseInt(covered[i]);
					}
					for (int i = 0; i < uncovered.length; i++) {
						uncov[i] = Integer.parseInt(uncovered[i]);
					}

					LineCoverage lc = new LineCoverage();

					lc.setLinesUncovered(uncov);
					lc.setLinesCovered(cov);

					newTest.setLineCoverage(lc);

					newTest.setScore(rs.getInt("Points"));
				}

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
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException se2) {
			} // Nothing we can do
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException se) {
				System.out.println(se);
			}
		}

		return testList;
	}

	public static ArrayList<TargetExecution> getTargetExecutionsForKey(String keyname, int id) {
		ArrayList<TargetExecution> executionList = new ArrayList<TargetExecution>();

		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {

			// Load the MultiplayerGame Data with the provided ID.
			conn = getConnection();

			stmt = conn.createStatement();
			sql = String.format("SELECT * FROM targetexecutions WHERE %s='%d';", keyname, id);
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				TargetExecution newExecution = new TargetExecution(rs.getInt("TargetExecution_ID"), rs.getInt("Test_ID"),
						rs.getInt("Mutant_ID"), TargetExecution.Target.valueOf(rs.getString("Target")),
						rs.getString("State"), rs.getString("Message"), rs.getString("Timestamp"));
				executionList.add(newExecution);
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
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException se2) {
			} // Nothing we can do
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException se) {
				System.out.println(se);
			}
		}

		return executionList;
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

	public static TargetExecution getTargetExecutionForMultiplayerMutant(MultiplayerMutant mutant, TargetExecution.Target target) {
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
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException se2) {
			} // Nothing we can do
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException se) {
				System.out.println(se);
			}
		}
		return null;
	}
}
