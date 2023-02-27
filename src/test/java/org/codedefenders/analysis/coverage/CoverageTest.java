package org.codedefenders.analysis.coverage;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.codedefenders.analysis.coverage.CoverageGenerator.CoverageGeneratorResult;
import org.codedefenders.analysis.coverage.line.DetailedLine;
import org.codedefenders.analysis.coverage.line.DetailedLineCoverage;
import org.codedefenders.analysis.coverage.line.NewLineCoverage;
import org.codedefenders.analysis.coverage.util.CoverageOutputWriter;
import org.codedefenders.analysis.coverage.util.InMemoryClassLoader;
import org.codedefenders.analysis.coverage.util.InMemoryJavaFileManager;
import org.codedefenders.util.JavaParserUtils;
import org.codedefenders.util.JavaVersionUtils;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jacoco.core.runtime.RuntimeData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import com.github.javaparser.ast.CompilationUnit;

import static com.google.common.truth.Truth.assert_;
import static com.google.common.truth.TruthJUnit.assume;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * <p>Adapted from the JaCoCo "CoreTutorial" API example.
 * See https://www.jacoco.org/jacoco/trunk/doc/api.html
 *
 * <p>Coverage and HTML output for easier debugging can be enabled in {@link CoverageOutputWriter}.
 */
public class CoverageTest {
    public final static String RESOURCE_DIR = "org/codedefenders/analysis/coverage";

    @BeforeEach
    public void checkJavaVersion() {
        int javaMajorVersion = JavaVersionUtils.getJavaMajorVersion();
        assume().withMessage("Coverage tests only work with Java version >= 16")
                .that(javaMajorVersion).isAtLeast(16);
    }

    @ParameterizedTest(name = "[{index}] {0} with {1}")
    @ArgumentsSource(CoverageTestParameters.class)
    public void test(String className,
                      String testName,
                      List<JavaFileObject> sourceFiles,
                      String classCode,
                      NewLineCoverage expectedCoverage,
                      String[] testArguments) throws Exception {
        // compile and instrument code
        final IRuntime runtime = new LoggerRuntime();
        final Map<String, byte[]> originals = compileCode(sourceFiles);
        final Map<String, byte[]> instrumented = instrumentCode(runtime, originals);

        // start up JaCoCo runtime
        final RuntimeData data = new RuntimeData();
        runtime.startup(data);

        // load instrumented code
        final InMemoryClassLoader memoryClassLoader = new InMemoryClassLoader(instrumented);
        memoryClassLoader.setDefaultAssertionStatus(true); // enable assertions for tests
        final Class<?> targetClass = memoryClassLoader.loadClass(testName);

        // run the test
        Method main = targetClass.getMethod("main", String[].class);
        main.invoke(null, new Object[]{testArguments});

        // collect JaCoCo data
        final ExecutionDataStore executionData = new ExecutionDataStore();
        final SessionInfoStore sessionInfos = new SessionInfoStore();
        data.collect(executionData, sessionInfos, false);
        runtime.shutdown();
        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);
        for (Map.Entry<String, byte[]> entry : originals.entrySet()) {
            analyzer.analyzeClass(entry.getValue(), entry.getKey());
        }

        // get coverage data and AST
        DetailedLineCoverage originalCoverage = extractLineCoverage(coverageBuilder, className);
        CompilationUnit compilationUnit = JavaParserUtils.parse(classCode)
                .orElseThrow(() -> new Exception("Could not parse fixture source code."));

        // transform the coverage
        CoverageGenerator coverageGenerator = new CoverageGenerator() {{
            testMode = true;
        }};
        CoverageGeneratorResult result = coverageGenerator.generate(originalCoverage, compilationUnit);

        // write HTML report if enabled
        CoverageOutputWriter writer = new CoverageOutputWriter(
                className,
                testName,
                classCode,
                originalCoverage,
                result.transformedCoverage,
                expectedCoverage,
                result.lineTokens);
        writer.writeCoverage();
        writer.writeHtml();

        // assertions
        assertSameCoverage(result.transformedCoverage, expectedCoverage);
    }

    public Map<String, byte[]> compileCode(List<JavaFileObject> sourceFiles) {
        // set up compiler and options
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assume().withMessage("Platform provided no java compiler.")
                .that(compiler).isNotNull();
        List<String> options = Arrays.asList(
                "-encoding", "UTF-8",
                "-source", "16",
                "-target", "16"
        );

        // set up in-memory file manager
        JavaFileManager standardFileManager = compiler.getStandardFileManager(
                null, null, StandardCharsets.UTF_8);
        InMemoryJavaFileManager fileManager = new InMemoryJavaFileManager(standardFileManager);

        // compile
        final JavaCompiler.CompilationTask task =
                compiler.getTask(null, fileManager, null, options, null, sourceFiles);
        final Boolean success = task.call();
        assume().withMessage("Failed to compile fixture code.")
                .that(success).isTrue();
        return fileManager.getClassFiles();
    }

    public Map<String, byte[]> instrumentCode(IRuntime runtime, Map<String, byte[]> classFiles) throws IOException {
        final Instrumenter instr = new Instrumenter(runtime);

        // iterate through class files and construct a new map with instrumented code
        Map<String, byte[]> instrumentedClassFiles = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : classFiles.entrySet()) {
            String name = entry.getKey();
            byte[] bytecode = entry.getValue();
            instrumentedClassFiles.put(name, instr.instrument(bytecode, name));
        }

        return instrumentedClassFiles;
    }

    public DetailedLineCoverage extractLineCoverage(CoverageBuilder coverageBuilder, String className) {
        DetailedLineCoverage coverage = new DetailedLineCoverage();

        for (ISourceFileCoverage sourceCoverage : coverageBuilder.getSourceFiles()) {
            if (!sourceCoverage.getName().endsWith(className + ".java")) {
                continue;
            }

            for (int line = sourceCoverage.getFirstLine(); line <= sourceCoverage.getLastLine(); line++) {
                final ILine lineCoverage = sourceCoverage.getLine(line);
                coverage.set(line, DetailedLine.fromJaCoCo(lineCoverage));
            }
        }

        return coverage;
    }

    public void assertSameCoverage(NewLineCoverage actual, NewLineCoverage expected) {
        int firstLine = Math.min(actual.getFirstLine(), expected.getFirstLine());
        int lastLine = Math.max(actual.getLastLine(), expected.getLastLine());

        List<Executable> assertions = new ArrayList<>();
        for (int line = firstLine; line <= lastLine; line++) {
            final int finalLine = line;
            assertions.add(() -> {
                assert_()
                        .withMessage("Coverage on line %s", finalLine)
                        .that(actual.getStatus(finalLine))
                        .isEqualTo(expected.getStatus(finalLine));
            });
        }
        assertAll(assertions);
    }
}
