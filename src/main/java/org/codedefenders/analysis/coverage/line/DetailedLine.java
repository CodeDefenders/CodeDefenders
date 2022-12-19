package org.codedefenders.analysis.coverage.line;

import org.jacoco.core.analysis.ILine;

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
