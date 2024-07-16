/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
package org.codedefenders.model;

import org.codedefenders.game.puzzle.Puzzle;

/**
 * This class contains meta information on a puzzle.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
public class PuzzleInfo {
    /**
     * Returns a {@link PuzzleInfo} instance for a given {@link Puzzle}.
     *
     * @param p the given {@link Puzzle}.
     * @return puzzle meta information.
     */
    public static PuzzleInfo of(Puzzle p) {
        return new PuzzleInfo(p.getPuzzleId(), p.getChapterId(), p.getPosition(), p.getTitle(), p.getDescription(),
            p.getMaxAssertionsPerTest(), p.getEditableLinesStart(), p.getEditableLinesEnd());
    }

    private int puzzleId;
    private Integer chapterId;
    private Integer position;
    private String title;
    private String description;
    private int maxAssertionsPerTest;
    private Integer editableLinesStart;
    private Integer editableLinesEnd;

    public PuzzleInfo(int puzzleId, Integer chapterId, Integer position, String title,
                       String description, int maxAssertionsPerTest,
                       Integer editableLinesStart, Integer editableLinesEnd) {
        this.puzzleId = puzzleId;
        this.chapterId = chapterId;
        this.position = position;
        this.title = title;
        this.description = description;
        this.maxAssertionsPerTest = maxAssertionsPerTest;
        this.editableLinesStart = editableLinesStart;
        this.editableLinesEnd = editableLinesEnd;
    }

    public int getPuzzleId() {
        return puzzleId;
    }

    public Integer getChapterId() {
        return chapterId;
    }

    public Integer getPosition() {
        return position;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getMaxAssertionsPerTest() {
        return maxAssertionsPerTest;
    }

    public Integer getEditableLinesStart() {
        return editableLinesStart;
    }

    public Integer getEditableLinesEnd() {
        return editableLinesEnd;
    }
}
