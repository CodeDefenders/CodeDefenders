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
package org.codedefenders.analysis.coverage.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.text.StringEscapeUtils;
import org.codedefenders.analysis.coverage.CoverageTest;
import org.codedefenders.analysis.coverage.line.CoverageTokens;
import org.codedefenders.analysis.coverage.line.DetailedLineCoverage;
import org.codedefenders.analysis.coverage.line.LineCoverageStatus;
import org.codedefenders.analysis.coverage.line.NewLineCoverage;
import org.codedefenders.util.ResourceUtils;

public class CoverageOutputWriter {
    private static final String HTML_OUTPUT_DIR = "/tmp/coverage";
    private static final boolean ENABLED = false;
    private static final boolean SPLIT_DIR_BY_JVM = true;

    private final String className;
    private final String testName;
    private final String sourceCode;
    private final DetailedLineCoverage originalCoverage;
    private final NewLineCoverage transformedCoverage;
    private final NewLineCoverage expectedCoverage;
    private final CoverageTokens coverageTokens;

    public CoverageOutputWriter(
            String className,
            String testName,
            String sourceCode,
            DetailedLineCoverage originalCoverage,
            NewLineCoverage transformedCoverage,
            NewLineCoverage expectedCoverage,
            CoverageTokens coverageTokens) {
        this.className = className;
        this.testName = testName;
        this.sourceCode = sourceCode;
        this.originalCoverage = originalCoverage;
        this.transformedCoverage = transformedCoverage;
        this.expectedCoverage = expectedCoverage;
        this.coverageTokens = coverageTokens;
    }

    public void writeCoverage() throws IOException {
        if (!ENABLED) {
            return;
        }

        String[] lines = sourceCode.split("\r?\n");
        String coverage = IntStream.rangeClosed(1, lines.length)
                .mapToObj(transformedCoverage::getStatus)
                .map(LineCoverageStatus::toString)
                .collect(Collectors.joining("\n"));

        write(coverage, "coverage");
    }

    public void writeHtml() throws IOException {
        if (!ENABLED) {
            return;
        }

        String pageTemplate = getPageTemplate();

        StringJoiner originalLines = new StringJoiner("\n");
        StringJoiner transformedLines = new StringJoiner("\n");
        StringJoiner expectedLines = new StringJoiner("\n");
        StringJoiner tokenLines = new StringJoiner("\n");

        String[] lines = sourceCode.split("\r?\n");
        for (int lineNum = 1; lineNum <= lines.length; lineNum++) {
            String code = lines[lineNum - 1];
            String escapedCode = StringEscapeUtils.escapeHtml4(code);

            LineCoverageStatus originalStatus = originalCoverage.getStatus(lineNum);
            LineCoverageStatus transformedStatus = transformedCoverage.getStatus(lineNum);
            LineCoverageStatus expectedStatus = expectedCoverage.getStatus(lineNum);
            CoverageTokens.Token rootToken = coverageTokens.getRoot(lineNum);

            originalLines.add(String.format("<div class=\"line %s\" data-line-num=\"%d\" data-status=\"%s\">%s</div>",
                    originalStatus.name(), lineNum, originalStatus.name(), escapedCode));
            transformedLines.add(String.format("<div class=\"line %s\" data-line-num=\"%d\" data-status=\"%s\">%s</div>",
                    transformedStatus.name(), lineNum, transformedStatus.name(), escapedCode));
            expectedLines.add(String.format("<div class=\"line %s\" data-line-num=\"%d\" data-status=\"%s\">%s</div>",
                    expectedStatus.name(), lineNum, expectedStatus.name(), escapedCode));
            tokenLines.add(String.format("<div class=\"line\" data-line-num=\"%d\">%s</div>",
                    lineNum, generateTokenLine(rootToken)));
        }

        String html = pageTemplate
                .replace("{title}", getPageTitle(className, testName))
                .replace("{code_original}", originalLines.toString())
                .replace("{code_transformed}", transformedLines.toString())
                .replace("{code_expected}", expectedLines.toString())
                .replace("{tokens}", tokenLines.toString());

        write(html, "html");
    }

    private void write(String content, String type) throws IOException {
        Path outputDir = getOutputDir().resolve(type);
        Files.createDirectories(outputDir);

        Path outputPath = outputDir.resolve(String.format("%s_%s.%s", className, testName, type));
        try (PrintWriter writer = new PrintWriter(outputPath.toString())) {
            writer.write(content);
        }
    }

    private Path getOutputDir() {
        Path dir = Paths.get(HTML_OUTPUT_DIR);

        if (!SPLIT_DIR_BY_JVM) {
            return dir;
        }

        String dirName = System.getProperty("coverageOutputName");
        if (dirName == null) {
            dirName = String.format("%s %s %s",
                    System.getProperty("java.vm.version"),
                    System.getProperty("java.vm.vendor"),
                    System.getProperty("java.vm.name"));
        }
        dirName = dirName.replaceAll("\\W+", "_");
        return dir.resolve(dirName);
    }

    private String getPageTemplate() {
        return ResourceUtils.loadResource(CoverageTest.RESOURCE_DIR, "page_template.html");
    }

    private String getPageTitle(String className, String testName) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return String.format("%s - %s - %s", timeFormatter.format(now), className, testName);
    }

    private String generateTokenLine(CoverageTokens.Token token) {
        StringBuilder line = new StringBuilder();

        while (true) {
            String text = token.originNode != null
                    ? String.format("%s[%s]", token.type.name(), token.originNode.getClass().getSimpleName())
                    : token.type.name();
            String coverageStatus = token.status != null
                    ? token.status.name()
                    : "";
            line.append(String.format("<span class=\"token %s\">%s</span>",
                            coverageStatus, text));

            if (token.children.size() > 1) {
                line.append(" ⮆ ");
            } else if (token.children.size() == 1) {
                line.append(" → ");
            }

            if (token.children.isEmpty()) {
                break;
            }
            token = token.children.get(0);
        }

        return line.toString();
    }
}
