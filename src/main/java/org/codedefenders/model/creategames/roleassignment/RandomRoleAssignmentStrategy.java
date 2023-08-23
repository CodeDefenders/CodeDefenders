package org.codedefenders.model.creategames.roleassignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class RandomRoleAssignmentStrategy extends RoleAssignmentStrategy {
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
