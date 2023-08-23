package org.codedefenders.model.creategames.gameassignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class SortedGameAssignmentStrategy extends GameAssignmentStrategy {
    private final Comparator<Integer> comparator;

    public SortedGameAssignmentStrategy(Comparator<Integer> comparator) {
        this.comparator = comparator;
    }

    /**
     * Ranks users according to the given comparator.
     * Then splits them into teams, putting users with similar values into similar games.
     * If the users cannot be assigned to games evently, some games are assigned an extra user.
     */
    @Override
    public List<List<Integer>> assignGames(Collection<Integer> userIds, int numGames) {
        List<Integer> list = new ArrayList<>(userIds);
        list.sort(comparator);
        return splitSortedList(list, numGames);
    }
}
