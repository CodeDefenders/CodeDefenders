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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import javax.tools.ToolProvider;

import org.codedefenders.dependencies.DependencyProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AntRunnerTest {
    @TempDir
    Path root;

    @Test
    void sandboxBlocksFileReadWriteEnvironmentNetworkProcessesAndPolicyRemoval() throws Exception {
        Path workspace = Files.createDirectory(root.resolve("workspace")).toRealPath();
        Path library = Files.createDirectory(workspace.resolve("lib"));
        Path canary = root.resolve("canary.txt");
        Files.writeString(canary, "private-review-canary");
        Path classes = Files.createDirectory(workspace.resolve("classes"));
        compile(classes, "TrustedReader", """
                public class TrustedReader {
                    public static Thread thread() {
                        return java.security.AccessController.doPrivileged(
                            (java.security.PrivilegedAction<Thread>) () -> new Thread(() -> {}));
                    }
                    public static String read(String file) throws Exception {
                        return java.security.AccessController.doPrivileged(
                            (java.security.PrivilegedExceptionAction<String>) () ->
                                java.nio.file.Files.readString(java.nio.file.Path.of(file)));
                    }
                }
                """);
        Path jar = library.resolve("trusted.jar");
        try (var output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry("TrustedReader.class"));
            output.write(Files.readAllBytes(classes.resolve("TrustedReader.class")));
            output.closeEntry();
        }
        Files.delete(classes.resolve("TrustedReader.class"));
        compile(classes, "SandboxProbe", """
                public class SandboxProbe {
                    interface Action { void run() throws Exception; }
                    static void denied(Action action) throws Exception {
                        try { action.run(); throw new AssertionError("escaped"); }
                        catch (SecurityException expected) { System.out.println("DENIED"); }
                    }
                    public static void main(String[] args) throws Exception {
                        denied(() -> java.nio.file.Files.readString(java.nio.file.Path.of(args[0])));
                        denied(() -> java.nio.file.Files.writeString(java.nio.file.Path.of(args[0]), "overwritten"));
                        denied(() -> System.getenv("MUTLE_SECRET"));
                        denied(() -> new java.net.Socket("127.0.0.1", 9));
                        denied(() -> new ProcessBuilder("/bin/true").start());
                        denied(() -> System.setSecurityManager(null));
                        denied(() -> TrustedReader.read(args[0]));
                        denied(() -> SandboxProbe.class.getProtectionDomain().getPermissions()
                            .add(new java.security.AllPermission()));
                        denied(() -> TrustedReader.thread());
                        denied(() -> new Thread(() -> {}));
                        denied(() -> java.security.AccessController.doPrivileged(
                            (java.security.PrivilegedAction<Thread>) () -> new Thread(() -> {})));
                        denied(() -> java.security.AccessController.doPrivileged(
                            (java.security.PrivilegedExceptionAction<String>) () ->
                                java.nio.file.Files.readString(java.nio.file.Path.of(args[0]))));
                    }
                }
                """, jar);
        Path policy = workspace.resolve("test.policy");
        Path security = library.resolve("test-security.jar");
        DependencyProvider.securityManagerJar(security);
        Files.writeString(policy, resource("security.policy")
                .replace("${codedef.library.url}", library.toUri().toString())
                .replace("${ant.library.url}", root.resolve("ant/lib").toUri().toString()));
        ProcessBuilder process = javaProcess("-Djava.security.manager=org.codedefenders.execution.TestSecurityManager",
                "-Dcodedef.library=" + library, "-Dcodedef.home=" + workspace,
                "-Dcut.dir=" + classes, "-Dtest.file=" + classes, "-Dmutant.file=" + classes,
                "-Dmutant.test.file=" + classes, "-Dtest.report.dir=" + workspace.resolve("tmp"),
                "-Dant.home=" + root.resolve("ant"), "-Djava.security.policy==" + policy,
                "-cp", classes + java.io.File.pathSeparator + jar + java.io.File.pathSeparator + security,
                "SandboxProbe", canary.toString());
        process.environment().put("MUTLE_SECRET", "private-review-canary");
        String output = AntRunner.runProcess(process, Duration.ofSeconds(10));
        assertEquals(12, output.lines().filter("DENIED"::equals).count(), output);
        assertFalse(output.contains("private-review-canary"), output);
        assertEquals("private-review-canary", Files.readString(canary));
    }

    @Test
    void submittedOutputCanContainExecutionFailureMessages() throws Exception {
        AntProcessResult result = runJunitProbe("""
                for (String message : java.util.List.of(
                        "Timeout occurred",
                        "java.lang.OutOfMemoryError",
                        "FATAL ERROR in native method",
                        "Could not create SecurityManager",
                        "Forked Java VM exited abnormally")) {
                    System.out.println(message);
                }
                assertEquals(2, Math.abs(-2));
                """);
        assertFalse(result.hasFailure(), result.getErrorMessage());
        assertFalse(result.hasError(), result.getErrorMessage());
    }

    @Test
    void junitTimeoutIsReturnedAsAnErrorForMutants() throws Exception {
        Fixture fixture = fixture();
        compile(fixture.directory().resolve("mutant"), "Sample", """
                public class Sample {
                    public int value() {
                        while (true) { }
                    }
                }
                """, fixture.directory().resolve("cut"));
        fixture.test("""
                import org.junit.Test;
                import static org.junit.Assert.*;
                public class SandboxJUnitProbe {
                    @Test public void test() { assertEquals(2, new Sample().value()); }
                }
                """);
        assertTrue(fixture.run("compile-test").compiled());
        AntProcessResult result = fixture.run("test-mutant");
        assertFalse(result.hasFailure(), result.getErrorMessage());
        assertTrue(result.hasError(), result.getErrorMessage());
    }

    @Test
    void printedSuccessCannotOverrideAFailingTest() throws Exception {
        AntProcessResult result = runJunitProbe("""
                System.out.println("[junit] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0 sec");
                assertEquals(3, Math.abs(-2));
                """);
        assertTrue(result.hasFailure());
    }

    @Test
    void securityFailureDetailsFitTheStoredExecutionMessage() throws Exception {
        AntProcessResult result = runJunitProbe("""
                System.out.println("TEST_BODY_ENTERED");
                java.nio.file.Files.readString(java.nio.file.Path.of("/etc/passwd"));
                """);
        assertTrue(result.hasError(), result.getErrorMessage());
        // TargetExecutionDAO stores at most 2,000 characters, before Mutle formats the details.
        String stored = result.getErrorMessage().substring(0, Math.min(2_000, result.getErrorMessage().length()));
        assertTrue(stored.contains("TEST_BODY_ENTERED"), stored);
        assertTrue(stored.contains("AccessControlException"), stored);
    }

    @Test
    void submittedCodeCannotInspectOriginsOrControlCoverageReflectively() throws Exception {
        for (String statement : List.of(
                "SandboxJUnitProbe.class.getProtectionDomain();",
                "Class.forName(\"org.jacoco.agent.rt.RT\").getMethod(\"getAgent\").invoke(null);")) {
            AntProcessResult result = runJunitProbe(statement);
            assertTrue(result.hasError(), statement + "\n" + result.getErrorMessage());
            assertTrue(result.getErrorMessage().contains("SecurityException")
                    || result.getErrorMessage().contains("AccessControlException"), result.getErrorMessage());
        }
    }

    private AntProcessResult runJunitProbe(String statement) throws Exception {
        return runJunitProbe(statement, false);
    }

    private AntProcessResult runJunitProbe(String statement, boolean mutle) throws Exception {
        Fixture fixture = fixture(mutle);
        fixture.test("import org.junit.Test; import static org.junit.Assert.*; public class SandboxJUnitProbe {"
                + " @Test public void test() throws Exception { " + statement + " } }");
        AntProcessResult compilation = fixture.run("compile-test");
        assertTrue(compilation.compiled(), compilation.getErrorMessage());
        return fixture.run("test-original");
    }

    private static String resource(String name) throws IOException {
        try (var input = AntRunnerTest.class.getResourceAsStream("/data/" + name)) {
            return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private Fixture fixture() throws Exception {
        return fixture(false);
    }

    private Fixture fixture(boolean mutle) throws Exception {
        Path directory = Files.createTempDirectory(root, "execution-").toRealPath();
        Path library = Files.createDirectory(directory.resolve("lib"));
        Path ant = Files.createDirectories(directory.resolve("ant/lib"));
        java.util.ArrayList<String> common = new java.util.ArrayList<>();
        List<String> entries = new java.util.ArrayList<>(List.of(
                System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"))
                        .split(java.util.regex.Pattern.quote(java.io.File.pathSeparator))));
        String runtimeLibraries = System.getProperty("sharedAnt.runtimeLibraryDir");
        if (runtimeLibraries != null) {
            entries.removeIf(entry -> !Path.of(entry).getFileName().toString().startsWith("ant-"));
            try (var paths = Files.walk(Path.of(runtimeLibraries))) {
                paths.filter(path -> path.toString().endsWith(".jar")).map(Path::toString).forEach(entries::add);
            }
        }
        for (String entry : entries) {
            Path source = Path.of(entry);
            if (entry.endsWith(".jar") && Files.isRegularFile(source)
                    && source.getFileName().toString().matches("(?:ant|junit|hamcrest|mockito|byte-buddy|objenesis"
                        + "|truth|guava|failureaccess|listenablefuture|checker|error_prone|j2objc|jsr305"
                        + "|org.jacoco|asm).*\\.jar")) {
                Path destination = source.getFileName().toString().startsWith("ant-")
                        ? ant.resolve(source.getFileName()) : library.resolve(source.getFileName());
                Files.copy(source, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                common.add(destination.toString());
            }
        }
        DependencyProvider.securityManagerJar(library.resolve("test-security.jar"));
        for (String name : List.of("build.xml", "security.policy", "security.properties")) {
            Files.writeString(directory.resolve(name), resource(name));
        }
        for (String name : List.of("cut", "test", "mutant", "recompiled")) {
            Files.createDirectory(directory.resolve(name));
        }
        // Runtime Code Defenders uses Mockito 4's subclass mock maker, not Mockito 5's inline default.
        Files.createDirectories(library.resolve("mockito-extensions"));
        Files.writeString(library.resolve("mockito-extensions/org.mockito.plugins.MockMaker"), "org.mockito.internal.creation.bytebuddy.SubclassByteBuddyMockMaker");
        common.add(library.toString());
        compile(directory.resolve("cut"), "Dependency", "public class Dependency { public static int value() { return 2; } }");
        compile(directory.resolve("cut"), "Sample",
                "public class Sample { public int value() { return Dependency.value(); } }", directory.resolve("cut"));
        compile(directory.resolve("mutant"), "Sample",
                "public class Sample { public int value() { return Dependency.value() + 1; } }", directory.resolve("cut"));
        Files.copy(root.resolve("Sample.java"), directory.resolve("mutant/Sample.java"));
        Path testDirectory = Files.createDirectories(directory.resolve(
                mutle ? "tests/mutle/1/8/original/1" : "tests/multiplayer/1/8/original/1"));
        return new Fixture(directory, String.join(java.io.File.pathSeparator, common), testDirectory);
    }

    private record Fixture(Path directory, String classpath, Path testDirectory) {
        void test(String source) throws IOException {
            Files.writeString(testDirectory.resolve("SandboxJUnitProbe.java"), source);
        }

        AntProcessResult run(String target) {
            ProcessBuilder builder = javaProcess("-cp", classpath, "org.apache.tools.ant.Main", target,
                    "-Dant.home=" + directory.resolve("ant"),
                    "-Dcodedef.home=" + directory,
                    "-Dcodedef.library=" + directory.resolve("lib"),
                    "-Dcommon.cp=" + classpath, "-Djacoco.cp=" + classpath,
                    "-Dcut.dir=" + directory.resolve("cut"), "-Dtest.file=" + testDirectory,
                    "-Dmutant.file=" + directory.resolve("mutant"),
                    "-Dmutant.test.file=" + directory.resolve("recompiled"),
                    "-DtestClassname=SandboxJUnitProbe");
            builder.directory(directory.toFile()).redirectErrorStream(true);
            try {
                return AntRunner.runAntProcess(builder);
            } catch (AntRunner.ExecutionLimitException e) {
                if (e.getCause() != null) {
                    throw new AssertionError(e.getCause().getMessage(), e);
                }
                throw e;
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sharedBuildSupportsAssertionsMocksCoverageAndBothMutantTargets(boolean mutle) throws Exception {
        Fixture fixture = fixture(mutle);
        fixture.test("""
                import org.junit.Test;
                import static org.junit.Assert.*;
                import static org.hamcrest.MatcherAssert.assertThat;
                import static org.hamcrest.Matchers.is;
                import static org.mockito.Mockito.*;
                public class SandboxJUnitProbe {
                    @Test public void test() throws Exception {
                        SandboxJUnitProbe.class.getDeclaredMethod("test").setAccessible(true);
                        assertNotNull(Class.forName("net.bytebuddy.ByteBuddy"));
                        Sample mock = mock(Sample.class);
                        when(mock.value()).thenReturn(7);
                        assertEquals(7, mock.value());
                        verify(mock).value();
                        assertEquals(2, new Sample().value());
                        assertThat(new Sample().value(), is(2));
                        com.google.common.truth.Truth.assertThat(new Sample().value()).isEqualTo(2);
                    }
                }
                """);
        assertTrue(fixture.run("compile-test").compiled());
        AntProcessResult original = fixture.run("test-original");
        assertFalse(original.hasFailure(), original.getErrorMessage());
        assertFalse(original.hasError(), original.getErrorMessage());
        Path coverage = fixture.testDirectory().resolve("jacoco.exec");
        byte[] originalCoverage = Files.readAllBytes(coverage);
        var loader = new org.jacoco.core.tools.ExecFileLoader();
        loader.load(coverage.toFile());
        assertTrue(loader.getExecutionDataStore().getContents().stream()
                .anyMatch(data -> data.getName().equals("Sample") && data.hasHits()));
        assertTrue(loader.getExecutionDataStore().getContents().stream()
                .anyMatch(data -> data.getName().equals("Dependency") && data.hasHits()));
        AntProcessResult mutant = fixture.run("test-mutant");
        assertTrue(mutant.hasFailure(), mutant.getErrorMessage());
        assertFalse(mutant.hasError(), mutant.getErrorMessage());
        AntProcessResult recompiled = fixture.run("recompiled-test-mutant");
        assertTrue(recompiled.hasFailure(), recompiled.getErrorMessage());
        assertFalse(recompiled.hasError(), recompiled.getErrorMessage());
        assertTrue(java.util.Arrays.equals(originalCoverage, Files.readAllBytes(coverage)),
                "Mutant executions must not overwrite original coverage");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sharedBuildDeniesValidationBypassesAndOrdinaryThreadCreation(boolean mutle) throws Exception {
        for (String statement : List.of(
                "java.nio.file.Files.readString(java.nio.file.Path.of(\"/etc/passwd\"));",
                "java.nio.file.Files.writeString(java.nio.file.Path.of(\"escape.txt\"), \"escape\");",
                "System.getenv(\"PATH\");",
                "new ProcessBuilder(\"/bin/true\").start();",
                "new java.net.Socket(\"127.0.0.1\", 9);",
                "new Thread(() -> {}).start();",
                "java.util.concurrent.CompletableFuture.runAsync(() -> {}).join();",
                "System.getProperty(\"mutant.file\");",
                "System.getProperty(\"java.class.path\");",
                "System.setSecurityManager(null);",
                "new ClassLoader() {};",
                "new net.bytebuddy.ByteBuddy().subclass(Object.class).make()"
                    + ".load(SandboxJUnitProbe.class.getClassLoader());")) {
            AntProcessResult result = runJunitProbe(statement, mutle);
            assertTrue(result.hasError(), statement + "\n" + result.getErrorMessage());
            assertTrue(result.getErrorMessage().contains("SecurityException")
                    || result.getErrorMessage().contains("AccessControlException"), result.getErrorMessage());
        }
    }

    @Test
    void mutantClassesCannotShadowTheSecurityManager() throws Exception {
        Fixture fixture = fixture();
        compile(fixture.directory().resolve("mutant"), "TestSecurityManager", """
                package org.codedefenders.execution;
                public class TestSecurityManager extends SecurityManager {
                    @Override public void checkPermission(java.security.Permission permission) { }
                }
                """);
        fixture.test("""
                import org.junit.Test;
                public class SandboxJUnitProbe {
                    @Test public void test() throws Exception {
                        java.nio.file.Files.readString(java.nio.file.Path.of("/etc/passwd"));
                    }
                }
                """);
        assertTrue(fixture.run("compile-test").compiled());
        for (String target : List.of("test-mutant", "recompiled-test-mutant")) {
            AntProcessResult result = fixture.run(target);
            assertTrue(result.hasError(), result.getErrorMessage());
            assertTrue(result.getErrorMessage().contains("AccessControlException"), result.getErrorMessage());
        }
    }

    @Test
    void coverageControlApiIsNotOnTheSubmittedCompilationClasspath() throws Exception {
        Fixture fixture = fixture();
        fixture.test("""
                import org.junit.Test;
                public class SandboxJUnitProbe {
                    @Test public void test() { org.jacoco.agent.rt.RT.getAgent().reset(); }
                }
                """);
        AntProcessResult result = fixture.run("compile-test");
        assertFalse(result.compiled(), result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("org.jacoco.agent.rt"), result.getErrorMessage());
    }

    @Test
    void securityManagerIsNotOnTheSubmittedCompilationClasspath() throws Exception {
        Fixture fixture = fixture();
        fixture.test("""
                import org.junit.Test;
                public class SandboxJUnitProbe {
                    @Test public void test() { new org.codedefenders.execution.TestSecurityManager(); }
                }
                """);
        AntProcessResult result = fixture.run("compile-test");
        assertFalse(result.compiled(), result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("TestSecurityManager"), result.getErrorMessage());
    }

    @Test
    void runnerFailureWithoutATestReportIsRejected() throws Exception {
        Path report = root.resolve("result.xml");
        assertThrows(AntRunner.ExecutionLimitException.class,
                () -> AntRunner.readTestOutcome(report, new AntProcessResult()));
        Files.writeString(report, "<testsuite tests=\"0\" failures=\"0\" errors=\"0\"/>");
        assertThrows(AntRunner.ExecutionLimitException.class,
                () -> AntRunner.readTestOutcome(report, new AntProcessResult()));
    }

    @Test
    void streamCleanupFailureDoesNotReplaceTimeout() throws Exception {
        IOException cleanupFailure = new IOException("output stream cleanup failed");
        var input = new ByteArrayInputStream(new byte[0]) {
            @Override
            public void close() throws IOException {
                throw cleanupFailure;
            }
        };
        Process process = mock(Process.class);
        ProcessBuilder builder = mock(ProcessBuilder.class);
        when(builder.start()).thenReturn(process);
        when(process.getInputStream()).thenReturn(input);
        when(process.waitFor(1_000, TimeUnit.MILLISECONDS)).thenReturn(false);
        when(process.descendants()).thenAnswer(invocation -> Stream.empty());
        var thrown = assertThrows(AntRunner.ExecutionLimitException.class,
                () -> AntRunner.runProcess(builder, Duration.ofSeconds(1)));
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(cleanupFailure, thrown.getSuppressed()[0]);
    }

    @Test
    void timeoutKillsChildProcessesAndExcessiveOutputIsRejected() throws Exception {
        Path classes = Files.createDirectory(root.resolve("classes"));
        compile(classes, "ProcessProbe", """
                public class ProcessProbe {
                    public static void main(String[] args) throws Exception {
                        if (args[0].equals("output")) {
                            System.out.println("x".repeat(100000));
                        } else if (args[0].equals("parent")) {
                            Process child = new ProcessBuilder(args[1], "-cp", args[2], "ProcessProbe", "child").start();
                            java.nio.file.Files.writeString(java.nio.file.Path.of(args[3]), "" + child.pid());
                            Thread.sleep(30000);
                        } else {
                            Thread.sleep(30000);
                        }
                    }
                }
                """);
        Path childPid = root.resolve("child.pid");
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        assertThrows(AntRunner.ExecutionLimitException.class,
                () -> AntRunner.runProcess(javaProcess("-cp", classes.toString(), "ProcessProbe", "parent",
                        java, classes.toString(), childPid.toString()), Duration.ofSeconds(2)));
        long pid = Long.parseLong(Files.readString(childPid));
        for (int wait = 0; wait < 100 && ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false); wait++) {
            Thread.sleep(20);
        }
        assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
        assertThrows(AntRunner.ExecutionLimitException.class,
                () -> AntRunner.runProcess(javaProcess("-cp", classes.toString(), "ProcessProbe", "output"),
                        Duration.ofSeconds(10)));
    }

    private void compile(Path classes, String name, String source, Path... dependencies) throws Exception {
        Path file = root.resolve(name + ".java");
        Files.writeString(file, source);
        List<String> arguments = new java.util.ArrayList<>(List.of("--release", "16", "-d", classes.toString()));
        if (dependencies.length > 0) {
            arguments.addAll(List.of("-classpath", String.join(java.io.File.pathSeparator,
                    java.util.Arrays.stream(dependencies).map(Path::toString).toList())));
        }
        arguments.add(file.toString());
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, arguments.toArray(String[]::new)));
    }

    private static ProcessBuilder javaProcess(String... arguments) {
        List<String> command = new java.util.ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.addAll(List.of(arguments));
        ProcessBuilder process = new ProcessBuilder(command).redirectErrorStream(true);
        process.environment().clear();
        return process;
    }
}
