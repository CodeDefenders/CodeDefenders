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
package org.codedefenders.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codedefenders.beans.game.MutantAccordionBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaVersionUtils {
    private static final Logger logger = LoggerFactory.getLogger(MutantAccordionBean.class);

    /**
     * Extracts the major version from a Java version string.
     *
     * <p>Allows these formats:
     * <ul>
     * <li>1.8.0_72-ea</li>
     * <li>9-ea</li>
     * <li>9</li>
     * <li>9.0.1</li>
     * </ul>
     */
    public static int getJavaMajorVersion(String version) {
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }

        int dotPos = version.indexOf('.');
        int dashPos = version.indexOf('-');

        int end;
        if (dotPos > -1) {
            end = dotPos;
        } else if (dashPos > -1) {
            end = dashPos;
        } else {
            end = version.length();
        }

        return Integer.parseInt(version.substring(0, end));
    }

    public static int getJavaMajorVersion() {
        String version = System.getProperty("java.version");
        return getJavaMajorVersion(version);
    }

    public static Optional<Integer> getMajorJavaVersionFromExecutable(Path pathToExecutable) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(pathToExecutable.toString(), "--version");

        Process process = null;
        String output;
        try {
            process = processBuilder.start();
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warn("Got IOException while running java executable: " + pathToExecutable, e);
            return Optional.empty();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }

        Pattern pattern = Pattern.compile("(\\d\\S*)");
        Matcher matcher = pattern.matcher(output);

        if (matcher.find()) {
            String match = matcher.group(1);
            return Optional.of(getJavaMajorVersion(match));
        } else {
            return Optional.empty();
        }
    }
}
