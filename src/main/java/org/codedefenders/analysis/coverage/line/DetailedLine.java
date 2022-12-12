package org.codedefenders.analysis.coverage.line;

import org.jacoco.core.analysis.ILine;

public class DetailedLine {
    private final int totalInstructions;
    private final int missedInstructions;
    private final int totalBranches;
    private final int missedBranches;

    public DetailedLine(int totalInstructions, int missedInstructions, int totalBranches, int missedBranches) {
        this.totalInstructions = totalInstructions;
        this.missedInstructions = missedInstructions;
        this.totalBranches = totalBranches;
        this.missedBranches = missedBranches;
    }

    public int getTotalInstructions() {
        return totalInstructions;
    }

    public int getMissedInstructions() {
        return missedInstructions;
    }

    public int getTotalBranches() {
        return totalBranches;
    }

    public int getMissedBranches() {
        return missedBranches;
    }

    public LineCoverageStatus getStatus() {
        int totalCount = totalInstructions + totalBranches;
        int missedCount = missedInstructions + missedBranches;
        if (totalCount == 0) {
            return LineCoverageStatus.EMPTY;
        } else if (missedCount == 0) {
            return LineCoverageStatus.FULLY_COVERED;
        } else if (missedCount == totalCount) {
            return LineCoverageStatus.NOT_COVERED;
        } else {
            return LineCoverageStatus.PARTLY_COVERED;
        }
    }

    public static DetailedLine empty() {
        return new DetailedLine(0, 0, 0, 0);
    }

    public static DetailedLine fromJaCoCo(ILine line) {
        return new DetailedLine(
                line.getInstructionCounter().getTotalCount(),
                line.getInstructionCounter().getMissedCount(),
                line.getBranchCounter().getMissedCount(),
                line.getBranchCounter().getMissedCount()
        );
    }
}
