package org.codedefenders.singleplayer;

import org.codedefenders.Game;

/**
 * Created by midcode on 20/06/16.
 */
public class AIDefender extends AIPlayer{

	public AIDefender(Game g) {
		super(g);
		role = Game.Role.DEFENDER;
	}
	public void turnHard() {
		//Run all generated tests for class.

		//Print something to show this block executes
		System.out.println("IT LIVES");
	}
}
