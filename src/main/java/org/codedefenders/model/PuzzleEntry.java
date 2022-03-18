/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
package org.codedefenders.model;

import org.codedefenders.game.GameState;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.servlets.games.puzzle.PuzzleOverview;

/**
 * This class is a wrapper around {@link Puzzle} and {@link PuzzleGame}
 * using {@link Type} as a discriminator. This class is used to display
 * puzzles or its games in the {@link PuzzleOverview}.
 *
 * <p>An instance can either contain a puzzle or a puzzle game.
 * A puzzle has an additional attribute {@code locked}, which indicates
 * that a puzzle cannot be played by a user.
 *
 * <p>Implements {@link Comparable} by comparing the identifiers of the
 * puzzles, retrieved from {@link #getPuzzleId()}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 * @see PuzzleChapterEntry
 */
public class PuzzleEntry implements Comparable {
    public enum Type {
        PUZZLE,
        GAME
    }

    private Type type;

    private Puzzle puzzle;
    private boolean locked;
    private PuzzleGame game;

    /**
     * Constructs a new puzzle entry for a given puzzle and locked status.
     *
     * @param puzzle the puzzle of this entry.
     * @param locked the locked status of this puzzle.
     */
    public PuzzleEntry(Puzzle puzzle, boolean locked) {
        this.puzzle = puzzle;
        this.locked = locked;

        this.type = Type.PUZZLE;
        this.game = null;
    }

    /**
     * Constructs a new puzzle entry for a given puzzle game.
     *
     * @param game the puzzle game of this entry.
     */
    public PuzzleEntry(PuzzleGame game) {
        this.game = game;

        this.type = Type.GAME;
        this.puzzle = null;
    }

    public Type getType() {
        return type;
    }

    public int getPuzzleId() {
        switch (this.type) {
            case PUZZLE:
                return this.puzzle.getPuzzleId();
            case GAME:
                return this.game.getPuzzleId();
            default:
                // ignored
        }
        return -1;
    }

    public Puzzle getPuzzle() {
        switch (this.type) {
            case PUZZLE:
                return this.puzzle;
            case GAME:
                return this.game.getPuzzle();
            default:
                // ignored
        }
        return null;
    }

    public boolean isLocked() {
        if (this.type != Type.PUZZLE) {
            throw new IllegalStateException();
        }
        return locked;
    }

    public PuzzleGame getGame() {
        if (this.type != Type.GAME) {
            throw new IllegalStateException();
        }
        return game;
    }

    public GameState getState() {
        if (this.type != Type.GAME) {
            throw new IllegalStateException();
        }
        return game.getState();
    }

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof PuzzleEntry)) {
            return -1;
        }
        final PuzzleEntry obj = (PuzzleEntry) o;
        final Integer pos1 = this.getPuzzle().getPosition();
        final Integer pos2 = obj.getPuzzle().getPosition();
        if (pos1 == null) {
            return 1;
        }
        if (pos2 == null) {
            return -1;
        }
        return pos1 - pos2;
    }
}
