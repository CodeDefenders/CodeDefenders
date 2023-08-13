package org.codedefenders.model.creategames.gameassignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class ScoreGameAssignment extends GameAssignment {
    private final Comparator<Integer> compareScore;

    public ScoreGameAssignment(Comparator<Integer> compareScore) {
        this.compareScore = compareScore;
    }

    @Override
    public List<List<Integer>> assignGames(Collection<Integer> userIds, int numGames) {
        List<Integer> list = new ArrayList<>(userIds);
        list.sort(compareScore);
        return splitSortedList(list, numGames);
    }
}
