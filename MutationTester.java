package gammut;

import java.io.*;
import java.util.ArrayList;

public class MutationTester {

	// Runs the related ant target that compiles a mutant
	public static int compileMutant(Mutant m, String className) {
		String[] resultArray = runAntTarget("compile-mutant", m.getFolder(), null, className);

		if (resultArray[0].toLowerCase().contains("build successful")) {
			TargetExecution newExec = new TargetExecution(0, m.getId(), "COMPILE_MUTANT", "SUCCESS", null);
			newExec.insert();
			return newExec.id;
		}
		else if (resultArray[1].toLowerCase().contains("build failed")) {
			// mutant compiled incorrectly, parse errors from is
			// Need to strip unneccessary information from the resultArray[0]
			TargetExecution newExec = new TargetExecution(0, m.getId(), "COMPILE_MUTANT", "FAIL", resultArray[0]);
			newExec.insert();
			return newExec.id;
		}
		else {
			return -1;
		}
	}

	// Runs the related ant target that compiles a test
	public static int compileTest(Test t, String className) {
		String[] resultArray = runAntTarget("compile-test", null, t.getFolder(), className);

		if (resultArray[0].toLowerCase().contains("build successful")) {
			// mutant compiled correctly
			// create an execution entry in db and return id
			TargetExecution newExec = new TargetExecution(t.getId(), 0, "COMPILE_TEST", "SUCCESS", null);
			newExec.insert();
			return newExec.id;
		}
		else if (resultArray[1].toLowerCase().contains("build failed")) {
			// mutant compiled incorrectly, parse errors from is
			// create an execution entry in db and return id
			TargetExecution newExec = new TargetExecution(t.getId(), 0, "COMPILE_TEST", "FAIL", resultArray[0]);
			newExec.insert();
			return newExec.id;
		}
		else {
			return -1;
		}
	}

	// Runs the related ant target that ensures a test passes against the original code
	public static int testOriginal(Test t, String className) {
		String[] resultArray = runAntTarget("test-original", null, t.getFolder(), className);
		if (resultArray[0].toLowerCase().contains("failures: 0")) {
			// None of the tests failed against the original so the test is ok.
			TargetExecution newExec = new TargetExecution(t.getId(), 0, "TEST_ORIGINAL", "SUCCESS", null);
			newExec.insert();
			return newExec.id;
		}

		else if (resultArray[1].toLowerCase().contains("failed")) {
			// One of the tests has failed.
			TargetExecution newExec = new TargetExecution(t.getId(), 0, "TEST_ORIGINAL", "FAIL", null);
			newExec.insert();
			return newExec.id;
		}
		else {
			return -1;
		}
	}

	// Runs the related ant target that runs a specified test for a specified mutant
	public static int testMutant(Mutant m, Test t, String className) {
		String[] resultArray = runAntTarget("test-mutant", m.getFolder(), t.getFolder(), className);
		if (resultArray[0].toLowerCase().contains("failures: 0")) {
			// None of the tests failed against the original so any mutants went undetected
			TargetExecution newExec = new TargetExecution(t.getId(), m.getId(), "TEST_MUTANT", "SUCCESS", null);
			newExec.insert();
			return newExec.id;
		}
		else if (resultArray[1].toLowerCase().contains("failed")) {
			// One of the tests has failed and a mutant has been detected.
			TargetExecution newExec = new TargetExecution(t.getId(), m.getId(), "TEST_MUTANT", "FAIL", null);
			newExec.insert();
			return newExec.id;
		}
		else {
			// otherwise possible third edge case where there is some sort of error
			return -1;
		}
	}

	// Runs all the mutation tests for a particular game, using all the alive mutants and all tests
	public static void runMutationTests(int gid) {

		for (Mutant m : DatabaseAccess.getMutantsForGame(gid)) {
			for (Test t : DatabaseAccess.getTestsForGame(gid)) {
				// If the mutant is alive
				if (m.isAlive()) {
					// If this mutant/test pairing hasnt been run before
					if (DatabaseAccess.getTargetExecutionForPair(t.getId(), m.getId()) == null) {
						// Run the test against the mutant and get the result
						System.out.println("MutationTester");
						int targetId = testMutant(m, t, DatabaseAccess.getGameForKey("Game_ID", gid).getClassName());

						ArrayList<TargetExecution> executedTarget = DatabaseAccess.getTargetExecutionsForKey("TargetExecution_ID", targetId);

						// If the test did NOT pass, the mutant was detected and should be killed.
						if (executedTarget.get(0).status.equals("FAIL")) {System.out.println("test didnt pass"); m.kill(); m.update(); t.killMutant(); t.update();}
					}
				}
			}
		}
	}

	// Runs an equivalence test using an attacker supplied test and a mutant thought to be equivalent
	public static void runEquivalenceTest(Test test, Mutant mutant, String className) {
		// The test created is new and was made by the attacker.

		// As a result of this test, either the test the attacker has written kills the mutant or doesnt.
		int targetId = testMutant(mutant, test, className);
		ArrayList<TargetExecution> executedTarget = DatabaseAccess.getTargetExecutionsForKey("TargetExecution_ID", targetId);
		// If the test did NOT pass, the mutant was detected and is proven to be non-equivalent
		if (executedTarget.get(0).status.equals("FAIL")) {mutant.setEquivalent("PROVEN_NOT");}
		// If the test DID pass, the mutant went undetected and it is assumed to be equivalent.
		else {mutant.setEquivalent("ASSUMED_YES");}
		
		// Then kill the mutant either way.
		mutant.kill();
		mutant.update();
	}

	// Runs a specific Ant Target, given the name of the target and files to supply as arguments.
	// Already knows the class name from the constructor of the Mutation Tester.
	// Returns a String Array with 4 Parts, the text from the Input Stream, Error Stream, Exception Names, and the Target that was run.
	private static String[] runAntTarget(String target, String mutantFile, String testFile, String className) {
		String[] resultArray = new String[4];
		String isLog = "";
		String esLog = "";
		String exLog = "";
		String debug = "Running Ant Target: " + target + " with mFile: " + mutantFile + " and tFile: " + testFile;

		boolean result = true;
		
		ProcessBuilder pb = new ProcessBuilder(System.getProperty("ant.home", "C:/apache-ant-1.9.5") + "/bin/ant.bat",
												target,
												"-Dmutant.file="+mutantFile,
												"-Dtest.file="+testFile,
												"-Dclassname="+className);
        pb.directory(new File(System.getProperty("catalina.home", "C:/apache-tomcat-7.0.62") + "/webapps/gammut/WEB-INF"));

		try {
			Process p = pb.start();
		    String line;

		    BufferedReader is = new BufferedReader(new InputStreamReader(p.getInputStream()));
    		while((line = is.readLine()) != null) {isLog += line + "\n";}

    		BufferedReader es = new BufferedReader(new InputStreamReader(p.getErrorStream()));
    		while((line = es.readLine()) != null) {esLog += line + "\n"; result = false;}

		} catch (Exception ex) {exLog += "Exception: " + ex.toString() + "\n"; result = false;}
		
		resultArray[0] = isLog;
		resultArray[1] = esLog;
		resultArray[2] = exLog;
		resultArray[4] = debug;
		System.out.println("is: " + isLog);
		System.out.println("es: " + esLog);
		System.out.println("ex: " + exLog);
		System.out.println("lg :" + debug);

		return resultArray;
	}
}