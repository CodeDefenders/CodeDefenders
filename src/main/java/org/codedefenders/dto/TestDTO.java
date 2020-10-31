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

import java.util.List;
import java.util.stream.Collectors;

import org.codedefenders.database.TestSmellsDAO;
import org.codedefenders.database.UserDAO;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.model.User;

import com.google.gson.annotations.Expose;

public class TestDTO {
    @Expose
    private int id;
    @Expose
    private UserDTO creator;
    @Expose
    private boolean canView = false;
    @Expose
    private List<Integer> coveredMutantIds;
    @Expose
    private List<Integer> killedMutantIds;
    @Expose
    private Integer points;
    @Expose
    private List<String> smells;
    private Test test;
    private List<Integer> linesCovered;
    private Integer gameId;
    private Integer playerId;
    private String source;

    public TestDTO(Test test) {
        this.test = test;
        this.id = test.getId();
        User creator = UserDAO.getUserForPlayer(test.getPlayerId());
        this.creator = new UserDTO(creator.getId(), creator.getUsername());
        this.points = test.getScore();
        this.smells = (new TestSmellsDAO()).getDetectedTestSmellsForTest(test);
        this.linesCovered = test.getLineCoverage().getLinesCovered();
        this.gameId = test.getGameId();
        this.playerId = test.getPlayerId();
        this.source = test.getAsString();
    }

    public int getId() {
        return id;
    }

    public List<Integer> getLinesCovered() {
        return linesCovered;
    }

    public TestDTO setViewable(boolean canView) {
        this.canView = canView;
        return this;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public Integer getGameId() {
        return gameId;
    }

    public String getSource() {
        return source;
    }

    public TestDTO setMutantData(List<Mutant> mutants) {
        this.coveredMutantIds = test.getCoveredMutants(mutants).stream()
                .map(Mutant::getId)
                .collect(Collectors.toList());
        this.killedMutantIds = test.getKilledMutants().stream()
                .map(Mutant::getId)
                .collect(Collectors.toList());
        return this;
    }
}
