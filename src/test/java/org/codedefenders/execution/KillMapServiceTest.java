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
import java.util.concurrent.ExecutorService;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.execution.KillMap.KillMapEntry;
import org.codedefenders.game.Mutant;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.persistence.database.TestRepository;
import org.codedefenders.util.concurrent.ExecutorServiceProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KillMapServiceTest {
    private final BackendExecutorService backend = mock(BackendExecutorService.class);
    private final Configuration config = mock(Configuration.class);
    private final ExecutorService executor = mock(ExecutorService.class);
    private final ExecutorServiceProvider executors = mock(ExecutorServiceProvider.class);

    private KillMapService service() {
        when(executors.createExecutorService(anyString(), anyInt())).thenReturn(executor);
        doAnswer(call -> { ((Runnable) call.getArgument(0)).run(); return null; })
                .when(executor).execute(any(Runnable.class));
        return new KillMapService(backend, executors, config, mock(TestRepository.class), mock(MutantRepository.class));
    }

    @Test
    void rejectedPairsAreRecordedAsErrorsWithoutAbortingOtherPairs() throws Exception {
        var test = mock(org.codedefenders.game.Test.class);
        when(test.getId()).thenReturn(71);
        Mutant rejected = mutant(61);
        Mutant killed = mutant(62);
        Mutant survived = mutant(63);
        when(backend.testMutant(rejected, test)).thenThrow(new AntRunner.ExecutionLimitException());
        when(backend.testMutant(killed, test)).thenReturn(
                new TargetExecution(71, 62, TargetExecution.Target.TEST_MUTANT, TargetExecution.Status.FAIL, null));
        when(backend.testMutant(survived, test)).thenReturn(
                new TargetExecution(71, 63, TargetExecution.Target.TEST_MUTANT, TargetExecution.Status.SUCCESS, null));
        KillMap map = new KillMap(List.of(test), List.of(rejected, killed, survived), 100, new ArrayList<>());
        try (var database = mockStatic(KillmapDAO.class)) {
            database.when(() -> KillmapDAO.insertKillMapEntry(any(KillMapEntry.class), eq(100))).thenReturn(true);
            assertSame(map, service().compute(map, executor));
            assertEquals(List.of(KillMapEntry.Status.ERROR, KillMapEntry.Status.KILL, KillMapEntry.Status.NO_KILL),
                    map.getEntries().stream().map(entry -> entry.status).toList());
            database.verify(() -> KillmapDAO.insertKillMapEntry(
                    argThat(entry -> entry.mutant == rejected && entry.status == KillMapEntry.Status.ERROR), eq(100)));
            verify(backend).testMutant(survived, test);
        }
    }

    @Test
    void interruptionDoesNotBecomeAPersistedPairError() throws Exception {
        var test = mock(org.codedefenders.game.Test.class);
        Mutant mutant = mutant(61);
        when(backend.testMutant(mutant, test)).thenAnswer(call -> {
            Thread.currentThread().interrupt();
            throw new AntRunner.ExecutionLimitException(new InterruptedException());
        });
        KillMap map = new KillMap(List.of(test), List.of(mutant), 100, new ArrayList<>());
        try (var database = mockStatic(KillmapDAO.class)) {
            assertThrows(InterruptedException.class, () -> service().compute(map, executor));
            assertTrue(Thread.currentThread().isInterrupted());
            database.verifyNoInteractions();
            assertTrue(map.getEntries().isEmpty());
        } finally {
            Thread.interrupted();
        }
    }

    private Mutant mutant(int id) {
        Mutant mutant = mock(Mutant.class);
        when(mutant.getId()).thenReturn(id);
        return mutant;
    }
}
