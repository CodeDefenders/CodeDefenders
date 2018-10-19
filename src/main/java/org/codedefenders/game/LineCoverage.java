package org.codedefenders.game;

import java.util.LinkedList;
import java.util.List;

/**
 * This class contains information about how many lines of a {@link GameClass} was
 * covered not or covered by a {@link Test}. Covered and uncovered lines are stored
 * in {@link List}s. Both lists can't be {@code null}.
 * <p>
 * Note that no {@link GameClass} or {@link Test} reference is stored.
 * <p>
 */
public class LineCoverage {
    private List<Integer> linesCovered;
    private List<Integer> linesUncovered;

    /**
     * Creating a empty line coverage in which zero covered and uncovered lines are stored.
     */
    public LineCoverage() {
        linesCovered = new LinkedList<>();
        linesUncovered = new LinkedList<>();
    }

    /**
     * Creates a line coverage for given covered and uncovered lines.
     *
     * @param linesCovered given covered lines.
     * @param linesUncovered given uncovered lines.
     */
    public LineCoverage(List<Integer> linesCovered, List<Integer> linesUncovered) {
        this.linesCovered = new LinkedList<>(linesCovered);
        this.linesUncovered = new LinkedList<>(linesUncovered);
    }

    public List<Integer> getLinesCovered() {
        return linesCovered;
    }

    public List<Integer> getLinesUncovered() {
        return linesUncovered;
    }
}
