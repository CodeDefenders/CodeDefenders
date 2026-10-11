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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MutleGuestLimitsTest {
    @Test
    void newCookiesDoNotResetAddressQuotaAndQuotaResetsAfterAnHour() {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(Instant.EPOCH);
        MutleGuestLimits limits = new MutleGuestLimits(clock);
        for (int cookie = 0; cookie < 40; cookie++) {
            limits.creation("192.0.2.1");
        }
        assertThrows(MutleGuestLimits.RateLimitException.class, () -> limits.creation("192.0.2.1"));
        assertDoesNotThrow(() -> limits.creation("192.0.2.2"));
        when(clock.instant()).thenReturn(Instant.EPOCH.plus(Duration.ofHours(1)));
        assertDoesNotThrow(() -> limits.creation("192.0.2.1"));
    }

    @Test
    void addressChurnCannotBypassGlobalQuota() {
        MutleGuestLimits limits = new MutleGuestLimits(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        for (int address = 0; address < 300; address++) {
            limits.creation("client-" + address);
        }
        assertThrows(MutleGuestLimits.RateLimitException.class, () -> limits.creation("another-client"));
    }

    @Test
    void boundsRequestsBeforeSessionCreationAndBoundsSubmissionsAcrossAddresses() {
        MutleGuestLimits limits = new MutleGuestLimits();
        for (int request = 0; request < 240; request++) {
            limits.request("client");
        }
        assertThrows(MutleGuestLimits.RateLimitException.class, () -> limits.request("client"));
        for (int request = 240; request < 3_000; request++) {
            limits.request("other-" + request);
        }
        assertThrows(MutleGuestLimits.RateLimitException.class, () -> limits.request("new-client"));
        for (int submission = 0; submission < 1_200; submission++) {
            limits.submission("submission-client-" + submission / 120, "source");
        }
        assertThrows(MutleGuestLimits.RateLimitException.class, () -> limits.submission("new-client", "source"));
    }

    @Test
    void sourceSizeAndSubmissionQuotasAreIndependentOfAcceptedGuesses() {
        MutleGuestLimits limits = new MutleGuestLimits();
        assertThrows(MutleGuestLimits.RequestTooLargeException.class,
                () -> limits.submission("client", "a".repeat(MutleGuestLimits.MAX_SOURCE_CHARACTERS + 1)));
        for (int attempt = 0; attempt < 120; attempt++) {
            limits.submission("client", "source");
        }
        assertThrows(MutleGuestLimits.RateLimitException.class, () -> limits.submission("client", "source"));
    }

    @Test
    void onlyTwoGuestOperationsCanRunAndFailedOperationsReleaseTheirPermit() {
        MutleGuestLimits limits = new MutleGuestLimits();
        try (var first = limits.acquire(); var second = limits.acquire()) {
            assertThrows(MutleGuestLimits.BusyException.class, limits::acquire);
        }
        try (var permit = limits.acquire()) {
            assertDoesNotThrow(() -> { });
        }
    }
}
