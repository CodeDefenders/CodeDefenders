package org.codedefenders.analysis.coverage.ast;


import org.codedefenders.analysis.coverage.line.LineCoverageStatus;

public enum AstCoverageStatus {
    EMPTY(false, false, 0),
    NOT_COVERED(false, false, 1),
    BEGIN_COVERED(true, false, 2),
    END_COVERED(true, true, 3),
    INITIALIZED(true, true, 3);

    final private boolean covered;
    final private boolean endCovered;
    final private int upgradePriority;

    AstCoverageStatus(boolean covered, boolean endCovered, int upgradePriority) {
        this.covered = covered;
        this.endCovered = endCovered;
        this.upgradePriority = upgradePriority;
    }

    public boolean isEmpty() {
        return this == AstCoverageStatus.EMPTY;
    }

    public boolean isNotCovered() {
        return this == AstCoverageStatus.NOT_COVERED;
    }

    public boolean isCovered() {
        return covered;
    }

    public boolean isEndCovered() {
        return endCovered;
    }

    public AstCoverageStatus toBlockCoverage() {
        if (this == INITIALIZED) {
            return END_COVERED;
        }
        return this;
    }

    public AstCoverageStatus upgrade(AstCoverageStatus other) {
        if (this.upgradePriority > other.upgradePriority) {
            return this.toBlockCoverage();
        } else {
            return other.toBlockCoverage();
        }
    }

    public LineCoverageStatus toLineCoverage() {
        switch (this) {
            case EMPTY:
                return LineCoverageStatus.EMPTY;
            case NOT_COVERED:
                return LineCoverageStatus.NOT_COVERED;
            case BEGIN_COVERED:
            case END_COVERED:
            case INITIALIZED:
                return LineCoverageStatus.FULLY_COVERED;
            default:
                throw new IllegalStateException("Unknown AST coverage status.");
        }
    }
}
