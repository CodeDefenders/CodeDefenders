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

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.codedefenders.util.JavaFileObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.util.Constants.TEST_CLASSPATH;

/**
 * This class handles compilation of Java classes using the
 * native {@link JavaCompiler}. This class includes a static internal class {@link JavaFileObject}.
 *
 * <p>Offering static methods, java files can be compiled, either by reading the file
 * content from the hard disk or providing it. The resulting {@code .class} file path
 * is returned.
 *
 * <p>Dependency files of a Java class are either removed stay where they are,
 * based on the parent directory of the associated Java class.
 *
 * <p>Test cases can also be compiled, but require a reference to the tested class.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 * @see CompileException
 * @see JavaFileObject
 */
public class Compiler {
    private static final Logger logger = LoggerFactory.getLogger(Compiler.class);

    /**
     * Compiles a java file for a given path. The compiled class
     * is stored in the same directory the specified java file lies.
     *
     * @param javaFilePath Path to the {@code .java} file.
     * @return A path to the {@code .class} file.
     * @throws CompileException If an error during compilation occurs.
     */
    public static String compileJavaFile(String javaFilePath) throws CompileException, IllegalStateException {
        return compileJavaFile(new JavaFileObject(javaFilePath));
    }

    /**
     * Compiles a java file for a given path <i>and</i> file content (so no IO required).
     * The class is stored in the same directory the specified java file lies.
     *
     * @param javaFilePath    Path to the {@code .java} file.
     * @param javaFileContent Content of the {@code .java} file.
     * @return A path to the {@code .class} file.
     * @throws CompileException If an error during compilation occurs.
     */
    public static String compileJavaFileForContent(String javaFilePath, String javaFileContent)
            throws CompileException, IllegalStateException {
        return compileJavaFile(new JavaFileObject(javaFilePath, javaFileContent));
    }

    /**
     * Before making adjustments:
     *
     * <p>To store the {@code .class} file in the same directory as the {@code .java} file,
     * {@code javac} requires no options, but here, somehow the standard tomcat directory
     * is used, so the option {@code -d} is required.
     */
    private static String compileJavaFile(JavaFileObject javaFile) throws CompileException, IllegalStateException {
        // the directory this java file is compiled to. If a class
        // is in a package the package folder structure starts here
        final Path baseDir = Paths.get(javaFile.getPath()).getParent();
        javax.tools.JavaCompiler compiler = getCompiler();

        final StringWriter writer = new StringWriter();
        final List<? extends javax.tools.JavaFileObject> compilationUnits = Arrays.asList(javaFile);
        final List<String> options = getCliParameters(baseDir.toString(), null);

        final JavaCompiler.CompilationTask task = compiler.getTask(writer, null, null, options, null, compilationUnits);

        final Boolean success = task.call();
        if (success) {
            try {
                return getClassPath(javaFile, baseDir).toString();
            } catch (IOException e) {
                throw new CompileException(e);
            }
        } else {
            throw new CompileException(writer.toString());
        }
    }

    /**
     * Compiles a java file for a given java file together with given dependencies.
     * The compiled classes are all stored in the same directory the specified java file lies.
     *
     * @param javaFilePath    Path to the {@code .java} file.
     * @param javaFileContent Content of the {@code .java} file.
     * @param dependencies    a list of {@link JavaFileObject JavaFileObjects}, which the given java file is compiled
     *                        together with. All these files must be in the same folder as the given java file.
     * @return A path to the {@code .class} file of the compiled given java file.
     * @throws CompileException If an error during compilation occurs.
     */
    public static String compileJavaFileForContentWithDependencies(String javaFilePath,
                                                                   String javaFileContent,
                                                                   List<JavaFileObject> dependencies)
            throws CompileException, IllegalStateException {
        return compileJavaFileWithDependencies(new JavaFileObject(javaFilePath, javaFileContent), dependencies, false);
    }

    /**
     * Similar to {@link #compileJavaFileForContentWithDependencies(String, String, List)}, but
     * gives an option to remove the generated {@code .class} files again.
     *
     * @param cleanUpDependencyClassFiles whether generated {@code .class} files of dependencies
     *                                    are removed after compilation. Otherwise they are moved to
     *                                    {@code dependencies/}.
     * @see #compileJavaFileForContentWithDependencies(String, String, List)
     */
    public static String compileJavaFileForContentWithDependencies(String javaFilePath,
                                                                   String javaFileContent,
                                                                   List<JavaFileObject> dependencies,
                                                                   boolean cleanUpDependencyClassFiles)
            throws CompileException, IllegalStateException {
        return compileJavaFileWithDependencies(
                new JavaFileObject(javaFilePath, javaFileContent),
                dependencies,
                cleanUpDependencyClassFiles
        );
    }

    /**
     * Compiles a java file for a given path together with given dependencies.
     * The compiled class is stored in the same directory the specified java file lies.
     *
     * @param javaFilePath Path to the {@code .java} file.
     * @param dependencies a list of {@link JavaFileObject JavaFileObjects}, which the given java file is compiled
     *                     together with. All these files must be in the same folder as the given java file.
     * @return A path to the {@code .class} file of the compiled given java file.
     * @throws CompileException If an error during compilation occurs.
     */
    public static String compileJavaFileWithDependencies(String javaFilePath, List<JavaFileObject> dependencies)
            throws CompileException, IllegalStateException {
        return compileJavaFileWithDependencies(new JavaFileObject(javaFilePath), dependencies, false);
    }

    /**
     * Similar to {@link #compileJavaFileWithDependencies(String, List)}, but
     * gives an option to remove the generated {@code .class} files again.
     *
     * @param cleanUpDependencyClassFiles whether generated {@code .class} files of dependencies
     *                                    are removed after compilation. Otherwise they are moved to
     *                                    {@code dependencies/}.
     * @see #compileJavaFileWithDependencies(String, List)
     */
    public static String compileJavaFileWithDependencies(String javaFilePath,
                                                         List<JavaFileObject> dependencies,
                                                         boolean cleanUpDependencyClassFiles)
            throws CompileException, IllegalStateException {
        return compileJavaFileWithDependencies(
                new JavaFileObject(javaFilePath), dependencies, cleanUpDependencyClassFiles);
    }

    /**
     * Similar to {@link #compileJavaFile(JavaFileObject)}, but the {@code dependency} parameter
     * is added to the compilation units.
     */
    @SuppressWarnings("Duplicates")
    private static String compileJavaFileWithDependencies(JavaFileObject javaFile,
                                                          List<JavaFileObject> dependencies,
                                                          boolean cleanUpDependencyClassFiles)
            throws CompileException, IllegalStateException {
        // the directory this java file is compiled to. If a class
        // is in a package the package folder structure starts here
        final Path baseDir = Paths.get(javaFile.getPath()).getParent();
        javax.tools.JavaCompiler compiler = getCompiler();

        final StringWriter writer = new StringWriter();

        final List<javax.tools.JavaFileObject> compilationUnits = new LinkedList<>(dependencies);
        compilationUnits.add(javaFile);
        final List<String> options = getCliParameters(baseDir.toString(), null);

        final JavaCompiler.CompilationTask task = compiler.getTask(writer, null, null, options, null, compilationUnits);
        final Boolean success = task.call();

        if (cleanUpDependencyClassFiles) {
            // Remove dependency .class files generated in baseDir
            cleanUpDependencies(dependencies, baseDir, success);
        }
        if (success) {
            try {
                return getClassPath(javaFile, baseDir).toString();
            } catch (IOException e) {
                throw new CompileException(e);
            }
        } else {
            throw new CompileException(writer.toString());
        }
    }

    /**
     * Compiles a java test file for a given path. The compiled class
     * is stored in the same directory the specified java file lies.
     *
     * <p>Similar to {@link #compileJavaFile(String)}, but includes libraries
     * required for testing.
     *
     * @param javaTestFilePath Path to the {@code .java} test file.
     * @param dependencies     a list of java files required for compilation.
     * @return A path to the {@code .class} file.
     * @throws CompileException If an error during compilation occurs.
     */
    public static String compileJavaTestFile(String javaTestFilePath, List<JavaFileObject> dependencies)
            throws CompileException, IllegalStateException {
        return compileJavaTestFile(new JavaFileObject(javaTestFilePath), dependencies, false);
    }

    /**
     * Similar to {@link #compileJavaTestFile(String, List)}, but
     * gives an option to remove the generated {@code .class} files again.
     *
     * @param cleanUpDependencyClassFiles whether generated {@code .class} files of dependencies
     *                                    are removed after compilation.
     * @see #compileJavaFileWithDependencies(String, List)
     */
    public static String compileJavaTestFile(String javaTestFilePath,
                                             List<JavaFileObject> dependencies,
                                             boolean cleanUpDependencyClassFiles)
            throws CompileException, IllegalStateException {
        return compileJavaTestFile(new JavaFileObject(javaTestFilePath), dependencies, cleanUpDependencyClassFiles);
    }

    /**
     * Compiles a java file for a given path <i>and</i> file content.
     * The class is stored in the same directory the specified java file lies.
     *
     * <p>Similar to {@link #compileJavaFileForContent(String, String)}, but includes libraries
     * required for testing.
     *
     * <p>Removes all {@code .class} files, but the class file of the test case.
     *
     * @param javaFilePath    Path to the {@code .java} test file.
     * @param javaFileContent Content of the {@code .java} test file.
     * @param dependencies    a list of java files required for compilation.
     * @return A path to the {@code .class} file.
     * @throws CompileException If an error during compilation occurs.
     */
    public static String compileJavaTestFileForContent(String javaFilePath,
                                                       String javaFileContent,
                                                       List<JavaFileObject> dependencies)
            throws CompileException, IllegalStateException {
        return compileJavaTestFile(new JavaFileObject(javaFilePath, javaFileContent), dependencies, false);
    }

    /**
     * Similar to {@link #compileJavaTestFileForContent(String, String, List)}, but
     * gives an option to remove the generated {@code .class} files again.
     *
     * @param cleanUpDependencyClassFiles whether generated {@code .class} files of dependencies
     *                                    are removed after compilation.
     */
    public static String compileJavaTestFileForContent(String javaFilePath,
                                                       String javaFileContent,
                                                       List<JavaFileObject> dependencies,
                                                       boolean cleanUpDependencyClassFiles)
            throws CompileException, IllegalStateException {
        return compileJavaTestFile(
                new JavaFileObject(javaFilePath, javaFileContent), dependencies, cleanUpDependencyClassFiles);
    }

    /**
     * Just like {@link #compileJavaFileWithDependencies(JavaFileObject, List, boolean)},
     * but includes JUnit, Hamcrest and Mockito libraries required for running the tests.
     */
    @SuppressWarnings("Duplicates")
    private static String compileJavaTestFile(JavaFileObject testFile,
                                              List<JavaFileObject> dependencies,
                                              boolean cleanUpDependencyClassFiles)
            throws CompileException, IllegalStateException {
        // the directory this java file is compiled to. If a class
        // is in a package the package folder structure starts here
        final Path baseDir = Paths.get(testFile.getPath()).getParent();
        javax.tools.JavaCompiler compiler = getCompiler();

        final StringWriter writer = new StringWriter();
        final List<javax.tools.JavaFileObject> compilationUnits = new LinkedList<>(dependencies);
        compilationUnits.add(testFile);

        final List<String> options = getCliParameters(baseDir.toString(), TEST_CLASSPATH);

        final JavaCompiler.CompilationTask task = compiler.getTask(writer, null, null, options, null, compilationUnits);

        final Boolean success = task.call();
        if (cleanUpDependencyClassFiles) {
            cleanUpDependencies(dependencies, baseDir, success);
        }
        if (success) {
            try {
                return getClassPath(testFile, baseDir).toString();
            } catch (IOException e) {
                throw new CompileException(e);
            }
        } else {
            throw new CompileException(writer.toString());
        }
    }

    /**
     * Removes the {@code .class} files for a given list of files. These files were dependencies
     * for other classes.
     *
     * @param dependencies  the {@code .java} files the {@code .class} were generated from
     *                      and will be removed.
     * @param baseDirectory the base directory the files or the package folders were in.
     * @param logError      {@code true} if IOExceptions should be logged.
     */
    private static void cleanUpDependencies(List<JavaFileObject> dependencies, Path baseDirectory, Boolean logError) {
        for (JavaFileObject dependency : dependencies) {
            try {
                final Path path = getClassPath(dependency, baseDirectory);
                logger.info("Removing dependency file:{}", path);
                Files.delete(path);
            } catch (IOException e) {
                if (logError) {
                    logger.warn("Failed to remove dependency class file in folder:{}", baseDirectory);
                }
            }
        }
    }

    /**
     * Retrieves the {@code .class} file for a given {@link JavaFileObject}
     * by looking through sub folders and matching file names.
     *
     * @param javaFile      the given java file as a {@link JavaFileObject}.
     * @param baseDirectory the base directory the files or the package folders were in.
     * @return the path to the {@code .class} of the given java file as a {@link Path}.
     * @throws IOException when finding files goes wrong.
     */
    private static Path getClassPath(JavaFileObject javaFile, Path baseDirectory) throws IOException {
        final String targetName = javaFile.getName().replace(".java", ".class");

        try (Stream<Path> pathStream = Files.walk(baseDirectory, 200)) {
            return pathStream
                    .filter(path -> path.getFileName().toString().equals(targetName))
                    .findFirst()
                    .map(Path::toAbsolutePath)
                    .orElseGet(() -> Paths.get(javaFile.getPath().replace(".java", ".class")));
        }
    }

    private static javax.tools.JavaCompiler getCompiler() {
        javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Platform provided no java compiler.");
        }
        return compiler;
    }

    private static List<String> getCliParameters(String basedir, String classpath) {
        List<String> options = new ArrayList<>();

        options.add("-encoding");
        options.add("UTF-8");

        if (basedir != null) {
            options.add("-d");
            options.add(basedir);
        }

        if (classpath != null) {
            options.add("-cp");
            options.add(classpath);
        }

        options.add("--release");
        options.add("16");

        return options;
    }
}
