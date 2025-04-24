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

/**
 * <p>Base class for role assignment strategies.
 * <p>Role assignment strategies are used for assigning users to roles for staged games.
 */
public abstract class RoleAssignmentStrategy {
    /**
     * Assigns (attacker and defender) roles to a collection of users.
     * The users will be added to the given {@code attackers} and {@code defenders} sets accordingly.
     * The passed {@code attackers} and {@code defenders} sets can be non-empty. In this case, the number of already
     * assigned attackers and defenders will be taken into account.
     */
    public abstract void assignRoles(Collection<Integer> userIds,
                                     int attackersPerGame, int defendersPerGame,
                                     Collection<Integer> attackers, Collection<Integer> defenders);

    public enum Type {
        /**
         * Users are assigned roles randomly, trying to assign the correct number of attackers and defenders.
         */
        RANDOM,

        /**
         * Users are assigned the role opposite of the last role they played as.
         * Users with no last role are assigned via the RANDOM strategy.
         */
        OPPOSITE
    }
}
