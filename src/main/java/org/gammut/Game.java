package org.gammut;

import static org.gammut.Mutant.Equivalence.PENDING_TEST;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class Game {

	private int id;

	private int classId;

	private int attackerId;
	private int defenderId;

	private int currentRound;
	private int finalRound;

	private String activePlayer;
	private String state;

	public static enum Level {EASY, MEDIUM, HARD};
	private Level level;

	public Game(int classId, int userId, int maxRounds, String role, Level level) {
		this.classId = classId;

		if (role.equals("ATTACKER")) {
			attackerId = userId;
		} else {
			defenderId = userId;
		}

		this.currentRound = 1;
		this.finalRound = maxRounds;

		this.activePlayer = "NEITHER";
		this.state = "CREATED";

		this.level = level;
	}

	public Game(int id, int attackerId, int defenderId, int classId, int currentRound, int finalRound, String activePlayer, String state, Level level) {
		this.id = id;
		this.attackerId = attackerId;
		this.defenderId = defenderId;
		this.classId = classId;
		this.currentRound = currentRound;
		this.finalRound = finalRound;
		this.activePlayer = activePlayer;
		this.state = state;
		this.level = level;
	}

	public int getId() {
		return id;
	}

	public int getClassId() {
		return classId;
	}

	public String getClassName() {
		return DatabaseAccess.getClassForKey("Class_ID", classId).name;
	}

	public GameClass getCUT() {
		return DatabaseAccess.getClassForKey("Class_ID", classId);
	}

	public int getAttackerId() {
		return attackerId;
	}

	public void setAttackerId(int aid) {
		attackerId = aid;
	}

	public int getDefenderId() {
		return defenderId;
	}

	public void setDefenderId(int did) {
		defenderId = did;
	}

	public boolean isUserInGame(int uid) {
		if ((uid == attackerId) || (uid == defenderId)) {
			return true;
		} else {
			return false;
		}
	}

	public int getCurrentRound() {
		return currentRound;
	}

	public int getFinalRound() {
		return finalRound;
	}

	public String getActivePlayer() {
		return activePlayer;
	}

	// ATTACKER, DEFENDER, NEITHER
	public void setActivePlayer(String ap) {
		activePlayer = ap;
	}

	public String getState() {
		return state;
	}

	// CREATED, IN PROGRESS, FINISHED
	public void setState(String s) {
		state = s;
	}

	public Level getLevel() {
		return this.level;
	}

	public void setLevel(Level level) {
		this.level = level;
	}

	public ArrayList<Mutant> getMutants() {
		return DatabaseAccess.getMutantsForGame(id);
	}

	public ArrayList<Mutant> getAliveMutants() {
		ArrayList<Mutant> aliveMutants = new ArrayList<Mutant>();
		for (Mutant m : getMutants()) {
			if (m.isAlive()) {
				aliveMutants.add(m);
			}
		}
		return aliveMutants;
	}

	public ArrayList<Mutant> getKilledMutants() {
		ArrayList<Mutant> killedMutants = new ArrayList<Mutant>();
		for (Mutant m : getMutants()) {
			if (! m.isAlive()) {
				killedMutants.add(m);
			}
		}
		return killedMutants;
	}

	public ArrayList<Mutant> getMutantsMarkedEquivalent() {
		ArrayList<Mutant> equivMutants = new ArrayList<Mutant>();
		for (Mutant m : getMutants()) {
			if (m.isAlive() && m.getEquivalent().equals(PENDING_TEST)) {
				equivMutants.add(m);
			}
		}
		return equivMutants;
	}

	public Mutant getMutantByID(int mutantID) {
		for (Mutant m : getMutants()) {
			if (m.getId() == mutantID)
				return m;
		}
		return null;
	}

	public ArrayList<Test> getTests() {
		return DatabaseAccess.getTestsForGame(id);
	}

	public int getAttackerScore() {
		int totalScore = 0;

		for (Mutant m : getMutants()) {
			totalScore += m.getPoints();
		}
		return totalScore;
	}

	public int getDefenderScore() {
		int totalScore = 0;

		for (Test t : getTests()) {
			totalScore += t.getPoints();
		}
		return totalScore;
	}

	public void passPriority() {
		if (activePlayer.equals("ATTACKER")) {
			activePlayer = "DEFENDER";
		} else if (activePlayer.equals("DEFENDER")) {
			activePlayer = "ATTACKER";
		}
	}

	public void endTurn() {
		if (activePlayer.equals("ATTACKER")) {
			activePlayer = "DEFENDER";
		} else if (activePlayer.equals("DEFENDER")) {
			activePlayer = "ATTACKER";
			if (currentRound < finalRound) {
				currentRound++;
			} else if ((currentRound == finalRound) && (state.equals("IN PROGRESS"))) {
				state = "FINISHED";
			}
		}
	}

	public boolean insert() {

		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		// Attempt to insert game info into database
		try {
			conn = DatabaseAccess.getConnection();

			stmt = conn.createStatement();
			System.out.println(attackerId);
			System.out.println(defenderId);
			if (attackerId != 0) {
				sql = String.format("INSERT INTO games (Attacker_ID, FinalRound, Class_ID, Level) VALUES ('%d', '%d', '%d', '%s');", attackerId, finalRound, classId, level.name());
			} else {
				sql = String.format("INSERT INTO games (Defender_ID, FinalRound, Class_ID, Level) VALUES ('%d', '%d', '%d', '%s');", defenderId, finalRound, classId, level.name());
			}

			stmt.execute(sql, Statement.RETURN_GENERATED_KEYS);

			ResultSet rs = stmt.getGeneratedKeys();

			if (rs.next()) {
				id = rs.getInt(1);
				stmt.close();
				conn.close();
				return true;
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

		return false;
	}

	public boolean update() {

		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {
			conn = DatabaseAccess.getConnection();

			// Get all rows from the database which have the chosen username
			stmt = conn.createStatement();
			sql = String.format("UPDATE games SET Attacker_ID='%d', Defender_ID='%d', CurrentRound='%d', FinalRound='%d', ActivePlayer='%s', State='%s', Level='%s' WHERE Game_ID='%d'",
					attackerId, defenderId, currentRound, finalRound, activePlayer, state, level.name(), id);
			stmt.execute(sql);
			return true;

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

		return false;
	}
}
