package org.codedefenders.game.scoring;

import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MultiplayerGame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by thoma on 07/07/2016.
 */
public class CoverageScorer extends Scorer {

    @Override
    protected int scoreTest(MultiplayerGame game, Test test, List<Mutant> killedMutants) {
        int totalLines = test.getLineCoverage().getLinesCovered().size() + test.getLineCoverage().getLinesUncovered().size();

        float percentCovered = test.getLineCoverage().getLinesCovered().size() / (float) totalLines;

        return killedMutants.size() + (int) (game.getDefenderValue() * percentCovered);
    }

    @Override
    protected int scoreMutant(MultiplayerGame game, Mutant mutant, List<Test> passedTests) {
        List<Mutant> mutants = game.getMutants();

        Map<Integer, List<Mutant>> mutantLines = new HashMap<>();

        for (Mutant m : mutants) {
            if (mutant.getId() == m.getId()) {
                continue;
            }
            for (int line : m.getLines()) {
                if (!mutantLines.containsKey(line)) {
                    mutantLines.put(line, new ArrayList<>());
                }
                mutantLines.get(line).add(m);
            }
        }

        int lineScore = game.getAttackerValue();
        for (int line : mutant.getLines()) {
            if (mutantLines.containsKey(line)) {
                float percent = mutants.size() == 0 ? 1f : 1f - (mutantLines.get(line).size() / (float) mutants.size());
                lineScore = (int) (lineScore * percent);
            }

        }

        return lineScore;
    }
}
