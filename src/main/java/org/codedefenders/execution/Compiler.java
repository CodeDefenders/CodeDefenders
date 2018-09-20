package org.codedefenders.execution;

import org.codedefenders.util.Constants;
import org.codedefenders.util.JavaFileObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

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
        final String outDir = javaFile.getPath().substring(0, javaFile.getPath().lastIndexOf("/"));
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
     * Compiles a java test file for a given path. The compiled class
     * is stored in the same directory the specified java file lies.
     * <p>
     * Similar to {@link #compileJavaFile(String)}, but includes libraries
     * required for testing.
     *
     * @param javaTestFilePath Path to the {@code .java} test file.
     * @param javaCutFilePath  Path to the {@code .java} file of the, which is tested.
     * @return A path to the {@code .class} file.
     * @throws CompileException If an error during compilation occurs.
     */
    public static String compileJavaTestFile(String javaTestFilePath, String javaCutFilePath) throws CompileException {
        if (javaCutFilePath == null) {
            throw new IllegalArgumentException("File path of provided class under test must not be null.");
        }
        return compileJavaTestFile(new JavaFileObject(javaTestFilePath), javaCutFilePath);
    }

    /**
     * Compiles a java file for a given path <i>and</i> file content.
     * The class is stored in the same directory the specified java file lies.
     *
     * <p>
     * Similar to {@link #compileJavaFileForContent(String, String)}, but includes libraries
     * required for testing.
     *
     * @param javaFilePath    Path to the {@code .java} test file.
     * @param javaFileContent Content of the {@code .java} test file.
     * @param javaCutFilePath Path to the {@code .java} file of the, which is tested.
     * @return A path to the {@code .class} file.
     * @throws CompileException If an error during compilation occurs.
     */
    public static String compileJavaTestFileForContent(String javaFilePath, String javaFileContent, String javaCutFilePath) throws CompileException {
        if (javaCutFilePath == null) {
            throw new IllegalArgumentException("File path of provided class under test must not be null.");
        }
        return compileJavaTestFile(new JavaFileObject(javaFilePath, javaFileContent), javaCutFilePath);
    }

    /**
     * Just like {@link #compileJavaFile(JavaFileObject)}, but includes JUnit, Hamcrest and
     * Mockito libraries required for running the tests.
     */
    private static String compileJavaTestFile(JavaFileObject testFile, String javaCutFilePath) throws CompileException {
        final Path cutPath = Paths.get(javaCutFilePath);

        final String outDir = testFile.getPath().substring(0, testFile.getPath().lastIndexOf("/"));
        final String cutDir = cutPath.getParent().toString();
        final String classPath = String.format(Constants.TEST_CLASSPATH_WITH_DIR, cutDir);

        javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        final StringWriter writer = new StringWriter();
        final List<? extends javax.tools.JavaFileObject> compilationUnits = Arrays.asList(testFile);

        final List<String> options = Arrays.asList(
                "-encoding", "UTF-8",
                "-d", outDir,
                "-classpath", classPath
        );

        final JavaCompiler.CompilationTask task = compiler.getTask(writer, null, null, options, null, compilationUnits);

        final Boolean success = task.call();
        try {
            // remove resulting CUT class file again.
            logger.info("removing CUT class file again");
            Files.deleteIfExists(Paths.get(outDir, cutPath.getFileName().toString().replace(".java", ".class")));
        } catch (IOException ignored) {
            if (success) {
                logger.warn("Failed to remove CUT class file in test folder:{}", outDir);
            }
        }
        if (success) {
            return testFile.getPath().replace(".java", ".class");
        } else {
            throw new CompileException(writer.toString());
        }
    }
}
