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
package org.codedefenders.execution;

import org.codedefenders.game.GameClass;
import org.codedefenders.game.LineCoverage;
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
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * This class offers a static method {@link #generate(GameClass, Path) generate()}, which
 * allows generation of line coverage for a given {@link GameClass} and {@link Path paht to a java test file}.
 */
public class LineCoverageGenerator {
    private static final Logger logger = LoggerFactory.getLogger(LineCoverageGenerator.class);
    private static final String JACOCO_REPORT_FILE = "jacoco.exec";

    /**
     * Generates and returns line coverage for a given {@link GameClass} and {@link Path path to a java test file}.
     * <p>
     * The method requires the file 'jacoco.exec' to be present in the
     * folder the test lies in, otherwise the generation fails and an
     * empty {@link LineCoverage} instance is returned.
     *
     * @param gameClass    the class that is tested.
     * @param testJavaFile the test java file in which parent folder the 'jacoco.exe' file exists as a {@link Path}.
     * @return a {@link LineCoverage} instance with covered and uncovered lines if successful,
     * empty lists for covered and uncovered lines if failed.
     */
    public static LineCoverage generate(GameClass gameClass, Path testJavaFile) {
        final File reportDirectory = testJavaFile.getParent().toFile();
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

        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(executionDataStore, coverageBuilder);

        /*
         * Classes with inner classes corresponds to multiple files on the file
         * system But inside the db they are not reported. So we need to look
         * into the folder
         */
        final File classFileFolder = new File(gameClass.getClassFile()).getParentFile();
        // List all the .class files in this folder. Not sure if FilenameFilter
        // is thread safe so I instantiate a new one every time.
        final String regex = new File(gameClass.getClassFile()).getName().split("\\.")[0] + "\\$?.*\\.class";
        final Pattern innerClassPatter = Pattern.compile(regex);
        for (File classFile : classFileFolder.listFiles((dir, name) -> innerClassPatter.matcher(name).matches())){
            try {
                analyzer.analyzeClass(new ClassReader(new FileInputStream(classFile)));
            } catch (IOException e) {
                logger.error("Failed to analyze file: " + classFile + ". Returning empty LineCoverage.", e);
                return new LineCoverage();
            }
        }

        final List<Integer> linesCovered = new LinkedList<>();
        final List<Integer> linesUncovered = new LinkedList<>();

        final String cutName = gameClass.getName();
        for (IClassCoverage cc : coverageBuilder.getClasses()) {
            final String fullyQualifiedName = cc.getName().replace("/", ".");
            if (fullyQualifiedName.startsWith(cutName)) {
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

        final Set<Integer> linesToAdd = new HashSet<>();
        // If there's at least one line covered, then static field initializer and compile time constants are covered
        if (!linesCovered.isEmpty()) {
            linesToAdd.addAll(gameClass.getCompileTimeConstants());
            linesToAdd.addAll(gameClass.getNonInitializedFields());
        }

        // Now we need to map lines covered with methods and then-branches of ifstatements in the class
        for (Integer coveredLine : linesCovered) {
            linesToAdd.addAll(gameClass.getMethodSignaturesForLine(coveredLine));
            linesToAdd.addAll(gameClass.getClosingBracketForLine(coveredLine));
            // If covered line belongs to method, add the method signature            
        }
        
        linesCovered.addAll(linesToAdd);

        // Include covered empty lines. This requires the lines covered so far can cover them
        linesToAdd.addAll(gameClass.getCoveredEmptyLines(linesCovered));
        
        // Remove the duplicates
        
        linesCovered.addAll(linesToAdd);
        linesUncovered.removeAll(linesToAdd);

        return new LineCoverage(linesCovered, linesUncovered);
    }
}