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
 * This class offers an interface for Scoring classes, which calculate the scores of a mutant or test.
 *
 * <p>This class also offers static methods, which call the {@link SizeScorer} implementation.
 */
@Deprecated
public abstract class Scorer {

    protected abstract int scoreTest(MultiplayerGame game, Test test, List<Mutant> killedMutants);

    protected abstract int scoreMutant(MultiplayerGame game, Mutant mutant, List<Test> passedTests);

    /**
     * Calls {@link SizeScorer#scoreTest(MultiplayerGame, Test, List)} for a new {@link SizeScorer} instance.
     */
    public static int score(MultiplayerGame game, Test test, List<Mutant> killedMutants) {
        return new SizeScorer().scoreTest(game, test, killedMutants);
    }

    /**
     * Calls {@link SizeScorer#scoreMutant(MultiplayerGame, Mutant, List)} for a new {@link SizeScorer} instance.
     */
    public static int score(MultiplayerGame game, Mutant mutant, List<Test> passedTests) {
        return new SizeScorer().scoreMutant(game, mutant, passedTests);
    }
}
