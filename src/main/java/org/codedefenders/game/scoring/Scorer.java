package org.codedefenders.game.scoring;

import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MultiplayerGame;

import java.util.List;

/**
 * This class offers an interface for Scoring classes, which calculate the scores of a mutant or test.
 * <p>
 * This class also offers static methods, which call the {@link SizeScorer} implementation.
 */
public abstract class Scorer {

    protected abstract int scoreTest(MultiplayerGame game, Test test, List<Mutant> killedMutants);

    protected abstract int scoreMutant(MultiplayerGame game, Mutant mutant, List<Test> passedTests);

    /**
     * Calls {@link SizeScorer#score(MultiplayerGame, Test, List)} for a new {@link SizeScorer} instance.
     */
    public static int score(MultiplayerGame game, Test test, List<Mutant> killedMutants) {
        return new SizeScorer().scoreTest(game, test, killedMutants);
    }

    /**
     * Calls {@link SizeScorer#score(MultiplayerGame, Mutant, List)} for a new {@link SizeScorer} instance.
     */
    public static int score(MultiplayerGame game, Mutant mutant, List<Test> passedTests) {
        return new SizeScorer().scoreMutant(game, mutant, passedTests);
    }
}
