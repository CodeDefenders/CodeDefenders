package org.codedefenders.scoring;

import org.codedefenders.Test;
import org.codedefenders.multiplayer.MultiplayerGame;
import org.codedefenders.Mutant;

import java.util.ArrayList;

/**
 * Created by thoma on 23/06/2016.
 */
public abstract class Scorer {

    private static class SizeScorer extends Scorer {

        @Override
        protected int scoreTest(MultiplayerGame g, Test t, ArrayList<Mutant> killed) {

            int points = 0;

            for (Mutant m : killed){
                if (m.getScore() == 0){
                    points++;
                } else {
                    points += m.getScore();
                }
            }

            return points;
        }

        @Override
        protected int scoreMutant(MultiplayerGame g, Mutant m, ArrayList<Test> passed) {
            return passed.size();
        }
    }

    private static Scorer scorer = new SizeScorer();

    protected abstract int scoreTest(MultiplayerGame g, Test t, ArrayList<Mutant> killed);
    protected abstract int scoreMutant(MultiplayerGame g, Mutant m, ArrayList<Test> passed);

    public static int score(MultiplayerGame g, Test t, ArrayList<Mutant> killed){
        return scorer.scoreTest(g, t, killed);
    }

    public static void setScorer(Scorer s){
        scorer = s;
    }

    public static int score(MultiplayerGame g, Mutant m, ArrayList<Test> passed){
        return scorer.scoreMutant(g, m, passed);
    }
}
