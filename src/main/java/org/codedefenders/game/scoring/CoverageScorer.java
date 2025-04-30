/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.game.scoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MultiplayerGame;

/**
 * Created by thoma on 07/07/2016.
 */
public class CoverageScorer extends Scorer {

    @Override
    protected int scoreTest(MultiplayerGame game, Test test, List<Mutant> killedMutants) {
        int totalLines = test.getLineCoverage().getLinesCovered().size()
                + test.getLineCoverage().getLinesUncovered().size();

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
