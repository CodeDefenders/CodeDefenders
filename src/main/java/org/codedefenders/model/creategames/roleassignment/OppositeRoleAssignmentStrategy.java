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
