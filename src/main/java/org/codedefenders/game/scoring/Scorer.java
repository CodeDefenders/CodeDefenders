package org.codedefenders.game.scoring;

import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.Mutant;

import java.util.List;

/**
 * Created by thoma on 23/06/2016.
 */
public abstract class Scorer {

    private static Scorer scorer = new SizeScorer();

    protected abstract int scoreTest(MultiplayerGame g, Test t, List<Mutant> killed);

    protected abstract int scoreMutant(MultiplayerGame g, Mutant m, List<Test> passed);

    public static int score(MultiplayerGame g, Test t, List<Mutant> killed){
        return scorer.scoreTest(g, t, killed);
    }

    public static void setScorer(Scorer s){
        scorer = s;
    }

    public static int score(MultiplayerGame g, Mutant m, List<Test> passed){
        return scorer.scoreMutant(g, m, passed);
    }
}
