package org.codedefenders.model.creategames.gameassignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class RandomGameAssignmentStrategy extends GameAssignmentStrategy {
    /**
     * Splits players into random teams.
     * If the users cannot be assigned to games evently, some games are assigned an extra user.
     */
    @Override
    public List<List<Integer>> assignGames(Collection<Integer> userIds, int numGames) {
        List<Integer> list = new ArrayList<>(userIds);
        Collections.shuffle(list);
        return splitSortedList(list, numGames);
    }
}
