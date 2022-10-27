package org.codedefenders.analysis.coverage.ast;


public enum AstCoverageStatus {
    EMPTY(false, false, 0),
    NOT_COVERED(false, false, 1),
    BEGIN_COVERED(true, false, 2),
    END_COVERED(true, true, 3),
    COVERED(true, true, 3),
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
        if (this == COVERED || this == INITIALIZED) {
            return END_COVERED;
        }
        return this;
    }

    public AstCoverageStatus toStmtCoverage() {
        if (this == END_COVERED || this == INITIALIZED) {
            return COVERED;
        }
        return this;
    }

    /**
     * Returns this coverage value to another and returns the higher value.
     * This should mainly be used in conjunction with {@link AstCoverageStatus#toStmtCoverage()} or
     * {@link AstCoverageStatus#toBlockCoverage()}, since {@link AstCoverageStatus#COVERED},
     * {@link AstCoverageStatus#END_COVERED} and {@link AstCoverageStatus#INITIALIZED} all have the same priority.
     */
    public AstCoverageStatus upgrade(AstCoverageStatus other) {
        if (this.upgradePriority > other.upgradePriority) {
            return this;
        } else {
            return other;
        }
    }
}
