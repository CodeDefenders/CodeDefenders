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
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

/** Bounded process execution and protected reports for the shared Ant runner. */
public final class TestExecutionSandbox {
    static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(30);
    static final int MAX_OUTPUT_BYTES = 65_536;

    private TestExecutionSandbox() {
    }

    static AntProcessResult run(ProcessBuilder builder) {
        // Keep execution configuration and scheduler variables, not application credentials.
        Map<String, String> environment = builder.environment();
        environment.keySet().removeIf(key -> !List.of("PATH", "JAVA_HOME", "ANT_HOME", "LANG", "LC_ALL")
                .contains(key) && !key.startsWith("SLURM_"));
        environment.put("ANT_OPTS", "-Xmx128m -XX:MaxMetaspaceSize=96m");
        try (var reports = new ReportDirectory(Files.createTempDirectory(builder.directory().toPath(), "test-report-"))) {
            builder.command().add("-Dtest.report.dir=" + reports.path());
            // Slurm owns queueing and the configured job deadline; the JUnit timeout starts on the worker.
            Duration deadline = "srun".equals(builder.command().get(0))
                    ? Duration.ofMillis(Long.MAX_VALUE) : PROCESS_TIMEOUT;
            String output = runProcess(builder, deadline);
            if (output.contains("Timeout occurred") || output.contains("java.lang.OutOfMemoryError")
                    || output.contains("FATAL ERROR in native method")
                    || output.contains("Could not create SecurityManager")
                    || output.contains("Forked Java VM exited abnormally")) {
                throw new ExecutionLimitException(new IOException(output));
            }
            AntProcessResult result = new AntProcessResult();
            result.setInputStream(new BufferedReader(new StringReader(output)));
            if (builder.command().stream().anyMatch(List.of("test-original", "test-mutant",
                    "recompiled-test-mutant")::contains)) {
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

    public static void securityManagerJar(Path destination) throws IOException {
        String name = "org/codedefenders/execution/TestSecurityManager.class";
        try (var output = new JarOutputStream(Files.newOutputStream(destination));
             var input = TestExecutionSandbox.class.getResourceAsStream("/" + name)) {
            if (input == null) {
                throw new IOException("Test security manager is missing");
            }
            output.putNextEntry(new JarEntry(name));
            input.transferTo(output);
            output.closeEntry();
        }
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
            } catch (ExecutionException | TimeoutException e) {
                throw new ExecutionLimitException(e);
            } finally {
                kill(process);
                output.cancel(true);
            }
        }
    }

    private static void kill(Process process) {
        List<ProcessHandle> children = process.descendants().toList();
        children.forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
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
