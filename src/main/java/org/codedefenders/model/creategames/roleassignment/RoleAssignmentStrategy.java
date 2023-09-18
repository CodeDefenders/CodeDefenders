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
