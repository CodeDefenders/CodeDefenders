package org.gammut;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;
import static org.gammut.Constants.Equivalence.*;

// Class that handles compilation and testing by creating a Process with the relevant ant target
public class MutationTester {

	private static final Logger logger = LoggerFactory.getLogger(MutationTester.class);

	// COMPILE MUTANT: Runs the related ant target that compiles a mutant

	// Inputs: Mutant object to be compiled
	// Outputs: ID of the resulting TargetExecution

	public static int compileMutant(ServletContext context, Mutant m) {

		// Gets the classname for the mutant from the game it is in
		String className = DatabaseAccess.getGameForKey("Game_ID", m.getGameId()).getClassName();
		String[] resultArray = runAntTarget(context, "compile-mutant", m.getFolder(), null, className);
		System.out.println("Compilation result:");
		System.out.println(Arrays.toString(resultArray));
		System.out.println("compiling mutant");

		// If the input stream returned a 'successful build' message, the mutant compiled correctly
		if (resultArray[0].toLowerCase().contains("build successful")) {
			// Create and insert a new target execution recording successful compile, with no message to report, and return its ID
			TargetExecution newExec = new TargetExecution(0, m.getId(), "COMPILE_MUTANT", "SUCCESS", null);
			System.out.println("about to insert");
			newExec.insert();
			return newExec.id;
		}

		// Otherwise the mutant failed to compile
		else {
			// New target execution recording failed compile, providing the return messages from the ant javac task
			String message = resultArray[0].substring(resultArray[0].indexOf("[javac]"));
			TargetExecution newExec = new TargetExecution(0, m.getId(), "COMPILE_MUTANT", "FAIL", message);
			System.out.println("about to insert");
			newExec.insert();
			return newExec.id;
		}
	}

	// COMPILE TEST: Runs the related ant target that compiles a test

	// Inputs: Test object to be compiled
	// Outputs: ID of the resulting TargetExecution

	public static int compileTest(ServletContext context, Test t) {

		String className = DatabaseAccess.getGameForKey("Game_ID", t.getGameId()).getClassName();
		String[] resultArray = runAntTarget(context, "compile-test", null, t.getFolder(), className);

		// If the input stream returned a 'successful build' message, the test compiled correctly
		if (resultArray[0].toLowerCase().contains("build successful")) {
			// Create and insert a new target execution recording successful compile, with no message to report, and return its ID
			TargetExecution newExec = new TargetExecution(t.getId(), 0, "COMPILE_TEST", "SUCCESS", null);
			newExec.insert();
			return newExec.id;
		}

		// Otherwise the test failed to compile
		else {
			// New target execution recording failed compile, providing the return messages from the ant javac task
			String message = resultArray[0].substring(resultArray[0].indexOf("[javac]"));
			TargetExecution newExec = new TargetExecution(t.getId(), 0, "COMPILE_TEST", "FAIL", message);
			newExec.insert();
			return newExec.id;
		}
	}

	// TEST ORIGINAL: Runs the related ant target that ensures a test passes against the original code

	// Inputs: Test object to be run against the original class
	// Outputs: ID of the resulting TargetExecution
	public static int testOriginal(ServletContext context, Test t) {

		String className = DatabaseAccess.getGameForKey("Game_ID", t.getGameId()).getClassName();
		String[] resultArray = runAntTarget(context, "test-original", null, t.getFolder(), className);

		// If the test doesn't return failure
		if (resultArray[0].toLowerCase().contains("failures: 0")) {
			// If the test doesn't return error
			if (resultArray[0].toLowerCase().contains("errors: 0")) {
				// New Target execution recording successful test against original
				TargetExecution newExec = new TargetExecution(t.getId(), 0, "TEST_ORIGINAL", "SUCCESS", null);
				newExec.insert();
				return newExec.id;
			} else {
				// New target execution recording failed test against original due to error
				// Not sure on what circumstances cause a junit error, return all streams
				String message = resultArray[0] + " " + resultArray[1] + " " + resultArray[2];
				TargetExecution newExec = new TargetExecution(t.getId(), 0, "TEST_ORIGINAL", "ERROR", message);
				newExec.insert();
				return newExec.id;
			}
		}

		// Otherwise the test failed
		else {
			// New target execution record failed test against original as it isn't valid
			String message = "Your test is invalid as it couldnt pass against the original code";
			TargetExecution newExec = new TargetExecution(t.getId(), 0, "TEST_ORIGINAL", "FAIL", message);
			newExec.insert();
			return newExec.id;
		}
	}

	// TEST MUTANT: Runs the related ant target that runs a specified test for a specified mutant

	// Inputs: A test object and a mutant to run it on
	// Outputs: ID of the resulting TargetExecution

	public static int testMutant(ServletContext context, Mutant m, Test t) {
		logger.debug("Running test {} on mutant {}", t.getId(), m.getId());
		System.out.println("Running test " + t.getId() + " on mutant " + m.getId());
		String className = DatabaseAccess.getGameForKey("Game_ID", m.getGameId()).getClassName();
		String[] resultArray = runAntTarget(context, "test-mutant", m.getFolder(), t.getFolder(), className);

		// If the test doesn't return failure
		if (resultArray[0].toLowerCase().contains("failures: 0")) {
			// If the test doesn't return any errors
			if (resultArray[0].toLowerCase().contains("errors: 0")) {
				// The test succeeded and a Target Execution for the mutant/test pairing is recorded. This means the test failed to detect the mutant
				TargetExecution newExec = new TargetExecution(t.getId(), m.getId(), "TEST_MUTANT", "SUCCESS", null);
				newExec.insert();
				return newExec.id;
			} else {
				// New target execution recording failed test against mutant due to error
				// Not sure on what circumstances cause a junit error, return all streams
				String message = resultArray[0] + " " + resultArray[1] + " " + resultArray[2];
				TargetExecution newExec = new TargetExecution(t.getId(), 0, "TEST_MUTANT", "ERROR", message);
				newExec.insert();
				return newExec.id;
			}
		} else {
			// The test failed and a Target Execution for the mutant/test pairing is recorded. The test detected the mutant.
			TargetExecution newExec = new TargetExecution(t.getId(), m.getId(), "TEST_MUTANT", "FAIL", null);
			newExec.insert();
			return newExec.id;
		}
	}

	// RUN MUTATION TESTS: Runs all the mutation tests for a particular game, using all the alive mutants and all tests

	// Inputs: The ID of the game to run mutation tests for
	// Outputs: None

	public static void runMutationTests(ServletContext context, int gid) {

		for (Mutant m : DatabaseAccess.getMutantsForGame(gid)) {
			for (Test t : DatabaseAccess.getTestsForGame(gid)) {
				// If the mutant is alive
				if (m.isAlive()) {
					// If this mutant/test pairing hasnt been run before and the test might kill the mutant
					if (DatabaseAccess.getTargetExecutionForPair(t.getId(), m.getId()) == null) {
						// Run the test against the mutant and get the result
						int targetId = testMutant(context, m, t);

						TargetExecution executedTarget = DatabaseAccess.getTargetExecutionsForKey("TargetExecution_ID", targetId).get(0);

						// If the test did NOT pass, the mutant was detected and should be killed.
						if (executedTarget.status.equals("FAIL")) {
							m.kill();
							m.update();
							t.killMutant();
							t.update();
						}
					}
				}
			}
		}
	}

	// RUN EQUIVALENCE TEST: Runs an equivalence test using an attacker supplied test and a mutant thought to be equivalent

	// Inputs: Attacker created test and a mutant to run it on
	// Outputs: None

	public static void runEquivalenceTest(ServletContext context, Test test, Mutant mutant) {
		logger.debug("Killing mutant in runEquivalenceTest.");
		System.out.println("Killing mutant in equivalenceTest.");
		// The test created is new and was made by the attacker (there is no need to check if the mutant/test pairing has been run already)

		// As a result of this test, either the test the attacker has written kills the mutant or doesnt.
		int targetId = testMutant(context, mutant, test);
		TargetExecution executedTarget = DatabaseAccess.getTargetExecutionsForKey("TargetExecution_ID", targetId).get(0);

		if (executedTarget.status.equals("FAIL")) {
			// If the test did NOT pass, the mutant was detected and is proven to be non-equivalent
			System.out.println("Mutant was killed, hence tagged not equivalent");
			mutant.setEquivalent(PROVEN_NO.name());
		} else {
			// If the test DID pass, the mutant went undetected and it is assumed to be equivalent.
			System.out.println("Test failed to kill the mutant, hence assumed equivalent");
			mutant.setEquivalent(ASSUMED_YES.name());
		}

		// Then kill the mutant either way.
		mutant.kill();
		mutant.update();
	}

	// RUN ANT TARGET: Runs a specific Ant Target in the build.xml file

	// Inputs: The target to run, any file locations necessary for that target, and the name of the relevant class
	// Outputs: String Array of length 4
	//    [0] : Input Stream for the process
	//    [1] : Error Stream for the process
	//    [2] : Any exceptions from running the process
	//    [3] : Message indicating which target was run, and with which files

	private static String[] runAntTarget(ServletContext context, String target, String mutantFile, String testFile, String className) {
		String[] resultArray = new String[4];
		String isLog = "";
		String esLog = "";
		String exLog = "";
		String debug = "Running Ant Target: " + target + " with mFile: " + mutantFile + " and tFile: " + testFile;

		ProcessBuilder pb = new ProcessBuilder();
		Map env = pb.environment();

		String antHome = (String) env.get("ANT_HOME");
		if (antHome == null)
			antHome = System.getProperty("ant.home", "/usr");

		String catalinaHome = (String) env.get("CATALINA_HOME");
		if (catalinaHome == null)
			catalinaHome = System.getProperty("catalina.home", "/usr/share/tomcat");

		pb.command(antHome + "/bin/ant", target,
				"-Dmutant.file=" + mutantFile,
				"-Dtest.file=" + testFile,
				"-Dclassname=" + className);

		String buildFileDir = catalinaHome + "/webapps" + context.getContextPath() + "/WEB-INF/data";
		pb.directory(new File(buildFileDir));
		pb. redirectErrorStream(true);

		System.out.println("Executing Ant Command: " + pb.command().toString());
		try {
			Process p = pb.start();
			String line;

			BufferedReader is = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((line = is.readLine()) != null) {
				isLog += line + "\n";
			}

			BufferedReader es = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while ((line = es.readLine()) != null) {
				esLog += line + "\n";
			}

		} catch (Exception ex) {
			exLog += "Exception: " + ex.toString() + "\n";
		}

		resultArray[0] = isLog;
		resultArray[1] = esLog;
		resultArray[2] = exLog;
		resultArray[3] = debug;
		System.out.println("is: " + isLog);
		System.out.println("es: " + esLog);
		System.out.println("ex: " + exLog);
		System.out.println("lg :" + debug);

		return resultArray;
	}
}