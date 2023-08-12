package org.codedefenders.model.creategames.roleassignment;

import java.util.Collection;

public class MeleeRoleAssignment extends RoleAssignment {
    @Override
    public void assignRoles(Collection<Integer> userIds, int attackersPerGame, int defendersPerGame,
                            Collection<Integer> attackers, Collection<Integer> defenders) {
        attackers.addAll(userIds);
    }
}
