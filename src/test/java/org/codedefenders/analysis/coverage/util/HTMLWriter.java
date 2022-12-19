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
import org.codedefenders.analysis.coverage.line.LineTokens;
import org.codedefenders.analysis.coverage.line.NewLineCoverage;

import static org.codedefenders.util.ResourceUtils.loadResource;

public class HTMLWriter {
    private final static String RESOURCE_DIR = "analysis/coverage";
    private final static String HTML_OUTPUT_DIR = "/tmp/coverage";
    private final static boolean OUTPUT_HTML = true;

    private final String pageTemplate;

    public HTMLWriter() {
        pageTemplate = loadResource(RESOURCE_DIR, "page_template.html");
    }

    public void write(String className,
                      String testName,
                      String sourceCode,
                      DetailedLineCoverage originalCoverage,
                      NewLineCoverage transformedCoverage,
                      NewLineCoverage expectedCoverage,
                      LineTokens lineTokens) throws Exception {
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
        StringJoiner tokenLines = new StringJoiner("\n");

        String[] lines = sourceCode.split("\r?\n");
        for (int lineNum = 1; lineNum <= lines.length; lineNum++) {
            String code = lines[lineNum - 1];
            String escapedCode = StringEscapeUtils.escapeHtml4(code);

            LineCoverageStatus originalStatus = originalCoverage.getStatus(lineNum);
            LineCoverageStatus transformedStatus = transformedCoverage.getStatus(lineNum);
            LineCoverageStatus expectedStatus = expectedCoverage.getStatus(lineNum);
            LineTokens.Token rootToken = lineTokens.getRoot(lineNum);

            originalLines.add(String.format("<div class=\"line %s\" data-line-num=\"%d\">%s</div>",
                    originalStatus.name(), lineNum, escapedCode));
            transformedLines.add(String.format("<div class=\"line %s\" data-line-num=\"%d\">%s</div>",
                    transformedStatus.name(), lineNum, escapedCode));
            expectedLines.add(String.format("<div class=\"line %s\" data-line-num=\"%d\">%s</div>",
                    expectedStatus.name(), lineNum, escapedCode));
            tokenLines.add(String.format("<div class=\"line\" data-line-num=\"%d\">%s</div>",
                    lineNum, generateTokenLine(rootToken)));
        }

        String html = pageTemplate
                .replace("{title}", title)
                .replace("{code_original}", originalLines.toString())
                .replace("{code_transformed}", transformedLines.toString())
                .replace("{code_expected}", expectedLines.toString())
                .replace("{tokens}", tokenLines.toString());

        Files.createDirectories(Paths.get(HTML_OUTPUT_DIR));
        try (PrintWriter writer = new PrintWriter(HTML_OUTPUT_DIR + "/" + fileName)) {
            writer.write(html);
        }
    }

    public String generateTokenLine(LineTokens.Token token) {
        StringBuilder line = new StringBuilder();

        while (true) {
            String text = token.originNode != null
                    ? String.format("%s[%s]", token.type.name(), token.originNode.getClass().getSimpleName())
                    : token.type.name();
            String coverageStatus = token.analyserStatus != null
                    ? token.analyserStatus.name()
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
