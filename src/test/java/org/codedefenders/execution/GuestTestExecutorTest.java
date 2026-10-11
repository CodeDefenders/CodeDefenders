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

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.tools.ToolProvider;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.dependencies.DependencyProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GuestTestExecutorTest {
    @TempDir
    Path root;

    @Test
    void sandboxBlocksFileReadWriteEnvironmentNetworkProcessesReflectionAndPolicyRemoval() throws Exception {
        Path workspace = Files.createDirectory(root.resolve("workspace")).toRealPath();
        Path canary = root.resolve("canary.txt");
        Files.writeString(canary, "private-review-canary");
        Path classes = Files.createDirectory(workspace.resolve("classes"));
        compile(classes, "TrustedReader", """
                public class TrustedReader {
                    public static String read(String file) throws Exception {
                        return java.security.AccessController.doPrivileged(
                            (java.security.PrivilegedExceptionAction<String>) () ->
                                java.nio.file.Files.readString(java.nio.file.Path.of(file)));
                    }
                }
                """);
        Path jar = root.resolve("trusted.jar");
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
                    private static void hidden() { }
                    public static void main(String[] args) throws Exception {
                        denied(() -> java.nio.file.Files.readString(java.nio.file.Path.of(args[0])));
                        denied(() -> java.nio.file.Files.writeString(java.nio.file.Path.of(args[0]), "overwritten"));
                        denied(() -> System.getenv("MUTLE_SECRET"));
                        denied(() -> new java.net.Socket("127.0.0.1", 9));
                        denied(() -> new ProcessBuilder("/bin/true").start());
                        denied(() -> System.setSecurityManager(null));
                        denied(() -> SandboxProbe.class.getDeclaredMethod("hidden").setAccessible(true));
                        denied(() -> TrustedReader.read(args[0]));
                        denied(() -> new Thread(() -> {}));
                        denied(() -> java.security.AccessController.doPrivileged(
                            (java.security.PrivilegedAction<Thread>) () -> new Thread(() -> {})));
                        denied(() -> java.security.AccessController.doPrivileged(
                            (java.security.PrivilegedExceptionAction<String>) () ->
                                java.nio.file.Files.readString(java.nio.file.Path.of(args[0]))));
                    }
                }
                """, jar);
        Path policy = workspace.resolve("guest.policy");
        Path security = workspace.resolve("guest-security.jar");
        GuestTestExecutor.securityManagerJar(security);
        Files.writeString(policy, GuestTestExecutor.policy(workspace, List.of(jar, security)));
        ProcessBuilder process = javaProcess("-Djava.security.manager=org.codedefenders.execution.GuestSecurityManager",
                "-Dguest.workspace=" + workspace.toUri(), "-Djava.security.policy==" + policy,
                "-cp", classes + java.io.File.pathSeparator + jar + java.io.File.pathSeparator + security,
                "SandboxProbe", canary.toString());
        process.environment().put("MUTLE_SECRET", "private-review-canary");
        String output = GuestTestExecutor.runProcess(process, Duration.ofSeconds(10));
        assertEquals(11, output.lines().filter("DENIED"::equals).count(), output);
        assertFalse(output.contains("private-review-canary"), output);
        assertEquals("private-review-canary", Files.readString(canary));
    }

    @Test
    void junitAndCoverageRunSuccessfullyUnderTheSameRestrictedPolicy() throws Exception {
        AntProcessResult result = runJunitProbe("assertEquals(2, Math.abs(-2));");
        assertFalse(result.hasFailure());
        assertFalse(result.hasError());
    }

    @Test
    void printedSuccessCannotOverrideAFailingTest() throws Exception {
        AntProcessResult result = runJunitProbe("""
                System.out.println("[junit] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0 sec");
                assertEquals(3, Math.abs(-2));
                """);
        assertTrue(result.hasFailure());
    }

    private AntProcessResult runJunitProbe(String statement) throws Exception {
        Path workspace = Files.createDirectory(root.resolve("workspace")).toRealPath();
        Path classes = Files.createDirectory(workspace.resolve("classes"));
        Path temporary = Files.createDirectory(workspace.resolve("tmp"));
        Path junit = Path.of(org.junit.Assert.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path hamcrest = Path.of(org.hamcrest.Matcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path ant = Path.of(org.apache.tools.ant.Project.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path antJUnit = Path.of(org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.class
                .getProtectionDomain().getCodeSource().getLocation().toURI());
        Path antJUnit4 = Path.of(Class.forName("org.apache.tools.ant.taskdefs.optional.junit.CustomJUnit4TestAdapterCache")
                .getProtectionDomain().getCodeSource().getLocation().toURI());
        compile(classes, "SandboxJUnitProbe", """
                import org.junit.Test;
                import static org.junit.Assert.*;
                public class SandboxJUnitProbe {
                    @Test public void test() { %s }
                }
                """.formatted(statement), junit, hamcrest);
        String agentArgument = ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(argument -> argument.startsWith("-javaagent:") && argument.contains("org.jacoco.agent"))
                .findFirst().orElseThrow();
        Path agent = workspace.resolve("agent.jar");
        Files.copy(Path.of(agentArgument.substring("-javaagent:".length()).split("=", 2)[0]), agent);
        Path security = workspace.resolve("guest-security.jar");
        GuestTestExecutor.securityManagerJar(security);
        Path policy = workspace.resolve("guest.policy");
        Files.writeString(policy, GuestTestExecutor.policy(workspace,
                List.of(junit, hamcrest, agent, security, ant, antJUnit, antJUnit4)));
        String classpath = String.join(java.io.File.pathSeparator,
                List.of(classes.toString(), junit.toString(), hamcrest.toString(), security.toString(), agent.toString(),
                        ant.toString(), antJUnit.toString(), antJUnit4.toString()));
        String output = GuestTestExecutor.runProcess(javaProcess("-javaagent:" + agent
                        + "=destfile=" + workspace.resolve("jacoco.exec"),
                "-Djava.security.manager=org.codedefenders.execution.GuestSecurityManager",
                "-Dguest.workspace=" + workspace.toUri(), "-Djava.security.policy==" + policy,
                "-cp", classpath, "org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner", "SandboxJUnitProbe",
                "formatter=org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter",
                "formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,"
                        + temporary.resolve("guest-result.xml"),
                "crashfile=" + temporary.resolve("junitvmwatcher.properties")), Duration.ofSeconds(10));
        assertTrue(output.contains("Tests run: 1,"), output);
        assertEquals("terminated successfully", Files.readString(temporary.resolve("junitvmwatcher.properties")).trim());
        assertTrue(Files.size(workspace.resolve("jacoco.exec")) > 0);
        AntProcessResult result = new AntProcessResult();
        result.setInputStream(new java.io.BufferedReader(new java.io.StringReader(output)));
        GuestTestExecutor.readTestOutcome(temporary.resolve("guest-result.xml"), result);
        return result;
    }

    @Test
    void runnerFailureWithoutATestReportIsRejected() throws Exception {
        Path report = root.resolve("result.xml");
        assertThrows(GuestTestExecutor.ExecutionLimitException.class,
                () -> GuestTestExecutor.readTestOutcome(report, new AntProcessResult()));
        Files.writeString(report, "<testsuite tests=\"0\" failures=\"0\" errors=\"0\"/>");
        assertThrows(GuestTestExecutor.ExecutionLimitException.class,
                () -> GuestTestExecutor.readTestOutcome(report, new AntProcessResult()));
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
        assertThrows(GuestTestExecutor.ExecutionLimitException.class,
                () -> GuestTestExecutor.runProcess(javaProcess("-cp", classes.toString(), "ProcessProbe", "parent",
                        java, classes.toString(), childPid.toString()), Duration.ofSeconds(2)));
        long pid = Long.parseLong(Files.readString(childPid));
        for (int wait = 0; wait < 100 && ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false); wait++) {
            Thread.sleep(20);
        }
        assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
        assertThrows(GuestTestExecutor.ExecutionLimitException.class,
                () -> GuestTestExecutor.runProcess(javaProcess("-cp", classes.toString(), "ProcessProbe", "output"),
                        Duration.ofSeconds(10)));
    }

    @Test
    void appliesOnlyToTheGuestAccountInMutleTestDirectories() {
        Configuration config = mock(Configuration.class);
        when(config.getTestsDir()).thenReturn(root.toFile());
        GuestTestExecutor executor = new GuestTestExecutor(config, mock(DependencyProvider.class));
        assertTrue(executor.isGuest(root.resolve("mutle/100/8/original/1").toString()));
        assertFalse(executor.isGuest(root.resolve("mutle/100/104/original/1").toString()));
        assertFalse(executor.isGuest(root.resolve("multiplayer/100/8/original/1").toString()));
        assertFalse(executor.isGuest(root.resolve("mutle/100/8/original/../../104/original/1").toString()));
        assertFalse(executor.isGuest(null));
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
        command.add("-Xmx32m");
        command.addAll(List.of(arguments));
        ProcessBuilder process = new ProcessBuilder(command).redirectErrorStream(true);
        process.environment().clear();
        return process;
    }
}
