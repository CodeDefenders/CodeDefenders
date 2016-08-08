package org.codedefenders.singleplayer;

import org.codedefenders.*;
import org.codedefenders.singleplayer.automated.attacker.AiAttacker;
import org.codedefenders.singleplayer.automated.attacker.MajorMaker;
import org.codedefenders.singleplayer.automated.defender.AiDefender;
import org.codedefenders.singleplayer.automated.defender.EvoSuiteMaker;

import java.util.ArrayList;

public class PrepareAI {

	public PrepareAI() {

	}

	public static boolean createTestsAndMutants(int classId) {

		AIDummyGame dummyGame = new AIDummyGame(classId);
		dummyGame.insert();
		dummyGame.addPlayer(AiAttacker.ID, Role.ATTACKER);
		dummyGame.addPlayer(AiDefender.ID, Role.DEFENDER);

		GameClass cut = DatabaseAccess.getClassForKey("Class_ID", classId);

		EvoSuiteMaker esMake = new EvoSuiteMaker(classId, dummyGame);
		esMake.makeSuite();
		if (esMake.addTimeoutsToSuite(4000)) {
			//Compile the modified suite.
			//TODO: Correct return value for compiling suite.
			if (!AntRunner.compileGenTestSuite(cut)) {
				return false; //Failed
			}
		} else {
			return false; //Failed
		}

		//TODO: Add correct check that everything succeeds.

		MajorMaker mMake = new MajorMaker(classId, dummyGame);
		mMake.createMutants();

		ArrayList<Test> tests = dummyGame.getTests();
		ArrayList<Mutant> mutants = dummyGame.getMutants();

		for (Test t : tests) {
			for (Mutant m : mutants) {
				//Find if mutant killed by test.
				if(AntRunner.testKillsMutant(m, t)) {
					m.incrementTimesKilledAi();
					t.incrementAiMutantsKilled();
				}
			}
		}

		//Store kill counts to SQL.
		for (Test t : tests) {
			t.update();
		}
		for (Mutant m: mutants) {
			m.update();
		}

		//Create XML files.
		esMake.createTestIndex();
		mMake.createMutantIndex();

		return true; //Succeeded
	}

	/**
	 * Select an index of an arraylist, with a bias to earlier or later values.
	 * @param length Number of indexes in the arraylist.
	 * @param bias The bias power to use. >1 biases towards earlier indexes, <1 biases towards later indexes.
	 * @return the resulting index.
	 */
	public static int biasedSelection(int length, double bias) {

		//Generate a random number biased towards smaller or larger values.
		double r = Math.pow(Math.random(), bias);

		return (int) Math.floor(r * length);
	}

	public static boolean isPrepared(GameClass cut) {
		//TODO: Use cleaner check for prepared class.
		if(DatabaseAccess.gameWithUserExistsForClass(1, cut.getId())) {
			return true;
		}
		return false;
	}
}
