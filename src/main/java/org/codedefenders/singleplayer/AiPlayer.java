package org.codedefenders.singleplayer;

import org.codedefenders.Game;

/**
 * @author Ben Clegg
 * Base class for AI players.
 */
public class AiPlayer {

	protected Game game;
	protected Game.Role role;

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
			case MEDIUM: b = turnMedium(); break;
			case HARD: b = turnHard(); break;
			default: b = turnHard(); break;
		}
		return b;
	}

	public boolean turnEasy() {
		//Override
		return true;
	}
	public boolean turnMedium() {
		//Override
		return true;
	}
	public boolean turnHard() {
		//Override
		return true;
	}
}
