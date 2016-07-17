package org.codedefenders;

import com.sun.org.apache.xpath.internal.operations.Mult;
import org.codedefenders.multiplayer.LineCoverage;
import org.codedefenders.multiplayer.MultiplayerGame;
import org.codedefenders.scoring.Scorer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.util.ArrayList;

import static org.codedefenders.Constants.*;
import static org.codedefenders.Mutant.Equivalence.ASSUMED_YES;
import static org.codedefenders.Mutant.Equivalence.PROVEN_NO;

// Class that handles compilation and testing by creating a Process with the relevant ant target
public class MutationTester {

	private static final Logger logger = LoggerFactory.getLogger(MutationTester.class);

	// RUN MUTATION TESTS: Runs all the mutation tests for a particular game, using all the alive mutants and all tests

	// Inputs: The ID of the game to run mutation tests for
	// Outputs: None

	public static void runTestOnAllMutants(Game game, Test test, ArrayList<String> messages) {
		int killed = 0;
		ArrayList<Mutant> mutants = game.getAliveMutants();
		for (Mutant mutant : mutants) {
			killed += testVsMutant(test, mutant) ? 1 : 0;
		}
		if (killed == 0)
			if (mutants.size() == 0)
				messages.add(TEST_SUBMITTED_MESSAGE);
			else
				messages.add(TEST_KILLED_ZERO_MESSAGE);
		else {
			if (killed == 1) {
				if (mutants.size() == 1)
					messages.add(TEST_KILLED_LAST_MESSAGE);
				else
					messages.add(TEST_KILLED_ONE_MESSAGE);
			} else {
				messages.add(String.format(TEST_KILLED_N_MESSAGE, killed));
			}

		}
		// coverage information
		LineCoverage lc = AntRunner.getLinesCovered(test, game.getCUT());
		test.setLineCoverage(lc);
		test.update();
	}

	public static void runTestOnAllMultiplayerMutants(MultiplayerGame game, Test test, ArrayList<String> messages) {
		int killed = 0;
		ArrayList<Mutant> mutants = game.getAliveMutants();
		mutants.addAll(game.getMutantsMarkedEquivalent());
		ArrayList<Mutant> killedMutants = new ArrayList<Mutant>();
		for (Mutant mutant : mutants) {
			boolean k = testVsMultiplayerMutant(game, test, mutant);
			if (k){
				killed++;
				killedMutants.add(mutant);
			}
		}

		ArrayList<Test> tests = new ArrayList<Test>();
		tests.add(test);

		for (Mutant mm : mutants){
			if (mm.isAlive()){
				mm.setScore(Scorer.score(game, mm, tests));

				mm.update();
			}
		}

		LineCoverage lc = AntRunner.getLinesCovered(test, game.getCUT());
		test.setLineCoverage(lc);

		test.setScore(Scorer.score(game, test, killedMutants));

		test.update();

		if (killed == 0)
			if (mutants.size() == 0)
				messages.add(TEST_SUBMITTED_MESSAGE);
			else
				messages.add(TEST_KILLED_ZERO_MESSAGE);
		else {
			if (killed == 1) {
				if (mutants.size() == 1)
					messages.add(TEST_KILLED_LAST_MESSAGE);
				else
					messages.add(TEST_KILLED_ONE_MESSAGE);
			} else {
				messages.add(String.format(TEST_KILLED_N_MESSAGE, killed));
			}

		}
	}

	public static void runAllTestsOnMutant(Game game, Mutant mutant, ArrayList<String> messages) {
		ArrayList<Test> tests = game.getExecutableTests();
		for (Test test : tests) {
			// If this mutant/test pairing hasnt been run before and the test might kill the mutant
			if (testVsMutant(test, mutant)) {
				messages.add(String.format(MUTANT_KILLED_BY_TEST_MESSAGE, test.getId()));
				return;
			}
		}
		if (tests.size() == 0)
			messages.add(MUTANT_SUBMITTED_MESSAGE);
		else if (tests.size() <= 1)
			messages.add(MUTANT_ALIVE_1_MESSAGE);
		else
			messages.add(String.format(MUTANT_ALIVE_N_MESSAGE,tests.size()));
	}

	public static void runAllTestsOnMultiplayerMutant(MultiplayerGame game, Mutant mutant, ArrayList<String> messages) {
		ArrayList<Test> tests = game.getExecutableTests();
		for (Test test : tests) {
			// If this mutant/test pairing hasnt been run before and the test might kill the mutant
			if (testVsMultiplayerMutant(game, test, mutant)) {
				messages.add(String.format(MUTANT_KILLED_BY_TEST_MESSAGE, test.getId()));
				ArrayList<Mutant> mlist = new ArrayList<Mutant>();
				mlist.add(mutant);
				test.setScore(Scorer.score(game, test, mlist));
				return;
			}
		}
		if (tests.size() == 0)
			messages.add(MUTANT_SUBMITTED_MESSAGE);
		else if (tests.size() <= 1)
			messages.add(MUTANT_ALIVE_1_MESSAGE);
		else
			messages.add(String.format(MUTANT_ALIVE_N_MESSAGE,tests.size()));
		ArrayList<Test> missedTests = new ArrayList<Test>();

		for (Test t : tests){
			for (int lm : mutant.getLines()){
				boolean found = false;
				for (int lc : t.getLineCoverage().getLinesCovered()){
					if (lc == lm){
						found = true;
						missedTests.add(t);
					}
				}
				if (found){
					break;
				}
			}
		}
		mutant.setScore(Scorer.score(game, mutant, missedTests));
		mutant.update();
	}

	/**
	 * Returns {@code true} iff {@code test} kills {@code mutant}.
	 * @param test
	 * @param mutant
	 * @return
	 */
	private static boolean testVsMutant(Test test, Mutant mutant) {
		if (DatabaseAccess.getTargetExecutionForPair(test.getId(), mutant.getId()) == null) {
			// Run the test against the mutant and get the result
			TargetExecution executedTarget = AntRunner.testMutant(mutant, test);

			// If the test did NOT pass, the mutant was detected and should be killed.
			if (executedTarget.status.equals("FAIL") || executedTarget.status.equals("ERROR")) {
				System.out.println(String.format("Test %d kills Mutant %d", test.getId(), mutant.getId()));
				mutant.kill();
				test.killMutant();
				return true;
			}
		} else
			System.err.println(String.format("No execution result found for (m: %d,t: %d)", mutant.getId(), test.getId()));
		return false;
	}

	private static boolean testVsMultiplayerMutant(MultiplayerGame g, Test test, Mutant mutant) {
		if (DatabaseAccess.getTargetExecutionForPair(test.getId(), mutant.getId()) == null) {
			// Run the test against the mutant and get the result
			TargetExecution executedTarget = AntRunner.testMutant(mutant, test);

			// If the test did NOT pass, the mutant was detected and should be killed.
			if (executedTarget.status.equals("FAIL") || executedTarget.status.equals("ERROR")) {
				System.out.println(String.format("Test %d kills Mutant %d", test.getId(), mutant.getId()));
				mutant.kill();
				ArrayList<Mutant> mlist = new ArrayList<Mutant>();

				mlist.add(mutant);
				test.killMutant();
				return true;
			}
		} else
			System.err.println(String.format("No execution result found for (m: %d,t: %d)", mutant.getId(), test.getId()));
		return false;
	}

	// RUN EQUIVALENCE TEST: Runs an equivalence test using an attacker supplied test and a mutant thought to be equivalent

	// Inputs: Attacker created test and a mutant to run it on
	// Outputs: None

	public static void runEquivalenceTest(Test test, Mutant mutant) {
		logger.info("Running equivalence test.");
		// The test created is new and was made by the attacker (there is no need to check if the mutant/test pairing has been run already)

		// As a result of this test, either the test the attacker has written kills the mutant or doesnt.
		TargetExecution executedTarget = AntRunner.testMutant(mutant, test);

		if (executedTarget.status.equals("ERROR") || executedTarget.status.equals("FAIL")) {
			// If the test did NOT pass, the mutant was detected and is proven to be non-equivalent
			mutant.setEquivalent(PROVEN_NO);
		} else {
			// If the test DID pass, the mutant went undetected and it is assumed to be equivalent.
			mutant.setEquivalent(ASSUMED_YES);
			mutant.setScore(0);
		}
		// Kill the mutant if it was killed by the test or if it's marked equivalent
		mutant.kill();
	}

}