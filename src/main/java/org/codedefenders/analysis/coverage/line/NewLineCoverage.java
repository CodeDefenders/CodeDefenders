package org.codedefenders.analysis.coverage.line;

public interface NewLineCoverage {
    int getFirstLine();

    int getLastLine();

    LineCoverageStatus getStatus(int line);
}
