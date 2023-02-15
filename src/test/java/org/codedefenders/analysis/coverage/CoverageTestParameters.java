package org.codedefenders.analysis.coverage;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.tools.JavaFileObject;

import org.codedefenders.analysis.coverage.line.LineCoverageStatus;
import org.codedefenders.analysis.coverage.line.NewLineCoverage;
import org.codedefenders.analysis.coverage.line.SimpleLineCoverage;
import org.codedefenders.analysis.coverage.util.InMemorySourceFile;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import static com.google.common.truth.TruthJUnit.assume;
import static org.codedefenders.util.ResourceUtils.loadResource;

class CoverageTestParameters implements ArgumentsProvider {
    private final static Pattern NAME_REGEX = Pattern.compile("([A-Za-z][A-Za-z1-9]*)\\.java");

    private final static List<String> UTILS_FILES = Arrays.asList(
            "utils/Call.java",
            "utils/MethodChain.java",
            "utils/TestClass.java",
            "utils/TestEnum.java",
            "utils/TestException.java",
            "utils/TestRuntimeException.java",
            "utils/ThrowingAutoCloseable.java",
            "utils/ThrowingClass.java",
            "utils/ThrowingIterable.java",
            "utils/Utils.java"
    );

    private String getClassNameFromPath(String path) {
        String filename = Paths.get(path).getFileName().toString();
        Matcher matcher = NAME_REGEX.matcher(filename);
        assume().withMessage("Could not determine class name from path '%s'", path)
                .that(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private NewLineCoverage readExpectedCoverage(String path) {
        String text = loadResource(CoverageTest.RESOURCE_DIR, path);
        SimpleLineCoverage expectedCoverage = new SimpleLineCoverage();
        String[] lines = text.split("\r?\n");
        for (int lineNum = 1; lineNum <= lines.length; lineNum++) {
            String line = lines[lineNum - 1].trim();
            if (!line.isEmpty()) {
                expectedCoverage.set(lineNum, LineCoverageStatus.valueOf(line));
            }
        }
        return expectedCoverage;
    }

    private Arguments testCase(String cutPath,
                               String testPath,
                               String[] testArguments,
                               List<String> additionalJavaFiles,
                               String expectedCoveragePath)
            throws Exception {
        String cutName = getClassNameFromPath(cutPath);
        String testName = getClassNameFromPath(testPath);
        String classCode = loadResource(CoverageTest.RESOURCE_DIR, cutPath);

        List<String> javaFiles = new ArrayList<>();
        javaFiles.add(cutPath);
        javaFiles.add(testPath);
        javaFiles.addAll(additionalJavaFiles);
        javaFiles.addAll(UTILS_FILES);

        List<JavaFileObject> sourceFiles = new ArrayList<>();
        for (String path : javaFiles) {
            String name = getClassNameFromPath(path);
            String code = loadResource(CoverageTest.RESOURCE_DIR, path);
            sourceFiles.add(new InMemorySourceFile(name, code));
        }

        NewLineCoverage expectedCoverage = readExpectedCoverage(expectedCoveragePath);
        return Arguments.of(cutName, testName,
                sourceFiles, classCode, expectedCoverage, testArguments);
    }

    private Arguments simpleTestCase(String cutPath, String testPath)
            throws Exception {
        String testName = getClassNameFromPath(testPath);
        String dirname = Paths.get(cutPath).getParent().toString();
        return testCase(cutPath,
                testPath, new String[0],
                Collections.emptyList(),
                String.format("%s/%s.coverage", dirname, testName));
    }

    private Arguments emptyRunnerTestCase(String cutPath) throws Exception {
        String dirname = Paths.get(cutPath).getParent().toString();
        return testCase(
                cutPath,
                "EmptyRunner.java", new String[0],
                Collections.emptyList(),
                dirname + "/EmptyRunner.coverage"
        );
    }

    private Arguments defaultRunnerTestCase(String cutPath) throws Exception {
        String dirname = Paths.get(cutPath).getParent().toString();
        String cutName = getClassNameFromPath(cutPath);
        return testCase(
                cutPath,
                "DefaultRunner.java", new String[]{cutName},
                Collections.emptyList(),
                dirname + "/DefaultRunner.coverage"
        );
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
        return Stream.of(
                // playground
                defaultRunnerTestCase("playground/Playground.java"),

                // class level
                simpleTestCase("classes/Classes.java", "classes/ClassesTest.java"),
                emptyRunnerTestCase("classes/Classes.java"),

                simpleTestCase("fields/Fields.java", "fields/FieldsTest.java"),
                emptyRunnerTestCase("fields/Fields.java"),

                emptyRunnerTestCase("initializerblocks/InitializerBlocks.java"),
                simpleTestCase("initializerblocks/InitializerBlocks.java",
                        "initializerblocks/InitializerBlocksTest.java"),

                // method level
                simpleTestCase("constructors/Constructors.java", "constructors/ConstructorsTest.java"),
                emptyRunnerTestCase("constructors/Constructors.java"),

                simpleTestCase("methods/Methods.java", "methods/MethodsTest.java"),
                emptyRunnerTestCase("methods/Methods.java"),

                // block level
                defaultRunnerTestCase("blocks/Blocks.java"),
                emptyRunnerTestCase("blocks/Blocks.java"),

                emptyRunnerTestCase("ifs/Ifs.java"),
                defaultRunnerTestCase("ifs/Ifs.java"),

                defaultRunnerTestCase("trycatchblocks/TryCatchBlocks.java"),
                emptyRunnerTestCase("trycatchblocks/TryCatchBlocks.java"),

                defaultRunnerTestCase("switchstmts/SwitchStmts.java"),
                emptyRunnerTestCase("switchstmts/SwitchStmts.java"),

                defaultRunnerTestCase("forloops/ForLoops.java"),
                emptyRunnerTestCase("forloops/ForLoops.java"),

                defaultRunnerTestCase("foreachloops/ForEachLoops.java"),
                emptyRunnerTestCase("foreachloops/ForEachLoops.java"),

                defaultRunnerTestCase("whileloops/WhileLoops.java"),
                emptyRunnerTestCase("whileloops/WhileLoops.java"),

                defaultRunnerTestCase("dowhileloops/DoWhileLoops.java"),
                emptyRunnerTestCase("dowhileloops/DoWhileLoops.java"),

                defaultRunnerTestCase("synchronizedblocks/SynchronizedBlocks.java"),
                emptyRunnerTestCase("synchronizedblocks/SynchronizedBlocks.java"),

                // statement level
                defaultRunnerTestCase("assignments/Assignments.java"),
                emptyRunnerTestCase("assignments/Assignments.java"),

                defaultRunnerTestCase("methodcalls/MethodCalls.java"),
                emptyRunnerTestCase("methodcalls/MethodCalls.java"),

                defaultRunnerTestCase("constructorcalls/ConstructorCalls.java"),
                emptyRunnerTestCase("constructorcalls/ConstructorCalls.java"),

                defaultRunnerTestCase("localvariables/LocalVariables.java"),
                emptyRunnerTestCase("localvariables/LocalVariables.java"),

                defaultRunnerTestCase("assertions/Assertions.java"),
                emptyRunnerTestCase("assertions/Assertions.java"),

                // expression level
                defaultRunnerTestCase("lambdas/Lambdas.java"),
                emptyRunnerTestCase("lambdas/Lambdas.java"),

                defaultRunnerTestCase("unaryexpressions/UnaryExpressions.java"),
                emptyRunnerTestCase("unaryexpressions/UnaryExpressions.java"),

                defaultRunnerTestCase("binaryexpressions/BinaryExpressions.java"),
                emptyRunnerTestCase("binaryexpressions/BinaryExpressions.java"),

                defaultRunnerTestCase("arrays/Arrays.java"),
                emptyRunnerTestCase("arrays/Arrays.java"),

                defaultRunnerTestCase("ternaryoperators/TernaryOperators.java"),
                emptyRunnerTestCase("ternaryoperators/TernaryOperators.java"),

                defaultRunnerTestCase("switchexprs/SwitchExprs.java"),
                emptyRunnerTestCase("switchexprs/SwitchExprs.java"),

                defaultRunnerTestCase("casts/Casts.java"),
                emptyRunnerTestCase("casts/Casts.java"),

                defaultRunnerTestCase("instanceof/Instanceof.java"),
                emptyRunnerTestCase("instanceof/Instanceof.java"),

                // real CUT tests
                simpleTestCase("xmlelement/XmlElement.java", "xmlelement/XmlElementTest.java")
        );
    }
}
