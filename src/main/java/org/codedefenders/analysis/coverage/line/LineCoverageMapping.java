package org.codedefenders.analysis.coverage.line;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codedefenders.game.LineCoverage;

public class LineCoverageMapping {
    private final Map<Integer, LineCoverageStatus> statusPerLine;

    public LineCoverageMapping() {
        statusPerLine = new HashMap<>();
    }

    public LineCoverageStatus get(int line) {
        return statusPerLine.getOrDefault(line, LineCoverageStatus.EMPTY);
    }

    public void put(int line, LineCoverageStatus status) {
        statusPerLine.put(line, status);
    }

    public Map<Integer, LineCoverageStatus> getMap() {
        return Collections.unmodifiableMap(statusPerLine);
    }

    public LineCoverage toLineCoverage() {
        int startLine = statusPerLine.keySet().stream()
                .mapToInt(Integer::intValue)
                .min()
                .orElse(0);
        int endLine = statusPerLine.keySet().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);
        List<Integer> coveredLines = new ArrayList<>();
        List<Integer> uncoveredLines = new ArrayList<>();
        for (int line = startLine; line <= endLine; line++) {
            switch (get(line)) {
                case PARTLY_COVERED:
                case FULLY_COVERED:
                    coveredLines.add(line);
                    break;
                case NOT_COVERED:
                    uncoveredLines.add(line);
                    break;
                case EMPTY:
                    break;
            }
        }
        return new LineCoverage(coveredLines, uncoveredLines);
    }
}
