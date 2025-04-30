/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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

/**
 * An immutable data class representing the coverage status of an AST node.
 * Saves the status of the node's subtree (status), the status of the node itself (selfStatus) and the status after the
 * node (statusAfter).
 */
public class AstCoverageStatus {

    /**
     * Represents the status of an AST node's subtree.
     * <ol>
     *     <li>if anything in the subtree is COVERED -> COVERED</li>
     *     <li>else if anything in the subtree is NOT_COVERED -> NOT_COVERED</li>
     *     <li>else -> EMPTY</li>
     * </ol>
     */
    private final Status status;

    /**
     * Represents the coverage status after the AST node.
     * <ol>
     *     <li>if the node always jumps -> ALWAYS_JUMPS</li>
     *     <li>else if the status can reliably be determined -> NOT_COVERED or COVERED</li>
     *     <li>else (if the node is EMPTY or the status can't be determined) -> MAYBE_COVERED</li>
     * </ol>
     */
    private final StatusAfter statusAfter;

    /**
     * Represents the status of the AST node itself, instead of the subtree. This is used for nodes where the subtree
     * status is different from the node's own status.
     *
     * <p>E.g. the AST structure of method chains is opposite to the control flow (last call is outermost expression),
     * so each node saves the merged status of the chain in status and the own status in selfStatus.
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

    /**
     * Constructs a new AstCoverageStatus based on the given tree status.
     */
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

    public static AstCoverageStatus fromLineStatus(LineCoverageStatus status) {
        return fromStatus(Status.fromLineCoverageStatus(status));
    }

    /**
     * Constructs a new empty AstCoverageStatus.
     */
    public static AstCoverageStatus empty() {
        return new AstCoverageStatus(Status.EMPTY, StatusAfter.MAYBE_COVERED);
    }

    /**
     * Constructs a new not-covered AstCoverageStatus.
     */
    public static AstCoverageStatus notCovered() {
        return new AstCoverageStatus(Status.NOT_COVERED, StatusAfter.NOT_COVERED);
    }

    /**
     * Constructs a new covered AstCoverageStatus.
     */
    public static AstCoverageStatus covered() {
        return new AstCoverageStatus(Status.COVERED, StatusAfter.COVERED);
    }

    /**
     * Constructs a new AstCoverageStatus with the new status (and same attributes otherwise).
     * Consider using {@link AstCoverageStatus#fromStatus(Status)} or
     * {@link AstCoverageStatus#updateStatus(Status)} instead.
     */
    public AstCoverageStatus withStatus(Status status) {
        return new AstCoverageStatus(status, this.statusAfter, this.selfStatus);
    }

    /**
     * Constructs a new AstCoverageStatus with the new statusAfter (and same attributes otherwise).
     */
    public AstCoverageStatus withStatusAfter(StatusAfter statusAfter) {
        return new AstCoverageStatus(this.status, statusAfter, this.selfStatus);
    }

    /**
     * Constructs a new AstCoverageStatus with the new selfStatus (and same attributes otherwise).
     */
    public AstCoverageStatus withSelfStatus(Status selfStatus) {
        return new AstCoverageStatus(this.status, this.statusAfter, selfStatus);
    }

    /**
     * Constructs a new AstCoverageStatus with the selfStatus = status (and same attributes otherwise).
     */
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

    /**
     * Constructs a new AstCoverageStatus with an updated tree status,
     * and an updated statusAfter as well if it can be updated from the new status.
     *
     * <p>Status and statusAfter are only updated if they make the AstCoverageStatus "more covered",
     * i.e. EMPTY -> NOT_COVERED, or NOT_COVERED -> COVERED.
     */
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

    /**
     * Constructs a new AstCoverageStatus with an updated statusAfter,
     * and updates the tree status accordingly if it was EMPTY before.
     */
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

        return withStatusAfter(newStatusAfter);
    }

    public enum Status {
        // order matters
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
        // order matters

        /**
         * The node always jumps, therefore space directly after the node is not coverable.
         */
        ALWAYS_JUMPS,

        /**
         * The space after the node is NOT_COVERED, e.g. because:
         * <ul>
         *     <li>the node is NOT_COVERED to begin with</li>
         *     <li>the node threw an exception</li>
         *     <li>the node jumped (but doesn't jump on every path)</li>
         * </ul>
         */
        NOT_COVERED,

        /**
         * The space after the node could be COVERED or NOT_COVERED, e.g. because
         * <ul>
         *     <li>the node's coverage is EMPTY</li>
         *     <li>we can't reliably determine if the node threw an exception or jumped</li>
         * </ul>
         */
        MAYBE_COVERED,

        /**
         * The space after the node is COVERED.
         */
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

        public LineCoverageStatus toLineCoverageStatus() {
            return toAstCoverageStatus().toLineCoverageStatus();
        }
    }
}
