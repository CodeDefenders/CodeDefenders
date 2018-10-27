/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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
