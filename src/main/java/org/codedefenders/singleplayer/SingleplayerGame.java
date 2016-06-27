package org.codedefenders.singleplayer;

import org.codedefenders.Game;

/**
 * @author Ben Clegg
 * Set up a singleplayer game.
 * Logic still handled by Game class.
 */

public class SingleplayerGame extends Game {

	public SingleplayerGame (int classId, int userId, int maxRounds, Role role, Level level) {
		super(classId, userId, maxRounds, role, level);

		//Set ai's role
		if(role.equals(Game.Role.ATTACKER)) {
			setDefenderId(1);
			ai = new AiDefender(this);
		} else {
			setAttackerId(1); //Potentially inefficient?
			ai = new AiAttacker(this);
		}
		setMode(Mode.SINGLE); //Set singleplayer mode.
	}

	/**
	 * Make the first turn if AI is an attacker.
	 */
	public void tryFirstTurn() {
		if(ai.role.equals(Role.ATTACKER)) {
			//If attacker, add mutant and advance turn.
			ai.makeTurn();
		} else {
			//Defender adds tests without advancing turn.
			ai.tryTurn();
		}
	}

}
