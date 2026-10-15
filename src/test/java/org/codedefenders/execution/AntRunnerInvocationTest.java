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
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.codedefenders.analysis.coverage.CoverageGenerator;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.dependencies.DependencyProvider;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.game.Mutant;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.persistence.database.PlayerRepository;
import org.codedefenders.persistence.database.TestRepository;
import org.codedefenders.util.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AntRunnerInvocationTest {
    @TempDir
    Path root;

    private final Configuration config = mock(Configuration.class);
    private final DependencyProvider dependencies = mock(DependencyProvider.class);
    private final CoverageGenerator coverage = mock(CoverageGenerator.class);
    private final GameRepository games = mock(GameRepository.class);
    private final PlayerRepository players = mock(PlayerRepository.class);
    private final MutantRepository mutants = mock(MutantRepository.class);
    private final TestRepository tests = mock(TestRepository.class);
    private final GameClassRepository classes = mock(GameClassRepository.class);
    private final GameClass cut = mock(GameClass.class);
    private final AntRunner runner = new AntRunner(config, coverage, games, players, mutants, tests, classes, dependencies);
    private MockedStatic<FileUtils> paths;
    private ProcessBuilder invocation;

    @BeforeEach
    void configureRunner() throws Exception {
        when(config.getDataDir()).thenReturn(root.toFile());
        when(config.getLibraryDir()).thenReturn(root.resolve("lib").toFile());
        when(config.getAntHome()).thenReturn(root.resolve("ant").toFile());
        when(config.getAntJavaHome()).thenReturn(Optional.of(root.resolve("local-jdk").toFile()));
        when(dependencies.getCommonClasspath()).thenReturn("common.jar");
        when(dependencies.getJacocoClasspath()).thenReturn("jacoco.jar");
        when(cut.getJavaFile()).thenReturn(root.resolve("sources/Sample/classes/Sample.java").toString());
        when(cut.getBaseName()).thenReturn("Sample");
        when(cut.getId()).thenReturn(100);
        when(classes.getClassForGameId(42)).thenReturn(Optional.of(cut));
        when(coverage.generate(eq(cut), any(Path.class))).thenReturn(LineCoverage.empty());
        paths = mockStatic(FileUtils.class);
        paths.when(() -> FileUtils.getTopLevelDirectoryFromJavaFile(Path.of(cut.getJavaFile())))
                .thenReturn(root.resolve("sources/Sample"));
    }

    @AfterEach
    void restorePaths() {
        paths.close();
    }

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "true,true"})
    void importedTestsKeepExecutionModeJavaHomeAndQualifiedClassName(boolean cluster, boolean forceLocal)
            throws Exception {
        when(config.isClusterModeEnabled()).thenReturn(cluster);
        when(config.isForceLocalExecution()).thenReturn(forceLocal);
        when(config.getClusterJavaHome()).thenReturn(root.resolve("cluster-jdk").toString());
        when(config.getClusterReservationName()).thenReturn("teaching");
        when(config.getClusterTimeout()).thenReturn(5);
        Path importedTests = root.resolve("uploaded tests");
        try (var execution = execution(outcome(TargetExecution.Status.SUCCESS))) {
            runner.testOriginal(cut, importedTests.toString(), "example.QualifiedTest");
            List<String> command = invocation.command();
            assertTrue(command.contains("test-original"));
            assertTrue(command.contains("-DtestClassname=example.QualifiedTest"));
            assertTrue(command.contains("-Dtest.file=" + importedTests));
            assertTrue(command.contains("-Dmutant.file=null"));
            assertEquals(root.toFile(), invocation.directory());
            boolean scheduled = cluster && !forceLocal;
            assertEquals(scheduled, command.get(0).equals("srun"));
            assertEquals(root.resolve(scheduled ? "cluster-jdk" : "local-jdk").toString(),
                    invocation.environment().get("JAVA_HOME"));
            if (scheduled) {
                assertTrue(command.contains("--reservation=teaching"));
                assertTrue(command.contains("--time=5"));
            }
        }
    }

    @Test
    void importRunnerFailuresUseTheDeclaredCheckedException() throws Exception {
        AntRunner.ExecutionLimitException failure = new AntRunner.ExecutionLimitException();
        try (var execution = mockStatic(AntRunner.class)) {
            execution.when(() -> AntRunner.runAntProcess(any(ProcessBuilder.class))).thenThrow(failure);
            var thrown = assertThrows(BackendExecutorService.ExecutionException.class,
                    () -> runner.testOriginal(cut, root.resolve("uploaded tests").toString(), "example.QualifiedTest"));
            assertSame(failure, thrown.getCause());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"compile-test", "compile-mutant"})
    void compilerEntryPointsRetainNullArgumentsAndRecordRejections(String target) throws Exception {
        AntProcessResult rejected = new AntProcessResult();
        rejected.setInputStream(new BufferedReader(new StringReader("[javac] Sample.java:1: error: invalid\nBUILD FAILED\n")));
        try (var execution = execution(rejected); var database = mockStatic(TargetExecutionDAO.class)) {
            if (target.equals("compile-mutant")) {
                runner.compileMutant(root.resolve("mutant").toFile(), "Sample.java", 42, cut, 101);
            } else {
                runner.compileTest(root.resolve("tests").toFile(), "QualifiedTest.java", 42, cut, 101);
            }
            assertTrue(invocation.command().contains(target));
            assertTrue(invocation.command().contains(target.equals("compile-mutant")
                    ? "-Dtest.file=null" : "-Dmutant.file=null"));
            assertTrue(invocation.command().contains("-DtestClassname=null"));
            assertFalse(invocation.command().stream().anyMatch(value -> value.startsWith("-Dmutant.test.file=")));
            var expectedTarget = target.equals("compile-mutant")
                    ? TargetExecution.Target.COMPILE_MUTANT : TargetExecution.Target.COMPILE_TEST;
            database.verify(() -> TargetExecutionDAO.storeTargetExecution(
                    argThat(value -> value.target == expectedTarget && value.status == TargetExecution.Status.FAIL)));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void mutantEntryPointSelectsRecompilationAndRecordsTheResult(boolean recompile) throws Exception {
        Mutant mutant = mock(Mutant.class);
        when(mutant.getId()).thenReturn(61);
        when(mutant.getGameId()).thenReturn(42);
        when(mutant.getDirectory()).thenReturn(root.resolve("mutants/mp/42/100/00000002").toString());
        when(mutant.doesRequireRecompilation()).thenReturn(recompile);
        var test = gameTest();
        try (var execution = execution(outcome(TargetExecution.Status.FAIL));
                var database = mockStatic(TargetExecutionDAO.class)) {
            TargetExecution result = runner.testMutant(mutant, test);
            assertEquals(TargetExecution.Status.FAIL, result.status);
            assertEquals(TargetExecution.Target.TEST_MUTANT, result.target);
            assertEquals(71, result.testId);
            assertEquals(61, result.mutantId);
            assertTrue(invocation.command().contains(recompile ? "recompiled-test-mutant" : "test-mutant"));
            Path recompiled = Path.of(test.getDirectory().replace("original", "42-100-00000002"));
            assertEquals(recompile, Files.isDirectory(recompiled));
            assertEquals(recompile, invocation.command().contains("-Dmutant.test.file=" + recompiled));
            database.verify(() -> TargetExecutionDAO.storeTargetExecution(result));
        }
    }

    @ParameterizedTest
    @EnumSource(TargetExecution.Status.class)
    void gameTestsRetainCoverageAndOriginalExecutionStatus(TargetExecution.Status status) throws Exception {
        var test = gameTest();
        try (var execution = execution(outcome(status)); var database = mockStatic(TargetExecutionDAO.class)) {
            TargetExecution result = runner.testOriginal(Path.of(test.getDirectory()).toFile(), test);
            assertEquals(status, result.status);
            assertEquals(TargetExecution.Target.TEST_ORIGINAL, result.target);
            verify(coverage).generate(cut, Path.of(test.getJavaFile()));
            verify(test).setLineCoverage(any(LineCoverage.class));
            verify(tests).updateTest(test);
            database.verify(() -> TargetExecutionDAO.storeTargetExecution(result));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void scheduledExecutionKeepsSchedulerSettingsAndDoesNotTimeOutWhileQueued(boolean scheduled) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(new ArrayList<>(scheduled
                ? List.of("srun", "ant", "test-original") : List.of("ant", "test-original"))).directory(root.toFile());
        var environment = builder.environment();
        environment.clear();
        environment.put("PATH", "/usr/bin");
        environment.put("JAVA_HOME", "/configured/jdk");
        environment.put("SLURM_CONF", "/etc/slurm/teaching.conf");
        environment.put("SLURM_MPI_TYPE", "none");
        environment.put("CODEDEFENDERS_DB_PASSWORD", "secret");
        environment.put("JAVA_TOOL_OPTIONS", "-Dapplication.secret=secret");
        try (var execution = mockStatic(AntRunner.class)) {
            execution.when(() -> AntRunner.runAntProcess(builder)).thenCallRealMethod();
            execution.when(() -> AntRunner.readTestOutcome(any(Path.class), any(AntProcessResult.class))).thenCallRealMethod();
            execution.when(() -> AntRunner.runProcess(eq(builder), any(Duration.class))).thenAnswer(call -> {
                Duration timeout = call.getArgument(1);
                assertEquals(scheduled, timeout.compareTo(Duration.ofDays(1)) > 0);
                String directory = builder.command().stream().filter(value -> value.startsWith("-Dtest.report.dir="))
                        .findFirst().orElseThrow().substring("-Dtest.report.dir=".length());
                Files.writeString(Path.of(directory, "result.xml"), "<testsuite tests=\"1\" failures=\"0\" errors=\"0\"/>");
                return "BUILD SUCCESSFUL\n";
            });
            AntProcessResult result = AntRunner.runAntProcess(builder);
            assertTrue(result.compiled());
            assertFalse(result.hasFailure());
            assertFalse(result.hasError());
            assertEquals("/configured/jdk", environment.get("JAVA_HOME"));
            assertEquals("/etc/slurm/teaching.conf", environment.get("SLURM_CONF"));
            assertEquals("none", environment.get("SLURM_MPI_TYPE"));
            assertFalse(environment.containsKey("CODEDEFENDERS_DB_PASSWORD"));
            assertFalse(environment.containsKey("JAVA_TOOL_OPTIONS"));
            try (var directories = Files.list(root)) {
                assertFalse(directories.anyMatch(path -> path.getFileName().toString().startsWith("test-report-")));
            }
        }
    }

    private org.codedefenders.game.Test gameTest() {
        var test = mock(org.codedefenders.game.Test.class);
        Path directory = root.resolve("tests/mp/42/101/original/1");
        when(test.getId()).thenReturn(71);
        when(test.getGameId()).thenReturn(42);
        when(test.getDirectory()).thenReturn(directory.toString());
        when(test.getJavaFile()).thenReturn(directory.resolve("QualifiedTest.java").toString());
        when(test.getFullyQualifiedClassName()).thenReturn("example.QualifiedTest");
        return test;
    }

    private MockedStatic<AntRunner> execution(AntProcessResult result) {
        var execution = mockStatic(AntRunner.class);
        execution.when(() -> AntRunner.runAntProcess(any(ProcessBuilder.class))).thenAnswer(call -> {
            invocation = call.getArgument(0);
            return result;
        });
        return execution;
    }

    private static AntProcessResult outcome(TargetExecution.Status status) {
        AntProcessResult result = new AntProcessResult();
        result.setInputStream(new BufferedReader(new StringReader("BUILD SUCCESSFUL\n")));
        result.setTestOutcome(status == TargetExecution.Status.FAIL, status == TargetExecution.Status.ERROR);
        return result;
    }
}
