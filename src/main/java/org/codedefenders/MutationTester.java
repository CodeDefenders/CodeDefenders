package org.codedefenders;

import static org.codedefenders.AntRunner.runAntTarget;
import static org.codedefenders.Constants.JAVA_CLASS_EXT;
import static org.codedefenders.Mutant.Equivalence.ASSUMED_YES;
import static org.codedefenders.Mutant.Equivalence.PROVEN_NO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

// Class that handles compilation and testing by creating a Process with the relevant ant target
public class MutationTester {

	private static final Logger logger = LoggerFactory.getLogger(MutationTester.class);

	// COMPILE MUTANT: Runs the related ant target that compiles a mutant

	// Inputs: Mutant object to be compiled
	// Outputs: ID of the resulting TargetExecution


	public static Mutant compileMutant(ServletContext context, File dir, String jFile, int gameID, GameClass classMutated) {
	//public static int compileMutant(ServletContext context, Mutant m2) {

		// Gets the classname for the mutant from the game it is in
		String[] resultArray = runAntTarget(context, "compile-mutant", dir.getAbsolutePath(), null, classMutated.getBaseName(), null);
		System.out.println("Compilation result:");
		System.out.println(Arrays.toString(resultArray));
		System.out.println("compiling mutant");

		Mutant newMutant = null;
		// If the input stream returned a 'successful build' message, the mutant compiled correctly
		if (resultArray[0].toLowerCase().contains("build successful")) {
			// Create and insert a new target execution recording successful compile, with no message to report, and return its ID
			// Locate .class file
			final String compiledClassName = classMutated.getBaseName() + JAVA_CLASS_EXT;
			LinkedList<File> matchingFiles = (LinkedList) FileUtils.listFiles(dir, FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter());
			assert (! matchingFiles.isEmpty()); // if compilation was successful, .class file must exist
			String cFile = matchingFiles.get(0).getAbsolutePath();
			newMutant = new Mutant(gameID, jFile, cFile);
			newMutant.insert();
			TargetExecution newExec = new TargetExecution(0, newMutant.getId(), TargetExecution.Target.COMPILE_MUTANT, "SUCCESS", null);
			newExec.insert();
		} else {
			// The mutant failed to compile
			// New target execution recording failed compile, providing the return messages from the ant javac task
			String message = resultArray[0].substring(resultArray[0].indexOf("[javac]"));
			newMutant = new Mutant(gameID, jFile, null);
			newMutant.insert();
			TargetExecution newExec = new TargetExecution(0, newMutant.getId(), TargetExecution.Target.COMPILE_MUTANT, "FAIL", message);
			newExec.insert();
		}
		return newMutant;
	}

	// COMPILE TEST: Runs the related ant target that compiles a test

	// Inputs: Test object to be compiled
	// Outputs: ID of the resulting TargetExecution

	public static Test compileTest(ServletContext context, File dir, String jFile, int gameID, GameClass classUnderTest) {
	//public static int compileTest(ServletContext context, Test t) {

		String className = classUnderTest.getName();
		String[] resultArray = runAntTarget(context, "compile-test", null, dir.getAbsolutePath(), className, null);

		// If the input stream returned a 'successful build' message, the test compiled correctly
		if (resultArray[0].toLowerCase().contains("build successful")) {
			// Create and insert a new target execution recording successful compile, with no message to report, and return its ID
			// Locate .class file
			final String compiledClassName = FilenameUtils.getBaseName(jFile) + JAVA_CLASS_EXT;
			LinkedList<File> matchingFiles = (LinkedList) FileUtils.listFiles(dir, FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter());
			assert (! matchingFiles.isEmpty()); // if compilation was successful, .class file must exist
			String cFile = matchingFiles.get(0).getAbsolutePath();
			Test newTest = new Test(gameID, jFile, cFile);
			newTest.insert();
			TargetExecution newExec = new TargetExecution(newTest.getId(), 0, TargetExecution.Target.COMPILE_TEST, "SUCCESS", null);
			newExec.insert();
			return newTest;
		} else {
			// The test failed to compile
			// New target execution recording failed compile, providing the return messages from the ant javac task
			String message = resultArray[0].substring(resultArray[0].indexOf("[javac]"));
			Test newTest = new Test(gameID, jFile, null);
			newTest.insert();
			TargetExecution newExec = new TargetExecution(newTest.getId(), 0, TargetExecution.Target.COMPILE_TEST, "FAIL", message);
			newExec.insert();
			return newTest;
		}
	}

	// TEST ORIGINAL: Runs the related ant target that ensures a test passes against the original code

	// Inputs: Test object to be run against the original class
	// Outputs: ID of the resulting TargetExecution
	public static int testOriginal(ServletContext context, File dir, Test t) {

		String className = DatabaseAccess.getGameForKey("Game_ID", t.getGameId()).getClassName();
		String[] resultArray = runAntTarget(context, "test-original", null, dir.getAbsolutePath(), className, t.getFullyQualifiedClassName());

		// If the test doesn't return failure
		if (resultArray[0].toLowerCase().contains("failures: 0")) {
			// If the test doesn't return error
			if (resultArray[0].toLowerCase().contains("errors: 0")) {
				// New Target execution recording successful test against original
				TargetExecution newExec = new TargetExecution(t.getId(), 0, TargetExecution.Target.TEST_ORIGINAL, "SUCCESS", null);
				newExec.insert();
				return newExec.id;
			} else {
				// New target execution recording failed test against original due to error
				// Not sure on what circumstances cause a junit error, return all streams
				String message = resultArray[0] + " " + resultArray[1] + " " + resultArray[2];
				TargetExecution newExec = new TargetExecution(t.getId(), 0, TargetExecution.Target.TEST_ORIGINAL, "ERROR", message);
				newExec.insert();
				return newExec.id;
			}
		} else {
			// New target execution record failed test against original as it isn't valid
			String message = "Test failed on the original class under test.";
			TargetExecution newExec = new TargetExecution(t.getId(), 0, TargetExecution.Target.TEST_ORIGINAL, "FAIL", message);
			newExec.insert();
			return newExec.id;
		}
	}

	// TEST MUTANT: Runs the related ant target that runs a specified test for a specified mutant

	// Inputs: A test object and a mutant to run it on
	// Outputs: ID of the resulting TargetExecution

	public static TargetExecution testMutant(ServletContext context, Mutant m, Test t) {
		logger.debug("Running test {} on mutant {}", t.getId(), m.getId());
		System.out.println("Running test " + t.getId() + " on mutant " + m.getId());
		String className = DatabaseAccess.getGameForKey("Game_ID", m.getGameId()).getClassName();
		String[] resultArray = runAntTarget(context, "test-mutant", m.getFolder(), t.getFolder(), className, t.getFullyQualifiedClassName());

		TargetExecution newExec = null;

		if (resultArray[0].toLowerCase().contains("failures: 0")) {
			// If the test doesn't return failure
			if (resultArray[0].toLowerCase().contains("errors: 0")) {
				// If the test doesn't return any errors
				// The test succeeded and a Target Execution for the mutant/test pairing is recorded. This means the test failed to detect the mutant
				newExec = new TargetExecution(t.getId(), m.getId(), TargetExecution.Target.TEST_MUTANT, "SUCCESS", null);
			} else {
				// New target execution recording failed test against mutant due to error
				// Not sure on what circumstances cause a junit error, return all streams
				String message = resultArray[0] + " " + resultArray[1] + " " + resultArray[2];
				newExec = new TargetExecution(t.getId(), m.getId(), TargetExecution.Target.TEST_MUTANT, "ERROR", message);
			}
		} else {
			// The test failed and a Target Execution for the mutant/test pairing is recorded. The test detected the mutant.
			newExec = new TargetExecution(t.getId(), m.getId(), TargetExecution.Target.TEST_MUTANT, "FAIL", null);
		}
		newExec.insert();
		return newExec;
	}

	// RUN MUTATION TESTS: Runs all the mutation tests for a particular game, using all the alive mutants and all tests

	// Inputs: The ID of the game to run mutation tests for
	// Outputs: None

	public static void runTestOnAllMutants(ServletContext context, Game game, Test test, ArrayList<String> messages) {
		int killed = 0;
		ArrayList<Mutant> mutants = game.getAliveMutants();
		for (Mutant mutant : mutants) {
			killed += testVsMutant(context, test, mutant, messages) ? 1 : 0;
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
					messages.add("Good job, your test killed a mutants!");
			} else {
				messages.add(String.format("Awesome! Your test killed \"%d mutants!", killed));
			}

		}
	}

	public static void runAllTestsOnMutant(ServletContext context, Game game, Mutant mutant, ArrayList<String> messages) {
		int killing = 0;
		ArrayList<Test> tests = game.getTests();
		for (Test test : tests) {
			if (test.isValid())
				// If this mutant/test pairing hasnt been run before and the test might kill the mutant
				killing += testVsMutant(context, test, mutant, messages) ? 1 : 0;
		}
		if (killing == 0) {
			if (tests.size() == 0)
				messages.add("Mutant submitted, may the force be with it.");
			else if (tests.size() == 1)
				messages.add("Cool, your mutant is alive.");
			else
				messages.add(String.format("Awesome, your mutant survived %d existing tests!",tests.size()));
		} else {
			String nTests = killing == 1 ? "An existing test" : String.format("%d existing tests", killing);
			messages.add(String.format("%s killed your mutant. Keep going!", nTests));
		}

	}

	public static boolean testVsMutant(ServletContext context, Test test, Mutant mutant, ArrayList<String> messages) {
		if (DatabaseAccess.getTargetExecutionForPair(test.getId(), mutant.getId()) == null) {
			// Run the test against the mutant and get the result
			TargetExecution executedTarget = testMutant(context, mutant, test);

			// If the test did NOT pass, the mutant was detected and should be killed.
			if (executedTarget.status.equals("FAIL")) {
				System.out.println(String.format("TEST %d KILLED MUTANT %d", test.getId(), mutant.getId()));
				mutant.kill();
				mutant.update();
				test.killMutant();
				test.update();
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
		TargetExecution executedTarget = testMutant(context, mutant, test);

		if (executedTarget.status.equals("ERROR")) {
			System.out.println("Error executing test on mutant.");
			return;
		} else if (executedTarget.status.equals("FAIL")) {
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
		mutant.update();
	}
}