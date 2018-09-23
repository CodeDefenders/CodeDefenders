package org.codedefenders.execution;

import org.codedefenders.game.GameClass;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.game.Test;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.tools.ExecFileLoader;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * // TODO Phil 23/09/18:
 * <p>
 * This class also contains a static method {@link #generate(GameClass, Test)},
 * which generates the line coverage for a given {@link GameClass} and {@link Test}.
 */
public class LineCoverageGenerator {
    private static final Logger logger = LoggerFactory.getLogger(LineCoverageGenerator.class);
    private static final String JACOCO_REPORT_FILE = "jacoco.exec";

    /**
     * Generates and returns line coverage for a given {@link GameClass} and {@link Test}.
     *
     * @param gameClass the class that is tested.
     * @param test      the test used for testing.
     * @return a {@link LineCoverage} instance with covered and uncovered lines if successful,
     * empty lists for covered and uncovered lines if failed.
     */
    public static LineCoverage generate(GameClass gameClass, Test test) {
        final File reportDirectory = Paths.get(test.getJavaFile()).getParent().toFile();
        final File executionDataFile = new File(reportDirectory, JACOCO_REPORT_FILE);
        final ExecFileLoader execFileLoader = new ExecFileLoader();
        try {
            execFileLoader.load(executionDataFile);
        } catch (IOException e) {
            logger.error("Failed to load jacoco.exec file. Returning empty LineCoverage.", e);
            return new LineCoverage();
        }
        // In memory data store for execution data
        final ExecutionDataStore executionDataStore = execFileLoader.getExecutionDataStore();

        // TODO Phil 23/09/18: remove
//        final File classDirectory = Paths.get(gameClass.getJavaFile()).getParent().toFile();
        final String classFile = gameClass.getClassFile();

        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);
        try {
            analyzer.analyzeClass(new ClassReader(new FileInputStream(classFile)));
//            analyzer.analyzeAll(classDirectory);
        } catch (IOException e) {
            logger.error("Failed to analyze file for file: " + classFile + ". Returning empty LineCoverage.", e);
            return new LineCoverage();
        }

        final List<Integer> linesCovered = new LinkedList<>();
        final List<Integer> linesUncovered = new LinkedList<>();

        for (IClassCoverage cc : coverageBuilder.getClasses()) {
            String fullyQualifiedName = cc.getName().replace("/", ".");
            if (fullyQualifiedName.startsWith(gameClass.getName())) {
                for (int i = cc.getFirstLine(); i <= cc.getLastLine(); i++) {
                    final ILine line = cc.getLine(i);
                    final int status = line.getInstructionCounter().getStatus();
                    if (status == ICounter.FULLY_COVERED || status == ICounter.PARTLY_COVERED) {
                        linesCovered.add(i);
                    } else if (status == ICounter.NOT_COVERED) {
                        linesUncovered.add(i);
                    }
                }
            }
        }

        Set<Integer> linesToAdd = new HashSet<>();
        // If there's at least one line covered, then static field initializer and compile time constants are covered
        if (!linesCovered.isEmpty()) {
            linesToAdd.addAll(gameClass.getLinesOfCompileTimeConstants());
            linesToAdd.addAll(gameClass.getLinesOfNonInitializedFields());
        }

        // Now we need to map lines covered with methods and then-branches of ifstatements in the class
        for (Integer coveredLine : linesCovered) {
            linesToAdd.addAll(gameClass.getLinesOfMethodSignaturesFor(coveredLine));
            linesToAdd.addAll(gameClass.getLineOfClosingBracketFor(coveredLine));
            // If covered line belongs to method, add the method signature
        }
        //
        linesCovered.addAll(linesToAdd);
        linesUncovered.removeAll(linesToAdd);

        // TODO Phil 23/09/18: remove
        logger.info("Successfully generated LineCoverage:{}:{}", Arrays.toString(linesCovered.toArray()), Arrays.toString(linesUncovered.toArray()));
        return new LineCoverage(linesCovered, linesUncovered);
    }
}