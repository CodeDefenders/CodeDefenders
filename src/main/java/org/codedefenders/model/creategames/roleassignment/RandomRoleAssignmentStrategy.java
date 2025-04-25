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
package org.codedefenders.model.creategames.roleassignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class RandomRoleAssignmentStrategy extends RoleAssignmentStrategy {
    /**
     * Assigns roles for staged games by assigning players to the opposite of their last played role.
     * Users with not last played role are assigned roles randomly, trying to balance the team sizes out.
     */
    @Override
    public void assignRoles(Collection<Integer> userIds, int attackersPerGame, int defendersPerGame,
                            Collection<Integer> attackers, Collection<Integer> defenders) {
        /* Calculate the number of attackers to assign, while taking into account how users have previously been
         * distributed. (This method can be called with non-empty attackers and defenders sets containing
         * already assigned users.) */
        int numUsers = userIds.size() + attackers.size() + defenders.size();
        int numAttackers = (int) Math.round(
                numUsers * ((double) attackersPerGame / (attackersPerGame + defendersPerGame)));
        int remainingNumAttackers = Math.max(0, numAttackers - attackers.size());
        remainingNumAttackers = Math.min(remainingNumAttackers, userIds.size());

        List<Integer> shuffledUsers = new ArrayList<>(userIds);
        Collections.shuffle(shuffledUsers);
        for (int i = 0; i < remainingNumAttackers; i++) {
            attackers.add(shuffledUsers.get(i));
        }
        for (int i = remainingNumAttackers; i < shuffledUsers.size(); i++) {
            defenders.add(shuffledUsers.get(i));
        }
    }
}
