package org.codedefenders.game.puzzle.solving;

import org.codedefenders.game.Mutant;
import org.codedefenders.game.puzzle.PuzzleGame;

/**
 * This {@link MutantSolvingStrategy} implementation marks a puzzle as solved, when
 * the mutant submitted by the user survives all system tests.
 *
 * @author <a href=https://github.com/werli>Phil Werli<a/>
 */
public final class SurvivedAllTestsSolvingStrategy implements MutantSolvingStrategy {
    @Override
    public boolean solve(PuzzleGame game, Mutant mutant) {
        return mutant.isAlive();
    }
}
