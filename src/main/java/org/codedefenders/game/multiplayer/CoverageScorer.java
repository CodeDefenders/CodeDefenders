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
    protected int scoreTest(MultiplayerGame g, Test t, List<Mutant> killed) {
        return (g.getDefenderValue() + killed.size()) * t.getLineCoverage().getLinesCovered().size();
    }

    @Override
    protected int scoreMutant(MultiplayerGame g, Mutant m, List<Test> passed) {
        return (g.getAttackerValue() + passed.size());
    }
}
