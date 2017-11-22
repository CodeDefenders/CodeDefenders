package org.codedefenders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Jose Rojas
 */
public class AntProcessResult {

	private static final Logger logger = LoggerFactory.getLogger(AntProcessResult.class);

	private String inputStreamText = "";
	private String errorStreamText = "";
	private String exceptionText = "";
	private String compilerOutput = "";
	private String testOutput = "";
	private boolean compiled;
	private boolean hasFailure;
	private boolean hasError;

	void setInputStream(BufferedReader reader) {
		StringBuilder isLog = new StringBuilder();
		StringBuilder compilerOutputBuilder = new StringBuilder();
		final String COMPILER_PREFIX = "[javac] ";

		StringBuilder testOutputBuilder = new StringBuilder();
		final String TEST_PREFIX = "[junit] ";
		final String JUNIT_RESULT_REGEX = "Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+), Time elapsed: ((\\d+)(\\.\\d+)?) sec";

		String line;
		try {
			Pattern pattern = Pattern.compile(JUNIT_RESULT_REGEX, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				isLog.append(line).append(System.lineSeparator());

				if (line.startsWith(COMPILER_PREFIX)) {
					compilerOutputBuilder.append(line).append(System.lineSeparator());
				} else if (line.startsWith(TEST_PREFIX)) {
					testOutputBuilder.append(line).append(System.lineSeparator());
					Matcher m = pattern.matcher(line);
					if (m.find()) {
						hasFailure = Integer.parseInt(m.group(2)) > 0;
						hasError = Integer.parseInt(m.group(3)) > 0;
					}
				} else if (line.equalsIgnoreCase("BUILD SUCCESSFUL"))
					compiled = true;
			}
			compilerOutput = compilerOutputBuilder.toString();
			testOutput = testOutputBuilder.toString();
			inputStreamText = isLog.toString();
		} catch (IOException e) {
			logger.error("Error while reading input stream", e);
		}
	}

	void setErrorStreamText(String errorStreamText) {
		this.errorStreamText = errorStreamText;
	}

	void setExceptionText(String exceptionText) {
		this.exceptionText = exceptionText;
	}

	boolean compiled() {
		return compiled;
	}

	boolean hasFailure() {
		return hasFailure;
	}

	boolean hasError() {
		return hasError;
	}

	String getCompilerOutput() {
		return compilerOutput;
	}

	String getJUnitMessage() {
		return testOutput;
	}

	String getErrorMessage() {
		return inputStreamText + " " + errorStreamText + " " + exceptionText;
	}

	@Override
	public String toString() {
		return "AntProcessResult{" +
				"inputStreamText='" + inputStreamText + '\'' +
				", errorStreamText='" + errorStreamText + '\'' +
				", exceptionText='" + exceptionText + '\'' +
				'}';
	}

}
