package org.codedefenders.game.puzzle.solving;

import org.codedefenders.game.Test;
import org.codedefenders.game.puzzle.PuzzleGame;

/**
 * This {@link TestSolvingStrategy} implementation marks a puzzle as solved, when
 * all system mutants are killed by tests submitted by the user.
 *
 * @author <a href=https://github.com/werli>Phil Werli<a/>
 */
public final class KilledAllMutantsTestSolvingStrategy implements TestSolvingStrategy {
    @Override
    public boolean solve(PuzzleGame game, Test test) {
        return game.getAliveMutants().isEmpty();
    }
}
