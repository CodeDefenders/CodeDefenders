package org.codedefenders.scoring;

import org.codedefenders.Mutant;
import org.codedefenders.Test;
import org.codedefenders.multiplayer.MultiplayerGame;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by thoma on 07/07/2016.
 */
public class CoverageScorer extends Scorer {

    @Override
    protected int scoreTest(MultiplayerGame g, Test t, ArrayList<Mutant> killed) {
        int totalLines = t.getLineCoverage().getLinesCovered().size() + t.getLineCoverage().getLinesUncovered().size();

        float percentCovered = t.getLineCoverage().getLinesCovered().size()/(float)totalLines;

        return killed.size() + (int)(g.getDefenderValue() * percentCovered);
    }

    @Override
    protected int scoreMutant(MultiplayerGame g, Mutant mm, ArrayList<Test> passed) {
        ArrayList<Mutant> mutants = g.getMutants();

        HashMap<Integer, ArrayList<Mutant>> mutantLines = new HashMap<Integer, ArrayList<Mutant>>();

        for (Mutant m : mutants) {
            if (mm.getId() ==m.getId()){
                continue;
            }
            for (int line : m.getLines()){
                if (!mutantLines.containsKey(line)){
                    mutantLines.put(line, new ArrayList<Mutant>());
                }

                mutantLines.get(line).add(m);

            }

        }

        int lineScore = g.getAttackerValue();
        for (int line : mm.getLines()){
            if (mutantLines.containsKey(line)){
                float percent = mutants.size() == 0 ? 1f : 1f - (mutantLines.get(line).size() / (float)mutants.size());
                lineScore = (int)(lineScore * percent);
            }

        }

        return lineScore;
    }
}
