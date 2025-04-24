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
package org.codedefenders.game;

import java.util.LinkedList;
import java.util.List;

/**
 * This class contains information about how many lines of a {@link GameClass} was
 * covered not or covered by a {@link Test}. Covered and uncovered lines are stored
 * in {@link List Lists}. Both lists can't be {@code null}.
 *
 * <p>Note that no {@link GameClass} or {@link Test} reference is stored.
 */
public class LineCoverage {
    private List<Integer> linesCovered;
    private List<Integer> linesUncovered;

    /**
     * Creating a empty line coverage in which zero covered and uncovered lines are stored.
     */
    private LineCoverage() {
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

    public static LineCoverage empty() {
        return new LineCoverage();
    }

    public List<Integer> getLinesCovered() {
        return linesCovered;
    }

    public List<Integer> getLinesUncovered() {
        return linesUncovered;
    }
}
