package org.codedefenders.singleplayer;

import org.codedefenders.Game;

/**
 * Created by midcode on 15/06/16.
 * Set up a singleplayer game.
 * Logic still handled by Game class.
 */

public class SingleplayerGame extends Game {

	public SingleplayerGame (int classId, int userId, int maxRounds, Role role, Level level) {
		super(classId, userId, maxRounds, role, level);

		//Set ai's role
		if(role.equals(Game.Role.ATTACKER)) {
			setDefenderId(1);
			ai = new AIDefender(this);
		} else {
			setAttackerId(1); //Potentially inefficient?
			ai = new AIAttacker(this);
		}
		setMode(Mode.SINGLE); //Set singleplayer mode.

		//TODO: Remove next line, forces difficulty to HARD.
		setLevel(Game.Level.HARD);
	}

	/**
	 * Make the first turn if AI is an attacker.
	 */
	public void tryFirstTurn() {
		if(ai.role.equals(Role.ATTACKER)) {
			ai.makeTurn();
		}
	}

}
