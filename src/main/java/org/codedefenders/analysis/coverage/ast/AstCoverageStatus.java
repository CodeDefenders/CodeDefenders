/*
 * Copyright (C) 2016-2023 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.analysis.coverage.ast;

import org.codedefenders.analysis.coverage.line.LineCoverageStatus;

public class AstCoverageStatus {

    private final Status status;
    private final StatusAfter statusAfter;

    /**
     * For nodes where the merged status from the node's children is different from the node's own status.
     * E.g. the AST structure of method chains is opposite to the control flow, so each node saves the merged status
     * of the chain in status and the own status in selfStatus.
     */
    private final Status selfStatus;

    public AstCoverageStatus(Status status, StatusAfter statusAfter, Status selfStatus) {
        this.status = status;
        this.statusAfter = statusAfter;
        this.selfStatus = selfStatus;
    }

    public AstCoverageStatus(Status status, StatusAfter statusAfter) {
        this(status, statusAfter, status);
    }

    public static AstCoverageStatus fromStatus(Status status) {
        switch (status) {
            case EMPTY:
                return AstCoverageStatus.empty();
            case NOT_COVERED:
                return AstCoverageStatus.notCovered();
            case COVERED:
                return AstCoverageStatus.covered();
            default:
                throw new IllegalArgumentException("Unknown line coverage status: " + status);
        }
    }

    public static AstCoverageStatus fromStatus(LineCoverageStatus status) {
        return fromStatus(Status.fromLineCoverageStatus(status));
    }

    public static AstCoverageStatus empty() {
        return new AstCoverageStatus(Status.EMPTY, StatusAfter.MAYBE_COVERED);
    }

    public static AstCoverageStatus notCovered() {
        return new AstCoverageStatus(Status.NOT_COVERED, StatusAfter.NOT_COVERED);
    }

    public static AstCoverageStatus covered() {
        return new AstCoverageStatus(Status.COVERED, StatusAfter.COVERED);
    }

    public AstCoverageStatus withStatus(Status status) {
        return new AstCoverageStatus(status, this.statusAfter, this.selfStatus);
    }

    public AstCoverageStatus withStatus(LineCoverageStatus status) {
        return withStatus(Status.fromLineCoverageStatus(status));
    }

    public AstCoverageStatus withStatusAfter(StatusAfter statusAfter) {
        return new AstCoverageStatus(this.status, statusAfter, this.selfStatus);
    }

    public AstCoverageStatus withSelfStatus(Status selfStatus) {
        return new AstCoverageStatus(this.status, this.statusAfter, selfStatus);
    }

    public AstCoverageStatus withSelfStatus(LineCoverageStatus selfStatus) {
        return withSelfStatus(Status.fromLineCoverageStatus(selfStatus));
    }

    public AstCoverageStatus clearSelfStatus() {
        return new AstCoverageStatus(this.status, this.statusAfter);
    }

    public Status status() {
        return status;
    }

    public Status selfStatus() {
        return selfStatus;
    }

    public StatusAfter statusAfter() {
        return statusAfter;
    }

    public boolean isEmpty() {
        return status == Status.EMPTY;
    }

    public boolean isNotCovered() {
        return status == Status.NOT_COVERED;
    }

    public boolean isCovered() {
        return status == Status.COVERED;
    }

    public AstCoverageStatus updateStatus(Status newStatus) {
        switch (newStatus) {
            case EMPTY:
                return this;
            case NOT_COVERED:
                if (status.isEmpty()) {
                    if (this.statusAfter.alwaysJumps()) {
                        return new AstCoverageStatus(Status.NOT_COVERED, StatusAfter.ALWAYS_JUMPS);
                    } else {
                        return new AstCoverageStatus(Status.NOT_COVERED, StatusAfter.NOT_COVERED);
                    }
                }
                break;
            case COVERED:
                if (status.isEmpty()) {
                    if (this.statusAfter.alwaysJumps()) {
                        return new AstCoverageStatus(Status.COVERED, StatusAfter.ALWAYS_JUMPS);
                    } else {
                        return new AstCoverageStatus(Status.COVERED, StatusAfter.COVERED);
                    }
                } else if (status.isNotCovered()) {
                    if (this.statusAfter.alwaysJumps()) {
                        return new AstCoverageStatus(Status.COVERED, StatusAfter.ALWAYS_JUMPS);
                    } else {
                        return new AstCoverageStatus(Status.COVERED, StatusAfter.NOT_COVERED);
                    }
                }
                break;
        }
        return this;
    }

    public AstCoverageStatus updateStatus(LineCoverageStatus newStatus) {
        return updateStatus(Status.fromLineCoverageStatus(newStatus));
    }

    public AstCoverageStatus updateStatusAfter(StatusAfter newStatusAfter) {
        if (newStatusAfter.isUnsure()) {
            return this;
        }

        if (status.isEmpty()) {
            if (newStatusAfter.isCovered()) {
                return new AstCoverageStatus(Status.COVERED, newStatusAfter);
            } else if (newStatusAfter.isNotCovered()) {
                return new AstCoverageStatus(Status.NOT_COVERED, newStatusAfter);
            }
        }

        return withStatusAfter(statusAfter);
    }

    public enum Status {
        EMPTY,
        NOT_COVERED,
        COVERED;

        public boolean isEmpty() {
            return this == EMPTY;
        }

        public boolean isNotCovered() {
            return this == NOT_COVERED;
        }

        public boolean isCovered() {
            return this == COVERED;
        }

        public Status upgrade(Status other) {
            return this.ordinal() >= other.ordinal() ? this : other;
        }

        public Status upgrade(LineCoverageStatus other) {
            return upgrade(fromLineCoverageStatus(other));
        }

        public static Status fromLineCoverageStatus(LineCoverageStatus status) {
            switch (status) {
                case EMPTY:
                    return EMPTY;
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
                case EMPTY:
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

        public static StatusAfter fromAstCoverageStatus(Status status) {
            switch (status) {
                case EMPTY:
                    return MAYBE_COVERED;
                case NOT_COVERED:
                    return NOT_COVERED;
                case COVERED:
                    return COVERED;
                default:
                    throw new IllegalArgumentException("Unknown line coverage status: " + status);
            }
        }

        public static StatusAfter fromLineCoverageStatus(LineCoverageStatus status) {
            return fromAstCoverageStatus(Status.fromLineCoverageStatus(status));
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

        public Status toAstCoverageStatus() {
            switch (this) {
                case ALWAYS_JUMPS:
                case MAYBE_COVERED:
                    return Status.EMPTY;
                case NOT_COVERED:
                    return Status.NOT_COVERED;
                case COVERED:
                    return Status.COVERED;
                default:
                    throw new IllegalArgumentException("Unknown StatusAfter value: " + this);
            }
        }
    }
}
