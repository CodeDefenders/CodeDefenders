package org.codedefenders.multiplayer;

import org.codedefenders.Mutant;
import org.codedefenders.Test;
import org.codedefenders.scoring.Scorer;

import java.util.ArrayList;
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
