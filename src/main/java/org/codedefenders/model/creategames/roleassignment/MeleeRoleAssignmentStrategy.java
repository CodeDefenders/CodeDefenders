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

public class MeleeRoleAssignmentStrategy extends RoleAssignmentStrategy {
    /**
     * Assigns roles for staged melee games by assigning all players to the attacker.
     */
    @Override
    public void assignRoles(Collection<Integer> userIds, int attackersPerGame, int defendersPerGame,
                            Collection<Integer> attackers, Collection<Integer> defenders) {
        attackers.addAll(userIds);
    }
}
