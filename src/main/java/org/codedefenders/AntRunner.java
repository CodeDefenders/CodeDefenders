package org.codedefenders;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

import static org.codedefenders.Constants.JAVA_CLASS_EXT;

/**
 * @author Jose Rojas
 */
public class AntRunner {

	private static final Logger logger = LoggerFactory.getLogger(AntRunner.class);

	/**
	 * Executes a test against a mutant
	 * @param context
	 * @param m A {@link Mutant} object
	 * @param t A {@link Test} object
	 * @return A {@link TargetExecution} object
	 */
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

	/**
	 * Executes a test against the original code
	 * @param context
	 * @param dir
	 * @param t A {@link Test} object
	 * @return A {@link TargetExecution} object
	 */
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
			String message = resultArray[0].substring(resultArray[0].indexOf("[junit]"));
			TargetExecution newExec = new TargetExecution(t.getId(), 0, TargetExecution.Target.TEST_ORIGINAL, "FAIL", message);
			newExec.insert();
			return newExec.id;
		}
	}

	/**
	 * Compiles CUT
	 * @param context
	 * @param className
	 * @return The Path to the compiled CUT
	 */
	public static String compileCUT(ServletContext context, final String className) {

		String[] resultArray = runAntTarget(context, "compile-cut", null, null, className, null);
		System.out.println("Compile New CUT, Compilation result:");
		System.out.println(Arrays.toString(resultArray));

		String pathCompiledClassName = null;
		if (resultArray[0].toLowerCase().contains("build successful")) {
			// If the input stream returned a 'successful build' message, the CUT compiled correctly
			System.out.println("Compiled uploaded CUT successfully");
			File f = new File(context.getRealPath(Constants.CUTS_DIR));
			final String compiledClassName = FilenameUtils.getBaseName(className) + Constants.JAVA_CLASS_EXT;
			LinkedList<File> matchingFiles = (LinkedList)FileUtils.listFiles(f, FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter());
			if (! matchingFiles.isEmpty())
				pathCompiledClassName = matchingFiles.get(0).getAbsolutePath();
		} else {
			// Otherwise the CUT failed to compile
			String message = resultArray[0].substring(resultArray[0].indexOf("[javac]"));
			System.err.println("Failed to compile uploaded CUT");
			System.err.println(message);
		}
		return pathCompiledClassName;
	}


	/**
	 * Compiles mutant
	 * @param context
	 * @param dir
	 * @param jFile
	 * @param gameID
	 * @param classMutated
	 * @return A {@link Mutant} object
	 */
	public static Mutant compileMutant(ServletContext context, File dir, String jFile, int gameID, GameClass classMutated, int ownerId) {
		//public static int compileMutant(ServletContext context, Mutant m2) {

		// Gets the classname for the mutant from the game it is in
		String[] resultArray = runAntTarget(context, "compile-mutant", dir.getAbsolutePath(), null, classMutated.getBaseName(), null);
		System.out.println("Compilation result:");
		System.out.println(Arrays.toString(resultArray));

		Mutant newMutant = null;
		// If the input stream returned a 'successful build' message, the mutant compiled correctly
		if (resultArray[0].toLowerCase().contains("build successful")) {
			// Create and insert a new target execution recording successful compile, with no message to report, and return its ID
			// Locate .class file
			final String compiledClassName = classMutated.getBaseName() + JAVA_CLASS_EXT;
			LinkedList<File> matchingFiles = (LinkedList) FileUtils.listFiles(dir, FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter());
			assert (! matchingFiles.isEmpty()); // if compilation was successful, .class file must exist
			String cFile = matchingFiles.get(0).getAbsolutePath();
			newMutant = new Mutant(gameID, jFile, cFile, true, ownerId);
			newMutant.insert();
			TargetExecution newExec = new TargetExecution(0, newMutant.getId(), TargetExecution.Target.COMPILE_MUTANT, "SUCCESS", null);
			newExec.insert();
		} else {
			// The mutant failed to compile
			// New target execution recording failed compile, providing the return messages from the ant javac task
			String message = resultArray[0].substring(resultArray[0].indexOf("[javac]")).replaceAll(context.getRealPath(Constants.DATA_DIR), "");
			newMutant = new Mutant(gameID, jFile, null, false, ownerId);
			newMutant.insert();
			TargetExecution newExec = new TargetExecution(0, newMutant.getId(), TargetExecution.Target.COMPILE_MUTANT, "FAIL", message);
			newExec.insert();
		}
		return newMutant;
	}

	/**
	 * Compiles test
	 * @param context
	 * @param dir
	 * @param jFile
	 * @param gameID
	 * @param classUnderTest
	 * @return A {@link Test} object
	 */
	public static Test compileTest(ServletContext context, File dir, String jFile, int gameID, GameClass classUnderTest, int ownerId) {
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
			Test newTest = new Test(gameID, jFile, cFile, ownerId);
			newTest.insert();
			TargetExecution newExec = new TargetExecution(newTest.getId(), 0, TargetExecution.Target.COMPILE_TEST, "SUCCESS", null);
			newExec.insert();
			return newTest;
		} else {
			// The test failed to compile
			// New target execution recording failed compile, providing the return messages from the ant javac task
			String message = resultArray[0].substring(resultArray[0].indexOf("[javac]")).replaceAll(context.getRealPath(Constants.DATA_DIR), "");
			Test newTest = new Test(gameID, jFile, null, ownerId);
			newTest.insert();
			TargetExecution newExec = new TargetExecution(newTest.getId(), 0, TargetExecution.Target.COMPILE_TEST, "FAIL", message);
			newExec.insert();
			return newTest;
		}
	}

	/**
	 * Generates mutant classes using Major
	 * @param context
	 * @param className CUT filename
	 */
	public static void generateMutantsFromCUT(ServletContext context, final String className) {
		String[] resultArray = runAntTarget(context, "mutant-gen-cut", null, null, className, null);
	}

	/**
	 * Generates tests using EvoSuite
	 * @param context
	 * @param className CUT filename
	 */
	public static void generateTestsFromCUT(ServletContext context, final String className) {
		String[] resultArray = runAntTarget(context, "test-gen-cut", null, null, className, null);
	}

	/**
	 * Compiles generated test suite
	 * @param context
	 * @param dir
	 * @param jFile
	 * @param gameID
	 * @param classUnderTest
	 * @return A {@link Test} object
	 */
	public static void compileGeneratedTests(ServletContext context, final String className) {
		String[] resultArray = runAntTarget(context, "compile-gen-test", null, null, className, null);
	}

		/**
		 * Runs a specific Ant target in the build.xml file
		 *
		 * @param context
		 * @param target An Ant target
		 * @param mutantFile
		 * @param testFile
		 * @param className Class
		 * @param testClassName
		 * @return String Array of length 4
		 * [0] : Input Stream for the process
		 * [1] : Error Stream for the process
		 * [2] : Any exceptions from running the process
		 * [3] : Message indicating which target was run, and with which files
		 */
	private static String[] runAntTarget(ServletContext context, String target, String mutantFile, String testFile, String className, String testClassName) {
		String[] resultArray = new String[4];
		String isLog = "";
		String esLog = "";
		String exLog = "";
		String debug = "Running Ant Target: " + target + " with mFile: " + mutantFile + " and tFile: " + testFile;

		ProcessBuilder pb = new ProcessBuilder();
		Map env = pb.environment();

		String antHome = (String) env.get("ANT_HOME");
		if (antHome == null) {
			System.err.println("ANT_HOME undefined.");
			antHome = System.getProperty("ant.home", "/usr/local");
		}

		pb.command(antHome + "/bin/ant", target, // "-v", "-d", for verbose, debug
				"-Dsrc.dir=" + context.getRealPath(Constants.CUTS_DIR),
				"-Dmutant.file=" + mutantFile,
				"-Dtest.file=" + testFile,
				"-Dclassname=" + className,
				"-DtestClassname=" + testClassName);

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
		resultArray[3] = debug;
		System.out.println("is: " + isLog);
		System.out.println("es: " + esLog);
		System.out.println("ex: " + exLog);
		System.out.println("lg :" + debug);

		return resultArray;
	}

}
