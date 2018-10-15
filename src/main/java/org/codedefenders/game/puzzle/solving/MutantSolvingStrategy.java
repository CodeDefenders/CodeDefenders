package org.codedefenders.game.puzzle.solving;

import org.codedefenders.game.Mutant;
import org.codedefenders.game.puzzle.PuzzleGame;

/**
 * Interface for a mutant solving strategy.
 * <p>
 * {@link #solve(PuzzleGame, Mutant)} returns whether a submitted mutant resulted in a state
 * of the puzzle game, which the strategy marks as solved.
 *
 * @author <a href=https://github.com/werli>Phil Werli<a/>
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
