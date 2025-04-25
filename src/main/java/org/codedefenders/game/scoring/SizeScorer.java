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

import java.util.List;

import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MultiplayerGame;

/**
 * This implementation of a {@link Scorer} calculates the score based on how many
 * mutants were killed by a test or tests were passed by a mutant.
 */
@Deprecated
public class SizeScorer extends Scorer {

    /**
     * Calculates the score of a given test by summing up how many mutants the test
     * killed and the points of these mutants.
     *
     * @param game          the game in which the test was created in.
     * @param test          the test, which score is calculated.
     * @param killedMutants a list of mutants, which were killed by the given test.
     * @return the calculated score for the test.
     */
    @Override
    protected int scoreTest(MultiplayerGame game, Test test, List<Mutant> killedMutants) {
        return killedMutants.size() + killedMutants.stream().mapToInt(Mutant::getScore).sum();
    }

    /**
     * Calculates the score of a given mutant by summing up the number of tests the
     * mutant has passed.
     *
     * @param game          the game in which the mutant was created in.
     * @param mutant          the mutant, which score is calculated.
     * @param passedTests a list of tests, which the given mutant passed.
     * @return the calculated score for the mutant.
     */
    @Override
    protected int scoreMutant(MultiplayerGame game, Mutant mutant, List<Test> passedTests) {
        return passedTests.size();
    }
}
