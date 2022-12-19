package org.codedefenders.analysis.coverage.ast;

import org.codedefenders.analysis.coverage.line.LineCoverageStatus;

public class AstCoverageStatus {
    // TODO: create a new enum for this?
    private final LineCoverageStatus status;
    private final boolean doesJump;

    private AstCoverageStatus(LineCoverageStatus status, boolean doesJump) {
        this.status = status;
        this.doesJump = doesJump;
    }

    static AstCoverageStatus empty() {
        return new AstCoverageStatus(LineCoverageStatus.EMPTY, false);
    }

    static AstCoverageStatus notCovered() {
        return new AstCoverageStatus(LineCoverageStatus.NOT_COVERED, false);
    }

    static AstCoverageStatus covered() {
        return new AstCoverageStatus(LineCoverageStatus.FULLY_COVERED, false);
    }

    AstCoverageStatus withJump(boolean doesJump) {
        return new AstCoverageStatus(status, doesJump);
    }

    AstCoverageStatus withJump() {
        return new AstCoverageStatus(status, true);
    }

    public LineCoverageStatus status() {
        return status;
    }

    // TODO: explain this in JavaDoc
    public boolean reachesEnd() {
        return !doesJump;
    }

    public boolean doesJump() {
        return doesJump;
    }

    public boolean isEmpty() {
        return status == LineCoverageStatus.EMPTY;
    }

    public boolean isCovered() {
        return status == LineCoverageStatus.FULLY_COVERED;
    }

    public boolean isNotCovered() {
        return status == LineCoverageStatus.NOT_COVERED;
    }

    public AstCoverageStatus upgrade(AstCoverageStatus other) {
        if (this.status.ordinal() > other.status.ordinal()) {
            return new AstCoverageStatus(this.status, this.doesJump && other.doesJump);
        } else {
            return new AstCoverageStatus(other.status, this.doesJump && other.doesJump);
        }
    }
}
