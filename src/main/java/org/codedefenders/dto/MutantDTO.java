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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;
import org.codedefenders.database.UserDAO;
import org.codedefenders.game.Mutant;
import org.codedefenders.model.UserEntity;

import com.google.gson.annotations.Expose;

public class MutantDTO {
    @Expose
    public final int id;
    @Expose
    public final SimpleUser creator;
    @Expose
    public final Mutant.State state;
    @Expose
    public final int points;
    @Expose
    public final String lineString;
    @Expose
    public Boolean covered;
    @Expose
    public final SimpleUser killedBy;
    @Expose
    public final boolean canMarkEquivalent;
    @Expose
    public final boolean canView;
    @Expose
    public final int killedByTestId;
    @Expose
    public final String killMessage;
    @Expose
    public final String description;

    private final List<Integer> lines;
    private final Integer gameId;
    private final Integer playerId;
    private final String patchString;

    public MutantDTO(
            int id,
            SimpleUser creator,
            Mutant.State state,
            int points,
            String description,
            String lineString,
            boolean covered,
            boolean canView,
            boolean canMarkEquivalent,
            SimpleUser killedBy,
            int killedByTestId,
            String killMessage,
            int gameId,
            int playerId,
            List<Integer> lines,
            String patchString
    ) {
        this.id = id;
        this.creator = creator;
        this.state = state;
        this.points = points;
        this.description = description;
        this.lineString = lineString;
        this.covered = covered;
        this.canView = canView;
        this.canMarkEquivalent = canMarkEquivalent;
        this.killedBy = killedBy;
        this.killedByTestId = killedByTestId;
        this.killMessage = killMessage;

        this.gameId = gameId;
        this.playerId = playerId;
        this.lines = lines;
        this.patchString = patchString;
    }

    public String getPatchString() {
        if (canView) {
            return patchString;
        } else {
            return null;
        }
    }

    public List<Integer> getLines() {
        return lines;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public Integer getGameId() {
        return gameId;
    }


    public static class LineNumberComparator implements Comparator<MutantDTO> {

        @Override
        public int compare(MutantDTO o1, MutantDTO o2) {
            List<Integer> lines1 = o1.lines;
            List<Integer> lines2 = o2.lines;

            if (lines1.isEmpty()) {
                if (lines2.isEmpty()) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (lines2.isEmpty()) {
                return 1;
            }

            return Collections.min(lines1) - Collections.min(lines2);
        }
    }
}
