package org.codedefenders.game.scoring;

import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.PlayerScore;

/**
 * This should replace {@link Scorer} and we will use it for dependency injection.
 *
 * @author gambi
 *
 */
public interface IScoringPolicy {

    /**
     * Associate this test with a score.
     * TODO Consider returning or updating PlayerScore instead of test
     */
    void scoreTest(Test test);

    /**
     * Associate this mutant with a score.
     * TODO Consider returning or updating PlayerScore instead of mutant
     */
    void scoreMutant(Mutant mutant);

    /**
     * Associate this PlayerScore with points gained by winning equivalence duels.
     */
    void scoreDuels(PlayerScore duelScore);

}
