package org.codedefenders.game.puzzle.solving;

import org.codedefenders.game.Test;
import org.codedefenders.game.puzzle.PuzzleGame;

/**
 * Interface for a test solving strategy.
 *
 * {@link #solve(PuzzleGame, Test)} returns whether a submitted test resulted in a state
 * of the puzzle game, which the strategy marks as solved.
 *
 * @author <a href=https://github.com/werli>Phil Werli<a/>
 */
public interface TestSolvingStrategy {
    boolean solve(PuzzleGame game, Test test);

    // or returns {@code null}
    static TestSolvingStrategy get(String name) {
        if (name == null) {
            return null;
        }
        try {
            return ((TestSolvingStrategy) Types.valueOf(name).clazz.newInstance());
        } catch (IllegalArgumentException | IllegalAccessException | InstantiationException ignored) {
        }
        return null;
    }

    enum Types {
        KILLED_ALL_MUTANTS(KilledAllMutantsTestSolvingStrategy.class, "Tests killed all mutants.");

        Class clazz;
        String description;

        Types(Class clazz, String description) {
            this.clazz = clazz;
            this.description = description;
        }
    }
}
