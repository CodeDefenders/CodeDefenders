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
package org.codedefenders.beans.page;

import java.util.List;
import java.util.Map;

import org.codedefenders.game.GameLevel;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.game.puzzle.PuzzleType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PuzzleNavigationBeanTest {
    @Test
    void keepsNewestActiveGameWhenRepeatedPracticeGamesSharePuzzle() {
        PuzzleGame newest = game(42, 12);
        PuzzleGame older = game(42, 11);

        Map<Integer, PuzzleGame> active = PuzzleNavigationBean.activePuzzles(List.of(newest, older));

        assertEquals(newest, active.get(42));
    }

    private static PuzzleGame game(int puzzleId, int gameId) {
        Puzzle puzzle = Puzzle.regularPuzzle(puzzleId, 1, PuzzleType.DEFENDER, false, GameLevel.EASY, 1,
                null, null, null, null, null);
        PuzzleGame game = new PuzzleGame(puzzle, 7);
        game.setId(gameId);
        return game;
    }
}
