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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.ManagedBean;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.codedefenders.analysis.coverage.CoverageGenerator;
import org.codedefenders.analysis.coverage.CoverageGenerator.CoverageGeneratorException;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.PlayerDAO;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.prometheus.client.Histogram;

import static org.codedefenders.util.Constants.COMMON_CLASSPATH;
import static org.codedefenders.util.Constants.CUTS_DEPENDENCY_DIR;
import static org.codedefenders.util.Constants.JACOCO_CLASSPATH;
import static org.codedefenders.util.Constants.JAVA_CLASS_EXT;

/**
 * @author Jose Rojas
 * @author Alessio Gambi (last edit)
 */
@ManagedBean
public class AntRunner implements BackendExecutorService, ClassCompilerService {
    private static final Logger logger = LoggerFactory.getLogger(AntRunner.class);

    private static final Histogram antProcessDuration = Histogram.build()
            .name("codedefenders_antProcess_duration")
            .unit("seconds")
            .help("Duration of executing various ant processes")
            .labelNames("antTarget")
            .register();

    private final Configuration config;
    private final CoverageGenerator coverageGenerator;

    @Inject
    public AntRunner(@SuppressWarnings("CdiInjectionPointsInspection") Configuration config,
                     CoverageGenerator coverageGenerator) {
        this.config = config;
        this.coverageGenerator = coverageGenerator;
    }

    /**
     * {@inheritDoc}
     */
    public TargetExecution testMutant(Mutant mutant, Test test) {
        logger.info("Running test {} on mutant {}", test.getId(), mutant.getId());

        GameClass cut = GameClassDAO.getClassForGameId(mutant.getGameId());
        if (cut == null) {
            cut = GameClassDAO.getClassForId(mutant.getClassId());
        }

        // Check if this mutant requires a test recompilation
        String target;
        if (mutant.doesRequireRecompilation()) {
            target = "recompiled-test-mutant";
        } else {
            target = "test-mutant";
        }
        AntProcessResult result = runAntTarget(target, mutant.getDirectory(), test.getDirectory(),
                cut, test.getFullyQualifiedClassName());


        TargetExecution newExec;

        if (result.hasFailure()) {
            // The test failed, i.e., it detected the mutant. Store JUnit output as kill message
            String killMessage = result.getJUnitMessage();
            newExec = new TargetExecution(test.getId(), mutant.getId(), TargetExecution.Target.TEST_MUTANT,
                    TargetExecution.Status.FAIL, killMessage);
        } else if (result.hasError()) {
            // The test is in error, interpreted also as detecting the mutant
            String message = result.getErrorMessage();
            newExec = new TargetExecution(test.getId(), mutant.getId(), TargetExecution.Target.TEST_MUTANT,
                    TargetExecution.Status.ERROR, message);
        } else {
            // The test passed, i.e., it did not detect the mutant
            newExec = new TargetExecution(test.getId(), mutant.getId(), TargetExecution.Target.TEST_MUTANT,
                    TargetExecution.Status.SUCCESS, null);
        }
        newExec.insert();
        return newExec;
    }

    /**
     * {@inheritDoc}
     */
    public void testOriginal(GameClass cut, String testDir, String testClassName) throws Exception {
        AntProcessResult result = runAntTarget("test-original", null, testDir, cut, testClassName,
                config.isForceLocalExecution());

        if (result.hasFailure() || result.hasError()) {
            if (result.hasFailure()) {
                logger.error(result.getJUnitMessage());
            } else if (result.hasError()) {
                logger.error(result.getErrorMessage());
            }

            logger.error("Test {} failed to run against class under test", testClassName);
            throw new Exception("Test failed to run against class under test.");
        } else {
            logger.info("Successfully tested original ");
        }
    }

    /**
     * {@inheritDoc}
     */
    public TargetExecution testOriginal(File dir, Test t) {
        GameClass cut = GameClassDAO.getClassForGameId(t.getGameId());

        AntProcessResult result = runAntTarget("test-original", null, dir.getAbsolutePath(),
                cut, t.getFullyQualifiedClassName(), config.isForceLocalExecution());

        // add coverage information
        LineCoverage coverage;
        try {
            coverage = coverageGenerator.generate(cut, Paths.get(t.getJavaFile()));
        } catch (CoverageGeneratorException e) {
            // TODO: don't return empty coverage here
            logger.error("Error while computing coverage for test " + t.getId(), e);
            coverage = LineCoverage.empty();
        }
        t.setLineCoverage(coverage);
        t.update();

        // record test execution
        TargetExecution.Status status;
        String message;
        if (result.hasFailure()) {
            status = TargetExecution.Status.FAIL;
            message = result.getJUnitMessage();
        } else if (result.hasError()) {
            status = TargetExecution.Status.ERROR;
            message = result.getErrorMessage();
        } else {
            status = TargetExecution.Status.SUCCESS;
            message = null;
        }
        TargetExecution testExecution = new TargetExecution(t.getId(), 0,
                TargetExecution.Target.TEST_ORIGINAL, status, message);
        testExecution.insert();
        return testExecution;
    }

    /**
     * {@inheritDoc}
     */
    public Mutant compileMutant(File dir, String javaFile, int gameId, GameClass cut, int ownerId) {

        // Gets the classname for the mutant from the game it is in
        AntProcessResult result = runAntTarget("compile-mutant", dir.getAbsolutePath(), null,
                cut, null, config.isForceLocalExecution());

        logger.info("Compilation result: {}", result);

        Mutant newMutant;
        // If the input stream returned a 'successful build' message, the mutant compiled correctly
        if (result.compiled()) {
            // Create and insert a new target execution recording successful compile,
            // with no message to report, and return its ID
            // Locate .class file
            final String compiledClassName = cut.getBaseName() + JAVA_CLASS_EXT;
            final LinkedList<File> matchingFiles = new LinkedList<>(FileUtils.listFiles(
                    dir, FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter()));
            assert (!matchingFiles.isEmpty()) : "if compilation was successful, .class file must exist";
            String classFile = matchingFiles.get(0).getAbsolutePath();
            int playerId = PlayerDAO.getPlayerIdForUserAndGame(ownerId, gameId);
            newMutant = new Mutant(gameId, cut.getId(), javaFile, classFile, true, playerId, GameDAO.getCurrentRound(gameId));
            newMutant.insert();
            TargetExecution newExec = new TargetExecution(0, newMutant.getId(),
                    TargetExecution.Target.COMPILE_MUTANT, TargetExecution.Status.SUCCESS, null);
            newExec.insert();
        } else {
            // The mutant failed to compile
            // New target execution recording failed compile, providing the return messages from the ant javac task
            String message = result.getCompilerOutput();
            logger.error("Failed to compile mutant {}: {}", javaFile, message);
            int playerId = PlayerDAO.getPlayerIdForUserAndGame(ownerId, gameId);
            newMutant = new Mutant(gameId, cut.getId(), javaFile, null, false, playerId, GameDAO.getCurrentRound(gameId));
            newMutant.insert();
            TargetExecution newExec = new TargetExecution(0, newMutant.getId(),
                    TargetExecution.Target.COMPILE_MUTANT, TargetExecution.Status.FAIL, message);
            newExec.insert();
        }
        return newMutant;
    }

    /**
     * {@inheritDoc}
     */
    public Test compileTest(File dir, String javaFile, int gameId, GameClass cut, int ownerId) {
        //public static int compileTest(ServletContext context, Test t) {

        AntProcessResult result = runAntTarget("compile-test", null, dir.getAbsolutePath(),
                cut, null, config.isForceLocalExecution());

        int playerId = PlayerDAO.getPlayerIdForUserAndGame(ownerId, gameId);

        // If the input stream returned a 'successful build' message, the test compiled correctly
        if (result.compiled()) {
            // Create and insert a new target execution recording successful compile,
            // with no message to report, and return its ID
            // Locate .class file
            final String compiledClassName = FilenameUtils.getBaseName(javaFile) + JAVA_CLASS_EXT;
            final List<File> matchingFiles = new LinkedList<>(FileUtils.listFiles(
                    dir, FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter()));
            assert (!matchingFiles.isEmpty()); // if compilation was successful, .class file must exist
            String classFile = matchingFiles.get(0).getAbsolutePath();
            logger.info("Compiled test {}", compiledClassName);
            Test newTest = new Test(cut.getId(), gameId, javaFile, classFile, playerId);
            boolean inserted = newTest.insert();
            assert (inserted); // if compilation was successful, .class file must exist
            TargetExecution newExec = new TargetExecution(newTest.getId(), 0,
                    TargetExecution.Target.COMPILE_TEST, TargetExecution.Status.SUCCESS, null);
            newExec.insert();
            return newTest;
        } else {
            // The test failed to compile
            // New target execution recording failed compile, providing the return messages from the ant javac task
            String message = result.getCompilerOutput();
            logger.error("Failed to compile test {}: {}", javaFile, message);
            Test newTest = new Test(cut.getId(), gameId, javaFile, null, playerId);
            newTest.insert();
            TargetExecution newExec = new TargetExecution(newTest.getId(), 0,
                    TargetExecution.Target.COMPILE_TEST, TargetExecution.Status.FAIL, message);
            newExec.insert();
            return newTest;
        }
    }

    /**
     * Runs a specific Ant target in the build.xml file
     *
     * @param target        An Ant target
     * @param mutantFile    Mutant Java source file
     * @param testDir       Test directory
     * @param cut           Class under test
     * @param testClassName Name of JUnit test class
     * @return Result an AntProcessResult object containing output details of the ant process
     */
    private AntProcessResult runAntTarget(String target, String mutantFile, String testDir,
                                          GameClass cut, String testClassName) {
        return runAntTarget(target, mutantFile, testDir, cut, testClassName, false);
    }

    private AntProcessResult runAntTarget(String target, String mutantDir, String testDir, GameClass cut,
                                          String testClassName, boolean forcedLocally) {
        logger.info("Running Ant Target: {} with mFile: {} and tFile: {}", target, mutantDir, testDir);

        ProcessBuilder pb = new ProcessBuilder();
        Map<String, String> env = pb.environment();
        List<String> command = new ArrayList<>();
        String cutDir = Paths.get(cut.getJavaFile()).getParent().toString();

        config.getAntJavaHome()
                .ifPresent(file -> env.put("JAVA_HOME", file.toString()));

        /*
         * Clustered execution uses almost the same command than normal
         * execution. But it prefixes that with "srun". This assumes that the
         * code-defender working dir is on the NFS.
         */

        if (config.isClusterModeEnabled() && !forcedLocally) {
            logger.info("Clustered Execution");
            if (config.getClusterJavaHome() != null) {
                env.put("JAVA_HOME", config.getClusterJavaHome());
            }

            command.add("srun");

            // Select reservation cluster
            if (config.getClusterReservationName() != null) {
                command.add("--reservation=" + config.getClusterReservationName());
            }

            // Timeout. Note that there's a plus 10 minutes of grace period
            // anyway
            // TODO This is unsafe we need to check this is a valid integer
            if (config.getClusterTimeout() > -1) {
                command.add("--time=" + config.getClusterTimeout());
            }

            command.add("ant");
        } else {
            logger.debug("Local Execution");

            String command_ = config.getAntHome() + "/bin/ant";

            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                command_ += ".bat";
                command_ = command_.replace("/", "\\").replace("\\", "\\\\");
            }
            command.add(command_);

        }

        // Add additional command parameters
        command.add(target); // "-v", "-d", for verbose, debug
        // This ensures that ant actually uses the data dir we setup
        command.add("-Dcodedef.home=" + config.getDataDir().getAbsolutePath());

        command.add("-Dmutant.file=" + mutantDir);
        command.add("-Dtest.file=" + testDir);
        command.add("-Dcut.dir=" + cutDir);
        command.add("-DtestClassname=" + testClassName);
        command.add("-Dcuts.deps=" + Paths.get(cutDir, CUTS_DEPENDENCY_DIR));

        command.add("-Dcommon.cp=" + COMMON_CLASSPATH);
        command.add("-Djacoco.cp=" + JACOCO_CLASSPATH);

        if (mutantDir != null && testDir != null
                // Limit this code path to targets that depend on the `mutant.test.file` variable.
                // Otherwise, this will create esp. for KillMaps a lot of empty directories
                // See: https://gitlab.infosun.fim.uni-passau.de/se2/codedefenders/CodeDefenders/-/issues/904
                && ("recompiled-test-mutant".equals(target) || "recompile-test-with-mutant".equals(target))) {
            String separator = File.separator;
            if (separator.equals("\\")) {
                separator = "\\\\";
            }
            String[] tokens = mutantDir.split(separator);
            String mutantFile = String.format("%s-%s", tokens[tokens.length - 2], tokens[tokens.length - 1]);
            String testMutantFile = testDir.replace("original", mutantFile);
            // TODO This might need refactoring
            File testMutantFileDir = new File(testMutantFile);
            if (!testMutantFileDir.exists()) {
                testMutantFileDir.mkdirs();
            }

            command.add("-Dmutant.test.file=" + testMutantFile);
        }
        // Execute whichever command was build
        pb.command(command);

        pb.directory(config.getDataDir());
        pb.redirectErrorStream(true);

        logger.info("Executing Ant Command {} from directory {}", pb.command().toString(),
                config.getDataDir().getAbsolutePath());

        try (Histogram.Timer ignored = antProcessDuration.labels(target).startTimer()) {
            return runAntProcess(pb);
        }
    }

    private static AntProcessResult runAntProcess(ProcessBuilder pb) {
        AntProcessResult res = new AntProcessResult();
        try {
            Process p = pb.start();

            BufferedReader is = new BufferedReader(new InputStreamReader(p.getInputStream()));
            res.setInputStream(is);

            String line;
            BufferedReader es = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            StringBuilder esLog = new StringBuilder();
            while ((line = es.readLine()) != null) {
                esLog.append(line).append(System.lineSeparator());
            }
            res.setErrorStreamText(esLog.toString());
        } catch (Exception ex) {
            res.setExceptionText(String.format("Exception: %s%s", ex.toString(), System.lineSeparator()));
        }
        return res;
    }

}
