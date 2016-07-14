package org.codedefenders.singleplayer;

import org.codedefenders.Game;
import org.codedefenders.Role;

/**
 * @author Ben Clegg
 * Base class for AI players.
 */
public class AiPlayer {

	public enum GenerationMethod {
		RANDOM, //Randomly select mutant.
		COVERAGE, //Select random mutant by least covered lines.
		FIRST //Choose the first mutant, for debugging.
	}

	protected Game game;
	protected Role role;

	public AiPlayer(Game g) {
		game = g;
	}

	/**
	 * Make the AI's turn if it is its turn.
	 */
	public void makeTurn() {
		if (game.getActiveRole().equals(role)) {
			for (int i = 0; i < 3; i++) {
				if (tryTurn()) { break; }
			}
			game.endTurn();
		}
	}

	public boolean tryTurn() {
		boolean b = false;
		switch (game.getLevel()) {
			case EASY: b = turnEasy(); break;
			case HARD: b = turnHard(); break;
			default: b = turnHard(); break;
		}
		return b;
	}

	public boolean turnEasy() {
		//Override
		return true;
	}
	public boolean turnHard() {
		//Override
		return true;
	}
}
