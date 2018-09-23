package org.codedefenders.game.scoring;

import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MultiplayerGame;

import java.util.List;

/**
 * This implementation of a {@link Scorer} calculates the score based on how many
 * mutants were killed by a test or tests were passed by a mutant.
 */
public class SizeScorer extends Scorer {

    /**
     * Calculates the score of a given test by summing up how many mutants the test
     * killed and the points of these mutants.
     *
     * @param game          the game in which the test was created in.
     * @param test          the test, which score is calculated.
     * @param killedMutants a list of mutants, which were killed by the given test.
     * @return the calculated score for the test.
     */
    @Override
    protected int scoreTest(MultiplayerGame game, Test test, List<Mutant> killedMutants) {
        return killedMutants.size() + killedMutants.stream().mapToInt(Mutant::getScore).sum();
    }

    /**
     * Calculates the score of a given mutant by summing up the number of tests the
     * mutant has passed.
     *
     * @param game          the game in which the mutant was created in.
     * @param mutant          the mutant, which score is calculated.
     * @param passedTests a list of tests, which the given mutant passed.
     * @return the calculated score for the mutant.
     */
    @Override
    protected int scoreMutant(MultiplayerGame game, Mutant mutant, List<Test> passedTests) {
        return passedTests.size();
    }
}
