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
import org.apache.commons.lang.StringEscapeUtils;
import org.codedefenders.database.UserDAO;
import org.codedefenders.game.Mutant;
import org.codedefenders.model.User;
import org.codedefenders.util.Constants;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MutantDTO {
    @Expose
    private final int id;
    @Expose
    private final String creatorName;
    @Expose
    private final Mutant.State state;
    @Expose
    private final int points;
    @Expose
    private final String lineString;
    @Expose
    private final Boolean covered;
    @Expose
    private final String killedByName;
    @Expose
    private final boolean canMarkEquivalent;
    @Expose
    private final boolean canView;
    private final List<Integer> lines;
    @Expose
    private final int killedByTestId;
    @Expose
    private final String killMessage;
    @Expose
    private final String description;

    public MutantDTO(Mutant mutant, User user, boolean playerCoverToClaim) {
        id = mutant.getId();
        creatorName = mutant.getCreatorName();
        points = mutant.getScore();
        state = mutant.getState();

        if (playerCoverToClaim) {
            covered = mutant.getCoveringTests().stream()
                    .anyMatch(t -> UserDAO.getUserForPlayer(t.getPlayerId()).getId() == user.getId());
        } else {
            covered = mutant.isCovered();
        }
        description = StringEscapeUtils.escapeJavaScript(mutant.getHTMLReadout()
                .stream()
                .filter(Objects::nonNull).collect(Collectors.joining("<br>")));
        if (mutant.getKillingTest() != null) {
            killedByName = UserDAO.getUserForPlayer(mutant.getKillingTest().getPlayerId()).getUsername();
            killedByTestId = mutant.getKillingTest().getId();
            killMessage = StringEscapeUtils.escapeJavaScript(mutant.getKillMessage());
        } else {
            killedByName = null;
            killedByTestId = -1;
            killMessage = null;
        }


        lines = mutant.getLines();
        lineString = lines.stream().map(String::valueOf).collect(Collectors.joining(","));

        canMarkEquivalent = mutant.getEquivalent().equals(Mutant.Equivalence.ASSUMED_NO)
                && mutant.getCreatorId() != Constants.DUMMY_ATTACKER_USER_ID
                && mutant.getCreatorId() != user.getId()
                && mutant.getLines().size() >= 1;
        canView = state == Mutant.State.KILLED
                || state == Mutant.State.EQUIVALENT
                || mutant.getCreatorId() == user.getId();
    }

    public int getId() {
        return id;
    }
}
