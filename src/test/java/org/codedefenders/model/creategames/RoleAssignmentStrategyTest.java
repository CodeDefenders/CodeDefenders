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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.codedefenders.beans.creategames.CreateGamesBean;
import org.codedefenders.game.Role;
import org.codedefenders.model.creategames.roleassignment.OppositeRoleAssignmentStrategy;
import org.codedefenders.model.creategames.roleassignment.RandomRoleAssignmentStrategy;
import org.codedefenders.model.creategames.roleassignment.RoleAssignmentStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import com.google.common.collect.Sets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class RoleAssignmentStrategyTest {
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

    private static Set<Integer> set(int... ids) {
        Set<Integer> userIds = new HashSet<>();
        for (int i : ids) {
            userIds.add(i);
        }
        return userIds;
    }

    private void testAssignRoles(RoleAssignmentStrategy roleAssignment,
                                 Set<Integer> users, Set<Integer> attackers, Set<Integer> defenders,
                                 int attackersPerGame, int defendersPerGame,
                                 int expectedNumAttackers, int expectedNumDefenders,
                                 Set<Integer> expectedAttackers, Set<Integer> expectedDefenders) {
        int numUsers = users.size() + attackers.size() + defenders.size();
        Set<Integer> usersBefore = new HashSet<>(users);
        Set<Integer> attackersBefore = new HashSet<>(attackers);
        Set<Integer> defendersBefore = new HashSet<>(defenders);

        roleAssignment.assignRoles(users, attackersPerGame, defendersPerGame, attackers, defenders);

        assertThat(attackers, hasSize(expectedNumAttackers));
        assertThat(defenders, hasSize(expectedNumDefenders));
        assertThat(Sets.union(Sets.union(attackers, defenders), users), hasSize(numUsers));

        assertThat(users, equalTo(usersBefore));
        assertThat(attackers.containsAll(attackersBefore), is(true));
        assertThat(defenders.containsAll(defendersBefore), is(true));

        assertThat(attackers.containsAll(expectedAttackers), is(true));
        assertThat(defenders.containsAll(expectedDefenders), is(true));
    }

    @ParameterizedTest(name = "Random role assignment: users: {0}, attackers: {1}, defenders: {2}, attackersPerGame: {3}, defendersPerGame: {4}")
    @ArgumentsSource(RandomRoleParameters.class)
    public void testAssignRoles_Random(Set<Integer> users, Set<Integer> attackers, Set<Integer> defenders,
                                        int attackersPerGame, int defendersPerGame,
                                        int expectedNumAttackers, int expectedNumDefenders) {
        RoleAssignmentStrategy roleAssignment = new RandomRoleAssignmentStrategy();
        testAssignRoles(roleAssignment, users, attackers, defenders, attackersPerGame, defendersPerGame,
                expectedNumAttackers, expectedNumDefenders, new HashSet<>(), new HashSet<>());
    }

    @ParameterizedTest(name = "Opposite role assignment: users: {0}, attackers: {1}, defenders: {2}, attackersPerGame: {3}, defendersPerGame: {4}")
    @ArgumentsSource(OppositeRoleParameters.class)
    public void testAssignRoles_Opposite(Set<Integer> users, Set<Integer> attackers, Set<Integer> defenders,
                                          int attackersPerGame, int defendersPerGame,
                                          int expectedNumAttackers, int expectedNumDefenders,
                                          Set<Integer> expectedAttackers, Set<Integer> expectedDefenders) {
        RoleAssignmentStrategy roleAssignment = new OppositeRoleAssignmentStrategy(
                userId -> userInfos.get(userId).getLastRole(),
                new RandomRoleAssignmentStrategy()
        );
        testAssignRoles(roleAssignment, users, attackers, defenders, attackersPerGame, defendersPerGame,
                expectedNumAttackers, expectedNumDefenders, expectedAttackers, expectedDefenders);
    }

    private static class RandomRoleParameters implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
            return Stream.of(
                    /* ===== Without already assigned users ===== */

                    /* Even teams, no remaining users. */
                    Arguments.of(set(1, 2, 3, 4, 5, 6, 7, 8), set(), set(), 1, 1, 4, 4),

                    /* Even teams, no remaining users. */
                    Arguments.of(set(1, 2, 3, 4, 5, 6, 7, 8), set(), set(), 2, 2, 4, 4),

                    /* Even teams, remaining users. */
                    Arguments.of(set(1, 2, 3, 4, 5, 6, 7, 8), set(), set(), 3, 3, 4, 4),

                    /* Even teams, too large team sizes. */
                    Arguments.of(set(1, 2, 3, 4, 5, 6, 7, 8), set(), set(), 10, 10, 4, 4),

                    /* Uneven teams, no remaining users. */
                    Arguments.of(set(1, 2, 3, 4, 5, 6, 7, 8), set(), set(), 1, 3, 2, 6),

                    /* Uneven teams, too large team sizes. */
                    Arguments.of(set(1, 2, 3, 4, 5, 6, 7, 8), set(), set(), 3, 9, 2, 6),

                    /* Uneven teams, remaining users. */
                    Arguments.of(set(1, 2, 3, 4, 5, 6, 7, 8), set(), set(), 1, 2, 3, 5),

                    /* Uneven teams, remaining users. */
                    Arguments.of(set(1, 2, 3, 4, 5, 6, 7, 8), set(), set(), 2, 1, 5, 3),

                    /* One team size 0. */
                    Arguments.of(set(1, 2, 3, 4, 5, 6, 7, 8), set(), set(), 0, 1, 0, 8),

                    /* One team size 0. */
                    Arguments.of(set(1, 2, 3, 4, 5, 6, 7, 8), set(), set(), 1, 0, 8, 0),

                    /* One team size 0, too large other team. */
                    Arguments.of(set(1, 2, 3, 4, 5, 6, 7, 8), set(), set(), 10, 0, 8, 0),

                    /* Uneven number of users, even teams. */
                    Arguments.of(set(1, 2, 3, 4, 5, 6, 7), set(), set(), 1, 1, 4, 3),

                    /* Uneven number of users, uneven teams, remaining users. */
                    Arguments.of(set(1, 2, 3, 4, 5, 6, 7), set(), set(), 1, 2, 2, 5),

                    /* Uneven number of users, uneven teams, no remaining users. */
                    Arguments.of(set(1, 2, 3, 4, 5, 6, 7), set(), set(), 3, 4, 3, 4),

                    /* 0 users. */
                    Arguments.of(set(), set(), set(), 1, 1, 0, 0),

                    /* ===== With already assigned users ===== */

                    /* Even teams, expected team distribution possible. */
                    Arguments.of(set(1, 2, 3, 4), set(5, 6), set(7, 8), 1, 1, 4, 4),

                    /* Uneven teams, expected team distribution possible. */
                    Arguments.of(set(1, 2, 3, 4), set(5, 6), set(7, 8), 1, 2, 3, 5),

                    /* Even teams, expected team distribution not possible. */
                    Arguments.of(set(1, 2, 3), set(4, 5, 6, 7, 8), set(), 1, 1, 5, 3),

                    /* Even teams, expected team distribution not possible. */
                    Arguments.of(set(1, 2), set(), set(3, 4, 5, 6, 7, 8), 1, 1, 2, 6),

                    /* Uneven teams, expected team distribution not possible. */
                    Arguments.of(set(1, 2, 3), set(4, 5, 6), set(7, 8), 1, 3, 3, 5),

                    /* Uneven number of users, uneven teams, expected team distribution not possible. */
                    Arguments.of(set(1, 2), set(4, 5, 6), set(7, 8), 1, 3, 3, 4),

                    /* All users already assigned. */
                    Arguments.of(set(), set(1, 2, 3, 4), set(5, 6, 7, 8), 1, 3, 4, 4)
            );
        }
    }

    private static class OppositeRoleParameters implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
            /*
             * UserId | Prev. Role
             * -------+-----------
             * 1      | ATTACKER
             * 2      | ATTACKER
             * 3      | ATTACKER
             * 4      | ATTACKER
             * 5      | PLAYER
             * 6      | PLAYER
             * 7      | DEFENDER
             * 8      | DEFENDER
             */

            return Stream.of(
                    /* Even teams, no remaining users, all roles. */
                    Arguments.of(set(1, 2, 3, 4, 5, 6, 7, 8), set(), set(), 1, 1, 4, 4, set(5, 6, 7, 8), set(1, 2, 3, 4)),

                    /* Uneven teams, no remaining users, all roles. */
                    Arguments.of(set(1, 2, 3, 4, 5, 6, 7, 8), set(), set(), 3, 1, 4, 4, set(5, 6, 7, 8), set(1, 2, 3, 4)),

                    /* Uneven teams, remaining users, all roles. */
                    Arguments.of(set(1, 2, 3, 4, 5, 6, 7, 8), set(), set(), 1, 2, 3, 5, set(7, 8), set(1, 2, 3, 4)),

                    /* Even teams, no remaining users, no previous roles (PLAYER). */
                    Arguments.of(set(5, 6), set(), set(), 1, 1, 1, 1, set(), set()),

                    /* Even teams, no remaining users, only attackers. */
                    Arguments.of(set(1, 2, 3, 4), set(), set(), 1, 1, 0, 4, set(), set(1, 2, 3, 4)),

                    /* Even teams, no remaining users, only attackers, already assigned attackers. */
                    Arguments.of(set(3, 4), set(1, 2), set(), 1, 1, 2, 2, set(1, 2), set(3, 4)),

                    /* All users already assigned. */
                    Arguments.of(set(), set(1, 2, 3, 4), set(5, 6, 7, 8), 0, 1, 4, 4, set(), set()),

                    /* 0 users. */
                    Arguments.of(set(), set(), set(), 1, 1, 0, 0, set(), set())
            );
        }
    }
}
