package org.codedefenders;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.codedefenders.multiplayer.CoverageGenerator;
import org.codedefenders.multiplayer.LineCoverage;
import org.codedefenders.util.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Map;

import static org.codedefenders.Constants.*;

/**
 * @author Jose Rojas
 */
public class AntRunner {

	private static final Logger logger = LoggerFactory.getLogger(AntRunner.class);

	public static boolean testKillsMutant(Mutant m, Test t) {
		GameClass cut = DatabaseAccess.getClassForGame(m.getGameId());

		AntProcessResult result = runAntTarget("test-mutant", m.getDirectory(), t.getDirectory(), cut, t.getFullyQualifiedClassName());

		// Return true iff test failed
		return result.hasFailure();
	}

	/**
	 * Executes a test against a mutant
	 * @param m A {@link Mutant} object
	 * @param t A {@link Test} object
	 * @return A {@link TargetExecution} object
	 */
	static TargetExecution testMutant(Mutant m, Test t) {
		logger.debug("Running test {} on mutant {}", t.getId(), m.getId());
		GameClass cut = DatabaseAccess.getClassForGame(m.getGameId());

		AntProcessResult result = runAntTarget("test-mutant", m.getDirectory(), t.getDirectory(), cut, t.getFullyQualifiedClassName());

		TargetExecution newExec;

		if (result.hasFailure()) {
			// The test failed, i.e., it detected the mutant
			newExec = new TargetExecution(t.getId(), m.getId(), TargetExecution.Target.TEST_MUTANT, "FAIL", null);
		} else if (result.hasError()) {
			// The test is in error, interpreted also as detecting the mutant
			String message = result.getErrorMessage();
			newExec = new TargetExecution(t.getId(), m.getId(), TargetExecution.Target.TEST_MUTANT, "ERROR", message);
		} else {
			// The test passed, i.e., it did not detect the mutant
			newExec = new TargetExecution(t.getId(), m.getId(), TargetExecution.Target.TEST_MUTANT, "SUCCESS", null);
		}
		newExec.insert();
		return newExec;
	}

	static boolean potentialEquivalent(Mutant m) {
		logger.debug("Checking if mutant {} is potentially equivalent.", m.getId());
		GameClass cut = DatabaseAccess.getClassForGame(m.getGameId());
		String suiteDir = AI_DIR + F_SEP + "tests" + F_SEP + cut.getAlias();

		// TODO: is this actually executing a whole test suite?
		AntProcessResult result = runAntTarget("test-mutant", m.getDirectory(), suiteDir, cut, cut.getName() + Constants.SUITE_EXT);

		// return true if tests pass without failures or errors
		return !(result.hasError() || result.hasFailure());
	}

	/**
	 * Executes a test against the original code
	 * @param dir Test directory
	 * @param t A {@link Test} object
	 * @return A {@link TargetExecution} object
	 */
	public static TargetExecution testOriginal(File dir, Test t) {
		GameClass cut = DatabaseAccess.getClassForGame(t.getGameId());

		AntProcessResult result = runAntTarget("test-original", null, dir.getAbsolutePath(), cut, t.getFullyQualifiedClassName());

		// add coverage information
		t.setLineCoverage(getLinesCovered(t, cut));
		t.update();

		// record test execution
		String status;
		String message;
		if (result.hasFailure()) {
			status = "FAIL";
			message = result.getJUnitMessage();
		} else if (result.hasError()) {
			status = "ERROR";
			message = result.getErrorMessage();
		} else {
			status = "SUCCESS";
			message = null;
		}
		TargetExecution testExecution = new TargetExecution(t.getId(), 0, TargetExecution.Target.TEST_ORIGINAL, status, message);
		testExecution.insert();
		return testExecution;
	}

	/**
	 * Compiles CUT
	 *
	 * @param cut Class under test
	 * @return The path to the compiled CUT
	 */
	static String compileCUT(GameClass cut) {

		AntProcessResult result = runAntTarget("compile-cut", null, null, cut, null);
		logger.info("Compile New CUT, Compilation result: {}", result);


		String pathCompiledClassName = null;
		if (result.compiled()) {
			// If the input stream returned a 'successful build' message, the CUT compiled correctly
			logger.info("Compiled uploaded CUT successfully");
			File f = new File(CUTS_DIR + Constants.F_SEP + cut.getAlias());
			final String compiledClassName = FilenameUtils.getBaseName(cut.getJavaFile()) + Constants.JAVA_CLASS_EXT;
			LinkedList<File> matchingFiles = (LinkedList<File>)FileUtils.listFiles(f, FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter());
			if (! matchingFiles.isEmpty())
				pathCompiledClassName = matchingFiles.get(0).getAbsolutePath();
		} else {
			// Otherwise the CUT failed to compile
			logger.error("Failed to compile uploaded CUT: {}", result.getCompilerOutput());
		}
		return pathCompiledClassName;
	}


	/**
	 * Compiles mutant
	 * @param dir Mutant directory
	 * @param jFile Java source file
	 * @param gameID Game identifier
	 * @param cut Class under test
	 * @param ownerId User who submitted mutant
	 * @return A {@link Mutant} object
	 */
	public static Mutant compileMutant(File dir, String jFile, int gameID, GameClass cut, int ownerId) {

		// Gets the classname for the mutant from the game it is in
		AntProcessResult result = runAntTarget("compile-mutant", dir.getAbsolutePath(), null, cut, null);
		logger.info("Compilation result: {}", result);

		Mutant newMutant;
		// If the input stream returned a 'successful build' message, the mutant compiled correctly
		if (result.compiled()) {
			// Create and insert a new target execution recording successful compile, with no message to report, and return its ID
			// Locate .class file
			final String compiledClassName = cut.getBaseName() + JAVA_CLASS_EXT;
			LinkedList<File> matchingFiles = (LinkedList<File>) FileUtils.listFiles(dir, FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter());
			assert (! matchingFiles.isEmpty()): "if compilation was successful, .class file must exist";
			String cFile = matchingFiles.get(0).getAbsolutePath();
			int playerId = DatabaseAccess.getPlayerIdForMultiplayerGame(ownerId, gameID);
			newMutant = new Mutant(gameID, jFile, cFile, true, playerId);
			newMutant.insert();
			TargetExecution newExec = new TargetExecution(0, newMutant.getId(), TargetExecution.Target.COMPILE_MUTANT, "SUCCESS", null);
			newExec.insert();
		} else {
			// The mutant failed to compile
			// New target execution recording failed compile, providing the return messages from the ant javac task
			String message = result.getCompilerOutput();
			logger.error("Failed to compile mutant {}: {}", jFile, message);
			int playerId = DatabaseAccess.getPlayerIdForMultiplayerGame(ownerId, gameID);
			newMutant = new Mutant(gameID, jFile, null, false, playerId);
			newMutant.insert();
			TargetExecution newExec = new TargetExecution(0, newMutant.getId(), TargetExecution.Target.COMPILE_MUTANT, "FAIL", message);
			newExec.insert();
		}
		return newMutant;
	}

	/**
	 * Compiles test
	 * @param dir Test directory
	 * @param jFile Java source file
	 * @param gameID Game identifier
	 * @param cut Class under test
	 * @param ownerId Player who submitted test
	 * @return A {@link Test} object
	 */
	public static Test compileTest(File dir, String jFile, int gameID, GameClass cut, int ownerId) {
		//public static int compileTest(ServletContext context, Test t) {

		AntProcessResult result = runAntTarget("compile-test", null, dir.getAbsolutePath(), cut, null);

		int playerId = DatabaseAccess.getPlayerIdForMultiplayerGame(ownerId, gameID);

		// If the input stream returned a 'successful build' message, the test compiled correctly
		if (result.compiled()) {
			// Create and insert a new target execution recording successful compile, with no message to report, and return its ID
			// Locate .class file
			final String compiledClassName = FilenameUtils.getBaseName(jFile) + JAVA_CLASS_EXT;
			LinkedList<File> matchingFiles = (LinkedList<File>) FileUtils.listFiles(dir, FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter());
			assert (! matchingFiles.isEmpty()); // if compilation was successful, .class file must exist
			String cFile = matchingFiles.get(0).getAbsolutePath();
			logger.error("Compiled test {}", compiledClassName);
			Test newTest = new Test(gameID, jFile, cFile, playerId);
			newTest.insert();
			TargetExecution newExec = new TargetExecution(newTest.getId(), 0, TargetExecution.Target.COMPILE_TEST, "SUCCESS", null);
			newExec.insert();
			return newTest;
		} else {
			// The test failed to compile
			// New target execution recording failed compile, providing the return messages from the ant javac task
			String message = result.getCompilerOutput();
			logger.error("Failed to compile test {}: {}", jFile, message);
			Test newTest = new Test(gameID, jFile, null, playerId);
			newTest.insert();
			TargetExecution newExec = new TargetExecution(newTest.getId(), 0, TargetExecution.Target.COMPILE_TEST, "FAIL", message);
			newExec.insert();
			return newTest;
		}
	}

	/**
	 * Generates mutant classes using Major
	 * @param cut game class
	 */
	public static void generateMutantsFromCUT(final GameClass cut) {
		runAntTarget("mutant-gen-cut", null, null, cut, null);
	}

	/**
	 * Generates tests using EvoSuite
	 * @param cut CUT filename
	 */
	public static void generateTestsFromCUT(final GameClass cut) {
		runAntTarget("test-gen-cut", null, null, cut, null);
	}

	/**
	 * Compiles generated test suite
	 * @param cut Class under test
	 */
	public static boolean compileGenTestSuite(final GameClass cut) {
		AntProcessResult result = runAntTarget("compile-gen-tests", null, null, cut, cut.getName() + Constants.SUITE_EXT);

		// Return true iff compilation succeeds
		return result.compiled();
	}

	/**
	 * Runs a specific Ant target in the build.xml file
	 *
	 * @param target An Ant target
	 * @param mutantFile Mutant Java source file
	 * @param testDir Test directory
	 * @param cut Class under test
	 * @param testClassName Name of JUnit test class
	 * @return Result an AntProcessResult object containing output details of the ant process
	 */
	private static AntProcessResult runAntTarget(String target, String mutantFile, String testDir, GameClass cut, String testClassName) {
		logger.debug("Running Ant Target: {} with mFile: {} and tFile: {}", target, mutantFile, testDir);

		ProcessBuilder pb = new ProcessBuilder();
		Map env = pb.environment();



		String antHome = (String) env.get("ANT_HOME");
		if (antHome == null) {
			logger.error("ANT_HOME undefined.");
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

		logger.debug("Executing Ant Command {} from directory {}", pb.command().toString(), buildFileDir);

		return runAntProcess(pb);
	}

	/**
	 * Executes a test against a mutant
	 * @param t A {@link Test} object
	 * @param c A {@link GameClass} object
	 * @return A {@link TargetExecution} object
	 */
	private static LineCoverage getLinesCovered(Test t, GameClass c) {
		CoverageGenerator cg = new CoverageGenerator(
				new File(t.getDirectory()),
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

	private static AntProcessResult runAntProcess(ProcessBuilder pb) {
		AntProcessResult res = new AntProcessResult();
		try {
			Process p = pb.start();

			BufferedReader is = new BufferedReader(new InputStreamReader(p.getInputStream()));
			res.setInputStream(is);

			String line;
			BufferedReader es = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			StringBuilder esLog = new StringBuilder();
			while ((line = es.readLine()) != null) {
				esLog.append(line).append(System.lineSeparator());
			}
			res.setErrorStreamText(esLog.toString());
		} catch (Exception ex) {
			res.setExceptionText(String.format("Exception: %s%s", ex.toString(), System.lineSeparator()));
		}
		return res;
	}

}
