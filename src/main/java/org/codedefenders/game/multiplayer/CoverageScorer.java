package org.codedefenders.game.multiplayer;

import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.scoring.Scorer;

import java.util.List;

/**
 * Created by thoma on 27/06/2016.
 */
public class CoverageScorer extends Scorer {
    @Override
    protected int scoreTest(MultiplayerGame game, Test test, List<Mutant> killedMutants) {
        return (game.getDefenderValue() + killedMutants.size()) * test.getLineCoverage().getLinesCovered().size();
    }

    @Override
    protected int scoreMutant(MultiplayerGame game, Mutant mutant, List<Test> passedTests) {
        return (game.getAttackerValue() + passedTests.size());
    }
}
