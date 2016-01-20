package org.codedefenders;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class DatabaseAccess {

	public static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	public static final String DB_URL = "jdbc:mysql://localhost/codedefenders?autoReconnect=true&useSSL=false";

	//  Database credentials
	public static final String USER = "root";
	public static final String PASS = "***REMOVED***";

	public static String addSlashes(String s) {
		return s.replaceAll("\\\\", "\\\\\\\\");
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
						Game.Level.valueOf(rs.getString("Level")));

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

	public static ArrayList<Game> getGamesForUser(int userId) {

		Connection conn = null;
		Statement stmt = null;
		String sql = null;
		ArrayList<Game> gameList = new ArrayList<Game>();

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			sql = String.format("SELECT * FROM games WHERE Attacker_ID=%d OR Defender_ID=%d;", userId, userId);
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				gameList.add(new Game(rs.getInt("Game_ID"), rs.getInt("Attacker_ID"), rs.getInt("Defender_ID"),
						rs.getInt("Class_ID"), rs.getInt("CurrentRound"), rs.getInt("FinalRound"),
						Game.Role.valueOf(rs.getString("ActiveRole")), Game.State.valueOf(rs.getString("State")),
						Game.Level.valueOf(rs.getString("Level"))));
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

	public static ArrayList<Game> getAllGames() {

		Connection conn = null;
		Statement stmt = null;
		String sql = null;
		ArrayList<Game> gameList = new ArrayList<Game>();

		try {
			conn = getConnection();

			stmt = conn.createStatement();
			sql = "SELECT * FROM games;";
			ResultSet rs = stmt.executeQuery(sql);

			while (rs.next()) {
				gameList.add(new Game(rs.getInt("Game_ID"), rs.getInt("Attacker_ID"), rs.getInt("Defender_ID"),
						rs.getInt("Class_ID"), rs.getInt("CurrentRound"), rs.getInt("FinalRound"),
						Game.Role.valueOf(rs.getString("ActiveRole")), Game.State.valueOf(rs.getString("State")),
						Game.Level.valueOf(rs.getString("Level"))));
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

	public static Connection getConnection() throws ClassNotFoundException, SQLException {
		Class.forName(JDBC_DRIVER);
		return DriverManager.getConnection(DatabaseAccess.DB_URL, DatabaseAccess.USER, DatabaseAccess.PASS);
	}

	public static ArrayList<Mutant> getMutantsForGame(int gid) {

		ArrayList<Mutant> mutList = new ArrayList<Mutant>();

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
			String sql = sql = String.format("SELECT * FROM mutants WHERE Mutant_ID='%d' AND Game_ID='%d';", mutantID, game.getId());
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

	public static ArrayList<Test> getTestsForGame(int gid) {
		String sql = String.format("SELECT * FROM tests WHERE Game_ID='%d';", gid);
		return getTests(sql);
	}

	public static ArrayList<Test> getExecutableTestsForGame(int gid) {
		String sql = String.format("SELECT * FROM tests WHERE Game_ID='%d' AND ClassFile IS NOT NULL;", gid);
		return getTests(sql);
	}

	private static ArrayList<Test> getTests(String sql) {

		ArrayList<Test> testList = new ArrayList<Test>();

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
