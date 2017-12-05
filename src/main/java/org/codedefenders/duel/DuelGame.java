package org.codedefenders.duel;

import static org.codedefenders.Mutant.Equivalence.PENDING_TEST;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.codedefenders.AbstractGame;
import org.codedefenders.GameLevel;
import org.codedefenders.GameMode;
import org.codedefenders.GameState;
import org.codedefenders.Mutant;
import org.codedefenders.Role;
import org.codedefenders.Test;
import org.codedefenders.util.DB;
import org.codedefenders.util.DatabaseValue;

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
		Connection conn = DB.getConnection();
		String query = "INSERT INTO players (Game_ID, User_ID, Points, Role) VALUES (?, ?, 0, ?);";
		DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(id),
				DB.getDBV(userId),
				DB.getDBV(role.toString())};
		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
		if (DB.executeUpdate(stmt, conn)) {
			if (role.equals(Role.ATTACKER))
				attackerId = userId;
			else
				defenderId = userId;
			return true;
		}
		return false;
	}

	@Override
	public boolean insert() {
		// Attempt to insert game info into database
		Connection conn = DB.getConnection();
		String query = "INSERT INTO games (Class_ID, Creator_ID, FinalRound, Level, Mode, State) VALUES (?, ?, ?, ?, ?, ?);";
		DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(classId),
				DB.getDBV((attackerId != 0) ? attackerId : defenderId),
				DB.getDBV(finalRound),
				DB.getDBV(level.name()),
				DB.getDBV(mode.name()),
				DB.getDBV(state.name())};
		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
		logger.info(stmt.toString());
		int res = DB.executeUpdateGetKeys(stmt, conn);
		if (res > -1) {
			id = res;
			return true;
		}
		return false;
	}

	@Override
	public boolean update() {
		Connection conn = DB.getConnection();
		String query = null;
		DatabaseValue[] valueList = null;
		if (this.mode.equals(GameMode.UTESTING)) {
			query = "UPDATE games SET CurrentRound=?, FinalRound=?, State=? WHERE ID=?";
			valueList = new DatabaseValue[]{
					DB.getDBV(currentRound), DB.getDBV(finalRound), DB.getDBV(state.name()), DB.getDBV(id)};
		} else {
			query = "UPDATE games SET CurrentRound=?, FinalRound=?, ActiveRole=?, State=? WHERE ID=?";
			valueList = new DatabaseValue[]{
					DB.getDBV(currentRound), DB.getDBV(finalRound), DB.getDBV(activeRole.toString()), DB.getDBV(state.name()), DB.getDBV(id)};
		}
		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
		return DB.executeUpdate(stmt, conn);
	}
}
