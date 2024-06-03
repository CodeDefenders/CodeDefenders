/*
 * Copyright (C) 2023 Code Defenders contributors
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

package org.codedefenders.cron;

import java.util.concurrent.TimeUnit;

import jakarta.annotation.Nonnull;

public abstract class FixedDelayCronJob implements Runnable {

    protected final int initialDelay;
    protected final int executionDelay;
    protected final TimeUnit timeUnit;

    protected FixedDelayCronJob(int initialDelay, int executionDelay, @Nonnull TimeUnit timeUnit) {
        this.initialDelay = initialDelay;
        this.executionDelay = executionDelay;
        this.timeUnit = timeUnit;
    }

    int getInitialDelay() {
        return initialDelay;
    }

    int getExecutionDelay() {
        return executionDelay;
    }

    @Nonnull
    TimeUnit getTimeUnit() {
        return timeUnit;
    }
}
