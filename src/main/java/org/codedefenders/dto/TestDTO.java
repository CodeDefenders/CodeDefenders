/*
 * Copyright (C) 2020 Code Defenders contributors
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

package org.codedefenders.dto;

import com.google.gson.annotations.Expose;
import org.codedefenders.database.TestSmellsDAO;
import org.codedefenders.database.UserDAO;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.model.User;

import java.util.List;
import java.util.stream.Collectors;

public class TestDTO {
    @Expose
    private int id;
    @Expose
    private String creatorName;
    @Expose
    private List<Integer> coveredMutantIds;
    @Expose
    private List<Integer> killedMutantIds;
    @Expose
    private int points;
    @Expose
    private List<String> smells;

    public TestDTO() {

    }

    public TestDTO(Test test, List<Mutant> mutants) {
        User creator = UserDAO.getUserForPlayer(test.getPlayerId());
        this.id = test.getId();
        this.creatorName = creator.getUsername();
        this.coveredMutantIds = test.getCoveredMutants(mutants).stream()
                .map(Mutant::getId)
                .collect(Collectors.toList());
        this.killedMutantIds = test.getKilledMutants().stream()
                .map(Mutant::getId)
                .collect(Collectors.toList());
        this.points = test.getScore();
        this.smells = (new TestSmellsDAO()).getDetectedTestSmellsForTest(test);
    }
}
