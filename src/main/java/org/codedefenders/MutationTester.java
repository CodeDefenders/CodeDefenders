package org.codedefenders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.util.ArrayList;

import static org.codedefenders.Mutant.Equivalence.ASSUMED_YES;
import static org.codedefenders.Mutant.Equivalence.PROVEN_NO;

// Class that handles compilation and testing by creating a Process with the relevant ant target
public class MutationTester {

	private static final Logger logger = LoggerFactory.getLogger(MutationTester.class);

	// RUN MUTATION TESTS: Runs all the mutation tests for a particular game, using all the alive mutants and all tests

	// Inputs: The ID of the game to run mutation tests for
	// Outputs: None

	public static void runTestOnAllMutants(ServletContext context, Game game, Test test, ArrayList<String> messages) {
		int killed = 0;
		ArrayList<Mutant> mutants = game.getAliveMutants();
		for (Mutant mutant : mutants) {
			killed += testVsMutant(context, test, mutant) ? 1 : 0;
		}
		if (killed == 0)
			if (mutants.size() == 0)
				messages.add("Test submitted and ready to kill mutant!");
			else
				messages.add("Your test did not kill any mutant, just yet.");
		else {
			if (killed == 1) {
				if (mutants.size() == 1)
					messages.add("Great, your test killed the last mutant!");
				else
					messages.add("Good job, your test killed a mutant!");
			} else {
				messages.add(String.format("Awesome! Your test killed %d mutants!", killed));
			}

		}
	}

	public static void runAllTestsOnMutant(ServletContext context, Game game, Mutant mutant, ArrayList<String> messages) {
		ArrayList<Test> tests = game.getExecutableTests();
		for (Test test : tests) {
			// If this mutant/test pairing hasnt been run before and the test might kill the mutant
			if (testVsMutant(context, test, mutant)) {
				messages.add(String.format("Test %d killed your mutant. Keep going!", test.getId()));
				return;
			}
		}
		if (tests.size() == 0)
			messages.add("Mutant submitted, may the force be with it.");
		else if (tests.size() <= 1)
			messages.add("Cool, your mutant is alive.");
		else
			messages.add(String.format("Awesome, your mutant survived %d existing tests!",tests.size()));
	}

	/**
	 * Returns {@code true} iff {@code test} kills {@code mutant}.
	 * @param context
	 * @param test
	 * @param mutant
	 * @return
	 */
	private static boolean testVsMutant(ServletContext context, Test test, Mutant mutant) {
		if (DatabaseAccess.getTargetExecutionForPair(test.getId(), mutant.getId()) == null) {
			// Run the test against the mutant and get the result
			TargetExecution executedTarget = AntRunner.testMutant(context, mutant, test);

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

	// RUN EQUIVALENCE TEST: Runs an equivalence test using an attacker supplied test and a mutant thought to be equivalent

	// Inputs: Attacker created test and a mutant to run it on
	// Outputs: None

	public static void runEquivalenceTest(ServletContext context, Test test, Mutant mutant) {
		logger.debug("Killing mutant in runEquivalenceTest.");
		System.out.println("Killing mutant in equivalenceTest.");
		// The test created is new and was made by the attacker (there is no need to check if the mutant/test pairing has been run already)

		// As a result of this test, either the test the attacker has written kills the mutant or doesnt.
		TargetExecution executedTarget = AntRunner.testMutant(context, mutant, test);

		if (executedTarget.status.equals("ERROR") || executedTarget.status.equals("FAIL")) {
			// If the test did NOT pass, the mutant was detected and is proven to be non-equivalent
			System.out.println("Mutant was killed, hence tagged not equivalent");
			mutant.setEquivalent(PROVEN_NO);
		} else {
			// If the test DID pass, the mutant went undetected and it is assumed to be equivalent.
			System.out.println("Test failed to kill the mutant, hence assumed equivalent");
			mutant.setEquivalent(ASSUMED_YES);
		}
		// Kill the mutant if it was killed by the test or if it's marked equivalent
		mutant.kill();
	}
}