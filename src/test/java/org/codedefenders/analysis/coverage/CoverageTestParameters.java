/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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
    private static final Pattern NAME_REGEX = Pattern.compile("([A-Za-z][A-Za-z1-9]*)\\.java");

    private static final List<String> UTILS_FILES = Arrays.asList(
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

    /**
     * Provides arguments for CoverageTest.
     * @param cutPath The path to the CUT java file.
     * @param testPath The path to the test java file.
     * @param testArguments Arguments passed to the test file's main function.
     * @param additionalJavaFiles Additional java files that should be compiled.
     * @param expectedCoveragePath Path to the expected coverage information.
     */
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
        return Arguments.of(cutName, testName, sourceFiles, classCode, expectedCoverage, testArguments);
    }

    /**
     * Provides arguments for CoverageTest in a simpler form.
     * The path to the expected coverage is derived from the class and test paths (class_dir/test_name.coverage).
     * @param cutPath The path to the CUT java file.
     * @param testPath The path to the test java file.
     */
    private Arguments simpleTestCase(String cutPath, String testPath)
            throws Exception {
        String testName = getClassNameFromPath(testPath);
        String dirname = Paths.get(cutPath).getParent().toString();
        return testCase(cutPath,
                testPath, new String[0],
                Collections.emptyList(),
                String.format("%s/%s.coverage", dirname, testName));
    }

    /**
     * Provides arguments to run a CoverageTest with EmptyRunner.
     * The path to the expected coverage is derived from the class and test paths (class_dir/EmptyRunner.coverage).
     * @param cutPath The path to the CUT java file.
     */
    private Arguments emptyRunnerTestCase(String cutPath) throws Exception {
        String dirname = Paths.get(cutPath).getParent().toString();
        return testCase(
                cutPath,
                "EmptyRunner.java", new String[0],
                Collections.emptyList(),
                dirname + "/EmptyRunner.coverage"
        );
    }

    /**
     * Provides arguments to run a CoverageTest with DefaultRunner.
     * The path to the expected coverage is derived from the class and test paths (class_dir/DefaultRunner.coverage).
     * @param cutPath The path to the CUT java file.
     */
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
                // defaultRunnerTestCase("playground/Playground.java"),

                // class level
                defaultRunnerTestCase("classes/Classes.java"),
                emptyRunnerTestCase("classes/Classes.java"),

                defaultRunnerTestCase("fields/Fields.java"),
                emptyRunnerTestCase("fields/Fields.java"),

                emptyRunnerTestCase("initializerblocks/InitializerBlocks.java"),
                defaultRunnerTestCase("initializerblocks/InitializerBlocks.java"),

                // method level
                defaultRunnerTestCase("constructors/Constructors.java"),
                emptyRunnerTestCase("constructors/Constructors.java"),

                defaultRunnerTestCase("methods/Methods.java"),
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

                defaultRunnerTestCase("enclosedexprs/EnclosedExprs.java"),
                emptyRunnerTestCase("enclosedexprs/EnclosedExprs.java"),

                // other tests
                defaultRunnerTestCase("etc/Etc.java"),
                emptyRunnerTestCase("etc/Etc.java"),

                // real CUT tests
                simpleTestCase("cuts/bytevector/ByteVector.java",
                        "cuts/bytevector/ByteVectorTest1.java"),
                simpleTestCase("cuts/bytevector/ByteVector.java",
                        "cuts/bytevector/ByteVectorTest2.java"),

                simpleTestCase("cuts/caseinsensitivestring/CaseInsensitiveString.java",
                        "cuts/caseinsensitivestring/CaseInsensitiveStringTest.java"),

                simpleTestCase("cuts/xmlelement/XmlElement.java",
                        "cuts/xmlelement/XmlElementTest.java")
        );
    }
}
