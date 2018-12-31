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
package org.codedefenders.util;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.codedefenders.util.Constants.F_SEP;
import static org.codedefenders.util.Constants.JAVA_SOURCE_EXT;
import static org.codedefenders.util.Constants.TEST_PREFIX;

/**
 * This class offers static methods for file functionality.
 */
public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static String createIndexXML(File dir, String fileName, String contents) throws IOException {
        String path = dir.getAbsolutePath() + F_SEP + fileName + ".xml";
        File infoFile = new File(path);
        FileWriter infoWriter = new FileWriter(infoFile);
        BufferedWriter bInfoWriter = new BufferedWriter(infoWriter);
        bInfoWriter.write(contents);
        bInfoWriter.close();
        return path;
    }

    public static String createJavaTestFile(File dir, String classBaseName, String testCode) throws IOException {
        String javaFile = dir.getAbsolutePath() + F_SEP + TEST_PREFIX + classBaseName + JAVA_SOURCE_EXT;
        Path path = Paths.get(javaFile);
        Files.write(path, testCode.getBytes());
        return path.toString();
    }

    public static File getNextSubDir(String path) {
        File folder = new File(path);
        folder.mkdirs();
        String[] directories = folder.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory() && (isParsable(name));
            }
        });
        Arrays.sort(directories);
        String newPath;
        if (directories.length == 0)
            newPath = folder.getAbsolutePath() + F_SEP + "00000001";
        else {
            File lastDir = new File(directories[directories.length - 1]);
            int newIndex = Integer.parseInt(lastDir.getName()) + 1;
            String formatted = String.format("%08d", newIndex);

            newPath = path + F_SEP + formatted;
        }
        File newDir = new File(newPath);
        newDir.mkdirs();
        return newDir;
    }

    private static boolean isParsable(String input) {
        boolean parsable = true;
        try {
            Integer.parseInt(input);
        } catch (NumberFormatException e) {
            parsable = false;
        }

        return parsable;
    }

    public static List<String> readLines(Path path) {
        List<String> lines = new ArrayList<>();
        try {
            if (Files.exists(path)) {
                lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            } else {
                logger.error("File not found {}. Returning empty lines", path);
            }
        } catch (IOException e) {
            logger.error("Error reading file.", e);
        }
        return lines;
    }

    /**
     * Reads a {@code .java} for a given file path, or if failed return a default.
     * <p>
     * If the file cannot be found, {@code [File Not Found]} is returned.
     * <p>
     * If the file cannot be read, {@code [File Not Readable]} is returned.
     *
     * @param javaFilePath the path to the java file.
     * @return the java file, or a default.
     */
    public static String readJavaFileWithDefault(Path javaFilePath) {
        try {
            return new String(Files.readAllBytes(javaFilePath));
        } catch (FileNotFoundException e) {
            logger.error("Could not find file " + javaFilePath);
            return "[File Not Found]";
        } catch (IOException e) {
            logger.error("Could not read file " + javaFilePath);
            return "[File Not Readable]";
        }
    }

    /**
     * Similar to {@link #readJavaFileWithDefault(Path)} but HTML escaped.
     */
    public static String readJavaFileWithDefaultHTMLEscaped(Path javaFilePath) {
        return StringEscapeUtils.escapeHtml(readJavaFileWithDefault(javaFilePath));
    }

    /**
     * Returns the file name without extension for a given file path.
     * <p>
     * E.g. for {@code Test.java}, the method returns {@code Test}.
     *
     * @param filePath the path of the file.
     * @return the file name without file extension.
     */
    public static String extractFileNameNoExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return fileName;
        }
        return fileName.substring(0, index);
    }
}
