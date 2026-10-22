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
package org.codedefenders.game.highlighting;

import java.util.List;

import org.codedefenders.game.Mutant;
import org.codedefenders.util.Constants;

import com.google.gson.annotations.Expose;

/**
 * Represents a mutant for the game highlighting.
 */
public class GHMutantDTO {
    @Expose
    public int id;
    @Expose
    public int score;
    @Expose
    public String lines;
    @Expose
    public List<Integer> mutatedLines;
    @Expose
    public String creatorName;
    @Expose
    public Mutant.State status;
    @Expose
    public boolean canClaim;

    public GHMutantDTO(Mutant mutant, Integer userId) {
        this.id = mutant.getId();
        this.score = mutant.getScore();
        this.creatorName = mutant.getCreatorName();
        this.lines = mutant.getSummaryString();
        this.mutatedLines = mutant.getLines();
        this.status = mutant.getState();
        this.canClaim = mutant.getEquivalent().equals(Mutant.Equivalence.ASSUMED_NO)
                && mutant.isAlive()
                && mutant.getCreatorId() != Constants.DUMMY_ATTACKER_USER_ID
                && (userId == null || mutant.getCreatorId() != userId)
                && !mutant.getLines().isEmpty();
    }
}
