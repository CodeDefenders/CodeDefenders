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

import java.lang.reflect.InvocationTargetException;

import org.codedefenders.game.Test;
import org.codedefenders.game.puzzle.PuzzleGame;

/**
 * Interface for a test solving strategy.
 *
 * <p>{@link #solve(PuzzleGame, Test)} returns whether a submitted test resulted in a state
 * of the puzzle game, which the strategy marks as solved.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
public interface TestSolvingStrategy {
    boolean solve(PuzzleGame game, Test test);

    // or returns {@code null}
    static TestSolvingStrategy get(String name) {
        if (name == null) {
            return null;
        }
        try {
            return ((TestSolvingStrategy) Types.valueOf(name).clazz.getDeclaredConstructor().newInstance());
        } catch (IllegalArgumentException | IllegalAccessException | InstantiationException | NoSuchMethodException
                 | InvocationTargetException ignored) {
            // ignored
        }
        return null;
    }

    enum Types {
        KILLED_ALL_MUTANTS(KilledAllMutantsTestSolvingStrategy.class, "Tests killed all mutants.");

        final Class<?> clazz;
        final String description;

        Types(Class<?> clazz, String description) {
            this.clazz = clazz;
            this.description = description;
        }
    }
}
