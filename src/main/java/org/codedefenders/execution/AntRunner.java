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
package org.codedefenders.execution;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.codedefenders.analysis.coverage.CoverageGenerator;
import org.codedefenders.analysis.coverage.CoverageGenerator.CoverageGeneratorException;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.dependencies.DependencyProvider;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.persistence.database.PlayerRepository;
import org.codedefenders.persistence.database.TestRepository;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import io.prometheus.client.Histogram;

import static org.codedefenders.util.Constants.JAVA_CLASS_EXT;

/**
 * @author Jose Rojas
 * @author Alessio Gambi (last edit)
 */
@ApplicationScoped
public class AntRunner implements BackendExecutorService, ClassCompilerService {
    private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_OUTPUT_BYTES = 65_536;
    // Reports also contain JVM properties and per-test metadata.
    private static final int MAX_REPORT_BYTES = 1_048_576;

    private static final Logger logger = LoggerFactory.getLogger(AntRunner.class);

    private static final Histogram antProcessDuration = Histogram.build()
            .name("codedefenders_antProcess_duration")
            .unit("seconds")
            .help("Duration of executing various ant processes")
            .labelNames("antTarget")
            .register();

    private final Configuration config;
    private final CoverageGenerator coverageGenerator;
    private final GameRepository gameRepo;
    private final PlayerRepository playerRepo;
    private final MutantRepository mutantRepo;
    private final TestRepository testRepo;
    private final GameClassRepository gameClassRepo;
    private final DependencyProvider dependencyProvider;

    @Inject
    public AntRunner(Configuration config,
                     CoverageGenerator coverageGenerator, GameRepository gameRepo, PlayerRepository playerRepo,
                     MutantRepository mutantRepo, TestRepository testRepo, GameClassRepository gameClassRepo,
                     DependencyProvider dependencyProvider) {
        this.config = config;
        this.coverageGenerator = coverageGenerator;
        this.gameRepo = gameRepo;
        this.playerRepo = playerRepo;
        this.mutantRepo = mutantRepo;
        this.testRepo = testRepo;
        this.gameClassRepo = gameClassRepo;
        this.dependencyProvider = dependencyProvider;
    }

    /**
     * {@inheritDoc}
     */
    public TargetExecution testMutant(Mutant mutant, Test test) {
        logger.info("Running test {} on mutant {}", test.getId(), mutant.getId());

        var cut = gameClassRepo.getClassForGameId(mutant.getGameId())
                .or(() -> gameClassRepo.getClassForId(mutant.getClassId()))
                .orElseThrow();

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
    public void testOriginal(GameClass cut, String testDir, String testClassName) throws ExecutionException {
        AntProcessResult result;
        try {
            result = runAntTarget("test-original", null, testDir, cut, testClassName, config.isForceLocalExecution());
        } catch (ExecutionLimitException e) {
            throw new ExecutionException("Test could not run safely against class under test.", e);
        }

        if (result.hasFailure() || result.hasError()) {
            if (result.hasFailure()) {
                logger.error(result.getJUnitMessage());
            } else if (result.hasError()) {
                logger.error(result.getErrorMessage());
            }

            logger.error("Test {} failed to run against class under test", testClassName);
            throw new ExecutionException("Test failed to run against class under test.");
        } else {
            logger.info("Successfully tested original ");
        }
    }

    /**
     * {@inheritDoc}
     */
    public TargetExecution testOriginal(File dir, Test t) {
        GameClass cut = gameClassRepo.getClassForGameId(t.getGameId())
                .orElseThrow();

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
        testRepo.updateTest(t);

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
            int playerId = playerRepo.getPlayerIdForUserAndGame(ownerId, gameId);
            newMutant = new Mutant(gameId, cut.getId(), javaFile, classFile, true, playerId, gameRepo.getCurrentRound(gameId));

            int mutantId = mutantRepo.storeMutant(newMutant);
            newMutant.setId(mutantId);

            TargetExecution newExec = new TargetExecution(0, newMutant.getId(),
                    TargetExecution.Target.COMPILE_MUTANT, TargetExecution.Status.SUCCESS, null);
            newExec.insert();
        } else {
            // The mutant failed to compile
            // New target execution recording failed compile, providing the return messages from the ant javac task
            String message = result.getCompilerOutput();
            logger.error("Failed to compile mutant {}: {}", javaFile, message);
            int playerId = playerRepo.getPlayerIdForUserAndGame(ownerId, gameId);
            newMutant = new Mutant(gameId, cut.getId(), javaFile, null, false, playerId, gameRepo.getCurrentRound(gameId));

            int mutantId = mutantRepo.storeMutant(newMutant);
            newMutant.setId(mutantId);

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

        int playerId = playerRepo.getPlayerIdForUserAndGame(ownerId, gameId);

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

            int currentRound = gameRepo.getCurrentRound(gameId);
            Test newTest = new Test(cut.getId(), gameId, currentRound, javaFile, classFile, playerId);

            int testId = testRepo.storeTest(newTest);
            newTest.setId(testId);

            TargetExecution newExec = new TargetExecution(newTest.getId(), 0,
                    TargetExecution.Target.COMPILE_TEST, TargetExecution.Status.SUCCESS, null);
            newExec.insert();
            return newTest;
        } else {
            // The test failed to compile
            // New target execution recording failed compile, providing the return messages from the ant javac task
            String message = result.getCompilerOutput();
            logger.error("Failed to compile test {}: {}", javaFile, message);

            int currentRound = gameRepo.getCurrentRound(gameId);
            Test newTest = new Test(cut.getId(), gameId, currentRound, javaFile, null, playerId);

            int testId = testRepo.storeTest(newTest);
            newTest.setId(testId);

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

        //This is the directory directly below "sources"
        Path topDir = org.codedefenders.util.FileUtils.getTopLevelDirectoryFromJavaFile(
                Paths.get(cut.getJavaFile()));
        Path cutDir = topDir.resolve(Constants.CUTS_CLASSES_DIR);

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

        command.add("-Dcodedef.library=" + config.getLibraryDir().getAbsolutePath());
        command.add("-Dcommon.cp=" + dependencyProvider.getCommonClasspath());
        command.add("-Djacoco.cp=" + dependencyProvider.getJacocoClasspath());

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
            String mutantFile = String.format("%s-%s-%s", tokens[tokens.length - 3],
                    tokens[tokens.length - 2], tokens[tokens.length - 1]);
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

    static AntProcessResult runAntProcess(ProcessBuilder builder) {
        // Keep execution configuration and scheduler variables, not application credentials.
        Map<String, String> environment = builder.environment();
        environment.keySet().removeIf(key -> !List.of("PATH", "JAVA_HOME", "ANT_HOME", "LANG", "LC_ALL")
                .contains(key) && !key.startsWith("SLURM_"));
        environment.put("ANT_OPTS", "-Xmx128m -XX:MaxMetaspaceSize=96m");
        boolean testTarget = builder.command().stream().anyMatch(List.of("test-original", "test-mutant",
                "recompiled-test-mutant")::contains);
        try (var reports = testTarget
                ? new ReportDirectory(Files.createTempDirectory(builder.directory().toPath(), "test-report-"))
                : null) {
            if (reports != null) {
                builder.command().add("-Dtest.report.dir=" + reports.path());
            }
            // Slurm owns queueing and the configured job deadline; the JUnit timeout starts on the worker.
            Duration deadline = "srun".equals(builder.command().get(0))
                    ? Duration.ofMillis(Long.MAX_VALUE) : PROCESS_TIMEOUT;
            String output = runProcess(builder, deadline);
            AntProcessResult result = new AntProcessResult();
            result.setInputStream(new BufferedReader(new StringReader(output)));
            if (testTarget) {
                if (!result.compiled()) {
                    throw new ExecutionLimitException(new IOException(output));
                }
                readTestOutcome(reports.path().resolve("result.xml"), result);
            }
            return result;
        } catch (IOException e) {
            throw new ExecutionLimitException(e);
        }
    }

    private record ReportDirectory(Path path) implements AutoCloseable {
        @Override
        public void close() throws IOException {
            FileUtils.deleteDirectory(path.toFile());
        }
    }

    static void readTestOutcome(Path report, AntProcessResult result) {
        try {
            if (Files.size(report) > MAX_REPORT_BYTES) {
                throw new ExecutionLimitException();
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            var suite = factory.newDocumentBuilder().parse(report.toFile()).getDocumentElement();
            if (!"testsuite".equals(suite.getTagName()) || Integer.parseInt(suite.getAttribute("tests")) < 1) {
                throw new ExecutionLimitException();
            }
            result.setTestOutcome(Integer.parseInt(suite.getAttribute("failures")) > 0,
                    Integer.parseInt(suite.getAttribute("errors")) > 0);
        } catch (IOException | ParserConfigurationException | SAXException | NumberFormatException e) {
            throw new ExecutionLimitException(e);
        }
    }

    /** Kills the whole process tree on timeout, excessive output, interruption or execution failure. */
    static String runProcess(ProcessBuilder builder, Duration timeout) throws IOException {
        Process process = builder.start();
        try (var input = process.getInputStream()) {
            FutureTask<String> output = new FutureTask<>(() -> {
                try (var bytes = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4_096];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        if (bytes.size() + read > MAX_OUTPUT_BYTES) {
                            kill(process);
                            throw new ExecutionLimitException();
                        }
                        bytes.write(buffer, 0, read);
                    }
                    return bytes.toString(StandardCharsets.UTF_8);
                }
            });
            Thread reader = new Thread(output, "test-output");
            reader.setDaemon(true);
            reader.start();
            try {
                if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    throw new ExecutionLimitException();
                }
                return output.get(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ExecutionLimitException(e);
            } catch (java.util.concurrent.ExecutionException | TimeoutException e) {
                throw new ExecutionLimitException(e);
            } finally {
                kill(process);
                output.cancel(true);
            }
        }
    }

    private static void kill(Process process) {
        try {
            List<ProcessHandle> children = process.descendants().toList();
            children.forEach(ProcessHandle::destroyForcibly);
            // Let the parent reap terminated children before killing it, including on interruption.
            CompletableFuture.allOf(children.stream().map(ProcessHandle::onExit).toArray(CompletableFuture[]::new))
                    .handle((ignored, failure) -> null)
                    .completeOnTimeout(null, 1, TimeUnit.SECONDS)
                    .join();
        } catch (RuntimeException e) {
            logger.warn("Could not clean up execution descendants", e);
        } finally {
            try {
                process.destroyForcibly();
            } catch (RuntimeException e) {
                logger.warn("Could not terminate execution process", e);
            }
        }
    }

    public static class ExecutionLimitException extends RuntimeException {
        public ExecutionLimitException() {
            super("Test execution exceeded its resource budget or could not run safely");
        }

        public ExecutionLimitException(Throwable cause) {
            super("Test execution could not run safely", cause);
        }
    }
}
