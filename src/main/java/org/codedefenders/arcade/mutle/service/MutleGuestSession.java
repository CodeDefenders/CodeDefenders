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
    private static final long serialVersionUID = 1L;
    private final Map<String, Integer> games = new LinkedHashMap<>();
    private final Map<Integer, Integer> submissions = new LinkedHashMap<>();
    private final Map<Integer, Instant> created = new LinkedHashMap<>();

    @Inject
    private MutleService service;

    @Inject
    private MutleGuestLimits limits;

    public MutleGuestSession() {
    }

    MutleGuestSession(MutleService service, MutleGuestLimits limits) {
        this.service = service;
        this.limits = limits;
    }

    public synchronized Optional<MutleStateView> current() {
        Integer gameId = currentGameId();
        return gameId == null ? Optional.empty() : Optional.of(state(gameId));
    }

    public synchronized MutleStateView start(String address) {
        prune();
        MutleFixtureView fixture = service.currentFixture();
        if (fixture == null) {
            throw new MutleService.NotFoundException();
        }
        String key = key(fixture);
        Integer gameId = games.get(key);
        if (gameId == null) {
            limits.creation(address);
            MutleStateView state = service.startGuest(fixture);
            games.put(key, state.gameId());
            created.put(state.gameId(), Instant.now());
            return state;
        }
        return state(gameId);
    }

    public synchronized MutleStateView state(Integer gameId) {
        Integer resolved = gameId == null ? currentGameId() : gameId;
        if (resolved == null || !owns(resolved)) {
            throw new MutleService.NotFoundException();
        }
        return service.state(MUTLE_GUEST_USER_ID, resolved);
    }

    public synchronized MutleStateView submit(int gameId, String source, I18n i18n, String address) throws IOException {
        if (!owns(gameId)) {
            throw new MutleService.NotFoundException();
        }
        if (submissions.getOrDefault(gameId, 0) >= MutleGuestLimits.MAX_SUBMISSIONS_PER_GAME) {
            throw new MutleGuestLimits.GameSubmissionLimitException();
        }
        limits.submission(address, source);
        submissions.merge(gameId, 1, Integer::sum);
        return service.submit(MUTLE_GUEST_USER_ID, gameId, source, i18n);
    }

    public synchronized boolean owns(int gameId) {
        prune();
        return games.containsValue(gameId);
    }

    private Integer currentGameId() {
        prune();
        MutleFixtureView fixture = service.currentFixture();
        return fixture == null ? null : games.get(key(fixture));
    }

    private void prune() {
        Instant cutoff = Instant.now().minus(MutleGuestLimits.RETENTION);
        games.values().removeIf(id -> created.getOrDefault(id, Instant.EPOCH).isBefore(cutoff));
        submissions.keySet().retainAll(games.values());
        created.keySet().retainAll(games.values());
    }

    private static String key(MutleFixtureView fixture) {
        return fixture.series() + "/" + fixture.fixtureKey();
    }
}
