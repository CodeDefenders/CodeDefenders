package org.codedefenders.analysis.coverage.line;

import java.util.ArrayList;
import java.util.List;

import org.codedefenders.game.LineCoverage;

public class SimpleLineCoverage extends LineMapping<LineCoverageStatus> implements NewLineCoverage {
    @Override
    public LineCoverageStatus getEmpty() {
        return LineCoverageStatus.EMPTY;
    }

    @Override
    public LineCoverageStatus get(int line) {
        return super.get(line);
    }

    @Override
    public void set(int line, LineCoverageStatus elem) {
        super.set(line, elem);
    }

    @Override
    public LineCoverageStatus getStatus(int line) {
        return get(line);
    }

    // TODO: either make this an interface default method on NewLineCoverage, or (better) replace LineCoverage with
    //       NewLineCoverage and extend the interface
    public LineCoverage toLineCoverage() {
        List<Integer> coveredLines = new ArrayList<>();
        List<Integer> uncoveredLines = new ArrayList<>();
        for (int line = getFirstLine(); line <= getLastLine(); line++) {
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
