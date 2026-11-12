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
import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;

import org.xnap.commons.i18n.I18n;

import static org.codedefenders.util.Constants.MUTLE_GUEST_USER_ID;

/** Authorizes guest games using server-side session state, never the shared account alone. */
@SessionScoped
public class MutleGuestSession implements Serializable {
    private static final long serialVersionUID = 2L;
    // Lock the actual session state, not a synchronized method copied onto Weld's shared client proxy.
    private final Map<String, GuestGame> games = new LinkedHashMap<>();

    private final MutleService service;
    private final MutleGuestLimits limits;

    @Inject
    public MutleGuestSession(MutleService service, MutleGuestLimits limits) {
        this.service = service;
        this.limits = limits;
    }

    public Optional<MutleStateView> current() {
        Integer gameId;
        synchronized (games) {
            gameId = currentGameId();
        }
        return gameId == null ? Optional.empty() : Optional.of(state(gameId));
    }

    public MutleStateView start(String address) {
        MutleFixtureView fixture = service.currentFixture();
        if (fixture == null) {
            throw new MutleService.NotFoundException();
        }
        int gameId;
        synchronized (games) {
            prune();
            String key = key(fixture);
            GuestGame game = games.get(key);
            if (game == null) {
                limits.creation(address);
                MutleStateView state = service.startGuest(fixture);
                games.put(key, new GuestGame(state.gameId()));
                return state;
            }
            gameId = game.id;
        }
        return state(gameId);
    }

    public MutleStateView state(Integer gameId) {
        int resolved;
        synchronized (games) {
            Integer candidate = gameId == null ? currentGameId() : gameId;
            if (candidate == null || ownedGame(candidate) == null) {
                throw new MutleService.NotFoundException();
            }
            resolved = candidate;
        }
        return service.state(MUTLE_GUEST_USER_ID, resolved);
    }

    public MutleStateView submit(int gameId, String source, I18n i18n, String address) throws IOException {
        GuestGame game;
        synchronized (games) {
            game = ownedGame(gameId);
            if (game == null) {
                throw new MutleService.NotFoundException();
            }
            if (game.submitting) {
                throw new MutleService.SubmissionInProgressException();
            }
            if (game.submissions >= MutleGuestLimits.MAX_SUBMISSIONS_PER_GAME) {
                throw new MutleGuestLimits.GameSubmissionLimitException();
            }
            limits.submission(address, source);
            game.submissions++;
            game.submitting = true;
        }
        try {
            return service.submit(MUTLE_GUEST_USER_ID, gameId, source, i18n);
        } finally {
            synchronized (games) {
                game.submitting = false;
            }
        }
    }

    private GuestGame ownedGame(int gameId) {
        prune();
        return games.values().stream().filter(game -> game.id == gameId).findFirst().orElse(null);
    }

    private Integer currentGameId() {
        prune();
        MutleFixtureView fixture = service.currentFixture();
        GuestGame game = fixture == null ? null : games.get(key(fixture));
        return game == null ? null : game.id;
    }

    private void prune() {
        Instant cutoff = Instant.now().minus(MutleGuestLimits.RETENTION);
        games.values().removeIf(game -> !game.submitting && game.created.isBefore(cutoff));
    }

    private static String key(MutleFixtureView fixture) {
        return fixture.series() + "/" + fixture.fixtureKey();
    }

    private static class GuestGame implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int id;
        private final Instant created = Instant.now();
        private int submissions;
        private transient boolean submitting;

        GuestGame(int id) {
            this.id = id;
        }
    }
}
