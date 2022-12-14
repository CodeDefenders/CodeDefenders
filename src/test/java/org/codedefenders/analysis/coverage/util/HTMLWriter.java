package org.codedefenders.analysis.coverage.util;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

import org.apache.commons.text.StringEscapeUtils;
import org.codedefenders.analysis.coverage.line.DetailedLineCoverage;
import org.codedefenders.analysis.coverage.line.LineCoverageStatus;
import org.codedefenders.analysis.coverage.line.NewLineCoverage;

import static org.codedefenders.util.ResourceUtils.loadResource;

public class HTMLWriter {
    private final static String RESOURCE_DIR = "analysis/coverage";
    private final static String HTML_OUTPUT_DIR = "/tmp/coverage";
    private final static boolean OUTPUT_HTML = false;

    private final String pageTemplate;
    private final String lineTemplate;

    public HTMLWriter() {
        pageTemplate = loadResource(RESOURCE_DIR, "page_template.html");
        lineTemplate = loadResource(RESOURCE_DIR, "line_template.html");
    }

    public void write(String className,
                      String testName,
                      String sourceCode,
                      DetailedLineCoverage originalCoverage,
                      NewLineCoverage transformedCoverage,
                      NewLineCoverage expectedCoverage) throws Exception {
        if (!OUTPUT_HTML) {
            return;
        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String fileName = String.format("%s_%s.html", className, testName);
        String title =  String.format("%s - %s - %s", timeFormatter.format(now), className, testName);

        StringJoiner originalLines = new StringJoiner("\n");
        StringJoiner transformedLines = new StringJoiner("\n");
        StringJoiner expectedLines = new StringJoiner("\n");

        String[] lines = sourceCode.split("\r?\n");
        for (int lineNum = 1; lineNum <= lines.length; lineNum++) {
            String line = lines[lineNum - 1];
            String escapedLine = StringEscapeUtils.escapeHtml4(line)
                    .replaceAll(" ", "&nbsp");

            String htmlLine = lineTemplate
                    .replace("{line_num}", Integer.toString(lineNum))
                    .replace("{code}", escapedLine);

            LineCoverageStatus originalStatus = originalCoverage.getStatus(lineNum);
            LineCoverageStatus transformedStatus = transformedCoverage.getStatus(lineNum);
            LineCoverageStatus expectedStatus = expectedCoverage.getStatus(lineNum);

            originalLines.add(htmlLine.replace("{coverage_status}", originalStatus.name()));
            transformedLines.add(htmlLine.replace("{coverage_status}", transformedStatus.name()));
            expectedLines.add(htmlLine.replace("{coverage_status}", expectedStatus.name()));
        }

        String html = pageTemplate
                .replace("{title}", title)
                .replace("{code_original}", originalLines.toString())
                .replace("{code_transformed}", transformedLines.toString())
                .replace("{code_expected}", expectedLines.toString());


        Files.createDirectories(Paths.get(HTML_OUTPUT_DIR));
        try (PrintWriter writer = new PrintWriter(HTML_OUTPUT_DIR + "/" + fileName)) {
            writer.write(html);
        }
    }
}
