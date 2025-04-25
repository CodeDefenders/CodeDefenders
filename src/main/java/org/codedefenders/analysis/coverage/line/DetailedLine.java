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
package org.codedefenders.analysis.coverage.line;

import org.jacoco.core.analysis.ILine;

/**
 * Saves the number of total/covered instructions and branches for a given line,
 * and translates to {@link LineCoverageStatus}.
 */
public class DetailedLine {
    private final int totalInstructions;
    private final int coveredInstructions;
    private final int totalBranches;
    private final int coveredBranches;

    public DetailedLine(int totalInstructions, int coveredInstructions, int totalBranches, int coveredBranches) {
        this.totalInstructions = totalInstructions;
        this.coveredInstructions = coveredInstructions;
        this.totalBranches = totalBranches;
        this.coveredBranches = coveredBranches;
    }

    public int totalInstructions() {
        return totalInstructions;
    }

    public int coveredInstructions() {
        return coveredInstructions;
    }

    public int missedInstructions() {
        return totalInstructions - coveredInstructions;
    }

    public int totalBranches() {
        return totalBranches;
    }

    public int coveredBranches() {
        return coveredBranches;
    }

    public int missedBranches() {
        return totalBranches - coveredBranches;
    }

    public boolean hasIns() {
        return totalInstructions > 0;
    }

    public boolean hasCoveredIns() {
        return coveredInstructions > 0;
    }

    public boolean hasMissedIns() {
        return missedInstructions() > 0;
    }

    public boolean hasBranches() {
        return totalBranches > 0;
    }

    public boolean hasCoveredBranches() {
        return coveredBranches > 0;
    }

    public boolean hasMissedBranches() {
        return missedBranches() > 0;
    }

    public LineCoverageStatus instructionStatus() {
        return computeStatus(totalInstructions, coveredInstructions);
    }

    public LineCoverageStatus branchStatus() {
        return computeStatus(totalBranches, coveredBranches);
    }

    public LineCoverageStatus combinedStatus() {
        return computeStatus(totalInstructions + totalBranches, coveredInstructions + coveredBranches);
    }

    private LineCoverageStatus computeStatus(int total, int covered) {
        if (total == 0) {
            return LineCoverageStatus.EMPTY;
        } else if (covered == 0) {
            return LineCoverageStatus.NOT_COVERED;
        } else if (covered == total) {
            return LineCoverageStatus.FULLY_COVERED;
        } else {
            return LineCoverageStatus.PARTLY_COVERED;
        }
    }

    public DetailedLine merge(DetailedLine other) {
        return new DetailedLine(
                totalInstructions + other.totalInstructions,
                coveredInstructions + other.coveredInstructions,
                totalBranches + other.totalBranches,
                coveredBranches + other.coveredBranches
        );
    }

    public static DetailedLine empty() {
        return new DetailedLine(0, 0, 0, 0);
    }

    public static DetailedLine fromJaCoCo(ILine line) {
        return new DetailedLine(
                line.getInstructionCounter().getTotalCount(),
                line.getInstructionCounter().getCoveredCount(),
                line.getBranchCounter().getTotalCount(),
                line.getBranchCounter().getCoveredCount()
        );
    }
}
