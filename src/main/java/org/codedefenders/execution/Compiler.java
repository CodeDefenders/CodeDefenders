package org.codedefenders.execution;

import org.codedefenders.util.Constants;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * This class handles compilation of Java classes using the
 * native {@link JavaCompiler}. This class includes a static internal class {@link JavaFile}.
 * <p>
 * Offering static methods, java files can be compiled, either by reading the file
 * content from the hard disk or providing it. The resulting {@code .class} is returned.
 * <p>
 * Test cases can also be compiled, but require a reference to the tested class.
 *
 * @author <a href=https://github.com/werli>Phil Werli<a/>
 * @see CompileException
 * @see JavaFile
 */
public class Compiler {

    /**
     * Compiles a java file for a given path. The compiled class
     * is stored in the same directory the specified java file lies.
     *
     * @param javaFilePath Path to the {@code .java} file.
     * @return A path to the {@code .class} file.
     * @throws CompileException If an error during compilation occurs.
     */
    public static String compileJavaFile(String javaFilePath) throws CompileException {
        return compileJavaFile(new JavaFile(javaFilePath));
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
        return compileJavaFile(new JavaFile(javaFilePath, javaFileContent));
    }

    /**
     * Before making adjustments:
     * <p>
     * To store the {@code .class} file in the same directory as the {@code .java} file,
     * {@code javac} requires no options, but here, somehow the standard tomcat directory
     * is used, so the option {@code -d} is required.
     */
    private static String compileJavaFile(JavaFile javaFile) throws CompileException {
        final String outDir = javaFile.path.substring(0, javaFile.path.lastIndexOf("/"));
        javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        final StringWriter writer = new StringWriter();
        final List<? extends JavaFileObject> compilationUnits = Arrays.asList(javaFile);
        final List<String> options = Arrays.asList(
                "-encoding", "UTF-8",
                "-d", outDir
        );

        final JavaCompiler.CompilationTask task = compiler.getTask(writer, null, null, options, null, compilationUnits);

        final Boolean success = task.call();
        if (success) {
            return javaFile.path.replace(".java", ".class");
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
        return compileJavaTestFile(new JavaFile(javaTestFilePath), javaCutFilePath);
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
        return compileJavaTestFile(new JavaFile(javaFilePath, javaFileContent), javaCutFilePath);
    }

    /**
     * Just like {@link #compileJavaFile(JavaFile)}, but includes JUnit, Hamcrest and
     * Mockito libraries required for running the tests.
     */
    private static String compileJavaTestFile(JavaFile testFile, String javaCutFilePath) throws CompileException {
        final String outDir = testFile.path.substring(0, testFile.path.lastIndexOf("/"));
        final String cutDir = javaCutFilePath.substring(0, javaCutFilePath.lastIndexOf("/"));
        final String classPath = String.format(Constants.TEST_CLASSPATH_WITH_DIR, cutDir);

        javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        final StringWriter writer = new StringWriter();
        final List<? extends JavaFileObject> compilationUnits = Arrays.asList(testFile);

        final List<String> options = Arrays.asList(
                "-encoding", "UTF-8",
                "-d", outDir,
                "-classpath", classPath
        );

        final JavaCompiler.CompilationTask task = compiler.getTask(writer, null, null, options, null, compilationUnits);

        final Boolean success = task.call();
        if (success) {
            return testFile.path.replace(".java", ".class");
        } else {
            throw new CompileException(writer.toString());
        }
    }

    /**
     * {@link SimpleJavaFileObject} implementation, which allows for reading file content
     * from memory (by calling constructor with path <i>and</i> content) or reading
     * the file content from the hard-disk (by calling constructor just with path).
     * <p>
     * Inherited attributes {@code uri} and {@code kind}.
     */
    private static class JavaFile extends SimpleJavaFileObject {
        private String path;
        private String content;

        /**
         * Constructor for reading file content.
         *
         * @param path File path.
         */
        JavaFile(String path) {
            super(URI.create("file:///" + path), JavaFileObject.Kind.SOURCE);
            this.path = path;
            this.content = null;
        }

        /**
         * Constructor with file content already given.
         *
         * @param path    File path.
         * @param content File content.
         */
        JavaFile(String path, String content) {
            super(URI.create("file:///" + path), JavaFileObject.Kind.SOURCE);
            this.path = path;
            this.content = content;
        }

        /**
         * Returns the content of the java file. If no content is specified yet, the
         * file content is read from the hard disk.
         *
         * @param ignoreEncodingErrors ignored to match parent method signature
         * @return the content of the java file.
         * @throws IOException when reading the file fails.
         */
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            if (content == null) {
                content = String.join("\n", Files.readAllLines(Paths.get(uri)));
            }
            return content;
        }
    }

}
