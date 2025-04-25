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
package org.codedefenders.game.multiplayer;

import java.util.List;

import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.scoring.Scorer;

/**
 * Created by thoma on 27/06/2016.
 *
 */
@Deprecated
public class CoverageScorer extends Scorer {
    @Override
    protected int scoreTest(MultiplayerGame game, Test test, List<Mutant> killedMutants) {
        return (game.getDefenderValue() + killedMutants.size()) * test.getLineCoverage().getLinesCovered().size();
    }

    @Override
    protected int scoreMutant(MultiplayerGame game, Mutant mutant, List<Test> passedTests) {
        return (game.getAttackerValue() + passedTests.size());
    }
}
