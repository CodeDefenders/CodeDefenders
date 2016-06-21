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
			gm.submitAiTestFullSuite(game);
		}
		//Do nothing else, test is automatically re-run on new mutants by GameManager.
		//TODO: Add equivalence check.
		//Call equivalent only if test suite passes on mutant.
	}

	public void turnMedium() {
		//Choose all tests which cover modified line(s)?
		//Perhaps just 1 or 2?
		//Perhaps higher chance of equivalence call? May happen due to weaker testing.
	}

	public void turnEasy() {
		//Choose a random test which covers the modified line(s)?
		//Perhaps just a random test?
		//Perhaps higher chance of equivalence call? May happen due to weaker testing.
	}
}
