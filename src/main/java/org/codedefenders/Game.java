package org.codedefenders;

import static org.codedefenders.Mutant.Equivalence.PENDING_TEST;

import org.codedefenders.singleplayer.automated.attacker.AiAttacker;
import org.codedefenders.singleplayer.automated.defender.AiDefender;
import org.codedefenders.singleplayer.AiPlayer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class Game extends AbstractGame {

	private int attackerId;
	private int defenderId;

	private int currentRound;
	private int finalRound;

	private Role activeRole;

	public Game(int classId, int userId, int maxRounds, Role role, Level level) {
		this.classId = classId;

		if (role.equals(Role.ATTACKER)) {
			attackerId = userId;
		} else {
			defenderId = userId;
		}

		this.currentRound = 1;
		this.finalRound = maxRounds;

		this.activeRole = Role.ATTACKER;
		this.state = State.CREATED;

		this.level = level;
		this.mode = Mode.DUEL;
	}

	public Game(int id, int attackerId, int defenderId, int classId, int currentRound, int finalRound, Role activeRole, State state, Level level, Mode mode) {
		this.id = id;
		this.attackerId = attackerId;
		this.defenderId = defenderId;
		this.classId = classId;
		this.currentRound = currentRound;
		this.finalRound = finalRound;
		this.activeRole = activeRole;
		this.state = state;
		this.level = level;
		this.mode = mode;
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

	public Role getActiveRole() {
		return activeRole;
	}

	// ATTACKER, DEFENDER, NEITHER
	public void setActiveRole(Role role) {
		activeRole = role;
	}

	public ArrayList<Mutant> getMutants() {
		return DatabaseAccess.getMutantsForGame(id);
	}

	public ArrayList<Mutant> getAliveMutants() {
		ArrayList<Mutant> aliveMutants = new ArrayList<>();
		for (Mutant m : getMutants()) {
			if (m.isAlive() && (m.getClassFile() != null)) {
				aliveMutants.add(m);
			}
		}
		return aliveMutants;
	}

	public ArrayList<Mutant> getKilledMutants() {
		ArrayList<Mutant> killedMutants = new ArrayList<>();
		for (Mutant m : getMutants()) {
			if (!m.isAlive() && (m.getClassFile() != null)) {
				killedMutants.add(m);
			}
		}
		return killedMutants;
	}

	public ArrayList<Mutant> getMutantsMarkedEquivalent() {
		ArrayList<Mutant> equivMutants = new ArrayList<>();
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

	public int getAttackerScore() {
		int totalScore = 0;

		for (Mutant m : getMutants())
			totalScore += m.getAttackerPoints();
		logger.debug("Attacker Score: " + totalScore);
		return totalScore;
	}

	public int getDefenderScore() {
		int totalScore = 0;

		for (Test t : getTests())
			totalScore += t.getDefenderPoints();

		for (Mutant m : getMutants())
			totalScore += m.getDefenderPoints();
		logger.debug("Defender Score: " + totalScore);
		return totalScore;
	}

	public void passPriority() {
		if (activeRole.equals(Role.ATTACKER)) {
			activeRole = Role.DEFENDER;
		} else
			activeRole = Role.ATTACKER;
	}

	public void endTurn() {
		if (activeRole.equals(Role.ATTACKER)) {
			activeRole = Role.DEFENDER;
		} else {
			activeRole = Role.ATTACKER;
			endRound();
		}
		update();
	}

	public void endRound() {
		if (currentRound < finalRound) {
			currentRound++;
		} else if ((currentRound == finalRound) && (state.equals(State.ACTIVE))) {
			state = State.FINISHED;
		}
	}

	public boolean addPlayer(int userId, Role role) {

		String sql = String.format("INSERT INTO players (Game_ID, User_ID, Points, Role) " +
				"VALUES (%d, %d, 0, '%s');", id, userId, role);
		if (runStatement(sql)) {
			if (role.equals(Role.ATTACKER))
				attackerId = userId;
			else
				defenderId = userId;
			return true;
		};
		return false;
	}

	@Override
	public boolean insert() {

		Connection conn = null;
		Statement stmt = null;
		String sql = String.format("INSERT INTO games (Class_ID, Creator_ID, FinalRound, Level, Mode, State) VALUES ('%d', '%d', '%d', '%s', '%s', '%s');", classId, (attackerId != 0) ? attackerId : defenderId, finalRound, level.name(), mode.name(), state.name());
		logger.info(sql);

		// Attempt to insert game info into database
		try {
			conn = DatabaseAccess.getConnection();

			stmt = conn.createStatement();

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

	@Override
	public boolean update() {

		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {
			conn = DatabaseAccess.getConnection();

			// Get all rows from the database which have the chosen username
			stmt = conn.createStatement();
			if (this.mode.equals(Mode.UTESTING))
				sql = String.format("UPDATE games SET CurrentRound='%d', FinalRound='%d', State='%s' WHERE ID='%d'",
						currentRound, finalRound, state.name(), id);
			else
				sql = String.format("UPDATE games SET CurrentRound='%d', FinalRound='%d', ActiveRole='%s', State='%s' WHERE ID='%d'",
						currentRound, finalRound, activeRole, state.name(), id);
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
