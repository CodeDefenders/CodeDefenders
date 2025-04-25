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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.codedefenders.game.Role;

public class OppositeRoleAssignmentStrategy extends RoleAssignmentStrategy {
    private final RoleAssignmentStrategy remaining;
    private final Function<Integer, Role> getLastRole;

    public OppositeRoleAssignmentStrategy(Function<Integer, Role> getLastRole, RoleAssignmentStrategy remaining) {
        this.getLastRole = getLastRole;
        this.remaining = remaining;
    }

    /**
     * Assigns roles for staged games by assigning players to the opposite of their last played role.
     * Users with not last played role are assigned roles randomly, trying to balance the team sizes out.
     */
    @Override
    public void assignRoles(Collection<Integer> userIds,
                            int attackersPerGame, int defendersPerGame,
                            Collection<Integer> attackers, Collection<Integer> defenders) {
        Set<Integer> remainingUsers = new HashSet<>();

        for (int userId : userIds) {
            Role lastRole = getLastRole.apply(userId);
            if (lastRole == Role.ATTACKER) {
                defenders.add(userId);
            } else if (lastRole == Role.DEFENDER) {
                attackers.add(userId);
            } else {
                remainingUsers.add(userId);
            }
        }

        /* Assign remaining users (that were neither attacker nor defender in their last game). */
        remaining.assignRoles(remainingUsers, attackersPerGame, defendersPerGame, attackers, defenders);
    }
}
