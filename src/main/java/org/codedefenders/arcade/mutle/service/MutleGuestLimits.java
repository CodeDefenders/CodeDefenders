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
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/** Bounds anonymous work independently of browser cookies. Addresses come from the servlet container. */
@ApplicationScoped
public class MutleGuestLimits {
    public static final int MAX_SOURCE_CHARACTERS = 16_384;
    public static final int MAX_SUBMISSIONS_PER_GAME = 30;
    public static final Duration RETENTION = Duration.ofDays(7);
    public static final int SESSION_TIMEOUT_SECONDS = (int) Duration.ofDays(1).toSeconds();
    private static final Duration WINDOW = Duration.ofHours(1);
    private final Cache<String, Window> clients = CacheBuilder.newBuilder()
            .maximumSize(10_000).expireAfterAccess(1, TimeUnit.HOURS).build();
    private final Semaphore workers = new Semaphore(2);
    private final Clock clock;
    private Window global;

    public MutleGuestLimits() {
        this(Clock.systemUTC());
    }

    MutleGuestLimits(Clock clock) {
        this.clock = clock;
    }

    public Permit acquire() {
        if (!workers.tryAcquire()) {
            throw new BusyException();
        }
        return new Permit();
    }

    public synchronized void request(String address) {
        Window client = window(address);
        if (client.requests >= 240 || global.requests >= 3_000) {
            throw new RateLimitException();
        }
        client.requests++;
        global.requests++;
    }

    public synchronized void creation(String address) {
        consume(address, true);
    }

    public synchronized void submission(String address, String source) {
        if (source.length() > MAX_SOURCE_CHARACTERS) {
            throw new RequestTooLargeException();
        }
        consume(address, false);
    }

    private void consume(String address, boolean creation) {
        Window client = window(address);
        if (creation ? client.creations >= 40 || global.creations >= 300
                : client.submissions >= 120 || global.submissions >= 1_200) {
            throw new RateLimitException();
        }
        if (creation) {
            client.creations++;
            global.creations++;
        } else {
            client.submissions++;
            global.submissions++;
        }
    }

    private Window window(String address) {
        Instant now = clock.instant();
        if (global == null || global.expired(now)) {
            global = new Window(now);
        }
        Window client = clients.getIfPresent(address);
        if (client == null || client.expired(now)) {
            client = new Window(now);
            clients.put(address, client);
        }
        return client;
    }

    public final class Permit implements AutoCloseable {
        private boolean closed;

        private Permit() {
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                workers.release();
            }
        }
    }

    private static final class Window {
        private final Instant started;
        private int creations;
        private int submissions;
        private int requests;

        private Window(Instant started) {
            this.started = started;
        }

        private boolean expired(Instant now) {
            return !now.isBefore(started.plus(WINDOW));
        }
    }

    public static class RateLimitException extends RuntimeException {
    }

    public static class GameSubmissionLimitException extends RateLimitException {
    }

    public static class BusyException extends RuntimeException {
    }

    public static class RequestTooLargeException extends RuntimeException {
    }
}
