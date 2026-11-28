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

import static org.junit.jupiter.api.Assertions.assertEquals;

class PuzzleChapterTest {

    private static final LocalDate FIRST_DECEMBER = LocalDate.of(2000, 12, 1);

    @Test
    void selectsPreviousSeasonDuringItsNewYearAvailabilityWindow() {
        var chapter = new PuzzleChapter(1, 1, FIRST_DECEMBER, 37);
        assertEquals(
                LocalDate.of(2026, 12, 1),
                chapter.getCurrentSeasonStartDate(LocalDate.of(2027, 1, 1))
        );
    }

    @Test
    void selectsUpcomingSeasonBeforeItsStartDate() {
        var chapter = new PuzzleChapter(1, 1, FIRST_DECEMBER, 37);
        assertEquals(
                LocalDate.of(2027, 12, 1),
                chapter.getCurrentSeasonStartDate(LocalDate.of(2027, 11, 30))
        );
    }
}
