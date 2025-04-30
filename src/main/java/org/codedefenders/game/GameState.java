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
package org.codedefenders.game;

/**
 * Created by gordon on 20/02/2017.
 */
public enum GameState {

    /**
     * The game / puzzle exists but is not ready to be played.
     */
    CREATED,

    /**
     * The game / puzzle is currently in progress.
     */
    ACTIVE,

    /**
     * The game is finished.
     */
    FINISHED,

    /**
     * The game is in the first grace period.
     */
    GRACE_ONE,

    /**
     * The game is in the second grace period.
     */
    GRACE_TWO,

    /**
     * The player solved the puzzle correctly.
     */
    SOLVED,

    /**
     * The player failed to solve the puzzle.
     */
    FAILED
}
