package org.codedefenders.duel;

import org.codedefenders.*;
import org.codedefenders.util.DatabaseAccess;

import static org.codedefenders.Mutant.Equivalence.PENDING_TEST;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DuelGame extends AbstractGame {

	private int attackerId;
	private int defenderId;

	private int currentRound;
	private int finalRound;

	private Role activeRole;

	public DuelGame(int classId, int userId, int maxRounds, Role role, GameLevel level) {
		this.classId = classId;

		if (role.equals(Role.ATTACKER)) {
			attackerId = userId;
		} else {
			defenderId = userId;
		}

		this.currentRound = 1;
		this.finalRound = maxRounds;

		this.activeRole = Role.ATTACKER;
		this.state = GameState.CREATED;

		this.level = level;
		this.mode = GameMode.DUEL;
	}

	public DuelGame(int id, int attackerId, int defenderId, int classId, int currentRound, int finalRound, Role activeRole, GameState state, GameLevel level, GameMode mode) {
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

	// TODO: Why is this different in the MultiplayerGame?
	public List<Mutant> getMutantsMarkedEquivalent() {
		List<Mutant> equivMutants = new ArrayList<>();
		for (Mutant m : getMutants()) {
			if (m.isAlive() && m.getEquivalent().equals(PENDING_TEST)) {
				equivMutants.add(m);
			}
		}
		return equivMutants;
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
		} else if ((currentRound == finalRound) && (state.equals(GameState.ACTIVE))) {
			state = GameState.FINISHED;
		}
	}

	public boolean addPlayer(int userId, Role role) {
		PreparedStatement stmt = null;
		Connection conn = null;
		boolean was_success = false;

		try {
			conn = DatabaseAccess.getConnection();
			stmt = conn.prepareStatement("INSERT INTO players (Game_ID, User_ID, Points, Role) " + "VALUES (?, ?, 0, ?);");
			stmt.setInt(1, id);
			stmt.setInt(2, userId);
			stmt.setString(3, role.toString());
			was_success = stmt.executeUpdate() >= 0;
	} catch (SQLException se) {
		logger.error("SQL exception caught", se);
	} catch (Exception e) {
		logger.error("Exception caught", e);
	} finally {
		DatabaseAccess.cleanup(conn, stmt);
	}

		if (was_success) {
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
		PreparedStatement stmt = null;

		// Attempt to insert game info into database
		try {
			conn = DatabaseAccess.getConnection();


			stmt=conn.prepareStatement("INSERT INTO games (Class_ID, Creator_ID, FinalRound, Level, Mode, State) VALUES (?, ?, ?, ?, ?, ?);");
			stmt.setInt(1, classId);
			stmt.setInt(2, (attackerId != 0) ? attackerId : defenderId);
			stmt.setInt(3, finalRound);
			stmt.setString(4, level.name());
			stmt.setString(5, mode.name());
			stmt.setString(6, state.name());

			logger.info(stmt.toString());
			stmt.execute();

			ResultSet rs = stmt.getGeneratedKeys();

			if (rs.next()) {
				id = rs.getInt(1);
				return true;
			}
		} catch (SQLException se) {
			logger.error("SQL exception caught", se);
		} catch (Exception e) {
			logger.error("Exception caught", e);
		} finally {
			DatabaseAccess.cleanup(conn, stmt);
		}

		return false;
	}

	@Override
	public boolean update() {
		if (this.mode.equals(GameMode.UTESTING))
			return DatabaseAccess.execute(String.format("UPDATE games SET CurrentRound='%d', FinalRound='%d', State='%s' WHERE ID='%d'",
					currentRound, finalRound, state.name(), id));
		else
			return DatabaseAccess.execute(String.format("UPDATE games SET CurrentRound='%d', FinalRound='%d', ActiveRole='%s', State='%s' WHERE ID='%d'",
					currentRound, finalRound, activeRole, state.name(), id));
	}
}
