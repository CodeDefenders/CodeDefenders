package gammut;

import java.io.*;
import java.util.ArrayList;

public class MutationTester {

	// Runs all the mutation tests for a particular game, using all the alive mutants and all tests
	public static void runMutationTests(int gid) {

		boolean pass;

		for (Mutant m : DatabaseAccess.getMutantsForGame(gid)) {
			for (Test t : DatabaseAccess.getTestsForGame(gid)) {
				if (m.isAlive()) {
					// Run the test against the mutant and get the result
					System.out.println("MutationTester");
					pass = testMutant(m, t, DatabaseAccess.getGameForKey("Game_ID", gid).getClassName());
					// If the test did NOT pass, the mutant was detected and should be killed.
					if (!pass) {System.out.println("test didnt pass"); m.kill(); m.update(); t.killMutant(); t.update();}
				}
			}
		}
	}

	// Runs an equivalence test using an attacker supplied test and a mutant thought to be equivalent
	public static void runEquivalenceTest(Test test, Mutant mutant, String className) {

		// As a result of this test, either the test the attacker has written kills the mutant or doesnt.
		boolean pass;
		pass = testMutant(mutant, test, className);
		// If the test did NOT pass, the mutant was detected and is proven to be non-equivalent
		if (!pass) {mutant.setEquivalent("PROVEN_NOT");}
		// If the test DID pass, the mutant went undetected and it is assumed to be equivalent.
		else {mutant.setEquivalent("ASSUMED_YES");}
		
		// Then kill the mutant either way.
		mutant.kill();
		mutant.update();
	}

	// Runs the related ant target that compiles a mutant
	public static boolean compileMutant(File f, String className) {
		return runAntTarget("compile-mutant", f.getAbsolutePath(), null, className);
	}

	// Runs the related ant target that compiles a test
	public static boolean compileTest(File f, String className) {
		return runAntTarget("compile-test", null, f.getAbsolutePath(), className);
	}

	// Runs the related ant target that ensures a test passes against the original code
	public static boolean testOriginal(File f, String className) {
		return runAntTarget("test-original", null, f.getAbsolutePath(), className);
	}

	// Runs the related ant target that runs a specified test for a specified mutant
	public static boolean testMutant(Mutant m, Test t, String className) {
		return runAntTarget("test-mutant", m.getFolder(), t.getFolder(), className);
	}

	// Runs a specific Ant Target, given the name of the target and files to supply as arguments.
	// Already knows the class name from the constructor of the Mutation Tester.
	private static boolean runAntTarget(String target, String mutantFile, String testFile, String className) {
		String log = "";
		boolean result = true;
		log += "Running Ant Target: " + target + "\n";

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
    		while((line = is.readLine()) != null) {log += line + "\n";}
    		BufferedReader es = new BufferedReader(new InputStreamReader(p.getErrorStream()));
    		while((line = es.readLine()) != null) {log += line + "\n"; result = false;}
		} catch (Exception ex) {log += "Exception: " + ex.toString() + "\n"; result = false;}
		System.out.println(log);
		return result;
	}
}