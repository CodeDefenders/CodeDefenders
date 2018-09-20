package org.codedefenders.execution;

import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * This class offers static methods to execute Java code using the java {@link Runtime}.
 *
 * @see AntRunner
 * @author <a href=https://github.com/werli>Phil Werli<a/>
 */
public class Runner {
    private static Logger logger = LoggerFactory.getLogger(Runner.class);

    /**
     * Executes a given test against a class (and if specified its dependencies).
     *
     *
     * @param javaTestFilePath the file path to the test class.
     * @param javaCutFilePath the file path to the
     * @throws Exception
     */
    public static void runTestAgainstClass(String javaTestFilePath, String javaCutFilePath) throws Exception {
        String testName = javaTestFilePath.substring(javaTestFilePath.lastIndexOf("/") + 1, javaTestFilePath.lastIndexOf("."));
        String cutDir = javaCutFilePath.substring(0, javaCutFilePath.lastIndexOf("/"));
        String testDir = javaTestFilePath.substring(0, javaTestFilePath.lastIndexOf("/"));

        String classPath = String.format(Constants.TEST_CLASSPATH_WITH_2DIR, cutDir, testDir);

        // java -classpath <classpath> org.junit.runner.JUnitCore <Name of test class>
        String[] commands = {
                "java",
                "-classpath", classPath,
                "org.junit.runner.JUnitCore",
                testName
        };

        final Runtime runtime = Runtime.getRuntime();
        try {
            final Process exec = runtime.exec(commands);

            if (exec.waitFor() == 0) {
                logger.info("Successfully ran {} against class", testName);
            } else {
                final InputStream inputStream = exec.getInputStream();
                final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                final BufferedReader errorReader = new BufferedReader(new InputStreamReader(exec.getErrorStream()));
                final String result = reader.lines().collect(Collectors.joining("\n"));
                final String error = errorReader.lines().collect(Collectors.joining("\n"));
                logger.warn("Java Program exited with exit code other than zero.\n{}\n{}", result, error);
                throw new Exception("Java Program exited with exit code other than zero.\n" + result + "\n" + error);
            }
        } catch (IOException | InterruptedException e) {
            logger.warn("Failed to execute Java program", e);
            throw new Exception("Failed to execute Java program");
        }
    }
}
