/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import org.codedefenders.analysis.coverage.ast.AstCoverageMapping;
import org.codedefenders.analysis.coverage.ast.AstCoverageStatus;
import org.codedefenders.analysis.coverage.ast.AstCoverageVisitor;
import org.codedefenders.analysis.coverage.line.LineCoverageMapping;
import org.codedefenders.analysis.coverage.line.LineCoverageStatus;
import org.codedefenders.analysis.coverage.line.LineTokenAnalyser;
import org.codedefenders.analysis.coverage.line.LineTokenVisitor;
import org.codedefenders.analysis.coverage.line.LineTokens;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.util.JavaParserUtils;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.tools.ExecFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

/**
 * This class offers a static method {@link #generate(GameClass, Path) generate()}, which
 * allows generation of line coverage for a given {@link GameClass} and {@link Path paht to a java test file}.
 */
@ApplicationScoped
public class CoverageGenerator {
    private static final Logger logger = LoggerFactory.getLogger(CoverageGenerator.class);
    private static final String JACOCO_REPORT_FILE = "jacoco.exec";

    /**
     * Generates and returns line coverage for a given {@link GameClass} and {@link Path path to a java test file}.
     *
     * <p>The method requires the file 'jacoco.exec' to be present in the
     * folder the test lies in, otherwise the generation fails and an
     * empty {@link LineCoverage} instance is returned.
     *
     * @param gameClass    the class that is tested.
     * @param testJavaFile the test java file in which parent folder the 'jacoco.exe' file exists as a {@link Path}.
     * @return             the extended line coverage
     */
    public LineCoverageMapping generate(GameClass gameClass, Path testJavaFile)
            throws CoverageGeneratorException {
        final File execFile = findJacocoExecFile(testJavaFile);
        final Collection<File> relevantClassFiles = findRelevantClassFiles(gameClass);

        CoverageBuilder coverageBuilder = readJacocoCoverage(execFile, relevantClassFiles);
        LineCoverageMapping lineMapping = extractLineCoverageMapping(coverageBuilder, gameClass);

        CompilationUnit compilationUnit = JavaParserUtils.parse(gameClass.getSourceCode())
                .orElseThrow(() -> new CoverageGeneratorException("Could not parse java file: " + gameClass.getJavaFile()));

        return generate(lineMapping, compilationUnit);
    }

    public LineCoverageMapping generate(LineCoverageMapping originalCoverage, CompilationUnit compilationUnit) {
        AstCoverageVisitor astVisitor = new AstCoverageVisitor(originalCoverage);
        astVisitor.visit(compilationUnit, null);
        AstCoverageMapping astMapping = astVisitor.finish();

        LineTokens lineTokens = LineTokens.fromJaCoCo(originalCoverage);
        LineTokenVisitor lineTokenVisitor = new LineTokenVisitor(astMapping, lineTokens);
        lineTokenVisitor.visit(compilationUnit, null);

        LineTokenAnalyser lineTokenAnalyser = new LineTokenAnalyser();
        return lineTokenAnalyser.analyse(lineTokens);
    }

    // TODO: this replicates the old behavior. replace this with better error handling
    public LineCoverage generateOrEmpty(GameClass gameClass, Path testJavaFile) {
        try {
            LineCoverageMapping coverageMapping = generate(gameClass, testJavaFile);
            return coverageMapping.toLineCoverage();
        } catch (CoverageGeneratorException e) {
            logger.error(e.getMessage(), e.getCause());
            return LineCoverage.empty();
        }
    }

    public CoverageBuilder readJacocoCoverage(File execFile, Collection<File> relevantClassFiles)
            throws CoverageGeneratorException {
        final ExecFileLoader execFileLoader = new ExecFileLoader();
        try {
            execFileLoader.load(execFile);
        } catch (IOException e) {
            throw new CoverageGeneratorException("Failed to load jacoco.exec file: " + execFile.getPath(), e);
        }

        final ExecutionDataStore executionDataStore = execFileLoader.getExecutionDataStore();
        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);

        for (File classFile : relevantClassFiles) {
            try (InputStream is = Files.newInputStream(classFile.toPath())) {
                analyzer.analyzeClass(is, classFile.getPath());
            } catch (IOException e) {
                throw new CoverageGeneratorException("Failed to analyze file: " + classFile.getPath(), e);
            }
        }

        return coverageBuilder;
    }

    public File findJacocoExecFile(Path testJavaFile)
            throws CoverageGeneratorException {
        final File reportDirectory = testJavaFile.getParent().toFile();
        final File execFile = new File(reportDirectory, JACOCO_REPORT_FILE);
        if (!execFile.isFile()) {
            throw new CoverageGeneratorException("Could not find JaCoCo exec file for test: " + testJavaFile);
        }
        return execFile;
    }

    public Collection<File> findRelevantClassFiles(GameClass gameClass)
            throws CoverageGeneratorException {
        final File classFileFolder = new File(gameClass.getClassFile()).getParentFile();

        final String innerClassRegex = gameClass.getName() + "(\\$.*)?.class";
        final Pattern innerClassPattern = Pattern.compile(innerClassRegex);

        File[] relevantFiles = classFileFolder.listFiles(
                (dir, name) -> innerClassPattern.matcher(name).matches());

        if (relevantFiles == null) {
            throw new CoverageGeneratorException("Could not list class files for class: " + gameClass.getClassFile());
        }
        return Arrays.asList(relevantFiles);
    }

    public LineCoverageMapping extractLineCoverageMapping(CoverageBuilder coverageBuilder, GameClass gameClass) {
        LineCoverageMapping lineMapping = new LineCoverageMapping();

        for (ISourceFileCoverage coverage : coverageBuilder.getSourceFiles()) {
            if (!gameClass.getJavaFile().endsWith(coverage.getName())) {
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

    public static class CoverageGeneratorException extends Exception {
        public CoverageGeneratorException(String message) {
            super(message);
        }

        public CoverageGeneratorException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
