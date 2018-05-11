package org.codedefenders.game.singleplayer;

import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.duel.DuelGame;
import org.codedefenders.game.Role;
import org.codedefenders.game.singleplayer.automated.attacker.AiAttacker;
import org.codedefenders.game.singleplayer.automated.defender.AiDefender;

/**
 * @author Ben Clegg
 * Set up a singleplayer game.
 * Logic still handled by DuelGame class.
 */

public class SinglePlayerGame extends DuelGame {

	protected AiPlayer ai = null;

	public SinglePlayerGame(int classId, int userId, int maxRounds, Role role, GameLevel level) {
		super(classId, userId, maxRounds, role, level);

		//Set ai's role
		if(role.equals(Role.ATTACKER)) {
			setDefenderId(AiDefender.ID);
			ai = new AiDefender(this);
		} else {
			setAttackerId(AiAttacker.ID);
			ai = new AiAttacker(this);
		}
		setMode(GameMode.SINGLE); //Set singleplayer mode.
		setState(GameState.ACTIVE);
	}

	public AiPlayer getAi() {
		return ai;
	}

	public SinglePlayerGame(int id, int attackerId, int defenderId, int classId, int currentRound, int finalRound, Role activeRole, GameState state, GameLevel level, GameMode mode) {
		super(id, attackerId, defenderId, classId, currentRound, finalRound, activeRole, state, level, mode);
		//Set ai's role
		if(defenderId == AiDefender.ID) {
			ai = new AiDefender(this);
		} else {
			ai = new AiAttacker(this);
		}
	}

	/**
	 * Make the first turn if AI is an attacker.
	 */
	public void tryFirstTurn() {
		if(ai.role.equals(Role.ATTACKER)) {
			//If attacker, add mutant and advance turn.
			ai.makeTurn();
		}
	}

	@Override
	public void endTurn() {
		if (getActiveRole().equals(Role.ATTACKER)) {
			setActiveRole(Role.DEFENDER);
		} else {
			setActiveRole(Role.ATTACKER);
			endRound();
		}

		//Make the ai's turn
		//ai.makeTurn();
		update();
	}
}
