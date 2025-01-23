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
package org.codedefenders.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.text.StringEscapeUtils;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.DependencyDAO;
import org.codedefenders.model.Dependency;
import org.codedefenders.persistence.database.GameClassRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javassist.ClassPool;
import javassist.CtClass;

import static org.codedefenders.util.Constants.JAVA_SOURCE_EXT;
import static org.codedefenders.util.Constants.TEST_PREFIX;
import static org.codedefenders.util.NamingUtils.nextFreeName;
import static org.codedefenders.util.NamingUtils.nextFreeNumber;

/**
 * This class offers static methods for file functionality.
 */
public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    private static Configuration getConfig() {
        return CDIUtil.getBeanFromCDI(Configuration.class);
    }

    /*
     * Some Notes regarding file organization of Code Defenders.
     *
     * Overview:
     * <data dir>/
     *   build.xml                               <- ant build/project file
     *   security.policy                         <- Java Security Manager config files
     *   lib/                                    <- Contains .jar files we need for execution e.g. JUnit
     *   sources/                                <- Contains a folder per uploaded class under test
     *     <ClassAlias>/
     *       <PackageStructure>/                 <- The CUT is direct in the folder (with package structure)
     *         <ClassName>.java
     *         <ClassName>.class
     *         <ClassName>$<InnerClassName>.class
     *       dependencies/
     *         <PackageStructure>/               <- Dependencies of the CUT (like other source files) are in the 'dependencies' subdirectory
     *           <DependencyClassName>.java
     *           <DependencyClassName>.class
     *           <DependencyClassName>$<InnerClassName>.class
     *   mutants/
     *     mp/
     *       <GameId>/
     *         <UserId>/
     *           <Nr>/
     *             <PackageStructure>/
     *               <ClassName>.java
     *               <ClassName>.class
     *               <ClassName>$<InnerClassName>.class
     *   tests/
     *     mp/
     *       <GameId>/
     *         <UserId>/
     *           original/                       <- Normal Test
     *             <Nr>/
     *               <PackageStructure>/
     *                 Test<ClassName>.java
     *                 Test<ClassName>.class
     *                 jacoco.exec
     *           <UserId>-<Nr>/                  <- Test recompiled against mutant from <UserId> with <Nr>
     *             <Nr>/
     *               <PackageStructure>/
     *                 Test<ClassName>.java
     *                 Test<ClassName>.class
     *                 jacoco.exec
     *
     * Notes: We split the CUT and it's dependencies in separate directories, so one can swap the CUT
     * with a mutant.
     * Normal classpath: <data dir>/sources/<ClassAlias>:<data dir>/sources/<ClassAlias>/dependencies
     * Mutant classpath: <data dir>/
     */

    public static String createIndexXML(File dir, String fileName, String contents) throws IOException {
        Path path = Paths.get(dir.getAbsolutePath(), fileName + ".xml");
        File infoFile = path.toFile();
        BufferedWriter infoWriter = new BufferedWriter(new FileWriter(infoFile));
        infoWriter.write(contents);
        infoWriter.close();
        return path.toString();
    }

    public static String createJavaTestFile(File dir, String classBaseName, String testCode) throws IOException {
        Path path = dir.toPath().resolve(TEST_PREFIX + classBaseName + JAVA_SOURCE_EXT);
        Files.write(path, testCode.getBytes());
        return path.toString();
    }

    public static File getNextSubDir(Path directory) {
        File folder = directory.toFile();
        folder.mkdirs();
        String[] directories =
                folder.list((current, name) -> new File(current, name).isDirectory() && (isParsable(name)));
        Arrays.sort(directories);
        Path newPath;
        if (directories.length == 0) {
            newPath = Paths.get(folder.getAbsolutePath(), "00000001");
        } else {
            File lastDir = new File(directories[directories.length - 1]);
            int newIndex = Integer.parseInt(lastDir.getName()) + 1;
            String formatted = String.format("%08d", newIndex);

            newPath = directory.resolve(formatted);
        }
        File newDir = newPath.toFile();
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
     *
     * <p>If the file cannot be found, {@code [File Not Found]} is returned.
     *
     * <p>If the file cannot be read, {@code [File Not Readable]} is returned.
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
     * Returns the file name without extension for a given file path.
     *
     * <p>E.g. for {@code Test.java}, the method returns {@code Test}.
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

    /**
     * Converts a path relative to {@link Configuration#getDataDir()} to an absolute path.
     * If the given path is absolute, returns the given path. If the given path is relative, returns an absolute path
     * that concatenates {@link Configuration#getDataDir()}} and the given path.
     *
     * @param path a path relative to {@link Configuration#getDataDir()}}, or an absolute path.
     * @return an absolute path that describes the given path.
     */
    public static Path getAbsoluteDataPath(String path) {
        return getAbsoluteDataPath(Paths.get(path));
    }

    /**
     * Converts a path relative to {@link Configuration#getDataDir()}} to an absolute path.
     * If the given path is absolute, returns the given path. If the given path is relative, returns an absolute path
     * that concatenates {@link Configuration#getDataDir()}} and the given path.
     *
     * @param path a path relative to {@link Configuration#getDataDir()}}, or an absolute path.
     * @return an absolute path that describes the given path.
     */
    public static Path getAbsoluteDataPath(Path path) {
        if (path.isAbsolute()) {
            return path;
        } else {
            return Paths.get(getConfig().getDataDir().toString(), path.toString());
        }
    }

    /**
     * Converts an absolute path to a path relative to {@link  Configuration#getDataDir()}} if the path is a descendant of
     * {@link Configuration#getDataDir()}}. Otherwise, returns the given path itself.
     *
     * @param path an absolute path.
     * @return a path relative to {@link Configuration#getDataDir()}} or the path itself.
     */
    public static Path getRelativeDataPath(String path) {
        return getRelativeDataPath(Paths.get(path));
    }

    /**
     * Converts an absolute path to a path relative to {@link Configuration#getDataDir()}} if the path is a descendant of
     * {@link Configuration#getDataDir()}}. Otherwise, returns the given path itself.
     *
     * @param path an absolute path.
     * @return a path relative to {@link Configuration#getDataDir()}} or the path itself.
     */
    public static Path getRelativeDataPath(Path path) {
        Configuration config = getConfig();
        if (path.startsWith(config.getDataDir().toString())) {
            return config.getDataDir().toPath().relativize(path);
        } else {
            return path;
        }
    }

    /**
     * Returns the qualified name of a java class for a given {@code .java} file content.
     *
     * <p>E.g. {@code java.util.Collection} for {@link Collection}.
     *
     * @param javaClassFilePath The path to the java class file.
     * @return A qualified name of the given java class.
     * @throws IOException when reading the java file fails.
     */
    public static String getFullyQualifiedName(String javaClassFilePath) throws IOException {
        ClassPool classPool = ClassPool.getDefault();
        CtClass cc = classPool.makeClass(new FileInputStream(javaClassFilePath));
        return cc.getName();
    }

    /**
     * Stores a file for given parameters on the hard drive.
     * If the path up to the file does not exist, all non-existed parent directories are created.
     *
     * @param folderPath  The path of the folder the file will be stored in as a {@link Path}.
     * @param fileName    The file name (e.g. {@code MyClass.java}).
     * @param fileContent The actual file content.
     * @return The path of the newly stored file.
     * @throws IOException when storing the file fails.
     */
    public static Path storeFile(Path folderPath, String fileName, String fileContent) throws IOException {

        final Path filePath = folderPath.resolve(fileName);
        try {
            Files.createDirectories(folderPath);
            final Path path = Files.createFile(filePath);
            Files.write(path, fileContent.getBytes());
            return path;
        } catch (IOException e) {
            logger.error("Could not store file.", e);
            throw e;
        }
    }

    /**
     * Uses nextFreeName to choose a non-existing filename within a directory.
     *
     * <p>In comparison to {@link FileUtils#getNextSubDir(Path)}, this method sidesteps collisions less effectively,
     * but doesn't require adding a level of depth to the file system.
     *
     * @param directory The directory to find a free filename in.
     * @param name The desired filename.
     * @return A path pointing to the non-existent file.
     */
    public static Path nextFreePath(Path directory, String name) {
        try (Stream<Path> files = Files.walk(directory, 1)) {
            List<String> filenames = files
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .toList();
            String numberedName = nextFreeName(filenames, name);
            return directory.resolve(numberedName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Uses nextFreeName to choose a non-existing storage directory for a class.
     * @param alias The alias of the class.
     * @return A path pointing to the non-existent directory.
     */
    public static Path nextFreeClassDirectory(String alias) {
        var path = Paths.get(getConfig().getSourcesDir().getAbsolutePath());
        return nextFreePath(path, alias);
    }

    public static Path nextFreeNumberPath(Path directory) {
        return nextFreeNumberPath(directory, 2);
    }

    public static Path nextFreeNumberPath(Path directory, int retries) {
        try (Stream<Path> files = Files.walk(directory, 1)) {
            List<String> existingNames = files
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .toList();

            int nextNumber = nextFreeNumber(existingNames);
            String numberName = String.format("%02d", nextNumber);
            Path freePath = directory.resolve(numberName);

            if (Files.exists(freePath)) {
                logger.warn("Path already exists: '{}'", freePath);
                if (retries > 0) {
                    return nextFreeNumberPath(directory, retries - 1);
                } else {
                    throw new RuntimeException("Couldn't find free path.");
                }
            }

            return freePath;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the code of the dependencies belonging to a class under test.
     * @param cutId The id of the class under test.
     * @return A list of String, properly escaped for JSON usage, containing the code of the dependencies.
     */
    public static List<String> getCodeFromDependencies(int cutId) {
        List<Dependency> dependencies = CDIUtil.getBeanFromCDI(GameClassRepository.class).getMappedDependenciesForClassId(cutId);
        List<String> code = new ArrayList<>();
        for (Dependency dep : dependencies) {
            Path filePath = Path.of(dep.getJavaFile());
            code.add(StringEscapeUtils.escapeJson(readJavaFileWithDefault(filePath)));
        }
        return code;
    }
}
