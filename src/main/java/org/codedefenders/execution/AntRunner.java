/*
 * Copyright (C) 2016-2019 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.execution;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import static org.codedefenders.util.Constants.AI_DIR;
import static org.codedefenders.util.Constants.CUTS_DEPENDENCY_DIR;
import static org.codedefenders.util.Constants.CUTS_DIR;
import static org.codedefenders.util.Constants.F_SEP;
import static org.codedefenders.util.Constants.JAVA_CLASS_EXT;

/**
 * @author Jose Rojas
 * @author Alessio Gambi (last edit)
 */
public class AntRunner {

	private static final Logger logger = LoggerFactory.getLogger(AntRunner.class);
	//
	private static String antHome = null;
	private static boolean clusterEnabled = false;
	private static boolean forceLocalExecution = false;
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
			NamingEnumeration<NameClassPair> list = initialContext.list("java:comp/env");
			Context environmentContext = (Context) initialContext.lookup("java:comp/env");

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
					case "forceLocalExecution":
						forceLocalExecution = "enabled".equalsIgnoreCase((String) environmentContext.lookup(name));
						break;
				}
			}

		} catch (NamingException e) {
			e.printStackTrace();
		}

		// Check Env
		if (antHome == null) {
			ProcessBuilder pb = new ProcessBuilder();
			Map env = pb.environment();
			antHome = (String) env.get("ANT_HOME");
		}


	}
	/////

	public static boolean testKillsMutant(Mutant m, Test t) {
		GameClass cut = GameClassDAO.getClassForGameId(m.getGameId());

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
	@SuppressWarnings("Duplicates")
	static TargetExecution testMutant(Mutant m, Test t) {
		logger.info("Running test {} on mutant {}", t.getId(), m.getId());
		GameClass cut = GameClassDAO.getClassForGameId(m.getGameId());
		if( cut == null ){
		    cut = GameClassDAO.getClassForId(m.getClassId());
		}

		// Check if this mutant requires a test recompilation
		AntProcessResult result = null;
		if( m.doesRequireRecompilation() ){
			result = runAntTarget("recompiled-test-mutant", m.getDirectory(), t.getDirectory(), cut, t.getFullyQualifiedClassName());
		} else {
			result = runAntTarget("test-mutant", m.getDirectory(), t.getDirectory(), cut, t.getFullyQualifiedClassName());
		}

		TargetExecution newExec;

		if (result.hasFailure()) {
			// The test failed, i.e., it detected the mutant
			newExec = new TargetExecution(t.getId(), m.getId(), TargetExecution.Target.TEST_MUTANT, TargetExecution.Status.FAIL, null);
		} else if (result.hasError()) {
			// The test is in error, interpreted also as detecting the mutant
			String message = result.getErrorMessage();
			newExec = new TargetExecution(t.getId(), m.getId(), TargetExecution.Target.TEST_MUTANT, TargetExecution.Status.ERROR, message);
		} else {
			// The test passed, i.e., it did not detect the mutant
			newExec = new TargetExecution(t.getId(), m.getId(), TargetExecution.Target.TEST_MUTANT, TargetExecution.Status.SUCCESS, null);
		}
		newExec.insert();
		return newExec;
	}

	@SuppressWarnings("Duplicates")
	static TargetExecution recompileTestAndTestMutant(Mutant m, Test t) {
		logger.info("Running test {} on mutant {}", t.getId(), m.getId());
		GameClass cut = GameClassDAO.getClassForGameId(m.getGameId());

		AntProcessResult result = runAntTarget("recompile-test-mutant", m.getDirectory(), t.getDirectory(), cut, t.getFullyQualifiedClassName());

		TargetExecution newExec;

		if (result.hasFailure()) {
			// The test failed, i.e., it detected the mutant
			newExec = new TargetExecution(t.getId(), m.getId(), TargetExecution.Target.TEST_MUTANT, TargetExecution.Status.FAIL, null);
		} else if (result.hasError()) {
			// The test is in error, interpreted also as detecting the mutant
			String message = result.getErrorMessage();
			newExec = new TargetExecution(t.getId(), m.getId(), TargetExecution.Target.TEST_MUTANT, TargetExecution.Status.ERROR, message);
		} else {
			// The test passed, i.e., it did not detect the mutant
			newExec = new TargetExecution(t.getId(), m.getId(), TargetExecution.Target.TEST_MUTANT, TargetExecution.Status.SUCCESS, null);
		}
		newExec.insert();
		return newExec;
	}

	public static boolean potentialEquivalent(Mutant m) {
		logger.info("Checking if mutant {} is potentially equivalent.", m.getId());
		GameClass cut = GameClassDAO.getClassForGameId(m.getGameId());
		String suiteDir = Paths.get(AI_DIR, "tests", cut.getAlias()).toString();

		// TODO: is this actually executing a whole test suite?
		AntProcessResult result = runAntTarget("test-mutant", m.getDirectory(), suiteDir, cut, cut.getName() + Constants.SUITE_EXT);

		// return true if tests pass without failures or errors
		return !(result.hasError() || result.hasFailure());
	}

	public static void testOriginal(GameClass cut, String testDir, String testClassName) throws Exception {
		AntProcessResult result = runAntTarget("test-original", null, testDir, cut, testClassName, forceLocalExecution);

		if (result.hasFailure() || result.hasError()) {
			logger.error("Test {} failed to run against class under test", testClassName);
			throw new Exception("Test failed to run against class under test.");
		} else {
		    logger.info("Successfully tested original ");
		}
	}

	/**
	 * Executes a test against the original code
	 * @param dir Test directory
	 * @param t A {@link Test} object
	 * @return A {@link TargetExecution} object
	 */
	public static TargetExecution testOriginal(File dir, Test t) {
		GameClass cut = GameClassDAO.getClassForGameId(t.getGameId());

		AntProcessResult result = runAntTarget("test-original", null, dir.getAbsolutePath(), cut, t.getFullyQualifiedClassName(), forceLocalExecution);

		// add coverage information
		final LineCoverage coverage = LineCoverageGenerator.generate(cut, Paths.get(t.getJavaFile()));
		t.setLineCoverage(coverage);
		t.update();

		// record test execution
		TargetExecution.Status status;
		String message;
		if (result.hasFailure()) {
			status = TargetExecution.Status.FAIL;
			message = result.getJUnitMessage();
		} else if (result.hasError()) {
			status = TargetExecution.Status.ERROR;
			message = result.getErrorMessage();
		} else {
			status = TargetExecution.Status.SUCCESS;
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
	public static String compileCUT(GameClass cut) throws CompileException {
		AntProcessResult result = runAntTarget("compile-cut", null, null, cut, null, forceLocalExecution);

		logger.info("Compile New CUT, Compilation result: {}", result);


		String pathCompiledClassName = null;
		if (result.compiled()) {
			// If the input stream returned a 'successful build' message, the CUT compiled correctly
			logger.info("Compiled uploaded CUT successfully");
			File f = Paths.get(CUTS_DIR, cut.getAlias()).toFile();
			final String compiledClassName = FilenameUtils.getBaseName(cut.getJavaFile()) + Constants.JAVA_CLASS_EXT;
			LinkedList<File> matchingFiles = (LinkedList<File>)FileUtils.listFiles(f, FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter());
			if (! matchingFiles.isEmpty())
				pathCompiledClassName = matchingFiles.get(0).getAbsolutePath();
		} else {
			// Otherwise the CUT failed to compile
			String message = result.getCompilerOutput();
			logger.error("Failed to compile uploaded CUT: {}", message);
			throw new CompileException(message);
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
		AntProcessResult result = runAntTarget("compile-mutant", dir.getAbsolutePath(), null, cut, null, forceLocalExecution);

		logger.info("Compilation result: {}", result);

		Mutant newMutant;
		// If the input stream returned a 'successful build' message, the mutant compiled correctly
		if (result.compiled()) {
			// Create and insert a new target execution recording successful compile, with no message to report, and return its ID
			// Locate .class file
			final String compiledClassName = cut.getBaseName() + JAVA_CLASS_EXT;
			final LinkedList<File> matchingFiles = new LinkedList<>(FileUtils.listFiles(dir, FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter()));
			assert (! matchingFiles.isEmpty()): "if compilation was successful, .class file must exist";
			String cFile = matchingFiles.get(0).getAbsolutePath();
			int playerId = DatabaseAccess.getPlayerIdForMultiplayerGame(ownerId, gameID);
			newMutant = new Mutant(gameID, cut.getId(), jFile, cFile, true, playerId);
			newMutant.insert();
			TargetExecution newExec = new TargetExecution(0, newMutant.getId(), TargetExecution.Target.COMPILE_MUTANT, TargetExecution.Status.SUCCESS, null);
			newExec.insert();
		} else {
			// The mutant failed to compile
			// New target execution recording failed compile, providing the return messages from the ant javac task
			String message = result.getCompilerOutput();
			logger.error("Failed to compile mutant {}: {}", jFile, message);
			int playerId = DatabaseAccess.getPlayerIdForMultiplayerGame(ownerId, gameID);
			newMutant = new Mutant(gameID, cut.getId(), jFile, null, false, playerId);
			newMutant.insert();
			TargetExecution newExec = new TargetExecution(0, newMutant.getId(), TargetExecution.Target.COMPILE_MUTANT, TargetExecution.Status.FAIL, message);
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

		AntProcessResult result = runAntTarget("compile-test", null, dir.getAbsolutePath(), cut, null, forceLocalExecution);

		int playerId = DatabaseAccess.getPlayerIdForMultiplayerGame(ownerId, gameID);

		// If the input stream returned a 'successful build' message, the test compiled correctly
		if (result.compiled()) {
			// Create and insert a new target execution recording successful compile, with no message to report, and return its ID
			// Locate .class file
			final String compiledClassName = FilenameUtils.getBaseName(jFile) + JAVA_CLASS_EXT;
			final List<File> matchingFiles = new LinkedList<>(FileUtils.listFiles(dir, FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter()));
			assert (! matchingFiles.isEmpty()); // if compilation was successful, .class file must exist
			String cFile = matchingFiles.get(0).getAbsolutePath();
			logger.info("Compiled test {}", compiledClassName);
			Test newTest = new Test(cut.getId(), gameID, jFile, cFile, playerId);
			boolean inserted = newTest.insert();
			assert ( inserted ); // if compilation was successful, .class file must exist
			TargetExecution newExec = new TargetExecution(newTest.getId(), 0, TargetExecution.Target.COMPILE_TEST, TargetExecution.Status.SUCCESS, null);
			newExec.insert();
			return newTest;
		} else {
			// The test failed to compile
			// New target execution recording failed compile, providing the return messages from the ant javac task
			String message = result.getCompilerOutput();
			logger.error("Failed to compile test {}: {}", jFile, message);
			Test newTest = new Test(cut.getId(), gameID, jFile, null, playerId);
			newTest.insert();
			TargetExecution newExec = new TargetExecution(newTest.getId(), 0, TargetExecution.Target.COMPILE_TEST, TargetExecution.Status.FAIL, message);
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
		return runAntTarget(target, mutantFile, testDir, cut, testClassName, false);
	}
	
	private static AntProcessResult runAntTarget(String target, String mutantDir, String testDir, GameClass cut, String testClassName, boolean forcedLocally) {
		logger.info("Running Ant Target: {} with mFile: {} and tFile: {}", target, mutantDir, testDir);

		ProcessBuilder pb = new ProcessBuilder();
		Map<String, String> env = pb.environment();
		List<String> command = new ArrayList<>();
		String cutDir = Paths.get(cut.getJavaFile()).getParent().toString();

		/*
		 * Clustered execution uses almost the same command than normal
		 * execution. But it prefixes that with "srun". This assumes that the
		 * code-defender working dir is on the NFS.
		 */

		if (clusterEnabled && !forcedLocally) {
			logger.info("Clustered Execution");
			if (clusterJavaHome != null) {
				env.put("JAVA_HOME", clusterJavaHome);
			}

			env.put("CLASSPATH", Constants.TEST_CLASSPATH);
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
			logger.debug("Local Execution");
			env.put("CLASSPATH", "lib/hamcrest-all-1.3.jar"+File.pathSeparator+"lib/junit-4.12.jar"+File.pathSeparator+"lib/mockito-all-1.9.5.jar");

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

		command.add("-Dmutant.file=" + mutantDir);
		command.add("-Dtest.file=" + testDir);
		command.add("-Dcut.dir=" + cutDir);
		command.add("-Dclassalias=" + cut.getAlias());
		command.add("-Dclassbasename=" + cut.getBaseName());
		command.add("-Dclassname=" + cut.getName());
		command.add("-DtestClassname=" + testClassName);
		command.add("-Dcuts.deps=" + cutDir + F_SEP + CUTS_DEPENDENCY_DIR);

		if (mutantDir != null && testDir != null) {
			String separator = F_SEP;
			if (separator.equals("\\")){
				separator = "\\\\";
			}
			String[] tokens = mutantDir.split(separator);
			String mutantFile = String.format("%s-%s", tokens[tokens.length - 2], tokens[tokens.length - 1]);
			String testMutantFile = testDir.replace("original", mutantFile);
			// TODO This might need refactoring
			File testMutantFileDir = new File(testMutantFile);
			if(!testMutantFileDir.exists()){
				testMutantFileDir.mkdirs();
			}
			//
			command.add("-Dmutant.test.file=" + testMutantFile);
		}
		// Execute whichever command was build
		pb.command(command);

		String buildFileDir = Constants.DATA_DIR;
		pb.directory(new File(buildFileDir));
		pb.redirectErrorStream(true);

		logger.info("Executing Ant Command {} from directory {}", pb.command().toString(), buildFileDir);

		return runAntProcess(pb);
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
