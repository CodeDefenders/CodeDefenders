/**
 * Copyright (C) 2016-2018 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.game.duel;

import org.codedefenders.database.DB;
import org.codedefenders.database.DatabaseValue;
import org.codedefenders.database.GameDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;

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
		if (GameDAO.addPlayerToGame(id, userId, role)) {
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
