package org.codedefenders.analysis.coverage.line;

import org.codedefenders.analysis.coverage.ast.AstCoverageStatus;
import org.jacoco.core.analysis.ICounter;

public enum LineCoverageStatus {
    EMPTY(false, AstCoverageStatus.EMPTY, 0, 0, 0),
    NOT_COVERED(false, AstCoverageStatus.NOT_COVERED, 1, 1,  2),
    PARTLY_COVERED(true, AstCoverageStatus.COVERED, 2, 3, 3),
    FULLY_COVERED(true, AstCoverageStatus.COVERED, 3 ,2, 1);

    final private boolean covered;
    final private AstCoverageStatus astStmtStatus;
    final int preferFullPriority;
    final int preferPartialPriority;
    final int mergePriority;

    LineCoverageStatus(boolean covered, AstCoverageStatus astStmtStatus, int preferFullPriority, int preferPartialPriority, int mergePriority) {
        this.covered = covered;
        this.astStmtStatus = astStmtStatus;
        this.preferFullPriority = preferFullPriority;
        this.preferPartialPriority = preferPartialPriority;
        this.mergePriority = mergePriority;
    }

    public static LineCoverageStatus fromJacoco(int status) {
        switch (status) {
            case ICounter.EMPTY:
                return EMPTY;
            case ICounter.NOT_COVERED:
                return NOT_COVERED;
            case ICounter.PARTLY_COVERED:
                return PARTLY_COVERED;
            case ICounter.FULLY_COVERED:
                return FULLY_COVERED;
            default:
                throw new IllegalArgumentException("Not a valid JaCoCo coverage status: " + status);
        }
    }

    public boolean isCovered() {
        return covered;
    }

    public LineCoverageStatus preferFull(LineCoverageStatus other) {
        if (this.preferFullPriority > other.preferFullPriority) {
            return this;
        } else {
            return other;
        }
    }

    public LineCoverageStatus preferPartial(LineCoverageStatus other) {
        if (this.preferPartialPriority > other.preferPartialPriority) {
            return this;
        } else {
            return other;
        }
    }

    public LineCoverageStatus merge(LineCoverageStatus other) {
        if (this.mergePriority > other.mergePriority) {
            return this;
        } else {
            return other;
        }
    }

    public AstCoverageStatus toAstCoverage() {
        return astStmtStatus;
    }
}
