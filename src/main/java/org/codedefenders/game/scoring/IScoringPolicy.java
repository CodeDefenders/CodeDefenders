package org.codedefenders.game.scoring;

import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.PlayerScore;

/**
 * This should replace {@link Scorer} and we will use it for dependency
 * injection
 * 
 * @author gambi
 *
 */
public interface IScoringPolicy {

    /**
     * Associate this test with a score
     * TODO Consider returning or updating PlayerScore instead of test
     * @return
     */
    public void scoreTest(Test test);

    /**
     * Associate this mutant with a score
     * TODO Consider returning or updating PlayerScore instead of mutant
     * @return
     */
    public void scoreMutant(Mutant mutant);

    /**
     * Associate this PlayerScore with points gained by winning equivalence duels
     * 
     * @return
     */
    public void scoreDuels(PlayerScore duelScore);

}
