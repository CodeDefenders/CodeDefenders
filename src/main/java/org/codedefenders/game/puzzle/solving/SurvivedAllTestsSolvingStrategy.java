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
package org.codedefenders.game.puzzle.solving;

import org.codedefenders.game.Mutant;
import org.codedefenders.game.puzzle.PuzzleGame;

/**
 * This {@link MutantSolvingStrategy} implementation marks a puzzle as solved, when
 * the mutant submitted by the user survives all system tests.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
public final class SurvivedAllTestsSolvingStrategy implements MutantSolvingStrategy {
    @Override
    public boolean solve(PuzzleGame game, Mutant mutant) {
        return mutant.isAlive();
    }
}
