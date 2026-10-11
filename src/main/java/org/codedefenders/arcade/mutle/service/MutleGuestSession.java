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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;

import static org.codedefenders.util.Constants.MUTLE_GUEST_USER_ID;

/** Authorizes guest games using server-side session state, never the shared account alone. */
@SessionScoped
public class MutleGuestSession implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<String, Integer> games = new LinkedHashMap<>();

    @Inject
    private MutleService service;

    public MutleGuestSession() {
    }

    MutleGuestSession(MutleService service) {
        this.service = service;
    }

    public synchronized Optional<MutleStateView> current() {
        Integer gameId = currentGameId();
        return gameId == null ? Optional.empty() : Optional.of(state(gameId));
    }

    public synchronized MutleStateView start() {
        MutleFixtureView fixture = service.currentFixture();
        if (fixture == null) {
            throw new MutleService.NotFoundException();
        }
        String key = key(fixture);
        Integer gameId = games.get(key);
        if (gameId == null) {
            MutleStateView state = service.startGuest();
            games.put(key, state.gameId());
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

    public synchronized MutleStateView submit(int gameId, String source) throws IOException {
        if (!owns(gameId)) {
            throw new MutleService.NotFoundException();
        }
        return service.submit(MUTLE_GUEST_USER_ID, gameId, source);
    }

    public synchronized boolean owns(int gameId) {
        return games.containsValue(gameId);
    }

    private Integer currentGameId() {
        MutleFixtureView fixture = service.currentFixture();
        return fixture == null ? null : games.get(key(fixture));
    }

    private static String key(MutleFixtureView fixture) {
        return fixture.series() + "/" + fixture.fixtureKey();
    }
}
