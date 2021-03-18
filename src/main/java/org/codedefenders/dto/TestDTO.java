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

import com.google.gson.annotations.Expose;

public class TestDTO {
    @Expose
    public final int id;
    @Expose
    public final SimpleUser creator;
    @Expose
    public final Integer points;
    @Expose
    public boolean canView;
    @Expose
    public List<Integer> coveredMutantIds;
    @Expose
    public List<Integer> killedMutantIds;
    @Expose
    public final List<String> smells;

    private final List<Integer> linesCovered;
    private final Integer gameId;
    private final Integer playerId;
    private final String source;

    public TestDTO(int id,
            SimpleUser creator,
            Integer points,
            boolean canView,
            List<Integer> coveredMutantIds,
            List<Integer> killedMutantIds,
            List<String> smells,
            int gameId,
            int playerId,
            List<Integer> linesCovered,
            String source
    ) {
        this.id = id;
        this.creator = creator;
        this.points = points;
        this.canView = canView;
        this.coveredMutantIds = coveredMutantIds;
        this.killedMutantIds = killedMutantIds;
        this.smells = smells;
        this.gameId = gameId;
        this.playerId = playerId;
        this.linesCovered = linesCovered;
        this.source = source;
    }

    public List<Integer> getLinesCovered() {
        return linesCovered;
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
}
