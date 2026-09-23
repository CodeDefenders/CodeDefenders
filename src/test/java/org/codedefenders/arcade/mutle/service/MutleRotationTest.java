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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MutleRotationTest {
    private static final Instant START = Instant.parse("2026-01-05T00:00:00Z");

    @Test
    void selectsFirstFixtureBeforeAndAtStart() {
        assertThat(rotationAt(START.minusSeconds(1)).currentIndex(4)).isEqualTo(0);
        assertThat(rotationAt(START).currentIndex(4)).isEqualTo(0);
    }

    @Test
    void changesAtPeriodBoundaryWithoutWrapping() {
        assertThat(rotationAt(START.plus(Duration.ofDays(7))).currentIndex(3)).isEqualTo(1);
        MutleRotation wrapped = rotationAt(START.plus(Duration.ofDays(21)));
        assertThrows(IllegalStateException.class, () -> wrapped.currentIndex(3));
        assertThat(wrapped.currentPeriod()).isEqualTo(3);
    }

    @Test
    void reportsNextUtcChange() {
        MutleRotation rotation = rotationAt(START.plus(Duration.ofDays(8)));

        assertThat(rotation.currentPeriodStart()).isEqualTo(START.plus(Duration.ofDays(7)));
        assertThat(rotation.nextChange()).isEqualTo(START.plus(Duration.ofDays(14)));
    }

    @Test
    void rejectsInvalidPeriodAndEmptyPack() {
        assertThrows(IllegalArgumentException.class,
                () -> new MutleRotation(START, Duration.ZERO, Clock.systemUTC()));
        assertThrows(IllegalArgumentException.class, () -> rotationAt(START).currentIndex(0));
    }

    private static MutleRotation rotationAt(Instant instant) {
        return new MutleRotation(START, Duration.ofDays(7), Clock.fixed(instant, ZoneOffset.UTC));
    }
}
