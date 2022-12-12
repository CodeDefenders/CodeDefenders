package org.codedefenders.analysis.coverage.line;

public class DetailedLineCoverage extends LineMapping<DetailedLine> implements NewLineCoverage {
    @Override
    public DetailedLine getEmpty() {
        return DetailedLine.empty();
    }

    @Override
    public LineCoverageStatus getStatus(int line) {
        return get(line).getStatus();
    }
}
