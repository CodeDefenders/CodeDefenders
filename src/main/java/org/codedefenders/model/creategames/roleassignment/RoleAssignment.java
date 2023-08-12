package org.codedefenders.model.creategames.roleassignment;

import java.util.Collection;

public abstract class RoleAssignment {
    /**
     * Assigns (attacker and defender) roles to a collection of users.
     * The users will be added to the given {@code attackers} and {@code defenders} sets accordingly.
     * The passed {@code attackers} and {@code defenders} sets can be non-empty. In this case, the number of already
     * assigned attackers and defenders will be taken into account.
     */
    public abstract void assignRoles(Collection<Integer> userIds,
                                     int attackersPerGame, int defendersPerGame,
                                     Collection<Integer> attackers, Collection<Integer> defenders);
}
