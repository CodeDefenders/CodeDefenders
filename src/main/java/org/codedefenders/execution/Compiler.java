package org.codedefenders.execution;

import org.codedefenders.util.Constants;
import org.codedefenders.util.JavaFileObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import static org.codedefenders.util.Constants.F_SEP;

/**
 * This class handles compilation of Java classes using the
 * native {@link JavaCompiler}. This class includes a static internal class {@link JavaFileObject}.
 * <p>
 * Offering static methods, java files can be compiled, either by reading the file
 * content from the hard disk or providing it. The resulting {@code .class} is returned.
 * <p>
 * Test cases can also be compiled, but require a reference to the tested class.
 *
 * @author <a href=https://github.com/werli>Phil Werli<a/>
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
    public static String compileJavaFile(String javaFilePath) throws CompileException {
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
    public static String compileJavaFileForContent(String javaFilePath, String javaFileContent) throws CompileException {
        return compileJavaFile(new JavaFileObject(javaFilePath, javaFileContent));
    }

    /**
     * Before making adjustments:
     * <p>
     * To store the {@code .class} file in the same directory as the {@code .java} file,
     * {@code javac} requires no options, but here, somehow the standard tomcat directory
     * is used, so the option {@code -d} is required.
     */
    private static String compileJavaFile(JavaFileObject javaFile) throws CompileException {
        final String outDir = Paths.get(javaFile.getPath()).getParent().toString();
        javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        final StringWriter writer = new StringWriter();
        final List<? extends javax.tools.JavaFileObject> compilationUnits = Arrays.asList(javaFile);
        final List<String> options = Arrays.asList(
                "-encoding", "UTF-8",
                "-d", outDir
        );

        final JavaCompiler.CompilationTask task = compiler.getTask(writer, null, null, options, null, compilationUnits);

        final Boolean success = task.call();
        if (success) {
            return javaFile.getPath().replace(".java", ".class");
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
     * @param dependencies    a list of {@link JavaFileObject}s, which the given java file is compiled together
     *                        with. All these files must be in the same folder as the given java file.
     * @return A path to the {@code .class} file of the compiled given java file.
     * @throws CompileException If an error during compilation occurs.
     */
    public static String compileJavaFileForContentWithDependencies(String javaFilePath, String javaFileContent, List<JavaFileObject> dependencies) throws CompileException {
        return compileJavaFileWithDependencies(new JavaFileObject(javaFilePath, javaFileContent), dependencies, false);
    }

    /**
     * Similar to {@link #compileJavaFileForContentWithDependencies(String, String, List)}, but
     * gives an option to remove the generated {@code .class} files again.
     *
     * @param cleanUpDependencyClassFiles whether generated {@code .class} files of dependencies
     *                                    are removed after compilation.
     * @see #compileJavaFileForContentWithDependencies(String, String, List)
     */
    public static String compileJavaFileForContentWithDependencies(String javaFilePath, String javaFileContent, List<JavaFileObject> dependencies, boolean cleanUpDependencyClassFiles) throws CompileException {
        return compileJavaFileWithDependencies(new JavaFileObject(javaFilePath, javaFileContent), dependencies, cleanUpDependencyClassFiles);
    }

    /**
     * Compiles a java file for a given path together with given dependencies.
     * The compiled class is stored in the same directory the specified java file lies.
     *
     * @param javaFilePath Path to the {@code .java} file.
     * @param dependencies a list of {@link JavaFileObject}s, which the given java file is compiled together
     *                     with. All these files must be in the same folder as the given java file.
     * @return A path to the {@code .class} file of the compiled given java file.
     * @throws CompileException If an error during compilation occurs.
     */
    public static String compileJavaFileWithDependencies(String javaFilePath, List<JavaFileObject> dependencies) throws CompileException {
        return compileJavaFileWithDependencies(new JavaFileObject(javaFilePath), dependencies, false);
    }

    /**
     * Similar to {@link #compileJavaFileWithDependencies(String, List)}, but
     * gives an option to remove the generated {@code .class} files again.
     *
     * @param cleanUpDependencyClassFiles whether generated {@code .class} files of dependencies
     *                                    are removed after compilation.
     * @see #compileJavaFileWithDependencies(String, List)
     */
    public static String compileJavaFileWithDependencies(String javaFilePath, List<JavaFileObject> dependencies, boolean cleanUpDependencyClassFiles) throws CompileException {
        return compileJavaFileWithDependencies(new JavaFileObject(javaFilePath), dependencies, cleanUpDependencyClassFiles);
    }

    /**
     * Similar to {@link #compileJavaFile(JavaFileObject)}, but the {@code dependency} parameter
     * is added to the compilation units.
     */
    private static String compileJavaFileWithDependencies(JavaFileObject javaFile, List<JavaFileObject> dependencies, boolean cleanUpDependencyClassFiles) throws CompileException {
        final String outDir = Paths.get(javaFile.getPath()).getParent().toString();
        javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final StringWriter writer = new StringWriter();

        final List<javax.tools.JavaFileObject> compilationUnits = new LinkedList<>(dependencies);
        compilationUnits.add(javaFile);

        final List<String> options = Arrays.asList(
                "-encoding", "UTF-8",
                "-d", outDir
        );

        final JavaCompiler.CompilationTask task = compiler.getTask(writer, null, null, options, null, compilationUnits);
        final Boolean success = task.call();

        if (cleanUpDependencyClassFiles) {
            // Remove dependency .class files generated in outDir
            cleanUpDependencies(dependencies, outDir, success);
        }
        if (success) {
            return javaFile.getPath().replace(".java", ".class");
        } else {
            throw new CompileException(writer.toString());
        }
    }

    /**
     * Compiles a java test file for a given path. The compiled class
     * is stored in the same directory the specified java file lies.
     * <p>
     * Similar to {@link #compileJavaFile(String)}, but includes libraries
     * required for testing.
     *
     * @param javaTestFilePath Path to the {@code .java} test file.
     * @param dependencies a list of java files required for compilation.
     * @return A path to the {@code .class} file.
     * @throws CompileException If an error during compilation occurs.
     */
    public static String compileJavaTestFile(String javaTestFilePath, List<JavaFileObject> dependencies) throws CompileException {
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
    public static String compileJavaTestFile(String javaTestFilePath, List<JavaFileObject> dependencies, boolean cleanUpDependencyClassFiles) throws CompileException {
        return compileJavaTestFile(new JavaFileObject(javaTestFilePath), dependencies, cleanUpDependencyClassFiles);
    }

    /**
     * Compiles a java file for a given path <i>and</i> file content.
     * The class is stored in the same directory the specified java file lies.
     * <p>
     * Similar to {@link #compileJavaFileForContent(String, String)}, but includes libraries
     * required for testing.
     * <p>
     * Removes all {@code .class} files, but the class file of the test case.
     *
     * @param javaFilePath    Path to the {@code .java} test file.
     * @param javaFileContent Content of the {@code .java} test file.
     * @param dependencies a list of java files required for compilation.
     * @return A path to the {@code .class} file.
     * @throws CompileException If an error during compilation occurs.
     */
    public static String compileJavaTestFileForContent(String javaFilePath, String javaFileContent, List<JavaFileObject> dependencies) throws CompileException {
        return compileJavaTestFile(new JavaFileObject(javaFilePath, javaFileContent), dependencies, false);
    }

    /**
     * Similar to {@link #compileJavaTestFileForContent(String, String, List)}, but
     * gives an option to remove the generated {@code .class} files again.
     *
     * @param cleanUpDependencyClassFiles whether generated {@code .class} files of dependencies
     *                                    are removed after compilation.
     */
    public static String compileJavaTestFileForContent(String javaFilePath, String javaFileContent, List<JavaFileObject> dependencies, boolean cleanUpDependencyClassFiles) throws CompileException {
        return compileJavaTestFile(new JavaFileObject(javaFilePath, javaFileContent), dependencies, cleanUpDependencyClassFiles);
    }

    /**
     * Just like {@link #compileJavaFileWithDependencies(JavaFileObject, List, boolean)},
     * but includes JUnit, Hamcrest and Mockito libraries required for running the tests.
     */
    private static String compileJavaTestFile(JavaFileObject testFile, List<JavaFileObject> dependencies, boolean cleanUpDependencyClassFiles) throws CompileException {
        final String outDir = Paths.get(testFile.getPath()).getParent().toString();

        final String classPath = Constants.TEST_CLASSPATH;
        javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        final StringWriter writer = new StringWriter();
        final List<javax.tools.JavaFileObject> compilationUnits = new LinkedList<>(dependencies);
        compilationUnits.add(testFile);

        final List<String> options = Arrays.asList(
                "-encoding", "UTF-8",
                "-d", outDir,
                "-classpath", classPath
        );

        final JavaCompiler.CompilationTask task = compiler.getTask(writer, null, null, options, null, compilationUnits);

        final Boolean success = task.call();
        if (cleanUpDependencyClassFiles) {
            cleanUpDependencies(dependencies, outDir, success);
        }
        if (success) {
            return testFile.getPath().replace(".java", ".class");
        } else {
            throw new CompileException(writer.toString());
        }
    }

    /**
     * Removes the {@code .class} files for a given list of files. These files were dependencies
     * for other classes.
     *
     * @param dependencies the {@code .java} files the {@code .class} were generated from
     *                     and will be removed.
     * @param directory    the directory the files were in.
     * @param logError     {@code true} if IOExceptions should be logged.
     */
    private static void cleanUpDependencies(List<JavaFileObject> dependencies, String directory, Boolean logError) {
        // Remove dependency .class files generated in the directory
        for (JavaFileObject dependency : dependencies) {
            try {
                final String path = directory + F_SEP + dependency.getName().replace(".java", ".class");
                logger.info("Removing dependency file:{}", path);
                Files.delete(Paths.get(path));
            } catch (IOException e) {
                if (logError) {
                    logger.warn("Failed to remove dependency class file in folder:{}", directory);
                }
            }
        }
    }
}
