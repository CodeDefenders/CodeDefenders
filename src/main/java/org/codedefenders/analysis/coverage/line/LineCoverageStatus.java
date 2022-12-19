package org.codedefenders.analysis.coverage.line;

import org.jacoco.core.analysis.ICounter;

public enum LineCoverageStatus {
    // order matters for AstCoverageStatus
    EMPTY,
    NOT_COVERED,
    PARTLY_COVERED,
    FULLY_COVERED;

    public static LineCoverageStatus fromJacocoStatus(int status) {
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
}
