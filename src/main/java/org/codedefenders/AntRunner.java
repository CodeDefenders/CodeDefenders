package org.codedefenders;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.codedefenders.multiplayer.CoverageGenerator;
import org.codedefenders.multiplayer.LineCoverage;
import org.codedefenders.util.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.xml.crypto.Data;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

import static org.codedefenders.Constants.*;

/**
 * @author Jose Rojas
 */
public class AntRunner {

	private static final Logger logger = LoggerFactory.getLogger(AntRunner.class);

	public static boolean testKillsMutant(Mutant m, Test t) {
		//String className = DatabaseAccess.getGameForKey("Game_ID", m.getGameId()).getClassName();

		GameClass cut = DatabaseAccess.getClassForGame(m.getGameId());

		String[] resultArray = runAntTarget("test-mutant", m.getFolder(), t.getFolder(), cut, t.getFullyQualifiedClassName());
		if (resultArray[0].toLowerCase().contains("failures: 0")) {
			//Mutant not killed.
			return false;
		}
		return true;
	}

	public static boolean testKillsMutantStory(PuzzleMutant m, PuzzleTest t) {

		StoryClass cut = DatabaseAccess.getClassForPuzzle(m.getPuzzleId());

		String[] resultArray = runAntTargetStory("test-mutant", m.getFolder(), t.getFolder(), cut, "Test" + cut.getClassName());
		if (resultArray[0].toLowerCase().contains("failures: 0")) {
			return false;
		}

		return true;

	}

	/**
	 * Executes a test against a mutant
	 * @param m A {@link Mutant} object
	 * @param t A {@link Test} object
	 * @return A {@link TargetExecution} object
	 */
	public static TargetExecution testMutant(Mutant m, Test t) {
		logger.debug("Running test {} on mutant {}", t.getId(), m.getId());
		GameClass cut = DatabaseAccess.getClassForGame(m.getGameId());

		String[] resultArray = runAntTarget("test-mutant", m.getFolder(), t.getFolder(), cut, t.getFullyQualifiedClassName());

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

	public static TargetExecution testPMutant(PuzzleMutant m, PuzzleTest t) {

		logger.debug("Running test{} on mutant{}", t.getTestId(), m.getMutantId());
		StoryClass cut = DatabaseAccess.getClassForPuzzle(m.getPuzzleId());

		String[] resultArray = runAntTargetStory("test-mutant", Constants.PUZZLE_MUTANTS_DIR, Constants.PUZZLE_TESTS_DIR, cut, "Test" + cut.getClassName());

		TargetExecution newExec = null;

		if (resultArray[0].toLowerCase().contains("failures: 0")) {
			if (resultArray[0].toLowerCase().contains("errors: 0")) {
				newExec = new TargetExecution(t.getTestId(), m.getMutantId(), TargetExecution.Target.TEST_MUTANT, "SUCCESS", null);
			} else {
				String message = resultArray[0] + " " + resultArray[1] + " " + resultArray[2];
				newExec = new TargetExecution(t.getTestId(), m.getMutantId(), TargetExecution.Target.TEST_MUTANT, "ERROR", message);
			}
		} else {
			newExec = new TargetExecution(t.getTestId(), m.getMutantId(), TargetExecution.Target.TEST_MUTANT, "FAIL", null);
		}
		newExec.insert();
		return newExec;

	}

	public static boolean potentialEquivalent(Mutant m) {
		logger.debug("Checking if mutant " + m.getId() + " is potentially equivalent.");
		GameClass cut = DatabaseAccess.getClassForGame(m.getGameId());
		String suiteDir = AI_DIR + F_SEP + "tests" + F_SEP + cut.getAlias();

		String[] resultArray = runAntTarget("test-mutant", m.getFolder(), suiteDir, cut, cut.getName() + Constants.SUITE_EXT);
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

	public static boolean potentialEquivalentP(PuzzleMutant m) {

		logger.debug("Cheking if mutant " + m.getMutantId() + " is potentially equivalent");
		StoryClass cut = DatabaseAccess.getClassForPuzzle(m.getPuzzleId());
		String suiteDir = AI_DIR + F_SEP + "tests" + F_SEP + cut.getAlias();

		String[] resultArray = runAntTargetStory("test-mutant", m.getFolder(), suiteDir, cut, cut.getClassName() + Constants.SUITE_EXT);
		if (resultArray[0].toLowerCase().contains("failures: 0")) {
			if (resultArray[0].toLowerCase().contains("errors: 0")) {
				return true;
			}
		}

		return false;

	}

	/**
	 * Executes a test against the original code
	 * @param dir
	 * @param t A {@link Test} object
	 * @return A {@link TargetExecution} object
	 */
	public static int testOriginal(File dir, Test t) {
		GameClass cut = DatabaseAccess.getClassForGame(t.getGameId());

		String[] resultArray = runAntTarget("test-original", null, dir.getAbsolutePath(), cut, t.getFullyQualifiedClassName());

		// add coverage information
		t.setLineCoverage(getLinesCovered(t, cut));
		t.update();

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
			String message = resultArray[0].substring(resultArray[0].indexOf("[junit]"));
			TargetExecution newExec = new TargetExecution(t.getId(), 0, TargetExecution.Target.TEST_ORIGINAL, "FAIL", message);
			newExec.insert();
			return newExec.id;
		}
	}

	public static int testOriginalStory(File dir, PuzzleTest t) {

		StoryClass cut = DatabaseAccess.getClassForPuzzle(t.getPuzzleId());

		String[] resultArray = runAntTargetStory("test-original", null, dir.getAbsolutePath(), cut, cut.getClassName());

		t.setLineCoverage(getLinesCoveredStory(t, cut));
		t.update();

		if (resultArray[0].toLowerCase().contains("failures: 0")) {

			if (resultArray[0].toLowerCase().contains("errors: 0")) {

				TargetExecution newExec = new TargetExecution(t.getTestId(), 0, TargetExecution.Target.TEST_ORIGINAL, "SUCCESS", null);
				newExec.insertStory();
				return newExec.id;

			} else {

				String message = resultArray[0] + " " + resultArray[1] + " " + resultArray[2];
				TargetExecution newExec = new TargetExecution(t.getTestId(), 0, TargetExecution.Target.TEST_ORIGINAL, "FAIL", message);
				newExec.insertStory();
				return newExec.id;

			}
		} else {

			String message = resultArray[0].substring(resultArray[0].indexOf("[junit]"));
			TargetExecution newExec = new TargetExecution(t.getTestId(), 0, TargetExecution.Target.TEST_ORIGINAL, "FAIL", message);
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
		System.out.println("Compile New CUT, Compilation result:");
		System.out.println(Arrays.toString(resultArray));

		String pathCompiledClassName = null;
		if (resultArray[0].toLowerCase().contains("build successful")) {
			// If the input stream returned a 'successful build' message, the CUT compiled correctly
			System.out.println("Compiled uploaded CUT successfully");
			File f = new File(CUTS_DIR + Constants.F_SEP + cut.getAlias());
			final String compiledClassName = FilenameUtils.getBaseName(cut.getJavaFile()) + Constants.JAVA_CLASS_EXT;
			LinkedList<File> matchingFiles = (LinkedList)FileUtils.listFiles(f, FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter());
			if (! matchingFiles.isEmpty())
				pathCompiledClassName = matchingFiles.get(0).getAbsolutePath();
		} else {
			// Otherwise the CUT failed to compile
			System.err.println("Failed to compile uploaded CUT");
			int index = resultArray[0].indexOf("javac");
			String message = resultArray[0];
			if (index >= 0){
				message = message.substring(message.indexOf("javac"));
			}
			System.err.println(message);
		}
		return pathCompiledClassName;
	}

	public static String compilePCUT(StoryClass cut) {

		String[] resultArray = runAntTargetStory("compile-cut", null, null, cut, null);
		System.out.println("Compile New CUT, Compilation result:");
		System.out.println(Arrays.toString(resultArray));

		String pathCompiledClassName = null;
		if (resultArray[0].toLowerCase().contains("build successful")) {
			// If the input stream returned a 'successful build' message, the CUT compiled correctly
			System.out.println("Compiled uploaded CUT successfully");
			File f = new File(CUTS_DIR + Constants.F_SEP + cut.getAlias());
			System.out.println(f.getAbsolutePath());
			final String compiledClassName = FilenameUtils.getBaseName(cut.getJavaFile()) + Constants.JAVA_CLASS_EXT;
			System.out.println(compiledClassName);
			LinkedList<File> matchingFiles = (LinkedList)FileUtils.listFiles(f, FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter());
			if (! matchingFiles.isEmpty())
				pathCompiledClassName = matchingFiles.get(0).getAbsolutePath();
		} else {
			// Otherwise the CUT failed to compile
			System.err.println("Failed to compile uploaded CUT");
			int index = resultArray[0].indexOf("javac");
			String message = resultArray[0];
			if (index >= 0){
				message = message.substring(message.indexOf("javac"));
			}
			System.err.println(message);
		}
		return pathCompiledClassName;
	}

	/**
	 * Compiles mutant
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
		System.out.println("Compilation result:");
		System.out.println(Arrays.toString(resultArray));

		Mutant newMutant = null;
		// If the input stream returned a 'successful build' message, the mutant compiled correctly
		if (resultArray[0].toLowerCase().contains("build successful")) {
			// Create and insert a new target execution recording successful compile, with no message to report, and return its ID
			// Locate .class file
			final String compiledClassName = cut.getBaseName() + JAVA_CLASS_EXT;
			LinkedList<File> matchingFiles = (LinkedList) FileUtils.listFiles(dir, FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter());
			assert (! matchingFiles.isEmpty()); // if compilation was successful, .class file must exist
			String cFile = matchingFiles.get(0).getAbsolutePath();
			int playerId = DatabaseAccess.getPlayerIdForMultiplayerGame(ownerId, gameID);
			newMutant = new Mutant(gameID, jFile, cFile, true, playerId);
			newMutant.insert();
			TargetExecution newExec = new TargetExecution(0, newMutant.getId(), TargetExecution.Target.COMPILE_MUTANT, "SUCCESS", null);
			newExec.insert();
		} else {
			// The mutant failed to compile
			// New target execution recording failed compile, providing the return messages from the ant javac task
			String message = resultArray[0].substring(resultArray[0].indexOf("[javac]")).replaceAll(Constants.DATA_DIR, "");
			int playerId = DatabaseAccess.getPlayerIdForMultiplayerGame(ownerId, gameID);
			newMutant = new Mutant(gameID, jFile, null, false, playerId);
			newMutant.insert();
			TargetExecution newExec = new TargetExecution(0, newMutant.getId(), TargetExecution.Target.COMPILE_MUTANT, "FAIL", message);
			newExec.insert();
		}
		return newMutant;
	}

	// same method as compileMutant but for puzzle objects
	public static PuzzleMutant compilePMutant(File mutantDir, String jFile, int puzzleID, StoryClass cut, int creatorId) {

		String[] resultArray = runAntTargetStory("compile-mutant", mutantDir.getAbsolutePath(), null, cut, "Test" + cut.getBaseName());
		System.out.println(Arrays.toString(resultArray));

		PuzzleMutant newMutant = null;

		if (resultArray[0].toLowerCase().contains("build successful")) {
			final String compiledClassName = FilenameUtils.getBaseName(jFile) + JAVA_CLASS_EXT;
			LinkedList<File> matchingFiles = (LinkedList) FileUtils.listFiles(mutantDir, FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter());
			assert (!matchingFiles.isEmpty());
			String cFile = matchingFiles.get(0).getAbsolutePath();
			newMutant = new PuzzleMutant(puzzleID, jFile, cFile, true, creatorId);
			newMutant.insert();
			TargetExecution newExec = new TargetExecution(0, newMutant.getMutantId(), TargetExecution.Target.COMPILE_MUTANT, "SUCCESS", null);
			newExec.insertStory();
		} else {
			String message = resultArray[0].substring(resultArray[0].indexOf("[javac]")).replaceAll(Constants.DATA_DIR, "");
			newMutant = new PuzzleMutant(puzzleID, jFile, null, false, creatorId);
			newMutant.insert();
			TargetExecution newExec = new TargetExecution(0, newMutant.getMutantId(), TargetExecution.Target.COMPILE_MUTANT, "FAIL", message);
			newExec.insertStory();
		}

		return newMutant;

	}

	/**
	 * Compiles test
	 * @param dir
	 * @param jFile
	 * @param gameID
	 * @param cut
	 * @param ownerId
	 * @return A {@link Test} object
	 */
	public static Test compileTest(File dir, String jFile, int gameID, GameClass cut, int ownerId) {
		//public static int compileTest(ServletContext context, Test t) {

		String[] resultArray = runAntTarget("compile-test", null, dir.getAbsolutePath(), cut, null);

		int playerId = DatabaseAccess.getPlayerIdForMultiplayerGame(ownerId, gameID);

		// If the input stream returned a 'successful build' message, the test compiled correctly
		if (resultArray[0].toLowerCase().contains("build successful")) {
			// Create and insert a new target execution recording successful compile, with no message to report, and return its ID
			// Locate .class file
			final String compiledClassName = FilenameUtils.getBaseName(jFile) + JAVA_CLASS_EXT;
			LinkedList<File> matchingFiles = (LinkedList) FileUtils.listFiles(dir, FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter());
			assert (! matchingFiles.isEmpty()); // if compilation was successful, .class file must exist
			String cFile = matchingFiles.get(0).getAbsolutePath();

			Test newTest = new Test(gameID, jFile, cFile, playerId);
			newTest.insert();
			TargetExecution newExec = new TargetExecution(newTest.getId(), 0, TargetExecution.Target.COMPILE_TEST, "SUCCESS", null);
			newExec.insert();
			return newTest;
		} else {
			// The test failed to compile
			// New target execution recording failed compile, providing the return messages from the ant javac task
			String message = resultArray[0].substring(resultArray[0].indexOf("[javac]")).replaceAll(Constants.DATA_DIR, "");
			Test newTest = new Test(gameID, jFile, null, playerId);
			newTest.insert();
			TargetExecution newExec = new TargetExecution(newTest.getId(), 0, TargetExecution.Target.COMPILE_TEST, "FAIL", message);
			newExec.insert();
			return newTest;
		}

	}

	// same as compileTest but for puzzle objects
	public static PuzzleTest compilePTest(File dir, String jFile, int puzzleId, StoryClass cut, int creatorId) {

		String[] resultArray = runAntTargetStory("compile-test", null, dir.getAbsolutePath(), cut, null);

		if (resultArray[0].toLowerCase().contains("build successful")) {

			final String compiledClassName = FilenameUtils.getBaseName(jFile) + JAVA_CLASS_EXT;
			LinkedList<File> matchingFiles = (LinkedList) FileUtils.listFiles(dir, FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter());
			assert (! matchingFiles.isEmpty());
			String cFile = matchingFiles.get(0).getAbsolutePath();
			PuzzleTest newTest = new PuzzleTest(puzzleId, jFile, cFile, creatorId);
			newTest.insert();
			TargetExecution newExec = new TargetExecution(newTest.getTestId(), 0, TargetExecution.Target.COMPILE_TEST, "SUCCESS", null);
			newExec.insertStory();
			return newTest;

		} else {

			String message = resultArray[0].substring(resultArray[0].indexOf("[javac]")).replaceAll(Constants.DATA_DIR, "");
			PuzzleTest newTest = new PuzzleTest(puzzleId, jFile, null, creatorId);
			newTest.insert();
			TargetExecution newExec = new TargetExecution(newTest.getTestId(), 0, TargetExecution.Target.COMPILE_TEST, "FAIL", message);
			newExec.insertStory();
			return newTest;

		}

	}

	/**
	 * Generates mutant classes using Major
	 * @param cut game class
	 */
	public static void generateMutantsFromCUT(final GameClass cut) {

		String[] resultArray = runAntTarget("mutant-gen-cut", null, null, cut, null);
	}

	public static void generatePMutantsFromCUT(final StoryClass cut, int pid) {

		String[] resultArray = runAntTargetStory("mutant-gen-cut", Constants.PUZZLE_MUTANTS_DIR, null, cut, null);

	}

	/**
	 * Generates tests using EvoSuite
	 * @param cut CUT filename
	 */
	public static void generateTestsFromCUT(final GameClass cut) {
		String[] resultArray = runAntTarget("test-gen-cut", null, null, cut, null);
	}

	/**
	 * Compiles and takes tests from file using TestMaker
	 * @param cut CUT filename
	 * @param pid puzzleID
	 */
	public static void generatePTestsFromCUT(final StoryClass cut, int pid) {

		String[] resultArray = runAntTargetStory("test-gen-cut", null, Constants.PUZZLE_TESTS_DIR, cut, "Test" + DatabaseAccess.getClassForPuzzle(pid).getClassName());

	}

	/**
	 * Compiles generated test suite
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
	 * @param target An Ant target
	 * @param mutantFile
	 * @param testDir
	 * @param cut Class
	 * @param testClassName
	 * @return String Array of length 4
	 * [0] : Input Stream for the process
	 * [1] : Error Stream for the process
	 * [2] : Any exceptions from running the process
	 * [3] : Message indicating which target was run, and with which files
	 */
	private static String[] runAntTarget(String target, String mutantFile, String testDir, GameClass cut, String testClassName) {
		String[] resultArray = new String[4];
		String isLog = "";
		String esLog = "";
		String exLog = "";
		String debug = "Running Ant Target: " + target + " with mFile: " + mutantFile + " and tFile: " + testDir;

		ProcessBuilder pb = new ProcessBuilder();
		Map env = pb.environment();

		String antHome = (String) env.get("ANT_HOME");
		if (antHome == null) {
			System.err.println("ANT_HOME undefined.");
			antHome = System.getProperty("ant.home", "/usr/local");
		}

		String command = antHome + "/bin/ant";

		if (System.getProperty("os.name").toLowerCase().contains("windows")){
			command += ".bat";
			command = command.replace("/", "\\").replace("\\", "\\\\");
		}

		pb.command(command, target, // "-v", "-d", for verbose, debug
				"-Dmutant.file=" + mutantFile,
				"-Dtest.file=" + testDir,
				"-Dcut.dir=" + CUTS_DIR + F_SEP + cut.getAlias(),
				"-Dclassalias=" + cut.getAlias(),
				"-Dclassbasename=" + cut.getBaseName(),
				"-Dclassname=" + cut.getName(),
				"-DtestClassname=" + testClassName);

		String buildFileDir = Constants.DATA_DIR;
		pb.directory(new File(buildFileDir));
		pb.redirectErrorStream(true);

		System.out.println("Executing Ant Command: " + pb.command().toString());
		System.out.println("Executing from directory: " + buildFileDir);
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

	/**
	 * Runs a specific Ant target in the build.xml file (For story mode)
	 *
	 * @param target An Ant target
	 * @param mutantFile is provided (should be /var/lib/codedefenders/puzzles/mutants)
	 * @param testDir is provided (should be /var/lib/codedefenders/puzzles/tests)
	 * @param cut Class
	 * @param testClassName - provided is the class name assuming the classname will be the same as the test class name
	 * @return String Array of length 4
	 * [0] : Input Stream for the process
	 * [1] : Error Stream for the process
	 * [2] : Any exceptions from running the process
	 * [3] : Message indicating which target was run, and with which files
	 */

	// same as runAntTarget but for puzzle objects
	private static String[] runAntTargetStory(String target, String mutantFile, String testDir, StoryClass cut, String testClassName) {
		String[] resultArray = new String[4];
		String isLog = "";
		String esLog = "";
		String exLog = "";
		String debug = "Running Ant Target: " + target + " with mFile: " + mutantFile + " and tFile: " + testDir;

		ProcessBuilder pb = new ProcessBuilder();
		Map env = pb.environment();



		String antHome = (String) env.get("ANT_HOME");
		if (antHome == null) {
			System.err.println("ANT_HOME undefined.");
			antHome = System.getProperty("ant.home", "/usr/local");
		}

		String command = antHome + "/bin/ant";

		if (System.getProperty("os.name").toLowerCase().contains("windows")){
			command += ".bat";
			command = command.replace("/", "\\").replace("\\", "\\\\");
		}

		pb.command(command, target, // "-v", "-d", for verbose, debug
				"-Dmutant.file=" + mutantFile,
				"-Dtest.file=" + testDir,
				"-Dcut.dir=" + CUTS_DIR + F_SEP + cut.getAlias(),
				"-Dclassalias=" + cut.getAlias(),
				"-Dclassbasename=" + cut.getBaseName(),
				"-Dclassname=" + cut.getClassName(),
				"-DtestClassname=" + testClassName);

		String buildFileDir = Constants.DATA_DIR;
		pb.directory(new File(buildFileDir));
		pb.redirectErrorStream(true);

		System.out.println("Executing Ant Command (Story): " + pb.command().toString());
		System.out.println("Executing from directory: " + buildFileDir);
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

	/**
	 * Executes a test against a mutant
	 * @param t A {@link Test} object
	 * @param c A {@link GameClass} object
	 * @return A {@link TargetExecution} object
	 */
	private static LineCoverage getLinesCovered(Test t, GameClass c) {
		CoverageGenerator cg = new CoverageGenerator(
				new File(t.getFolder()),
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

	private static LineCoverage getLinesCoveredStory(PuzzleTest t, StoryClass s) {

		CoverageGenerator cg = new CoverageGenerator(
				new File(Constants.PUZZLE_TESTS_DIR),
				new File(Constants.CUTS_DIR + F_SEP + s.getClassName()));

		try {
			cg.create(s.getClassName());
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

		if (System.getProperty("os.name").toLowerCase().contains("windows")){
			command += ".bat";
		}

		command.replace("\\", "\\\\");
			pb.command(command, "process-jacoco", // "-v", "-d", for verbose, debug
					"-Dsrc.dir=" + context.getRealPath(Constants.CUTS_DIR),
					"-Dtest.file=" + testFile);



		String buildFileDir = context.getRealPath(Constants.DATA_DIR);
		pb.directory(new File(buildFileDir));
		pb. redirectErrorStream(true);

		System.out.println("Executing Ant Command: " + pb.command().toString());
		System.out.println("Executing from directory: " + buildFileDir);
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
		System.out.println("is: " + isLog);
		System.out.println("es: " + esLog);
		System.out.println("ex: " + exLog);

		return resultArray;
	}

}
