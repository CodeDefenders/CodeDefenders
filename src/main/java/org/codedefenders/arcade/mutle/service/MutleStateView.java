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

import java.util.List;

public record MutleStateView(int gameId, long series, String title, String specification,
                             String originalSource, int acceptedGuesses, int remainingGuesses,
                             boolean expired, boolean solved, boolean failed, String testTemplate,
                             List<MutleGuessView> guesses, MutleRevealView reveal) {
    public MutleStateView {
        guesses = List.copyOf(guesses);
    }

    public int getGameId() {
        return gameId;
    }

    public long getSeries() {
        return series;
    }

    public String getTitle() {
        return title;
    }

    public int getAcceptedGuesses() {
        return acceptedGuesses;
    }

    public int getRemainingGuesses() {
        return remainingGuesses;
    }

    public boolean isExpired() {
        return expired;
    }

    public boolean isSolved() {
        return solved;
    }

    public boolean isFailed() {
        return failed;
    }
}
