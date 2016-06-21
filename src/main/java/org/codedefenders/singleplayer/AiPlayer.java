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
			switch (game.getLevel()) {
				case EASY: turnEasy(); break;
				case MEDIUM: turnMedium(); break;
				case HARD: turnHard(); break;
				default: turnHard(); break;
			}

			game.endTurn();
		}
	}

	public void turnEasy() {
		//Override
	}
	public void turnMedium() {
		//Override
	}
	public void turnHard() {
		//Override
	}
}
