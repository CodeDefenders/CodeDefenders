import java.io.*;
import java.util.ArrayList;

public class MutationTester {

	private String className;
	private String log;

	public MutationTester(String className) {
		this.className = className;
		log = "";
	}

	public void runMutationTests(ArrayList<Test> tests, ArrayList<Mutant> mutants) {

		Process mutationTest = null;
		boolean pass;

		for (Mutant m : mutants) {
			for (Test t : tests) {
				if (m.isAlive()) {
					pass = testMutant(m, t);
					if (!pass) {m.setAlive(false); t.scorePoints(1);}
				}
			}
			if (m.isAlive()) {m.scorePoints(1);}
		}
	}

	public boolean compileMutant(Mutant m) {
		return runAntTarget("compile-mutant", m.getFolder(), null);
	}

	public boolean compileTest(Test t) {
		return runAntTarget("compile-test", null, t.getFolder());
	}

	public boolean testOriginal(Test t) {
		return runAntTarget("test-original", null, t.getFolder());
	}

	public boolean testMutant(Mutant m, Test t) {
		return runAntTarget("test-mutant", m.getFolder(), t.getFolder());
	}

	// Runs a specific Ant Target, given the name of the target and files to supply as arguments.
	// Already knows the class name from the constructor of the Mutation Tester.
	private boolean runAntTarget(String target, String mutantFile, String testFile) {
		boolean result = true;
		log += "-- Running Ant Target: " + target + "\n";

		ProcessBuilder pb = new ProcessBuilder("C:\\apache-ant-1.9.5\\bin\\ant.bat",
												target,
												"-Dmutant.file="+mutantFile,
												"-Dtest.file="+testFile,
												"-Dclassname="+className);
        pb.directory(new File("C:\\apache-tomcat-7.0.62\\webapps\\gammut\\WEB-INF"));

		try {
			Process p = pb.start();
		    String line;
    		BufferedReader es = new BufferedReader(new InputStreamReader(p.getErrorStream()));
    		while((line = es.readLine()) != null) {log += line + "\n"; result = false;}
		} catch (Exception ex) {log += ex.toString() + "\n"; result = false;}

		return result;
	}

	public String getLog() {return log;}
}