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
import java.util.Objects;

/** Selects the current fixture from an append-only series using fixed UTC intervals. */
public class MutleRotation {
    private final Instant start;
    private final Duration period;
    private final Clock clock;

    public MutleRotation(Instant start, Duration period, Clock clock) {
        this.start = Objects.requireNonNull(start);
        this.period = Objects.requireNonNull(period);
        this.clock = Objects.requireNonNull(clock);
        if (period.isZero() || period.isNegative()) {
            throw new IllegalArgumentException("Mutle rotation period must be positive.");
        }
    }

    public long currentPeriod() {
        Duration elapsed = Duration.between(start, clock.instant());
        if (elapsed.isNegative()) {
            return 0;
        }
        return elapsed.dividedBy(period);
    }

    public int currentIndex(int fixtureCount) {
        if (fixtureCount <= 0) {
            throw new IllegalArgumentException("Mutle requires at least one fixture.");
        }
        long period = currentPeriod();
        if (period >= fixtureCount) {
            throw new IllegalStateException("Mutle fixture schedule is exhausted at period " + period
                    + "; add fixtures before this period starts.");
        }
        return Math.toIntExact(period);
    }

    public Instant currentPeriodStart() {
        return start.plus(period.multipliedBy(currentPeriod()));
    }

    public Instant nextChange() {
        return currentPeriodStart().plus(period);
    }
}
