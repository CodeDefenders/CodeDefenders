package org.codedefenders.analysis.coverage;

import java.util.ArrayList;
import java.util.Arrays;
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
    private final static String RESOURCE_DIR = "analysis/coverage";
    private final static Pattern NAME_REGEX = Pattern.compile("([A-Za-z][A-Za-z1-9]*)\\.java");

    private String getClassNameFromPath(String path) {
        Matcher matcher = NAME_REGEX.matcher(path);
        assume().withMessage("Could not determine class name from path '%s'", path)
                .that(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private NewLineCoverage readExpectedCoverage(String path) {
        String text = loadResource(RESOURCE_DIR, path);
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

    private Arguments testCase(String classPath, String testPath, String expectedCoveragePath, String... utilsPaths)
            throws Exception {
        String className = getClassNameFromPath(classPath);
        String testName = getClassNameFromPath(testPath);

        String classCode = loadResource(RESOURCE_DIR, classPath);
        String testCode = loadResource(RESOURCE_DIR, testPath);

        List<JavaFileObject> sourceFiles = new ArrayList<>();
        sourceFiles.add(new InMemorySourceFile(className, classCode));
        sourceFiles.add(new InMemorySourceFile(testName, testCode));

        for (String path : utilsPaths) {
            String name = getClassNameFromPath(path);
            String code = loadResource(RESOURCE_DIR, path);
            sourceFiles.add(new InMemorySourceFile(name, code));
        }

        NewLineCoverage expectedCoverage = readExpectedCoverage(expectedCoveragePath);
        return Arguments.of(className, testName, sourceFiles, classCode, expectedCoverage);
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
        return Stream.of(
                testCase(
                        "classes/Classes.java",
                        "classes/ClassesTest.java",
                        "classes/ClassesTest.coverage",
                        "Utils.java"
                ),
                testCase(
                        "classes/Classes.java",
                        "EmptyTest.java",
                        "classes/EmptyTest.coverage"
                ),
                testCase(
                        "fields/Fields.java",
                        "fields/FieldsTest.java",
                        "fields/FieldsTest.coverage",
                        "Utils.java"
                ),
                testCase(
                        "fields/Fields.java",
                        "EmptyTest.java",
                        "fields/EmptyTest.coverage",
                        "Utils.java"
                ),
                testCase(
                        "constructors/Constructors.java",
                        "constructors/ConstructorsTest.java",
                        "constructors/ConstructorsTest.coverage",
                        "Utils.java"
                ),
                testCase(
                        "constructors/Constructors.java",
                        "EmptyTest.java",
                        "constructors/EmptyTest.coverage",
                        "Utils.java"
                ),
                testCase(
                        "methods/Methods.java",
                        "methods/MethodsTest.java",
                        "methods/MethodsTest.coverage",
                        "Utils.java"
                ),
                testCase(
                        "methods/Methods.java",
                        "EmptyTest.java",
                        "methods/EmptyTest.coverage",
                        "Utils.java"
                ),
                testCase(
                        "localvariables/LocalVariables.java",
                        "localvariables/LocalVariablesTest.java",
                        "localvariables/LocalVariablesTest.coverage",
                        "Utils.java"
                ),
                testCase(
                        "localvariables/LocalVariables.java",
                        "EmptyTest.java",
                        "localvariables/EmptyTest.coverage",
                        "Utils.java"
                ),
                testCase(
                        "blocks/Blocks.java",
                        "blocks/BlocksTest.java",
                        "blocks/BlocksTest.coverage",
                        "Utils.java"
                ),
                testCase(
                        "blocks/Blocks.java",
                        "EmptyTest.java",
                        "blocks/EmptyTest.coverage",
                        "Utils.java"
                ),
                testCase(
                        "initializerblocks/InitializerBlocks.java",
                        "initializerblocks/InitializerBlocksTest.java",
                        "initializerblocks/InitializerBlocksTest.coverage",
                        "Utils.java"
                ),
                testCase(
                        "initializerblocks/InitializerBlocks.java",
                        "EmptyTest.java",
                        "initializerblocks/EmptyTest.coverage",
                        "Utils.java"
                ),
                testCase(
                        "playground/Playground.java",
                        "playground/PlaygroundTest.java",
                        "playground/PlaygroundTest.coverage",
                        "Utils.java"
                )
        );
    }
}
