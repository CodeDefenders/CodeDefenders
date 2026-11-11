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
package org.codedefenders.arcade.mutle.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xnap.commons.i18n.I18n;

import static org.codedefenders.util.Constants.MUTLE_GUEST_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MutleGuestSessionTest {
    private MutleService service;
    private MutleGuestSession first;
    private MutleGuestSession second;
    private final I18n english = mock(I18n.class);
    private static final String ADDRESS = "127.0.0.1";

    @BeforeEach
    void setUp() {
        service = mock(MutleService.class);
        MutleGuestLimits limits = new MutleGuestLimits();
        first = new MutleGuestSession(service, limits);
        second = new MutleGuestSession(service, limits);
        when(service.currentFixture()).thenReturn(new MutleFixtureView("example/one", 1, "Example"));
        when(service.startGuest(any())).thenReturn(state(10, 1), state(11, 1));
        when(service.state(MUTLE_GUEST_USER_ID, 10)).thenReturn(state(10, 1));
        when(service.state(MUTLE_GUEST_USER_ID, 11)).thenReturn(state(11, 1));
    }

    @Test
    void overviewDoesNotCreateGuestGames() {
        assertFalse(first.current().isPresent());
        verify(service, never()).startGuest(any());
    }

    @Test
    void sessionsHaveIndependentGamesAndResumeWithoutResettingAttempts() {
        assertEquals(10, first.start(ADDRESS).gameId());
        assertEquals(11, second.start(ADDRESS).gameId());
        assertEquals(10, first.start(ADDRESS).gameId());
        assertEquals(10, first.state(null).gameId());
    }

    @Test
    void cannotReadOrSubmitAnotherSessionsGame() throws IOException {
        first.start(ADDRESS);
        second.start(ADDRESS);
        assertThrows(MutleService.NotFoundException.class, () -> second.state(10));
        assertThrows(MutleService.NotFoundException.class, () -> second.submit(10, "source", english, ADDRESS));
        verify(service, never()).submit(MUTLE_GUEST_USER_ID, 10, "source", english);
    }

    @Test
    void newPublicationStartsANewGuestGameEvenForTheSameFixture() {
        first.start(ADDRESS);
        when(service.currentFixture()).thenReturn(new MutleFixtureView("example/one", 2, "Example"));
        when(service.startGuest(any())).thenReturn(state(12, 2));
        assertFalse(first.current().isPresent());
        assertEquals(12, first.start(ADDRESS).gameId());
        assertEquals(10, first.state(10).gameId());
    }

    @Test
    void unavailablePublicationDoesNotCreateGame() {
        when(service.currentFixture()).thenReturn(null);
        assertThrows(MutleService.NotFoundException.class, () -> first.start(ADDRESS));
        assertThrows(MutleService.NotFoundException.class, () -> first.state(null));
        verify(service, never()).startGuest(any());
    }

    @Test
    void publicationChangeDuringCreationDoesNotStoreTheNextCaseUnderThePreviousKey() {
        MutleFixtureView oldCase = new MutleFixtureView("example/one", 1, "Example");
        MutleFixtureView newCase = new MutleFixtureView("example/two", 2, "Next");
        when(service.startGuest(oldCase)).thenAnswer(invocation -> {
            when(service.currentFixture()).thenReturn(newCase);
            return state(10, 1);
        });
        when(service.startGuest(newCase)).thenReturn(state(12, 2));
        when(service.state(MUTLE_GUEST_USER_ID, 12)).thenReturn(state(12, 2));

        assertEquals(1, first.start(ADDRESS).series());
        assertEquals(12, first.start(ADDRESS).gameId());
        assertEquals(12, first.start(ADDRESS).gameId());
        verify(service, times(1)).startGuest(oldCase);
        verify(service, times(1)).startGuest(newCase);
    }

    @Test
    void passesEachVisitorsLocaleWithoutChangingTheSharedAccount() throws IOException {
        I18n german = mock(I18n.class);
        first.start(ADDRESS);
        second.start(ADDRESS);
        first.submit(10, "english source", english, ADDRESS);
        second.submit(11, "german source", german, ADDRESS);
        verify(service).submit(MUTLE_GUEST_USER_ID, 10, "english source", english);
        verify(service).submit(MUTLE_GUEST_USER_ID, 11, "german source", german);
    }

    @Test
    void rejectedTestsStillConsumeTheExecutionBudget() throws IOException {
        first.start(ADDRESS);
        when(service.submit(MUTLE_GUEST_USER_ID, 10, "source", english))
                .thenThrow(new MutleService.RejectedSubmissionException("rejected"));
        for (int attempt = 0; attempt < MutleGuestLimits.MAX_SUBMISSIONS_PER_GAME; attempt++) {
            assertThrows(MutleService.RejectedSubmissionException.class,
                    () -> first.submit(10, "source", english, ADDRESS));
        }
        assertThrows(MutleGuestLimits.RateLimitException.class, () -> first.submit(10, "source", english, ADDRESS));
        verify(service, times(MutleGuestLimits.MAX_SUBMISSIONS_PER_GAME))
                .submit(MUTLE_GUEST_USER_ID, 10, "source", english);
    }

    @Test
    void runningSubmissionDoesNotBlockReadsAndDuplicateSubmissionsFailImmediately() throws Exception {
        first.start(ADDRESS);
        second.start(ADDRESS);
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(service.submit(MUTLE_GUEST_USER_ID, 10, "source", english)).thenAnswer(invocation -> {
            running.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return state(10, 1);
        });
        var executor = Executors.newFixedThreadPool(2);
        try {
            var submission = executor.submit(() -> first.submit(10, "source", english, ADDRESS));
            assertTrue(running.await(5, TimeUnit.SECONDS));
            assertEquals(10, executor.submit(() -> first.current().orElseThrow().gameId()).get(1, TimeUnit.SECONDS));
            assertEquals(11, second.current().orElseThrow().gameId());
            executor.submit(() -> assertThrows(MutleService.SubmissionInProgressException.class,
                    () -> first.submit(10, "source", english, ADDRESS))).get(1, TimeUnit.SECONDS);
            release.countDown();
            assertEquals(10, submission.get(5, TimeUnit.SECONDS).gameId());
            assertEquals(10, first.submit(10, "source", english, ADDRESS).gameId());
            verify(service, times(2)).submit(MUTLE_GUEST_USER_ID, 10, "source", english);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private static MutleStateView state(int gameId, int series) {
        return new MutleStateView(gameId, series, "Example", "Specification", "Source",
                0, 6, false, false, false, "Template", List.of(), null);
    }
}
