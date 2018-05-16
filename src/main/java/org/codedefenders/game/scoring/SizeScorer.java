package org.codedefenders.game.scoring;

import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MultiplayerGame;

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
