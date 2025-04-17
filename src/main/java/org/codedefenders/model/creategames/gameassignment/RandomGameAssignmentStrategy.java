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
