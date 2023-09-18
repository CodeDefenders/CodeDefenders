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
