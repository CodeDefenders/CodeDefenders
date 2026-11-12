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
package org.codedefenders.execution;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import org.codedefenders.database.EventDAO;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.UserEntity;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.LiveFeedback;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.persistence.database.TestRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MutationExecutionRejectionTest {
    private final BackendExecutorService backend = mock(BackendExecutorService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final EventDAO events = mock(EventDAO.class);
    private final TestRepository tests = mock(TestRepository.class);
    private final MutantRepository mutants = mock(MutantRepository.class);
    private final INotificationService notifications = mock(INotificationService.class);
    private final GameManagingUtils gameManagingUtils = mock(GameManagingUtils.class);
    private final LiveFeedback liveFeedback = mock(LiveFeedback.class);
    private final org.codedefenders.game.Test test = mock(org.codedefenders.game.Test.class);
    private final Mutant mutant = mock(Mutant.class);
    private final AntRunner.ExecutionLimitException rejection = new AntRunner.ExecutionLimitException();

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectedExecutionsCannotKillMutantsOrResolveEquivalence(boolean equivalence) {
        when(backend.testMutant(mutant, test)).thenThrow(rejection);
        MutationTester tester = new MutationTester(backend, users, events, tests, mutants, notifications,
                false, gameManagingUtils, liveFeedback);
        try (var database = mockStatic(TargetExecutionDAO.class)) {
            assertSame(rejection, assertThrows(AntRunner.ExecutionLimitException.class, () -> {
                if (equivalence) tester.runEquivalenceTest(test, mutant);
                else tester.testVsMutant(test, mutant);
            }));
            verifyNoInteractions(events, tests, mutants, notifications, gameManagingUtils, liveFeedback);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void parallelRejectionsPropagateBeforeSurvivalScoresAndEvents(boolean submittingTest) {
        when(test.getPlayerId()).thenReturn(101);
        when(mutant.getPlayerId()).thenReturn(102);
        when(users.getUserIdForPlayerId(anyInt())).thenReturn(Optional.of(1));
        when(users.getUserById(1)).thenReturn(Optional.of(mock(UserEntity.class)));
        when(backend.testMutant(mutant, test)).thenThrow(rejection);
        MultiplayerGame game = mock(MultiplayerGame.class);
        when(game.getAliveMutants()).thenReturn(new ArrayList<>(List.of(mutant)));
        when(game.getMutantsMarkedEquivalentPending()).thenReturn(List.of());
        when(game.getTests(true)).thenReturn(List.of(test));
        ExecutorService executor = mock(ExecutorService.class);
        doAnswer(call -> { ((Runnable) call.getArgument(0)).run(); return null; })
                .when(executor).execute(any(Runnable.class));
        ParallelMutationTester tester = new ParallelMutationTester(backend, users, events, tests, mutants, notifications,
                false, gameManagingUtils, executor, liveFeedback);
        try (var database = mockStatic(TargetExecutionDAO.class)) {
            assertSame(rejection, assertThrows(AntRunner.ExecutionLimitException.class, () -> {
                if (submittingTest) tester.runTestOnAllMultiplayerMutants(game, test);
                else tester.runAllTestsOnMutant(game, mutant, ignored -> new ArrayList<>(List.of(test)));
            }));
            verifyNoInteractions(events, tests, mutants, notifications, gameManagingUtils, liveFeedback);
        }
    }
}
