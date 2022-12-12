package org.codedefenders.analysis.coverage;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.apache.commons.text.StringEscapeUtils;
import org.codedefenders.analysis.coverage.line.LineCoverageMapping;
import org.codedefenders.analysis.coverage.line.LineCoverageStatus;
import org.codedefenders.analysis.coverage.util.InMemoryClassLoader;
import org.codedefenders.analysis.coverage.util.InMemoryJavaFileManager;
import org.codedefenders.analysis.coverage.util.InMemorySourceFile;
import org.codedefenders.util.JavaParserUtils;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.javaparser.ast.CompilationUnit;

import static com.google.common.truth.TruthJUnit.assume;

// Adapted from the JaCoCo "CoreTutorial.java" API example.
// See https://www.jacoco.org/jacoco/trunk/doc/api.html
public class CoverageTest {
    private final static String DEMO_CLASS_NAME = "CoverageDemo";
    private final static String DEMO_CLASS_PATH = "analysis/CoverageDemo.java";

    // output HTML with original and extended coverage for easier comparison
    private final static String HTML_PATH = "/tmp/coverage.html";
    private final static boolean OUTPUT_HTML = true;

    // TODO: extract getJavaMajorVersion into a util class and use it in Configuration as well?
    // TODO: put the InMemory* JavaCompiler into a src package and use it in Compiler as well?
    // TODO: use CDI for CoverageGenerator

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
        final String demoSourceCode = getDemoSourceCode();
        final List<JavaFileObject> demoSourceFiles = getDemoSourceFiles();
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

        LineCoverageMapping originalCoverage = extractLineCoverageMapping(coverageBuilder);
        CompilationUnit compilationUnit = JavaParserUtils.parse(demoSourceCode)
                .orElseThrow(() -> new Exception("Could not parse demo source code."));

        CoverageGenerator coverageGenerator = new CoverageGenerator();
        LineCoverageMapping extendedCoverage = coverageGenerator.generate(originalCoverage, compilationUnit);

        if (OUTPUT_HTML) {
            try (PrintWriter writer = new PrintWriter(HTML_PATH)) {
                writer.write(generateHtml(demoSourceCode, originalCoverage, extendedCoverage));
            }
        }
    }

    private static String getDemoSourceCode() throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource(DEMO_CLASS_PATH);
        return new String(Files.readAllBytes(Paths.get(url.getPath())), StandardCharsets.UTF_8);
    }

    private static List<JavaFileObject> getDemoSourceFiles() throws Exception {
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

    public Map<String, byte[]> instrumentCode(IRuntime runtime, Map<String, byte[]> classFiles) throws IOException {
        final Instrumenter instr = new Instrumenter(runtime);
        Map<String, byte[]> instrumentedClassFiles = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : classFiles.entrySet()) {
            String name = entry.getKey();
            byte[] bytecode = entry.getValue();
            instrumentedClassFiles.put(name, instr.instrument(bytecode, name));
        }
        return instrumentedClassFiles;
    }

    public LineCoverageMapping extractLineCoverageMapping(CoverageBuilder coverageBuilder) {
        LineCoverageMapping lineMapping = new LineCoverageMapping();

        for (ISourceFileCoverage coverage : coverageBuilder.getSourceFiles()) {
            if (!DEMO_CLASS_PATH.endsWith(coverage.getName())) {
                continue;
            }

            for (int line = coverage.getFirstLine(); line <= coverage.getLastLine(); line++) {
                final ILine lineCoverage = coverage.getLine(line);
                int totalIns = lineCoverage.getInstructionCounter().getTotalCount();
                int missedIns = lineCoverage.getInstructionCounter().getMissedCount();
                int totalBr = lineCoverage.getBranchCounter().getTotalCount();
                int missedBr = lineCoverage.getBranchCounter().getMissedCount();
                final int status = lineCoverage.getInstructionCounter().getStatus();
                lineMapping.put(line, LineCoverageStatus.fromJacoco(status));
            }
        }

        return lineMapping;
    }

    public String generateHtml(String sourceCode, LineCoverageMapping originalCoverage, LineCoverageMapping extendedCoverage) {
        String template = String.join("\n",
                "<!DOCTYPE html>",
                "<html>",
                "    <head>",
                "        <meta charset=\"utf-8\">",
                "        <title>{title}</title>",
                "        <style>",
                "            .line {",
                "                font-family: monospace;",
                "                width: 100%;",
                "                white-space: nowrap;",
                "                overflow: hidden;",
                "                text-overflow: ellipsis;",
                "            }",
                "            .line::before {",
                "                font-family: monospace;",
                "                content: attr(line-num);",
                "                display: inline-block;",
                "                width: 3em;",
                "            }",
                "            .EMPTY {",
                "                background: transparent;",
                "            }",
                "            .FULLY_COVERED {",
                "                background: #73ff73;",
                "            }",
                "            .PARTLY_COVERED {",
                "                background: #fff673;",
                "            }",
                "            .NOT_COVERED {",
                "                background: #ff7373;",
                "            }",
                "        </style>",
                "    </head>",
                "    <body>",
                "        <div style=\"display: flex; flex-direction: row; gap: .5em;\">",
                "            <div style=\"width: calc(50% - .25em);\">",
                "                {code_original}",
                "            </div>",
                "            <div style=\"width: calc(50% - .25em);\">",
                "                {code_extended}",
                "            </div>",
                "    </body>",
                "</html>"
        );
        String lineTemplate = String.join("\n",
                "<div class=\"line {coverage_status}\" line-num=\"{line_num}\">",
                "    {code}",
                "</div>"
        );

        StringJoiner originalLines = new StringJoiner("\n");
        StringJoiner extendedLines = new StringJoiner("\n");

        int lineNum = 1;
        for (String line : sourceCode.split("\r?\n")) {
            String escapedLine = StringEscapeUtils.escapeHtml4(line)
                    .replaceAll(" ", "&nbsp");

            String htmlLine = lineTemplate
                    .replace("{line_num}", Integer.toString(lineNum))
                    .replace("{code}", escapedLine);

            LineCoverageStatus originalStatus = originalCoverage.get(lineNum);
            LineCoverageStatus extendedStatus = extendedCoverage.get(lineNum);

            originalLines.add(htmlLine.replace("{coverage_status}", originalStatus.name()));
            extendedLines.add(htmlLine.replace("{coverage_status}", extendedStatus.name()));

            lineNum++;
        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        return template
                .replace("{title}", timeFormatter.format(now))
                .replace("{code_original}", originalLines.toString())
                .replace("{code_extended}", extendedLines.toString());
    }
}
