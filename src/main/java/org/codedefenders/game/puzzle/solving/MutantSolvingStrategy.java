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
package org.codedefenders.game.puzzle.solving;

import org.codedefenders.game.Mutant;
import org.codedefenders.game.puzzle.PuzzleGame;

/**
 * Interface for a mutant solving strategy.
 *
 * <p>{@link #solve(PuzzleGame, Mutant)} returns whether a submitted mutant resulted in a state
 * of the puzzle game, which the strategy marks as solved.
 *
 * @author <a href=https://github.com/werli>Phil Werli</a>
 */
public interface MutantSolvingStrategy {
    boolean solve(PuzzleGame game, Mutant mutant);

    // or returns {@code null}
    static MutantSolvingStrategy get(String name) {
        if (name == null) {
            return null;
        }
        try {
            return ((MutantSolvingStrategy) Types.valueOf(name).clazz.newInstance());
        } catch (IllegalArgumentException | IllegalAccessException | InstantiationException ignored) {
            // ignored
        }
        return null;
    }

    enum Types {
        SURVIVED_ALL_MUTANTS(SurvivedAllTestsSolvingStrategy.class, "Mutant survived all tests.");

        Class clazz;
        String description;

        Types(Class clazz, String description) {
            this.clazz = clazz;
            this.description = description;
        }
    }
}
