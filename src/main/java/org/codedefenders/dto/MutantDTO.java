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

import org.codedefenders.database.UserDAO;
import org.codedefenders.game.Mutant;
import org.codedefenders.model.User;

import com.google.gson.annotations.Expose;

public class MutantDTO {
    @Expose
    private final int id;
    @Expose
    private UserDTO creator;
    @Expose
    private Mutant.State state;
    @Expose
    private int points;
    @Expose
    private String lineString;
    @Expose
    private Boolean covered;
    @Expose
    private String killedByName;
    @Expose
    private boolean canMarkEquivalent;
    @Expose
    private boolean canView = false;
    private String sourceCode;
    private List<Integer> lines;
    @Expose
    private int killedByTestId;
    @Expose
    private String killMessage;
    @Expose
    private String description;
    private Integer playerId;
    private Integer gameId;
    private String patchString;

    public MutantDTO(Mutant mutant) {
        creator = new UserDTO(mutant.getCreatorId(), mutant.getCreatorName());
        id = mutant.getId();
        points = mutant.getScore();
        state = mutant.getState();
        description = mutant.getHTMLReadout().stream()
                .filter(Objects::nonNull).collect(Collectors.joining("<br>"));
        if (mutant.getKillingTest() != null) {
            killedByName = UserDAO.getUserForPlayer(mutant.getKillingTest().getPlayerId()).getUsername();
            killedByTestId = mutant.getKillingTest().getId();
            killMessage = mutant.getKillMessage();
        } else {
            killedByName = null;
            killedByTestId = -1;
            killMessage = null;
        }
        lines = mutant.getLines();
        lineString = lines.stream().map(String::valueOf).collect(Collectors.joining(","));
        playerId = mutant.getPlayerId();
        gameId = mutant.getGameId();
        patchString = mutant.getPatchString();
    }

    @Deprecated
    public MutantDTO(Mutant mutant, User user, boolean playerCoverToClaim) {
        this(mutant);
    }

    public int getId() {
        return id;
    }

    public MutantDTO setCovered(boolean covered) {
        this.covered = covered;
        return this;
    }

    public boolean isViewable() {
        return canView;
    }

    public MutantDTO setViewable(boolean canView) {
        this.canView = canView;
        return this;
    }

    public String getSourceCode() {
        if (canView) {
            return sourceCode;
        } else {
            return null;
        }
    }

    public String getPatchString() {
        if (canView) {
            return patchString;
        } else {
            return null;
        }
    }

    public MutantDTO setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
        return this;
    }

    public boolean isCanMarkEquivalent() {
        return canMarkEquivalent;
    }

    public MutantDTO setCanMarkEquivalent(boolean canMarkEquivalent) {
        this.canMarkEquivalent = canMarkEquivalent;
        return this;
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
