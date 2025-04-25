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

import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.PlayerScore;

/**
 * This should replace {@link Scorer} and we will use it for dependency injection.
 *
 * @author gambi
 *
 */
public interface IScoringPolicy {

    /**
     * Associate this test with a score.
     * TODO Consider returning or updating PlayerScore instead of test
     */
    void scoreTest(Test test);

    /**
     * Associate this mutant with a score.
     * TODO Consider returning or updating PlayerScore instead of mutant
     */
    void scoreMutant(Mutant mutant);

    /**
     * Associate this PlayerScore with points gained by winning equivalence duels.
     */
    void scoreDuels(PlayerScore duelScore);

}
