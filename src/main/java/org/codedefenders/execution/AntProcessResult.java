/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        final String JUNIT_RESULT_REGEX = "Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+), Time elapsed: ((\\d+)(([.,])\\d+)?) sec";
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
                } else if (line.equalsIgnoreCase("BUILD SUCCESSFUL")) {
                    compiled = true;
                }
            }
            compilerOutput = sanitize(compilerOutputBuilder.toString());
            testOutput = reduce(testOutputBuilder.toString());
            inputStreamText = isLog.toString();
        } catch (IOException e) {
            logger.error("Error while reading input stream", e);
        }
    }

    private String reduce(String fullTestOutput) {
        // Remove redundant content.
        StringBuilder reduced = new StringBuilder();
        // Skip all the lines until: [junit] Testcase: test took
        Iterator<String> lineIterator = Arrays.asList(fullTestOutput.split("\n")).iterator();
        while (lineIterator.hasNext()) {
            String line = lineIterator.next().trim();
            if (line.startsWith("[junit] Testcase: test took")) {
                reduced.append(line).append("\n");
                break;
            }
        }
        // The next line is FAILED we can skip it
        if (lineIterator.hasNext()) {
            lineIterator.next();
        }

        // Add all the line until the last one
        // "[junit] Test TestLift FAILED";
        while (lineIterator.hasNext()) {
            String line = lineIterator.next().trim();

            if (line.startsWith("[junit] Test ") && line.endsWith("FAILED")) {
                break;
            }
            reduced.append(line)
                    .append("\n");
        }
        return reduced.toString();
    }

    /**
     * Sanitize the compiler output by identifying the output folder and
     * removing it from the compiler output before sending it over the clients.
     */
    private String sanitize(String fullCompilerOutput) {
        String outputFolder = null; // This is what we shall remove from the log
        StringBuilder sanitized = new StringBuilder();
        for (String line : fullCompilerOutput.split("\n")) {
            // This might not work with dependencies, but we should always
            // mutate one file at time, right?
            if (line.startsWith("[javac] Compiling 1 source file to ")) {
                // Get the value of the output folder
                outputFolder = line.replace("[javac] Compiling 1 source file to ", "");
            } else {
                if (outputFolder != null && line.contains(outputFolder)) {
                    line = line.replace(outputFolder + File.separator, "");
                }
                // Pass it along the output
                sanitized.append(line).append("\n");
            }
        }
        return sanitized.toString();
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
        return "AntProcessResult{"
                + "inputStreamText='" + inputStreamText + '\''
                + ", errorStreamText='" + errorStreamText + '\''
                + ", exceptionText='" + exceptionText + '\''
                + '}';
    }

}
