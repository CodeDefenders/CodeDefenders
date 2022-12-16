package org.codedefenders.analysis.coverage.ast;

import org.codedefenders.analysis.coverage.line.LineCoverageStatus;

public enum AstCoverageStatus {
    EMPTY(false, false, 0),
    BEGIN_NOT_COVERED(false, true, 1),
    // TODO: use this in AstCoverageVisitor correctly
    END_NOT_COVERED(false, false, 2),
    BEGIN_COVERED(true, true, 3),
    INITIALIZED(true, false, 4),
    END_COVERED(true, false, 5);

    final private boolean isCovered;
    final private boolean isBreak;
    final private int upgradePriority;

    AstCoverageStatus(boolean isCovered, boolean isBreak, int upgradePriority) {
        this.isCovered = isCovered;
        this.isBreak = isBreak;
        this.upgradePriority = upgradePriority;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public boolean isCovered() {
        return isCovered;
    }

    public boolean isEndCovered() {
        return isCovered && !isBreak;
    }

    public boolean isNotCovered() {
        return !isCovered && this != EMPTY;
    }

    public boolean isEndNotCovered() {
        return !isCovered && this != EMPTY && !isBreak;
    }

    public boolean isBreak() {
        return isBreak;
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
            case BEGIN_NOT_COVERED:
            case END_NOT_COVERED:
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
