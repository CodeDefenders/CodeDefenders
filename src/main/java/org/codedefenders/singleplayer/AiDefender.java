package org.codedefenders.singleplayer;

import org.codedefenders.Game;
import org.codedefenders.GameManager;
import org.codedefenders.MutationTester;

/**
 * Created by midcode on 20/06/16.
 */
public class AiDefender extends AiPlayer {

	public AiDefender(Game g) {
		super(g);
		role = Game.Role.DEFENDER;
	}
	public void turnHard() {
		//Run all generated tests for class.
		if(game.getTests().isEmpty()) {
			//Add test suite to game if it isn't present.
			GameManager gm = new GameManager();
			gm.submitAiTestFullSuite(game); //TODO: Find solution to passing ServletContext.
		}
		//Do nothing else, test is automatically re-run on new mutants by GameManager.
		//TODO: Add equivalence check.
	}
}
