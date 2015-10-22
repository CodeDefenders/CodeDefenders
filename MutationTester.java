package gammut;

import java.io.*;
import java.util.ArrayList;

public class MutationTester {

	public static void runMutationTests(ArrayList<Test> tests, ArrayList<Mutant> mutants, String className) {

		boolean pass;

		for (Mutant m : mutants) {
			for (Test t : tests) {
				if (m.isAlive() && t.isValidTest()) {
					pass = testMutant(m, t, className);
					if (!pass) {m.setAlive(false); t.scorePoints(1);}
				}
			}
			if (m.isAlive()) {m.scorePoints(1);}
		}
	}

	public static void runEquivalenceTest(Test test, Mutant mutant, String className) {

		boolean pass;
		if (mutant.isAlive() && test.isValidTest()) {
			pass = testMutant(mutant, test, className);
			if (!pass) {mutant.setAlive(false); mutant.scorePoints(3);}
			else {mutant.setAlive(false); test.scorePoints(2);}
		}
	}

	public static boolean compileMutant(File f, String className) {
		return runAntTarget("compile-mutant", f.getAbsolutePath(), null, className);
	}

	public static boolean compileTest(Test t, String className) {
		return runAntTarget("compile-test", null, t.getFolder(), className);
	}

	public static boolean testOriginal(Test t, String className) {
		return runAntTarget("test-original", null, t.getFolder(), className);
	}

	public static boolean testMutant(Mutant m, Test t, String className) {
		return runAntTarget("test-mutant", m.getFolder(), t.getFolder(), className);
	}

	// Runs a specific Ant Target, given the name of the target and files to supply as arguments.
	// Already knows the class name from the constructor of the Mutation Tester.
	private static boolean runAntTarget(String target, String mutantFile, String testFile, String className) {
		String log = "";
		boolean result = true;
		log += "Running Ant Target: " + target + "\n";

		ProcessBuilder pb = new ProcessBuilder("%ANT_HOME%\\bin\\ant.bat",
												target,
												"-Dmutant.file="+mutantFile,
												"-Dtest.file="+testFile,
												"-Dclassname="+className);
        pb.directory(new File("%CATALINA_HOME%\\webapps\\gammut\\WEB-INF"));

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