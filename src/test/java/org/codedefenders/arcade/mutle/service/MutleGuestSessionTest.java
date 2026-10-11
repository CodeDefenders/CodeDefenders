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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.codedefenders.util.Constants.MUTLE_GUEST_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MutleGuestSessionTest {
    private MutleService service;
    private MutleGuestSession first;
    private MutleGuestSession second;

    @BeforeEach
    void setUp() {
        service = mock(MutleService.class);
        first = new MutleGuestSession(service);
        second = new MutleGuestSession(service);
        when(service.currentFixture()).thenReturn(new MutleFixtureView("example/one", 1, "Example"));
        when(service.startGuest()).thenReturn(state(10, 1), state(11, 1));
        when(service.state(MUTLE_GUEST_USER_ID, 10)).thenReturn(state(10, 1));
        when(service.state(MUTLE_GUEST_USER_ID, 11)).thenReturn(state(11, 1));
    }

    @Test
    void overviewDoesNotCreateGuestGames() {
        assertFalse(first.current().isPresent());
        verify(service, never()).startGuest();
    }

    @Test
    void sessionsHaveIndependentGamesAndResumeWithoutResettingAttempts() {
        assertEquals(10, first.start().gameId());
        assertEquals(11, second.start().gameId());
        assertEquals(10, first.start().gameId());
        assertEquals(10, first.state(null).gameId());
    }

    @Test
    void cannotReadOrSubmitAnotherSessionsGame() throws IOException {
        first.start();
        second.start();
        assertThrows(MutleService.NotFoundException.class, () -> second.state(10));
        assertThrows(MutleService.NotFoundException.class, () -> second.submit(10, "source"));
        verify(service, never()).submit(MUTLE_GUEST_USER_ID, 10, "source");
    }

    @Test
    void newPublicationStartsANewGuestGameEvenForTheSameFixture() {
        first.start();
        when(service.currentFixture()).thenReturn(new MutleFixtureView("example/one", 2, "Example"));
        when(service.startGuest()).thenReturn(state(12, 2));
        assertFalse(first.current().isPresent());
        assertEquals(12, first.start().gameId());
        assertEquals(10, first.state(10).gameId());
    }

    @Test
    void unavailablePublicationDoesNotCreateGame() {
        when(service.currentFixture()).thenReturn(null);
        assertThrows(MutleService.NotFoundException.class, first::start);
        assertThrows(MutleService.NotFoundException.class, () -> first.state(null));
        verify(service, never()).startGuest();
    }

    private static MutleStateView state(int gameId, int series) {
        return new MutleStateView(gameId, series, "Example", "Specification", "Source",
                0, 6, false, false, false, "Template", List.of(), null);
    }
}
