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
package org.codedefenders.model.creategames.gameassignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>Base class for game assignment strategies.
 * <p>Game assignment strategies are used for assigning lists of players to staged games.
 */
public abstract class GameAssignmentStrategy {
    public abstract List<List<Integer>> assignGames(Collection<Integer> userIds, int numGames);

    /**
     * Utility method to split a user list into evenly-sized games while respecting the order of the list.
     * This means the first couple users get put into the first game, the next couple into the second, and so on.
     * If the users cannot be split into teams evenly, the size of some teams is increased by 1 to fit the remaining
     * users into teams.
     *
     * @param userIds The sorted list of users.
     * @param numGames The number of games to produce.
     * @return The game assignments.
     */
    protected List<List<Integer>> splitSortedList(List<Integer> userIds, int numGames) {
        int numUsersPerTeam = userIds.size() / numGames;
        int numRemainingUsers = userIds.size() % numGames;

        List<List<Integer>> games = new ArrayList<>();

        int index = 0;
        for (int i = 0; i < numGames; i++) {
            List<Integer> subList;
            if (i < numRemainingUsers) {
                subList = userIds.subList(index, index + numUsersPerTeam + 1);
                index += numUsersPerTeam + 1;
            } else {
                subList = userIds.subList(index, index + numUsersPerTeam);
                index += numUsersPerTeam;
            }
            games.add(new ArrayList<>(subList));
        }

        return games;
    }

    public enum Type {
        /**
         * Teams are assigned randomly.
         */
        RANDOM,

        /**
         * Teams are assigned based on the total score of users,
         * putting users with similar total scores in the same team.
         */
        SCORE_DESCENDING
    }
}
