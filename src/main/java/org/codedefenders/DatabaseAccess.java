package org.codedefenders;

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

	public static GameClass getClassForKey(String keyName, int id) {

		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {
			conn = getConnection();
			stmt = conn.createStatement();
			sql = String.format("SELECT * FROM classes WHERE %s=%d;", keyName, id);
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				GameClass classRecord = new GameClass(rs.getInt("Class_ID"), rs.getString("Name"), rs.getString("JavaFile"), rs.getString("ClassFile"));
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
				classList.add(new GameClass(rs.getInt("Class_ID"), rs.getString("Name"), rs.getString("JavaFile"), rs.getString("ClassFile")));
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
				User userRecord = new User(rs.getInt("User_ID"), rs.getString("Username"), rs.getString("Password"));

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
				User newUser = new User(rs.getInt("User_ID"), rs.getString("Username"), rs.getString("Password"));
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

			// Load the Game Data with the provided ID.
			conn = getConnection();

			stmt = conn.createStatement();
			sql = String.format("SELECT * FROM games WHERE %s='%d';", keyName, id);
			ResultSet rs = stmt.executeQuery(sql);

			if (rs.next()) {
				Game gameRecord = new Game(rs.getInt("Game_ID"), rs.getInt("Attacker_ID"), rs.getInt("Defender_ID"), rs.getInt("Class_ID"),
						rs.getInt("CurrentRound"), rs.getInt("FinalRound"), Game.Role.valueOf(rs.getString("ActiveRole")), Game.State.valueOf(rs.getString("State")),
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
		String sql = String.format("SELECT * FROM games WHERE (Attacker_ID=%d OR Defender_ID=%d) AND State!='FINISHED';", userId, userId);
		return getGames(sql);
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
		String sql = "SELECT * FROM games where (Mode='DUEL' AND State='CREATED') OR (Mode='PARTY' AND State!='FINISHED');";
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
				gameList.add(new Game(rs.getInt("Game_ID"), rs.getInt("Attacker_ID"), rs.getInt("Defender_ID"),
						rs.getInt("Class_ID"), rs.getInt("CurrentRound"), rs.getInt("FinalRound"),
						Game.Role.valueOf(rs.getString("ActiveRole")), Game.State.valueOf(rs.getString("State")),
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

	public static ArrayList<Mutant> getMutantsForGame(int gid) {

		ArrayList<Mutant> mutList = new ArrayList<>();

		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {

			// Load the Game Data with the provided ID.
			conn = getConnection();

			stmt = conn.createStatement();
			sql = String.format("SELECT * FROM mutants WHERE Game_ID='%d';", gid);
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				Mutant newMutant = new Mutant(rs.getInt("Mutant_ID"), rs.getInt("Game_ID"),
						rs.getString("JavaFile"), rs.getString("ClassFile"),
						rs.getBoolean("Alive"), Mutant.Equivalence.valueOf(rs.getString("Equivalent")),
						rs.getInt("RoundCreated"), rs.getInt("RoundKilled"), rs.getInt("Owner_ID"));
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
			String sql = String.format("SELECT * FROM mutants WHERE Mutant_ID='%d' AND Game_ID='%d';", mutantID, game.getId());
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next()) {
				newMutant = new Mutant(rs.getInt("Mutant_ID"), rs.getInt("Game_ID"),
						rs.getString("JavaFile"), rs.getString("ClassFile"),
						rs.getBoolean("Alive"), Mutant.Equivalence.valueOf(rs.getString("Equivalent")),
						rs.getInt("RoundCreated"), rs.getInt("RoundKilled"), rs.getInt("Owner_ID"));
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

	private static ArrayList<Test> getTests(String sql) {

		ArrayList<Test> testList = new ArrayList<>();

		Connection conn = null;
		Statement stmt = null;

		try {

			// Load the Game Data with the provided ID.
			conn = getConnection();

			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				Test newTest = new Test(rs.getInt("Test_ID"), rs.getInt("Game_ID"),
						rs.getString("JavaFile"), rs.getString("ClassFile"),
						rs.getInt("RoundCreated"), rs.getInt("MutantsKilled"), rs.getInt("Owner_ID"));
				testList.add(newTest);
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

		return testList;
	}

	public static ArrayList<TargetExecution> getTargetExecutionsForKey(String keyname, int id) {
		ArrayList<TargetExecution> executionList = new ArrayList<TargetExecution>();

		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {

			// Load the Game Data with the provided ID.
			conn = getConnection();

			stmt = conn.createStatement();
			sql = String.format("SELECT * FROM targetexecutions WHERE %s='%d';", keyname, id);
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				TargetExecution newExecution = new TargetExecution(rs.getInt("TargetExecution_ID"), rs.getInt("Test_ID"),
						rs.getInt("Mutant_ID"), TargetExecution.Target.valueOf(rs.getString("Target")),
						rs.getString("Status"), rs.getString("Message"), rs.getString("Timestamp"));
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
