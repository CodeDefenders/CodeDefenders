/*
 * Copyright (C) 2016-2023 Code Defenders contributors
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.codedefenders.analysis.coverage.ast.AstCoverage;
import org.codedefenders.analysis.coverage.ast.AstCoverageGenerator;
import org.codedefenders.analysis.coverage.line.CoverageTokenAnalyser;
import org.codedefenders.analysis.coverage.line.CoverageTokenGenerator;
import org.codedefenders.analysis.coverage.line.CoverageTokens;
import org.codedefenders.analysis.coverage.line.DetailedLine;
import org.codedefenders.analysis.coverage.line.DetailedLineCoverage;
import org.codedefenders.analysis.coverage.line.SimpleLineCoverage;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.util.JavaParserUtils;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.tools.ExecFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CoverageGenerator {
    private static final Logger logger = LoggerFactory.getLogger(CoverageGenerator.class);
    private static final String JACOCO_REPORT_FILE = "jacoco.exec";


    private final AstCoverageGenerator astCoverageGenerator;
    private final CoverageTokenGenerator coverageTokenGenerator;
    private final CoverageTokenAnalyser coverageTokenAnalyser;

    @Inject
    public CoverageGenerator(AstCoverageGenerator astCoverageGenerator, CoverageTokenGenerator coverageTokenGenerator,
                             CoverageTokenAnalyser coverageTokenAnalyser) {
        this.astCoverageGenerator = astCoverageGenerator;
        this.coverageTokenGenerator = coverageTokenGenerator;
        this.coverageTokenAnalyser = coverageTokenAnalyser;
    }

    /**
     * Reads the coverage data for a test execution and extends it.
     *
     * <p>A 'jacoco.exec' file must be present in the test folder.
     *
     * @param gameClass The CUT.
     * @param testJavaFile The path to the test Java file (a 'jacoco.exe' file must exist in the same directory).
     * @return The LineCoverage of the Test on the CUT.
     */
    public LineCoverage generate(GameClass gameClass, Path testJavaFile)
            throws CoverageGeneratorException {
        final Path execFile = findJacocoExecFile(testJavaFile);
        final Collection<Path> relevantClassFiles = findRelevantClassFiles(gameClass);

        CoverageBuilder coverageBuilder = readJacocoCoverage(execFile, relevantClassFiles);
        DetailedLineCoverage originalCoverage = extractLineCoverage(coverageBuilder, gameClass);

        CompilationUnit compilationUnit = JavaParserUtils.parse(gameClass.getSourceCode())
                .orElseThrow(() -> new CoverageGeneratorException("Could not parse java file: " + gameClass.getJavaFile()));

        return generate(originalCoverage, compilationUnit).getLineCoverage();
    }

    /**
     * Extends the given line coverage.
     *
     * @param originalCoverage The original line coverage.
     * @param compilationUnit The CompilationUnit for the file that the line coverage is for.
     * @return The extended line coverage.
     */
    CoverageGeneratorResult generate(DetailedLineCoverage originalCoverage, CompilationUnit compilationUnit) {
        AstCoverage astCoverage = astCoverageGenerator.generate(compilationUnit, originalCoverage);

        CoverageTokens coverageTokens = coverageTokenGenerator.generate(compilationUnit, originalCoverage, astCoverage);

        SimpleLineCoverage transformedCoverage = coverageTokenAnalyser.analyse(coverageTokens);

        return new CoverageGeneratorResult(originalCoverage, transformedCoverage, coverageTokens);
    }

    /**
     * Reads the coverage from a 'jacoco.exec' file.
     *
     * @param execFile The path to the 'jacoco.exec' file.
     * @param relevantClassFiles Paths to all class files that are relevant for the coverage.
     * @return The coverage in JaCoCo's format.
     */
    CoverageBuilder readJacocoCoverage(Path execFile, Collection<Path> relevantClassFiles)
            throws CoverageGeneratorException {
        final ExecFileLoader execFileLoader = new ExecFileLoader();
        try {
            execFileLoader.load(execFile.toFile());
        } catch (IOException e) {
            throw new CoverageGeneratorException("Failed to load jacoco.exec file: " + execFile, e);
        }

        final ExecutionDataStore executionDataStore = execFileLoader.getExecutionDataStore();
        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);

        for (Path classFile : relevantClassFiles) {
            try (InputStream is = Files.newInputStream(classFile)) {
                analyzer.analyzeClass(is, classFile.toString());
            } catch (IOException e) {
                throw new CoverageGeneratorException("Failed to analyze file: " + classFile, e);
            }
        }

        return coverageBuilder;
    }

    /**
     * Given a path to a test's Java file, finds the path to the test's 'jacoco.exec' file.
     */
    Path findJacocoExecFile(Path testJavaFile)
            throws CoverageGeneratorException {
        final Path reportDirectory = testJavaFile.getParent();
        final Path execFile = reportDirectory.resolve(JACOCO_REPORT_FILE);
        if (!Files.isRegularFile(execFile)) {
            throw new CoverageGeneratorException("Could not find JaCoCo exec file for test: " + testJavaFile);
        }
        return execFile;
    }

    /**
     * Given a CUT, finds paths to all class files that are relevant for computing coverage for the CUT.
     */
    Collection<Path> findRelevantClassFiles(GameClass gameClass) throws CoverageGeneratorException {
        final Path classFileFolder = Paths.get(gameClass.getClassFile()).getParent();

        try (Stream<Path> files = Files.list(classFileFolder)
                .filter(path -> path.toString().endsWith(".class"))) {
            return files.collect(Collectors.toList());
        } catch (IOException e) {
            throw new CoverageGeneratorException("Could not list class files for class: " + gameClass.getClassFile());
        }
    }

    /**
     * Extracts JaCoCo's (source file) coverage in to a {@link DetailedLineCoverage}.
     */
    DetailedLineCoverage extractLineCoverage(CoverageBuilder coverageBuilder, GameClass gameClass) {
        DetailedLineCoverage coverage = new DetailedLineCoverage();

        for (ISourceFileCoverage sourceCoverage : coverageBuilder.getSourceFiles()) {
            if (!gameClass.getJavaFile().endsWith(sourceCoverage.getName())) {
                continue;
            }
            for (int line = sourceCoverage.getFirstLine(); line <= sourceCoverage.getLastLine(); line++) {
                final ILine lineCoverage = sourceCoverage.getLine(line);
                coverage.set(line, DetailedLine.fromJaCoCo(lineCoverage));
            }
        }

        return coverage;
    }

    public static class CoverageGeneratorException extends Exception {
        public CoverageGeneratorException(String message) {
            super(message);
        }

        public CoverageGeneratorException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    static class CoverageGeneratorResult {
        public final DetailedLineCoverage originalCoverage;
        public final SimpleLineCoverage transformedCoverage;
        public final CoverageTokens coverageTokens;

        public CoverageGeneratorResult(
                DetailedLineCoverage originalCoverage,
                SimpleLineCoverage transformedCoverage,
                CoverageTokens coverageTokens) {
            this.originalCoverage = originalCoverage;
            this.transformedCoverage = transformedCoverage;
            this.coverageTokens = coverageTokens;
        }

        public LineCoverage getLineCoverage() {
            List<Integer> coveredLines = new ArrayList<>();
            List<Integer> uncoveredLines = new ArrayList<>();
            int firstLine = transformedCoverage.getFirstLine();
            int lastLine = transformedCoverage.getLastLine();
            for (int line = firstLine; line <= lastLine; line++) {
                switch (transformedCoverage.get(line)) {
                    case PARTLY_COVERED:
                    case FULLY_COVERED:
                        coveredLines.add(line);
                        break;
                    case NOT_COVERED:
                        uncoveredLines.add(line);
                        break;
                    case EMPTY:
                        break;
                    default:
                        throw new IllegalStateException("Encountered unknown LineCoverageStatus " + transformedCoverage.get(line));
                }
            }
            return new LineCoverage(coveredLines, uncoveredLines);
        }
    }
}
