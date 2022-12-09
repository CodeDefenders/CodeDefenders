package org.codedefenders.analysis.coverage;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.codedefenders.analysis.coverage.util.InMemoryClassLoader;
import org.codedefenders.analysis.coverage.util.InMemoryJavaFileManager;
import org.codedefenders.analysis.coverage.util.InMemorySourceFile;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jacoco.core.runtime.RuntimeData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.TruthJUnit.assume;

// Adapted from the JaCoCo "CoreTutorial.java" API example.
// See https://www.jacoco.org/jacoco/trunk/doc/api.html
public class CoverageTest {
    private final static String DEMO_CLASS_NAME = "CoverageDemo";
    private final static String DEMO_CLASS_PATH = "analysis/CoverageDemo.java";

    // TODO: extract getJavaMajorVersion into a util class and use it in Configuration as well?
    // TODO: put the InMemory* JavaCompiler into a src package and use it in Compiler as well?

    @BeforeAll
    public static void checkJavaVersion() {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion.startsWith("1.")) {
            javaVersion = javaVersion.substring(2);
        }
        int dotPos = javaVersion.indexOf('.');
        int dashPos = javaVersion.indexOf('-');
        int javaMajorVersion = Integer.parseInt(javaVersion.substring(0,
                dotPos > -1 ? dotPos : dashPos > -1 ? dashPos : 1));
        assume()
                .withMessage("This test only works with Java versions greater >= 16")
                .that(javaMajorVersion).isAtLeast(16);
    }

    @Test
    public void test() throws Exception {
        final IRuntime runtime = new LoggerRuntime();
        final List<JavaFileObject> demoSourceFiles = getDemoSourceCode();
        final Map<String, byte[]> originals = compileCode(demoSourceFiles);
        final Map<String, byte[]> instrumented = instrumentCode(runtime, originals);

        final RuntimeData data = new RuntimeData();
        runtime.startup(data);

        final InMemoryClassLoader memoryClassLoader = new InMemoryClassLoader(instrumented);
        final Class<?> targetClass = memoryClassLoader.loadClass(DEMO_CLASS_NAME);

        // run the code
        // TODO: extract multiple test methods/files
        Method main = targetClass.getMethod("main", String[].class);
        main.invoke(null, (Object) null);

        final ExecutionDataStore executionData = new ExecutionDataStore();
        final SessionInfoStore sessionInfos = new SessionInfoStore();
        data.collect(executionData, sessionInfos, false);
        runtime.shutdown();

        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(executionData, coverageBuilder);
        for (byte[] byteCode : originals.values()) {
            analyzer.analyzeClass(byteCode, DEMO_CLASS_NAME);
        }

        // print coverage
        // for (ISourceFileCoverage coverage : coverageBuilder.getSourceFiles()) {
        //     System.out.println(coverage.getName());
        //     for (int line = coverage.getFirstLine(); line <= coverage.getLastLine(); line++) {
        //         final ILine lineCoverage = coverage.getLine(line);
        //         int totalIns = lineCoverage.getInstructionCounter().getTotalCount();
        //         int missedIns = lineCoverage.getInstructionCounter().getMissedCount();
        //         int totalBr = lineCoverage.getBranchCounter().getTotalCount();
        //         int missedBr = lineCoverage.getBranchCounter().getMissedCount();
        //         final int status = lineCoverage.getInstructionCounter().getStatus();
        //         System.out.printf("line: %d, totalIns: %d, missedIns: %d, totalBr: %d, missedBr: %d, status: %d\n",
        //                 line, totalIns, missedIns, totalBr, missedBr, status);
        //     }
        // }
    }

    private static List<JavaFileObject> getDemoSourceCode() throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource(DEMO_CLASS_PATH);
        String code = new String(Files.readAllBytes(Paths.get(url.getPath())), StandardCharsets.UTF_8);
        JavaFileObject sourceFile = new InMemorySourceFile(DEMO_CLASS_NAME, code);
        return Collections.singletonList(sourceFile);
    }

    public Map<String, byte[]> compileCode(List<JavaFileObject> sourceFiles) throws Exception {
        // set up compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new Exception("Platform provided no java compiler.");
        }

        // set up compiler options
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
        if (!success) {
            throw new Exception("Compilation failed.");
        }
        return fileManager.getClassFiles();
    }

    public Map<String, byte[]> instrumentCode(IRuntime runtime, Map<String, byte[]> classFiles) {
        final Instrumenter instr = new Instrumenter(runtime);
        return classFiles.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            try {
                                return instr.instrument(entry.getValue(), entry.getKey());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }));
    }
}
