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
package org.codedefenders.model.creategames;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.codedefenders.beans.creategames.CreateGamesBean;
import org.codedefenders.game.Role;
import org.codedefenders.model.creategames.gameassignment.GameAssignmentStrategy;
import org.codedefenders.model.creategames.gameassignment.RandomGameAssignmentStrategy;
import org.codedefenders.model.creategames.gameassignment.SortedGameAssignmentStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

public class GameAssignmentStrategyTest {
    private HashMap<Integer, CreateGamesBean.UserInfo> userInfos;

    @BeforeEach
    public void initializeData() {
        userInfos = new HashMap<>();
        userInfos.put(1, new CreateGamesBean.UserInfo(1, "userA", "userA@email.com", null, Role.ATTACKER, 1));
        userInfos.put(2, new CreateGamesBean.UserInfo(2, "userB", "userB@email.com", null, Role.ATTACKER, 2));
        userInfos.put(3, new CreateGamesBean.UserInfo(3, "userC", "userC@email.com", null, Role.ATTACKER, 3));
        userInfos.put(4, new CreateGamesBean.UserInfo(4, "userD", "userD@email.com", null, Role.ATTACKER, 4));
        userInfos.put(5, new CreateGamesBean.UserInfo(5, "userE", "userE@email.com", null, Role.PLAYER, 5));
        userInfos.put(6, new CreateGamesBean.UserInfo(6, "userF", "userF@email.com", null, Role.PLAYER, 6));
        userInfos.put(7, new CreateGamesBean.UserInfo(7, "userG", "userG@email.com", null, Role.DEFENDER, 7));
        userInfos.put(8, new CreateGamesBean.UserInfo(8, "userH", "userH@email.com", null, Role.DEFENDER, 8));
    }

    private Set<Integer> set(int... ids) {
        Set<Integer> userIds = new HashSet<>();
        for (int i : ids) {
            userIds.add(i);
        }
        return userIds;
    }

    @Test
    public void testSplitIntoTeams_Random() {
        GameAssignmentStrategy gameAssignment = new RandomGameAssignmentStrategy();

        /* No remaining users. */
        List<List<Integer>> teams = gameAssignment.assignGames(set(1, 2, 3, 4, 5, 6, 7, 8), 2);
        assertThat(teams, hasSize(2));
        assertThat(teams.get(0), hasSize(4));
        assertThat(teams.get(1), hasSize(4));
        assertThat(teams.stream().flatMap(List::stream).collect(Collectors.toList()), hasSize(8));

        /* With remaining users. */
        teams = gameAssignment.assignGames(set(1, 2, 3, 4, 5, 6, 7, 8), 3);
        assertThat(teams, hasSize(3));
        assertThat(teams.get(0), hasSize(3));
        assertThat(teams.get(1), hasSize(3));
        assertThat(teams.get(2), hasSize(2));
        assertThat(teams.stream().flatMap(List::stream).collect(Collectors.toList()), hasSize(8));

        /* 0 users. */
        teams = gameAssignment.assignGames(set(), 1);
        assertThat(teams, hasSize(1));
        assertThat(teams.get(0), hasSize(0));
    }

    @Test
    public void testSplitIntoTeams_Score() {
        GameAssignmentStrategy gameAssignment = new SortedGameAssignmentStrategy(
                Comparator.comparingInt((Integer userId) -> userInfos.get(userId).getTotalScore()).reversed());

        /*
         * UserId | Tot. Score
         * -------+-----------
         * 1      | 1
         * 2      | 2
         * 3      | 3
         * 4      | 4
         * 5      | 5
         * 6      | 6
         * 7      | 7
         * 8      | 8
         */

        /* No remaining users. */
        List<List<Integer>> teams = gameAssignment.assignGames(set(1, 2, 3, 4, 5, 6, 7, 8), 2);
        assertThat(teams, hasSize(2));
        assertThat(teams.get(0), hasSize(4));
        assertThat(teams.get(1), hasSize(4));
        assertThat(teams.stream().flatMap(List::stream).collect(Collectors.toList()), hasSize(8));
        assertThat(teams.get(0), containsInAnyOrder(set(8, 7, 6, 5).toArray()));
        assertThat(teams.get(1), containsInAnyOrder(set(4, 3, 2, 1).toArray()));

        /* With remaining users. */
        teams = gameAssignment.assignGames(set(1, 2, 3, 4, 5, 6, 7, 8), 3);
        assertThat(teams, hasSize(3));
        assertThat(teams.get(0), hasSize(3));
        assertThat(teams.get(1), hasSize(3));
        assertThat(teams.get(2), hasSize(2));
        assertThat(teams.stream().flatMap(List::stream).collect(Collectors.toList()), hasSize(8));
        assertThat(teams.get(0), containsInAnyOrder(set(8, 7, 6).toArray()));
        assertThat(teams.get(1), containsInAnyOrder(set(5, 4, 3).toArray()));
        assertThat(teams.get(2), containsInAnyOrder(set(2, 1).toArray()));

        /* 0 users. */
        teams = gameAssignment.assignGames(set(), 1);
        assertThat(teams, hasSize(1));
        assertThat(teams.get(0), hasSize(0));
    }
}
