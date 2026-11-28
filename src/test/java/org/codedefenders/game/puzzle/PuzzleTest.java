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
package org.codedefenders.game.puzzle;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuzzleTest {

    private static final LocalDate FIRST_DECEMBER = LocalDate.of(2000, 12, 1);

    @Test
    void seasonalPuzzle1LockedBeforeItsScheduledDay() {
        var position = 1;
        var puzzle = Puzzle.forPuzzleInfo(1, 1, position, 1, null, null);
        var chapter = new PuzzleChapter(1, 1, FIRST_DECEMBER, 37);
        assertFalse(puzzle.isSeasonalUnlocked(chapter, LocalDate.of(2026, 11, 30)));
    }

    @Test
    void seasonalPuzzle24LockedBeforeItsScheduledDay() {
        var position = 24;
        var puzzle = Puzzle.forPuzzleInfo(1, 1, position, 1, null, null);
        var chapter = new PuzzleChapter(1, 1, FIRST_DECEMBER, 37);
        assertFalse(puzzle.isSeasonalUnlocked(chapter, LocalDate.of(2026, 12, 23)));
    }

    @Test
    void seasonalPuzzle1UnlockedAtItsScheduledDay() {
        var puzzle = Puzzle.forPuzzleInfo(1, 1, 1, 1, null, null);
        var chapter = new PuzzleChapter(1, 1, FIRST_DECEMBER, 37);
        assertTrue(puzzle.isSeasonalUnlocked(chapter, LocalDate.of(2026, 12, 1)));
    }

    @Test
    void seasonalPuzzle1UnlockedAfterItsScheduledDay() {
        var puzzle = Puzzle.forPuzzleInfo(1, 1, 1, 1, null, null);
        var chapter = new PuzzleChapter(1, 1, FIRST_DECEMBER, 37);
        assertTrue(puzzle.isSeasonalUnlocked(chapter, LocalDate.of(2026, 12, 2)));
    }

    @Test
    void seasonalPuzzle1StillUnlockedAfterNewYear() {
        var puzzle = Puzzle.forPuzzleInfo(1, 1, 1, 1, null, null);
        var chapter = new PuzzleChapter(1, 1, FIRST_DECEMBER, 37);
        assertTrue(puzzle.isSeasonalUnlocked(chapter, LocalDate.of(2027, 1, 1)));
    }

    @Test
    void seasonalPuzzle1LockedAfterAvailabilityWindowOver() {
        var puzzle = Puzzle.forPuzzleInfo(1, 1, 1, 1, null, null);
        var chapter = new PuzzleChapter(1, 1, FIRST_DECEMBER, 37);
        assertFalse(puzzle.isSeasonalUnlocked(chapter, LocalDate.of(2027, 1, 7)));
    }

    @Test
    void seasonalPuzzle1StillUnlockedAtLastDayOfAvailabilityWindow() {
        var puzzle = Puzzle.forPuzzleInfo(1, 1, 1, 1, null, null);
        var chapter = new PuzzleChapter(1, 1, FIRST_DECEMBER, 37);
        assertTrue(puzzle.isSeasonalUnlocked(chapter, LocalDate.of(2027, 1, 6)));
    }
}
