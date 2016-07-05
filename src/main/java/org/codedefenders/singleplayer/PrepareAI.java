package org.codedefenders.singleplayer;

import org.codedefenders.Game;

public class PrepareAI {

	public PrepareAI(int classId) {
		Game dummyGame = new Game(classId, 1, 3, Game.Role.ATTACKER, Game.Level.EASY);
		dummyGame.insert();
		dummyGame.setDefenderId(1);
		dummyGame.setState(Game.State.ACTIVE);
		dummyGame.update();

		MajorMaker mMake = new MajorMaker(classId, dummyGame);
		mMake.createMutants();

		EvoSuiteMaker esMake = new EvoSuiteMaker(classId, dummyGame);
		esMake.makeSuite();

		//TODO: Find how many tests "kill" mutants.
		//Don't actually kill mutants to run all tests.
		//Perhaps rank mutants in how many tests kill them.
	}
}
