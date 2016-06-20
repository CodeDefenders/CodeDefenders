package org.codedefenders.singleplayer;

import org.codedefenders.Game;

/**
 * Created by midcode on 20/06/16.
 */
public class AiAttacker extends AiPlayer {

	public AiAttacker(Game g) {
		super(g);
		role = Game.Role.ATTACKER;
	}

	public void turnHard() {
		//Use only one mutant per round.
		//Perhaps modify the line with the least test coverage?
	}

	public void turnMedium() {
		//Use one randomly selected mutant per round.
	}

	public void turnEasy() {
		//Use multiple mutants per round, up to maximum amount.
		int mutantsTotal = 1; //TODO: Find actual mutants total.
		double maxNumMutants = Math.floor(mutantsTotal / game.getFinalRound());
	}
}
