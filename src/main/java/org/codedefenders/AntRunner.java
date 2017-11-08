package org.codedefenders;

import static org.codedefenders.Constants.AI_DIR;
import static org.codedefenders.Constants.CUTS_DIR;
import static org.codedefenders.Constants.F_SEP;
import static org.codedefenders.Constants.JAVA_CLASS_EXT;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.servlet.ServletContext;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.codedefenders.multiplayer.CoverageGenerator;
import org.codedefenders.multiplayer.LineCoverage;
import org.codedefenders.util.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jose Rojas
 * @author Alessio Gambi (last edit)
 */
public class AntRunner {

	private static final Logger logger = LoggerFactory.getLogger(AntRunner.class);
	private static String antHome = null;
	private static boolean clusterEnabled = false;
	//
	private static String clusterJavaHome = null;
	private static String clusterReservationName = null;
	private static String clusterTimeOutMinutes = "2";

	// Alessio: DO NOT REALLY LIKE THOSE...
	static {
		// First check the Web abb context
		InitialContext initialContext;
		try {
			initialContext = new InitialContext();
			NamingEnumeration<NameClassPair> list = initialContext.list("java:/comp/env");
			Context environmentContext = (Context) initialContext.lookup("java:/comp/env");

			// Looking up a name which is not there causes an exception
			// Some are unsafe !
			while (list.hasMore()) {
				String name = list.next().getName();
				switch (name) {
				case "ant.home":
					antHome = (String) environmentContext.lookup(name);
					break;
				case "cluster.mode":
					clusterEnabled = "enabled".equalsIgnoreCase((String) environmentContext.lookup(name));
					break;
				case "cluster.java.home":
					clusterJavaHome = (String) environmentContext.lookup(name);
					break;
				case "cluster.reservation.name":
					clusterReservationName = (String) environmentContext.lookup(name);
					break;
				case "cluster.timeout":
					clusterTimeOutMinutes = (String) environmentContext.lookup(name);
					break;
				}
				System.out.println("AntRunner Setting Env " + name);

			}

		} catch (NamingException e) {
			e.printStackTrace();
		}

		// Check Env
		if (antHome == null) {
			ProcessBuilder pb = new ProcessBuilder();
			Map env = pb.environment();
			antHome = (String) env.get("CODEDEFENDERS_DATA");
		}
		// Check System properties
		if (antHome == null) {
			if (System.getProperty("os.name").toLowerCase().contains("windows")) {
				antHome = System.getProperty("codedefenders.data", "C:/codedefenders-data");
			} else {
				antHome = System.getProperty("codedefenders.data", "/var/lib/codedefenders");
			}
		}
	}
	/////

	public static boolean testKillsMutant(Mutant m, Test t) {
		// String className = DatabaseAccess.getGameForKey("Game_ID",
		// m.getGameId()).getClassName();

		GameClass cut = DatabaseAccess.getClassForGame(m.getGameId());

		String[] resultArray = runAntTarget("test-mutant", m.getFolder(), t.getFolder(), cut,
				t.getFullyQualifiedClassName());
		if (resultArray[0].toLowerCase().contains("failures: 0")) {
			// Mutant not killed.
			return false;
		}
		return true;
	}

	/**
	 * Executes a test against a mutant
	 * 
	 * @param m
	 *            A {@link Mutant} object
	 * @param t
	 *            A {@link Test} object
	 * @return A {@link TargetExecution} object
	 */
	public static TargetExecution testMutant(Mutant m, Test t) {
		logger.debug("Running test {} on mutant {}", t.getId(), m.getId());
		GameClass cut = DatabaseAccess.getClassForGame(m.getGameId());

		String[] resultArray = runAntTarget("test-mutant", m.getFolder(), t.getFolder(), cut,
				t.getFullyQualifiedClassName());

		TargetExecution newExec = null;

		if (resultArray[0].toLowerCase().contains("failures: 0")) {
			// If the test doesn't return failure
			if (resultArray[0].toLowerCase().contains("errors: 0")) {
				// If the test doesn't return any errors
				// The test succeeded and a Target Execution for the mutant/test
				// pairing is recorded. This means the test failed to detect the
				// mutant
				newExec = new TargetExecution(t.getId(), m.getId(), TargetExecution.Target.TEST_MUTANT, "SUCCESS",
						null);
			} else {
				// New target execution recording failed test against mutant due
				// to error
				// Not sure on what circumstances cause a junit error, return
				// all streams
				String message = resultArray[0] + " " + resultArray[1] + " " + resultArray[2];
				newExec = new TargetExecution(t.getId(), m.getId(), TargetExecution.Target.TEST_MUTANT, "ERROR",
						message);
			}
		} else {
			// The test failed and a Target Execution for the mutant/test
			// pairing is recorded. The test detected the mutant.
			newExec = new TargetExecution(t.getId(), m.getId(), TargetExecution.Target.TEST_MUTANT, "FAIL", null);
		}
		newExec.insert();
		return newExec;
	}

	public static boolean potentialEquivalent(Mutant m) {
		logger.debug("Checking if mutant " + m.getId() + " is potentially equivalent.");
		GameClass cut = DatabaseAccess.getClassForGame(m.getGameId());
		String suiteDir = AI_DIR + F_SEP + "tests" + F_SEP + cut.getAlias();

		String[] resultArray = runAntTarget("test-mutant", m.getFolder(), suiteDir, cut,
				cut.getName() + Constants.SUITE_EXT);
		if (resultArray[0].toLowerCase().contains("failures: 0")) {
			// If the test doesn't return failure
			if (resultArray[0].toLowerCase().contains("errors: 0")) {
				// If the test doesn't return any errors
				// Test succeeded, so could be equivalent.
				return true;
			}
		}

		return false;
	}

	/**
	 * Executes a test against the original code
	 * 
	 * @param dir
	 * @param t
	 *            A {@link Test} object
	 * @return A {@link TargetExecution} object
	 */
	public static int testOriginal(File dir, Test t) {
		GameClass cut = DatabaseAccess.getClassForGame(t.getGameId());

		String[] resultArray = runAntTarget("test-original", null, dir.getAbsolutePath(), cut,
				t.getFullyQualifiedClassName());

		// add coverage information
		t.setLineCoverage(getLinesCovered(t, cut));
		t.update();

		// If the test doesn't return failure
		if (resultArray[0].toLowerCase().contains("failures: 0")) {
			// If the test doesn't return error
			if (resultArray[0].toLowerCase().contains("errors: 0")) {
				// New Target execution recording successful test against
				// original
				TargetExecution newExec = new TargetExecution(t.getId(), 0, TargetExecution.Target.TEST_ORIGINAL,
						"SUCCESS", null);
				newExec.insert();
				return newExec.id;
			} else {
				// New target execution recording failed test against original
				// due to error
				// Not sure on what circumstances cause a junit error, return
				// all streams
				String message = resultArray[0] + " " + resultArray[1] + " " + resultArray[2];
				TargetExecution newExec = new TargetExecution(t.getId(), 0, TargetExecution.Target.TEST_ORIGINAL,
						"ERROR", message);
				newExec.insert();
				return newExec.id;
			}
		} else {
			// New target execution record failed test against original as it
			// isn't valid
			String message = resultArray[0].substring(resultArray[0].indexOf("[junit]"));
			TargetExecution newExec = new TargetExecution(t.getId(), 0, TargetExecution.Target.TEST_ORIGINAL, "FAIL",
					message);
			newExec.insert();
			return newExec.id;
		}
	}

	/**
	 * Compiles CUT
	 *
	 * @param cut
	 * @return The Path to the compiled CUT
	 */
	public static String compileCUT(GameClass cut) {

		String[] resultArray = runAntTarget("compile-cut", null, null, cut, null);
		logger.info(String.format("Compile New CUT, Compilation result: %s", Arrays.toString(resultArray)));

		String pathCompiledClassName = null;
		if (resultArray[0].toLowerCase().contains("build successful")) {
			// If the input stream returned a 'successful build' message, the
			// CUT compiled correctly
			logger.info("Compiled uploaded CUT successfully");
			File f = new File(CUTS_DIR + Constants.F_SEP + cut.getAlias());

			final String compiledClassName = FilenameUtils.getBaseName(cut.getJavaFile()) + Constants.JAVA_CLASS_EXT;
			LinkedList<File> matchingFiles = (LinkedList) FileUtils.listFiles(f,
					FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter());
			if (!matchingFiles.isEmpty())
				pathCompiledClassName = matchingFiles.get(0).getAbsolutePath();

		} else {
			// Otherwise the CUT failed to compile
			int index = resultArray[0].indexOf("javac");
			String message = resultArray[0];
			if (index >= 0) {
				message = message.substring(message.indexOf("javac"));
			}
			logger.error(String.format("Failed to compile uploaded CUT: %s", message));
		}
		return pathCompiledClassName;
	}

	/**
	 * Compiles mutant
	 * 
	 * @param dir
	 * @param jFile
	 * @param gameID
	 * @param cut
	 * @param ownerId
	 * @return A {@link Mutant} object
	 */
	public static Mutant compileMutant(File dir, String jFile, int gameID, GameClass cut, int ownerId) {

		// Gets the classname for the mutant from the game it is in
		String[] resultArray = runAntTarget("compile-mutant", dir.getAbsolutePath(), null, cut, null);
		logger.info(String.format("Compilation result: %s", Arrays.toString(resultArray)));

		Mutant newMutant = null;
		// If the input stream returned a 'successful build' message, the mutant
		// compiled correctly
		if (resultArray[0].toLowerCase().contains("build successful")) {
			// Create and insert a new target execution recording successful
			// compile, with no message to report, and return its ID
			// Locate .class file
			final String compiledClassName = cut.getBaseName() + JAVA_CLASS_EXT;
			// <<<<<<< HEAD
			// LinkedList<File> matchingFiles = (LinkedList)
			// FileUtils.listFiles(dir,
			// FileFilterUtils.nameFileFilter(compiledClassName),
			// FileFilterUtils.trueFileFilter());
			// assert (!matchingFiles.isEmpty()); // if compilation was
			// successful,
			// // .class file must exist
			// =======
			LinkedList<File> matchingFiles = (LinkedList) FileUtils.listFiles(dir,
					FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter());
			assert (!matchingFiles.isEmpty()) : "if compilation was successful, .class file must exist";
			// >>>>>>> master
			String cFile = matchingFiles.get(0).getAbsolutePath();
			int playerId = DatabaseAccess.getPlayerIdForMultiplayerGame(ownerId, gameID);
			newMutant = new Mutant(gameID, jFile, cFile, true, playerId);
			newMutant.insert();
			TargetExecution newExec = new TargetExecution(0, newMutant.getId(), TargetExecution.Target.COMPILE_MUTANT,
					"SUCCESS", null);
			newExec.insert();
		} else {
			// The mutant failed to compile
			// New target execution recording failed compile, providing the
			// return messages from the ant javac task
			String message = resultArray[0].substring(resultArray[0].indexOf("[javac]")).replaceAll(Constants.DATA_DIR,
					"");
			logger.error(String.format("Failed to compile mutant %s: %s", jFile, message));
			int playerId = DatabaseAccess.getPlayerIdForMultiplayerGame(ownerId, gameID);
			newMutant = new Mutant(gameID, jFile, null, false, playerId);
			newMutant.insert();
			TargetExecution newExec = new TargetExecution(0, newMutant.getId(), TargetExecution.Target.COMPILE_MUTANT,
					"FAIL", message);
			newExec.insert();
		}
		return newMutant;
	}

	/**
	 * Compiles test
	 * 
	 * @param dir
	 * @param jFile
	 * @param gameID
	 * @param cut
	 * @param ownerId
	 * @return A {@link Test} object
	 */
	public static Test compileTest(File dir, String jFile, int gameID, GameClass cut, int ownerId) {
		// public static int compileTest(ServletContext context, Test t) {

		String[] resultArray = runAntTarget("compile-test", null, dir.getAbsolutePath(), cut, null);

		int playerId = DatabaseAccess.getPlayerIdForMultiplayerGame(ownerId, gameID);

		// If the input stream returned a 'successful build' message, the test
		// compiled correctly
		if (resultArray[0].toLowerCase().contains("build successful")) {
			// Create and insert a new target execution recording successful
			// compile, with no message to report, and return its ID
			// Locate .class file
			final String compiledClassName = FilenameUtils.getBaseName(jFile) + JAVA_CLASS_EXT;
			LinkedList<File> matchingFiles = (LinkedList) FileUtils.listFiles(dir,
					FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter());
			assert (!matchingFiles.isEmpty()); // if compilation was successful,
												// .class file must exist
			String cFile = matchingFiles.get(0).getAbsolutePath();
			logger.error(String.format("Compiled test %s", compiledClassName));
			Test newTest = new Test(gameID, jFile, cFile, playerId);
			newTest.insert();
			TargetExecution newExec = new TargetExecution(newTest.getId(), 0, TargetExecution.Target.COMPILE_TEST,
					"SUCCESS", null);
			newExec.insert();
			return newTest;
		} else {
			// The test failed to compile
			// New target execution recording failed compile, providing the
			// return messages from the ant javac task
			String message = resultArray[0].substring(resultArray[0].indexOf("[javac]")).replaceAll(Constants.DATA_DIR,
					"");
			logger.error(String.format("Failed to compile test %s: %s", jFile, message));
			Test newTest = new Test(gameID, jFile, null, playerId);
			newTest.insert();
			TargetExecution newExec = new TargetExecution(newTest.getId(), 0, TargetExecution.Target.COMPILE_TEST,
					"FAIL", message);
			newExec.insert();
			return newTest;
		}
	}

	/**
	 * Generates mutant classes using Major
	 * 
	 * @param cut
	 *            game class
	 */
	public static void generateMutantsFromCUT(final GameClass cut) {

		String[] resultArray = runAntTarget("mutant-gen-cut", null, null, cut, null);
	}

	/**
	 * Generates tests using EvoSuite
	 * 
	 * @param cut
	 *            CUT filename
	 */
	public static void generateTestsFromCUT(final GameClass cut) {
		String[] resultArray = runAntTarget("test-gen-cut", null, null, cut, null);
	}

	/**
	 * Compiles generated test suite
	 * 
	 * @param cut
	 */
	public static boolean compileGenTestSuite(final GameClass cut) {
		String[] resultArray = runAntTarget("compile-gen-tests", null, null, cut, cut.getName() + Constants.SUITE_EXT);
		if (resultArray[0].toLowerCase().contains("build successful")) {
			return true;
		}
		return false;
	}

	/**
	 * Runs a specific Ant target in the build.xml file
	 *
	 * @param target
	 *            An Ant target
	 * @param mutantFile
	 * @param testDir
	 * @param cut
	 *            Class
	 * @param testClassName
	 * @return String Array of length 4 [0] : Input Stream for the process [1] :
	 *         Error Stream for the process [2] : Any exceptions from running
	 *         the process [3] : Message indicating which target was run, and
	 *         with which files
	 */
	private static String[] runAntTarget(String target, String mutantFile, String testDir, GameClass cut,
			String testClassName) {
		String[] resultArray = new String[4];
		String isLog = "";
		String esLog = "";
		String exLog = "";

		String debug = "Running Ant Target: " + target + " with mFile: " + mutantFile + " and tFile: " + testDir;

		ProcessBuilder pb = new ProcessBuilder();
		Map env = pb.environment();
		List<String> command = new ArrayList<>();

		/**
		 * Clustered execution uses almost the same command than normal
		 * execution. But it prefixes that with "srun". This assumes that the
		 * code-defender working dir is on the NFS.
		 */

		if (clusterEnabled) {
			System.out.println("AntRunner.runAntTarget() Clustered Execution");
			System.out.println("AntRunner.runAntTarget() JAVA_HOME " + clusterJavaHome);
			System.out.println("AntRunner.runAntTarget() Reservation name " + clusterReservationName);
			// For some reasong we need to specify JAVA_HOME to /usr/bin/java
			// TODO Is this a config parameter ?
			if (clusterJavaHome != null) {
				env.put("JAVA_HOME", clusterJavaHome);
			}
			// TODO
			env.put("CLASSPATH", "lib/hamcrest-all-1.3.jar:lib/junit-4.12.jar");
			//
			command.add("srun");

			// Select reservation cluster
			if (clusterReservationName != null)
				command.add("--reservation=" + clusterReservationName);

			// Timeout. Note that there's a plus 10 minutes of grace period
			// anyway
			// TODO This is unsafe we need to check this is a valid integer
			if (clusterTimeOutMinutes != null)
				command.add("--time=" + clusterTimeOutMinutes);
			//
			command.add("ant");
		} else {
			System.out.println("AntRunner.runAntTarget() Normal Execution");
			env.put("CLASSPATH", "lib/hamcrest-all-1.3.jar:lib/junit-4.12.jar");
			// String antHome = (String) env.get("ANT_HOME");
			// if (antHome == null) {
			// System.err.println("ANT_HOME undefined.");
			// antHome = System.getProperty("ant.home", "/usr/local");
			// }

			String command_ = antHome + "/bin/ant";

			if (System.getProperty("os.name").toLowerCase().contains("windows")) {
				command_ += ".bat";
				command_ = command_.replace("/", "\\").replace("\\", "\\\\");
			}
			command.add(command_);

		}

		// Add additional command parameters
		command.add(target); // "-v", "-d", for verbose, debug
		// This ensures that ant actually uses the data dir we setup
		command.add("-Dcodedef.home=" + Constants.DATA_DIR);
		///
		command.add("-Dmutant.file=" + mutantFile);
		command.add("-Dtest.file=" + testDir);
		command.add("-Dcut.dir=" + CUTS_DIR + F_SEP + cut.getAlias());
		command.add("-Dclassalias=" + cut.getAlias());
		command.add("-Dclassbasename=" + cut.getBaseName());
		command.add("-Dclassname=" + cut.getName());
		command.add("-DtestClassname=" + testClassName);

		// Execute whichever command was build
		pb.command(command);

		System.out.println("Executing Command: " + pb.command().toString());
		//
		String buildFileDir = Constants.DATA_DIR;
		pb.directory(new File(buildFileDir));
		pb.redirectErrorStream(true);

		logger.debug("Executing Ant Command: " + pb.command().toString());
		logger.debug("Executing from directory: " + buildFileDir);
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

		// TODO For clustered executions we need to clean up the output and
		// remove the cluster specific output

		resultArray[0] = isLog;
		resultArray[1] = esLog;
		resultArray[2] = exLog;
		resultArray[3] = debug;
		logger.debug("is: " + isLog);
		logger.debug("es: " + esLog);
		logger.debug("ex: " + exLog);
		logger.debug("lg :" + debug);

		return resultArray;

	}

	/**
	 * Executes a test against a mutant
	 * 
	 * @param t
	 *            A {@link Test} object
	 * @param c
	 *            A {@link GameClass} object
	 * @return A {@link TargetExecution} object
	 */
	private static LineCoverage getLinesCovered(Test t, GameClass c) {
		CoverageGenerator cg = new CoverageGenerator(new File(t.getFolder()),
				new File(Constants.CUTS_DIR + F_SEP + c.getAlias()));

		try {
			cg.create(c.getName());
		} catch (IOException e) {
			e.printStackTrace();
		}

		LineCoverage lc = new LineCoverage();
		lc.setLinesCovered(cg.getLinesCovered());
		lc.setLinesUncovered(cg.getLinesUncovered());
		return lc;
	}

	private static String[] processJacoco(ServletContext context, String testFile) {
		String[] resultArray = new String[4];
		String isLog = "";
		String esLog = "";
		String exLog = "";

		ProcessBuilder pb = new ProcessBuilder();
		Map env = pb.environment();

		String antHome = (String) env.get("ANT_HOME");
		if (antHome == null) {
			System.err.println("ANT_HOME undefined.");
			antHome = System.getProperty("ant.home", "/usr/local");
		}

		String command = antHome + "/bin/ant";

		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			command += ".bat";
		}

		command.replace("\\", "\\\\");
		pb.command(command, "process-jacoco", // "-v", "-d", for verbose, debug
				"-Dsrc.dir=" + context.getRealPath(Constants.CUTS_DIR), "-Dtest.file=" + testFile);

		String buildFileDir = context.getRealPath(Constants.DATA_DIR);
		pb.directory(new File(buildFileDir));
		pb.redirectErrorStream(true);

		logger.debug("Executing Ant Command: " + pb.command().toString());
		logger.debug("Executing from directory: " + buildFileDir);
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
		logger.debug("is: " + isLog);
		logger.debug("es: " + esLog);
		logger.debug("ex: " + exLog);

		return resultArray;
	}

}
