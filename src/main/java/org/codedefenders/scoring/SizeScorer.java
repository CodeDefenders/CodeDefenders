package org.codedefenders.scoring;

import org.codedefenders.Mutant;
import org.codedefenders.Test;
import org.codedefenders.multiplayer.MultiplayerGame;

import java.util.List;

/**
 * Created by gordon on 20/02/2017.
 */
public class SizeScorer extends Scorer {

    @Override
    protected int scoreTest(MultiplayerGame g, Test t, List<Mutant> killed) {

        int points = 0;

        for (Mutant m : killed) {
            points += m.getScore() + 1;
        }

        return points;
    }

    @Override
    protected int scoreMutant(MultiplayerGame g, Mutant m, List<Test> passed) {
        return passed.size();
    }
}
