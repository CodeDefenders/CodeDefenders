package org.codedefenders.singleplayer;

import org.codedefenders.Game;

/**
 * Created by midcode on 20/06/16.
 */
public class AIPlayer {

	private Game game;

	protected Game.Role role;

	public AIPlayer(Game g) {
		game = g;
	}

	public void makeTurn() {
		switch (game.getLevel()) {
			case EASY: turnEasy(); break;
			case MEDIUM: turnMedium(); break;
			case HARD: turnHard(); break;
			default: turnHard(); break;
		}

		game.endTurn();
	}

	public void turnEasy() {

	}
	public void turnMedium() {

	}
	public void turnHard() {

	}
}
