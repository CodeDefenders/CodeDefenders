package org.codedefenders.model;

import org.codedefenders.game.GameState;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.servlets.games.puzzle.PuzzleOverview;

/**
 * This class is a wrapper around {@link Puzzle} and {@link PuzzleGame}
 * using {@link Type} as a discriminator. This class is used to display
 * puzzles or its games in the {@link PuzzleOverview}.
 * <p>
 * An instance can either contain a puzzle or a puzzle game.
 * A puzzle has an additional attribute {@code locked}, which indicates
 * that a puzzle cannot be played by a user.
 * <p>
 * Implements {@link Comparable} by comparing the identifiers of the
 * puzzles, retrieved from {@link #getPuzzleId()}.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
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
        }
        return -1;
    }

    public Puzzle getPuzzle() {
        switch (this.type) {
            case PUZZLE:
                return this.puzzle;
            case GAME:
                return this.game.getPuzzle();
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
        return this.getPuzzleId() - obj.getPuzzleId();
    }
}
