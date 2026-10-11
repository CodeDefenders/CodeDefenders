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
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.dependencies.DependencyProvider;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/** Executes anonymous tests with a separate workspace, policy, environment and resource budget. */
@ApplicationScoped
public class GuestTestExecutor {
    private static final Logger logger = LoggerFactory.getLogger(GuestTestExecutor.class);
    static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(30);
    static final int MAX_OUTPUT_BYTES = 65_536;
    private final Configuration config;
    private final DependencyProvider dependencies;

    @Inject
    public GuestTestExecutor(Configuration config, DependencyProvider dependencies) {
        this.config = config;
        this.dependencies = dependencies;
    }

    boolean isGuest(String directory) {
        if (directory == null) {
            return false;
        }
        Path root = config.getTestsDir().toPath().toAbsolutePath().normalize().resolve("mutle");
        Path path = Path.of(directory).toAbsolutePath().normalize();
        if (!path.startsWith(root)) {
            return false;
        }
        Path relative = root.relativize(path);
        return relative.getNameCount() >= 4 && relative.getName(0).toString().matches("[1-9][0-9]*")
                && relative.getName(1).toString().equals(String.valueOf(Constants.MUTLE_GUEST_USER_ID))
                && relative.getName(2).toString().equals("original");
    }

    AntProcessResult run(String target, String mutantDirectory, String testDirectory,
                         Path cutDirectory, String testClassName) {
        Path workspace = null;
        try {
            workspace = Files.createTempDirectory("codedefenders-mutle-").toRealPath();
            Path test = workspace.resolve("test");
            copyCode(Path.of(testDirectory), test);
            copyCode(cutDirectory, workspace.resolve("cut"));
            if (mutantDirectory != null) {
                copyCode(Path.of(mutantDirectory), workspace.resolve("mutant"));
            }
            Files.createDirectory(workspace.resolve("tmp"));
            try (var resource = GuestTestExecutor.class.getResourceAsStream("/data/guest-build.xml")) {
                if (resource == null) {
                    throw new IOException("Guest build resource is missing");
                }
                Files.copy(resource, workspace.resolve("build.xml"));
            }
            // Mutle uses JUnit 4/Hamcrest. Do not expose bytecode injection/agent libraries to guest code.
            List<Path> common = classpath(dependencies.getCommonClasspath()).stream()
                    .filter(path -> path.getFileName().toString().matches("(?:junit-4\\..*|hamcrest-.*)\\.jar"))
                    .toList();
            List<Path> jars = new ArrayList<>(common);
            try (var antJars = Files.list(config.getAntHome().toPath().resolve("lib"))) {
                antJars.filter(path -> path.toString().endsWith(".jar")).forEach(jars::add);
            }
            extractAgent(classpath(dependencies.getJacocoClasspath()), workspace.resolve("agent.jar"));
            jars.add(workspace.resolve("agent.jar"));
            securityManagerJar(workspace.resolve("guest-security.jar"));
            jars.add(workspace.resolve("guest-security.jar"));
            Files.writeString(workspace.resolve("guest.policy"), policy(workspace, jars));

            String commonClasspath = String.join(File.pathSeparator, common.stream().map(Path::toString).toList());
            List<String> command = new ArrayList<>(List.of(config.getAntHome().toPath().resolve("bin/ant").toString(),
                    target, "-Dcommon.cp=" + commonClasspath,
                    "-Dguest.workspace=" + workspace.toUri(),
                    "-DtestClassname=" + (testClassName == null ? "" : testClassName)));
            ProcessBuilder process = new ProcessBuilder(command).directory(workspace.toFile()).redirectErrorStream(true);
            // Do not expose database, mail, API or deployment credentials to submitted code.
            process.environment().clear();
            process.environment().put("PATH", "/usr/bin:/bin");
            process.environment().put("JAVA_HOME", config.getAntJavaHome().map(File::toString)
                    .orElse(System.getProperty("java.home")));
            process.environment().put("ANT_OPTS", "-Xmx128m -XX:MaxMetaspaceSize=96m");
            String output = runProcess(process, PROCESS_TIMEOUT);
            if (output.contains("Timeout occurred") || output.contains("java.lang.OutOfMemoryError")
                    || output.contains("FATAL ERROR in native method")) {
                throw new ExecutionLimitException();
            }
            AntProcessResult result = new AntProcessResult();
            result.setInputStream(new BufferedReader(new StringReader(output)));
            if (!"compile-test".equals(target)) {
                if (!result.compiled()) {
                    logger.warn("Guest runner failed on {}: {}", target, output);
                    throw new ExecutionLimitException();
                }
                readTestOutcome(workspace.resolve("tmp/guest-result.xml"), result);
            }
            if ("compile-test".equals(target) && result.compiled()) {
                copyCode(test, Path.of(testDirectory));
            }
            Path coverage = workspace.resolve("jacoco.exec");
            if ("test-original".equals(target) && Files.exists(coverage)) {
                Files.copy(coverage, Path.of(testDirectory, "jacoco.exec"), StandardCopyOption.REPLACE_EXISTING);
            }
            return result;
        } catch (IOException e) {
            throw new ExecutionLimitException(e);
        } finally {
            if (workspace != null) {
                try {
                    org.apache.commons.io.FileUtils.deleteDirectory(workspace.toFile());
                } catch (IOException e) {
                    // A failed cleanup must be visible rather than leaving anonymous artifacts unnoticed.
                    throw new ExecutionLimitException(e);
                }
            }
        }
    }

    private static List<Path> classpath(String value) {
        return Arrays.stream(value.split(Pattern.quote(File.pathSeparator)))
                .map(Path::of).map(Path::toAbsolutePath).map(Path::normalize).toList();
    }

    private static void copyCode(Path source, Path destination) throws IOException {
        Files.createDirectories(destination);
        try (var paths = Files.walk(source)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String name = path.getFileName().toString();
                if (!Files.isSymbolicLink(path) && (name.endsWith(".java") || name.endsWith(".class"))) {
                    Path target = destination.resolve(source.relativize(path));
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void extractAgent(List<Path> jars, Path destination) throws IOException {
        for (Path jar : jars) {
            if (jar.getFileName().toString().startsWith("org.jacoco.agent-")) {
                try (var zip = new ZipFile(jar.toFile())) {
                    var entry = zip.getEntry("jacocoagent.jar");
                    if (entry != null) {
                        try (var input = zip.getInputStream(entry)) {
                            Files.copy(input, destination);
                        }
                        return;
                    }
                }
            }
        }
        throw new IOException("Guest coverage agent is missing");
    }

    /** Neither submitted classes nor libraries receive access to application files, sockets or environment variables. */
    static String policy(Path workspace, List<Path> jars) {
        StringBuilder policy = new StringBuilder("grant { permission java.util.PropertyPermission \"*\", \"read\"; };\n");
        List<Path> dependencies = jars.stream().distinct().toList();
        for (Path jar : dependencies) {
            policy.append("grant codeBase \"").append(jar.toUri()).append("\" {\n")
                    .append("permission java.io.FilePermission \"").append(escape(workspace.toString()))
                    .append("\", \"read\";\n")
                    .append("permission java.io.FilePermission \"").append(escape(workspace + "/-"))
                    .append("\", \"read\";\n")
                    // Ant's VM watcher is updated through JUnit's test-listener callback stack.
                    .append("permission java.io.FilePermission \"")
                    .append(escape(workspace.resolve("tmp") + "/-"))
                    .append("\", \"read,write,delete\";\n")
                    .append("permission java.lang.RuntimePermission \"getClassLoader\";\n")
                    .append("permission java.lang.RuntimePermission \"accessDeclaredMembers\";\n")
                    .append("permission java.lang.RuntimePermission \"shutdownHooks\";\n")
                    .append("permission java.lang.reflect.ReflectPermission \"suppressAccessChecks\";\n");
            if (jar.equals(workspace.resolve("agent.jar")) || jar.getFileName().toString().startsWith("ant")) {
                policy.append("permission java.lang.RuntimePermission \"createClassLoader\";\n");
            }
            if (jar.getFileName().toString().startsWith("ant")) {
                // Ant's forked JUnit launcher writes its VM watcher and redirects the test's output.
                policy.append("permission java.lang.RuntimePermission \"setIO\";\n")
                        .append("permission java.lang.RuntimePermission \"exitVM\";\n")
                        .append("permission java.util.PropertyPermission \"*\", \"read,write\";\n");
            }
            for (Path dependency : dependencies) {
                policy.append("permission java.io.FilePermission \"").append(escape(dependency.toString()))
                        .append("\", \"read\";\n");
            }
            if (jar.equals(workspace.resolve("agent.jar"))) {
                policy.append("permission java.lang.RuntimePermission \"defineClass\";\n")
                        .append("permission java.io.FilePermission \"")
                        .append(escape(workspace.toString())).append("\", \"write\";\n")
                        .append("permission java.io.FilePermission \"")
                        .append(escape(workspace.resolve("jacoco.exec").toString()))
                        .append("\", \"write\";\n");
            }
            if (jar.equals(workspace.resolve("guest-security.jar"))) {
                policy.append("permission java.lang.RuntimePermission \"getProtectionDomain\";\n")
                        .append("permission java.lang.RuntimePermission \"modifyThread\";\n")
                        .append("permission java.lang.RuntimePermission \"modifyThreadGroup\";\n");
            }
            policy.append("};\n");
        }
        return policy.toString();
    }

    static void securityManagerJar(Path destination) throws IOException {
        String name = "org/codedefenders/execution/GuestSecurityManager.class";
        try (var output = new JarOutputStream(Files.newOutputStream(destination));
             var input = GuestTestExecutor.class.getResourceAsStream("/" + name)) {
            if (input == null) {
                throw new IOException("Guest security manager is missing");
            }
            output.putNextEntry(new JarEntry(name));
            input.transferTo(output);
            output.closeEntry();
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    static void readTestOutcome(Path report, AntProcessResult result) {
        try {
            if (Files.size(report) > MAX_OUTPUT_BYTES) {
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
        FutureTask<String> output = new FutureTask<>(() -> {
            try (var input = process.getInputStream(); var bytes = new ByteArrayOutputStream()) {
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
        Thread reader = new Thread(output, "mutle-guest-output");
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
        } catch (ExecutionException | TimeoutException e) {
            throw new ExecutionLimitException(e);
        } finally {
            kill(process);
            process.getInputStream().close();
            output.cancel(true);
        }
    }

    private static void kill(Process process) {
        List<ProcessHandle> children = process.descendants().toList();
        children.forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    public static class ExecutionLimitException extends RuntimeException {
        public ExecutionLimitException() {
            super("Guest execution exceeded its resource budget or could not run safely");
        }

        public ExecutionLimitException(Throwable cause) {
            super("Guest execution could not run safely", cause);
        }
    }
}
