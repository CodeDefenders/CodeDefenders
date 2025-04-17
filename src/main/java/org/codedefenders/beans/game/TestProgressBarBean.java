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
package org.codedefenders.beans.game;

import jakarta.enterprise.context.RequestScoped;

/**
 * <p>Provides data for the test progress bar game component.</p>
 * <p>Bean Name: {@code testProgressBar}</p>
 */
@RequestScoped
public class TestProgressBarBean {
    /**
     * The game id of the currently played game.
     * Used for event parameters.
     */
    private Integer gameId;

    public TestProgressBarBean() {
        gameId = null;
    }

    public void setGameId(Integer gameId) {
        this.gameId = gameId;
    }

    // --------------------------------------------------------------------------------

    public Integer getGameId() {
        return gameId;
    }
}
