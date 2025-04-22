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

import org.codedefenders.game.Mutant;

import com.google.gson.annotations.Expose;

public class MutantDTO {
    @Expose
    private final int id;
    @Expose
    private final SimpleUser creator;
    @Expose
    private final Mutant.State state;
    @Expose
    private final int points;
    @Expose
    private final String lineString;
    @Expose
    private final SimpleUser killedBy;
    @Expose
    private final boolean canMarkEquivalent;
    @Expose
    private final boolean canView;
    @Expose
    private final int killedByTestId;
    @Expose
    private final String killMessage;
    @Expose
    private final String description;
    @Expose
    private final boolean covered;
    @Expose
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
        return Collections.unmodifiableList(lines);
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public Integer getGameId() {
        return gameId;
    }

    public int getId() {
        return id;
    }

    public SimpleUser getCreator() {
        return creator;
    }

    public Mutant.State getState() {
        return state;
    }

    public int getPoints() {
        return points;
    }

    public String getLineString() {
        return lineString;
    }

    public boolean getCovered() {
        return covered;
    }

    public SimpleUser getKilledBy() {
        return killedBy;
    }

    public boolean isCanMarkEquivalent() {
        return canMarkEquivalent;
    }

    public boolean isCanView() {
        return canView;
    }


    /**
     * killedByTestId is either:
     * <ul>
     * <li>a valid id >= 100 (only if the current user is an attacker),</li>
     * <li>'0' to indicate that there exists an external test can kill the mutant (only for defenders),</li>
     * <li>'-1' to indicate that the mutant is probably not killable</li>
     * </ul>
     *
     * @return the test id that killed the mutant, or 0 if the external killing test is not specified, or -1 if the mutant
     * is not killable by the selection of external tests.
     */
    public int getKilledByTestId() {
        return killedByTestId;
    }

    public String getKillMessage() {
        return killMessage;
    }

    public String getDescription() {
        return description;
    }

    public MutantDTO copyWithKillingTest(int killedByTestId, SimpleUser killedByUser, String killMessage) {
        return new MutantDTO(
                id,
                creator,
                state,
                points,
                description,
                lineString,
                covered,
                canView,
                canMarkEquivalent,
                killedByUser,
                killedByTestId,
                killMessage,
                gameId,
                playerId,
                lines,
                patchString
        );
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
