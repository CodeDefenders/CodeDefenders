package org.codedefenders.analysis.coverage.ast;

import org.codedefenders.analysis.coverage.line.LineCoverageStatus;

// TODO: replace reachesEnd with throwsException and add a getter reachesEnd() that returns !throwsException && !doesJump
public class AstCoverageStatus {
    private final LineCoverageStatus status;
    private final StatusAfter statusAfter;

    /**
     * For nodes where the merged status from the node's children is different from the node's own status.
     * E.g. the AST structure of method chains is opposite to the control flow, so each node saves the merged status
     * of the chain in status and the own status in selfStatus.
     */
    private final LineCoverageStatus selfStatus;

    public AstCoverageStatus(LineCoverageStatus status, StatusAfter statusAfter, LineCoverageStatus selfStatus) {
        this.status = status;
        this.statusAfter = statusAfter;
        this.selfStatus = selfStatus;
    }

    public AstCoverageStatus(LineCoverageStatus status, StatusAfter statusAfter) {
        this(status, statusAfter, status);
    }

    public static AstCoverageStatus fromStatus(LineCoverageStatus status) {
        switch (status) {
            case EMPTY:
                return AstCoverageStatus.empty();
            case NOT_COVERED:
                return AstCoverageStatus.notCovered();
            case PARTLY_COVERED:
            case FULLY_COVERED:
                return AstCoverageStatus.covered();
            default:
                throw new IllegalArgumentException("Unknown line coverage status: " + status);
        }
    }

    public static AstCoverageStatus empty() {
        return new AstCoverageStatus(LineCoverageStatus.EMPTY, StatusAfter.MAYBE_COVERED);
    }

    public static AstCoverageStatus notCovered() {
        return new AstCoverageStatus(LineCoverageStatus.NOT_COVERED, StatusAfter.NOT_COVERED);
    }

    public static AstCoverageStatus covered() {
        return new AstCoverageStatus(LineCoverageStatus.FULLY_COVERED, StatusAfter.COVERED);
    }

    public AstCoverageStatus withStatus(LineCoverageStatus status) {
        if (status == LineCoverageStatus.PARTLY_COVERED) {
            status = LineCoverageStatus.FULLY_COVERED;
        }

        return new AstCoverageStatus(status, this.statusAfter, this.selfStatus);
    }

    public AstCoverageStatus withStatusAfter(StatusAfter statusAfter) {
        return new AstCoverageStatus(this.status, statusAfter, this.selfStatus);
    }

    public AstCoverageStatus withSelfStatus(LineCoverageStatus selfStatus) {
        if (selfStatus == LineCoverageStatus.PARTLY_COVERED) {
            selfStatus = LineCoverageStatus.FULLY_COVERED;
        }
        return new AstCoverageStatus(this.status, this.statusAfter, selfStatus);
    }

    public AstCoverageStatus clearSelfStatus() {
        return new AstCoverageStatus(this.status, this.statusAfter);
    }

    public LineCoverageStatus status() {
        return status;
    }

    public LineCoverageStatus selfStatus() {
        return selfStatus;
    }

    public StatusAfter statusAfter() {
        return statusAfter;
    }

    public boolean isEmpty() {
        return status == LineCoverageStatus.EMPTY;
    }

    public boolean isNotCovered() {
        return status == LineCoverageStatus.NOT_COVERED;
    }

    public boolean isCovered() {
        return status == LineCoverageStatus.FULLY_COVERED;
    }

    public AstCoverageStatus updateStatus(LineCoverageStatus newStatus) {
        switch (newStatus) {
            case EMPTY:
                return this;
            case NOT_COVERED:
                if (status == LineCoverageStatus.EMPTY) {
                    if (this.statusAfter.alwaysJumps()) {
                        return new AstCoverageStatus(LineCoverageStatus.NOT_COVERED, StatusAfter.ALWAYS_JUMPS);
                    } else {
                        return new AstCoverageStatus(LineCoverageStatus.NOT_COVERED, StatusAfter.NOT_COVERED);
                    }
                }
                break;
            case PARTLY_COVERED:
            case FULLY_COVERED:
                if (status == LineCoverageStatus.EMPTY) {
                    if (this.statusAfter.alwaysJumps()) {
                        return new AstCoverageStatus(LineCoverageStatus.FULLY_COVERED, StatusAfter.ALWAYS_JUMPS);
                    } else {
                        return new AstCoverageStatus(LineCoverageStatus.FULLY_COVERED, StatusAfter.COVERED);
                    }
                } else if (status == LineCoverageStatus.NOT_COVERED) {
                    if (this.statusAfter.alwaysJumps()) {
                        return new AstCoverageStatus(LineCoverageStatus.FULLY_COVERED, StatusAfter.ALWAYS_JUMPS);
                    } else {
                        return new AstCoverageStatus(LineCoverageStatus.FULLY_COVERED, StatusAfter.NOT_COVERED);
                    }
                }
                break;
        }
        return this;
    }

    public AstCoverageStatus updateStatusAfter(StatusAfter newStatusAfter) {
        if (newStatusAfter.isUnsure()) {
            return this;
        }

        if (status == LineCoverageStatus.EMPTY) {
            if (newStatusAfter.isCovered()) {
                return new AstCoverageStatus(LineCoverageStatus.FULLY_COVERED, newStatusAfter);
            } else if (newStatusAfter.isNotCovered()) {
                return new AstCoverageStatus(LineCoverageStatus.NOT_COVERED, newStatusAfter);
            }
        }

        return withStatusAfter(statusAfter);
    }

    public enum StatusAfter {
        ALWAYS_JUMPS,
        NOT_COVERED,
        MAYBE_COVERED,
        COVERED;

        public StatusAfter upgrade(StatusAfter other) {
            return this.ordinal() > other.ordinal() ? this : other;
        }

        public StatusAfter downgrade(StatusAfter other) {
            return this.ordinal() < other.ordinal() ? this : other;
        }

        public boolean alwaysJumps() {
            return this == ALWAYS_JUMPS;
        }

        public boolean isNotCovered() {
            return this == NOT_COVERED;
        }

        public boolean isCovered() {
            return this == COVERED;
        }

        public boolean isUnsure() {
            return this == MAYBE_COVERED;
        }

        public static StatusAfter fromLineCoverageStatus(LineCoverageStatus status) {
            switch (status) {
                case EMPTY:
                    return MAYBE_COVERED;
                case NOT_COVERED:
                    return NOT_COVERED;
                case PARTLY_COVERED:
                case FULLY_COVERED:
                    return COVERED;
                default:
                    throw new IllegalArgumentException("Unknown line coverage status: " + status);
            }
        }

        public LineCoverageStatus toLineCoverageStatus() {
            switch (this) {
                case ALWAYS_JUMPS:
                case MAYBE_COVERED:
                    return LineCoverageStatus.EMPTY;
                case NOT_COVERED:
                    return LineCoverageStatus.NOT_COVERED;
                case COVERED:
                    return LineCoverageStatus.FULLY_COVERED;
                default:
                    throw new IllegalArgumentException("Unknown StatusAfter value: " + this);
            }
        }
    }
}
