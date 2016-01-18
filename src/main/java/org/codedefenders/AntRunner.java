package org.codedefenders;

import javax.servlet.ServletContext;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * @author Jose Rojas
 */
public class AntRunner {

	// RUN ANT TARGET: Runs a specific Ant Target in the build.xml file

	// Inputs: The target to run, any file locations necessary for that target, and the name of the relevant class
	// Outputs: String Array of length 4
	//    [0] : Input Stream for the process
	//    [1] : Error Stream for the process
	//    [2] : Any exceptions from running the process
	//    [3] : Message indicating which target was run, and with which files

	public static String[] runAntTarget(ServletContext context, String target, String mutantFile, String testFile, String className, String testClassName) {
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


	public static String[] compileClass(ServletContext context, String className) {
		String[] resultArray = new String[4];
		String isLog = "";
		String esLog = "";
		String exLog = "";
		String debug = "Compiling Class Under Test with Ant: " + className;

		ProcessBuilder pb = new ProcessBuilder();
		Map env = pb.environment();

		String antHome = (String) env.get("ANT_HOME");
		if (antHome == null) {
			System.err.println("ANT_HOME undefined.");
			antHome = System.getProperty("ant.home", "/usr");
		}

		pb.command(antHome + "/bin/ant", "compile-cut",
				"-Dsrc.dir=" + context.getRealPath(Constants.CUTS_DIR),
				"-Dclassname=" + className);

		String buildFileDir = context.getRealPath(Constants.DATA_DIR);
		pb.directory(new File(buildFileDir));
		pb.redirectErrorStream(true);

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
