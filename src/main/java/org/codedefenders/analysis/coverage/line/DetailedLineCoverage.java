package org.codedefenders.analysis.coverage.line;

public class DetailedLineCoverage extends LineMapping<DetailedLine> implements NewLineCoverage {
    @Override
    public DetailedLine getEmpty() {
        return DetailedLine.empty();
    }

    @Override
    public DetailedLine get(int line) {
        return super.get(line);
    }

    @Override
    public void set(int line, DetailedLine elem) {
        super.set(line, elem);
    }

    @Override
    public LineCoverageStatus getStatus(int line) {
        return get(line).computeStatus();
    }
}
