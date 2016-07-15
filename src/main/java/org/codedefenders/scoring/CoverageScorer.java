package org.codedefenders.scoring;

import org.codedefenders.Test;
import org.codedefenders.multiplayer.MultiplayerGame;
import org.codedefenders.multiplayer.MultiplayerMutant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by thoma on 07/07/2016.
 */
public class CoverageScorer extends Scorer {

    @Override
    protected int scoreTest(MultiplayerGame g, Test t, ArrayList<MultiplayerMutant> killed) {
        int totalLines = t.getLineCoverage().getLinesCovered().length + t.getLineCoverage().getLinesUncovered().length;

        float percentCovered = t.getLineCoverage().getLinesCovered().length/(float)totalLines;

        return killed.size() + (int)(g.getDefenderValue() * percentCovered);
    }

    @Override
    protected int scoreMutant(MultiplayerGame g, MultiplayerMutant mm, ArrayList<Test> passed) {
        ArrayList<MultiplayerMutant> mutants = g.getMutants();

        HashMap<Integer, ArrayList<MultiplayerMutant>> mutantLines = new HashMap<Integer, ArrayList<MultiplayerMutant>>();

        for (MultiplayerMutant m : mutants) {
            if (mm.getId() ==m.getId()){
                continue;
            }
            for (int line : m.getLines()){
                if (!mutantLines.containsKey(line)){
                    mutantLines.put(line, new ArrayList<MultiplayerMutant>());
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
