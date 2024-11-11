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
package org.codedefenders.game.puzzle;

import java.util.List;
import java.util.stream.Collectors;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.PuzzleRepository;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.util.Constants;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.util.Constants.DUMMY_ATTACKER_USER_ID;
import static org.codedefenders.util.Constants.DUMMY_DEFENDER_USER_ID;

/**
 * Represents an instance of a {@link Puzzle puzzle}, which is played by one
 * player.
 *
 * @see Puzzle
 */
public class PuzzleGame extends AbstractGame {

    protected static final Logger logger = LoggerFactory.getLogger(PuzzleGame.class);

    /*
     * Attributes from AbstractGame protected int id; protected int classId;
     * protected int creatorId; protected GameState state; protected GameLevel
     * level; protected GameMode mode;
     */


    /**
     * The current round of the puzzle. Every mutant or test (valid or invalid) the
     * player submits to the puzzle advances the current round.
     */
    private int currentRound;

    /**
     * The type of the puzzle.
     */
    private PuzzleType type;

    /**
     * The ID of the {@link Puzzle} this is an instance of.
     */
    private int puzzleId;

    /**
     * The {@link Puzzle} this is an instance of.
     */
    private Puzzle puzzle;

    /**
     * Constructor for creating a new puzzle game (instantiating a puzzle).
     *
     * @param puzzle The {@link Puzzle} the {@link PuzzleGame} is for.
     * @param uid    The user id of the user who the puzzle is for.
     */
    public PuzzleGame(Puzzle puzzle, int uid) {
        /* AbstractGame attributes */
        this.id = 0; // ID will be set when inserting the game.
        this.classId = puzzle.getClassId();
        this.creatorId = uid;
        this.state = GameState.CREATED;
        this.level = puzzle.getLevel();
        this.mode = GameMode.PUZZLE;
        this.mutants = null;
        this.tests = null;

        /* Other game attributes */
        this.maxAssertionsPerTest = puzzle.getMaxAssertionsPerTest();

        this.mutantValidatorLevel = puzzle.getMutantValidatorLevel();
        this.currentRound = 1;
        this.type = puzzle.getType();

        /* Own attributes */
        this.puzzleId = puzzle.getPuzzleId();
        this.puzzle = puzzle;
    }

    /**
     * Constructor for reading a puzzle game from the database.
     */
    public PuzzleGame(GameClass cut, int puzzleId, int id, int classId, GameLevel level, int creatorId,
            int maxAssertionsPerTest, CodeValidatorLevel mutantValidatorLevel, GameState state, int currentRound,
            PuzzleType type) {
        /* AbstractGame attributes */
        this.cut = cut;
        this.id = id;
        this.classId = classId;
        this.creatorId = creatorId;
        this.state = state;
        this.level = level;
        this.mode = GameMode.PUZZLE;
        this.mutants = null;
        this.tests = null;

        /* Other game attributes */
        this.maxAssertionsPerTest = maxAssertionsPerTest;
        this.mutantValidatorLevel = mutantValidatorLevel;
        this.currentRound = currentRound;
        this.type = type;

        /* Own attributes */
        this.puzzleId = puzzleId;
        this.puzzle = null;
    }

    /**
     * Returns the pre-defined mutants from the puzzle. If the mutants are requested
     * for the first time for this instance, they will be queried from the database.
     *
     * @return The pre-defined mutants from the puzzle.
     */
    public List<Mutant> getPuzzleMutants() {
        if (mutants == null) {
            mutants = getMutants();
        }
        return mutants.stream().filter(m -> m.getPlayerId() == DUMMY_ATTACKER_USER_ID).collect(Collectors.toList());
    }

    /**
     * Returns the pre-defined tests from the puzzle. If the tests are requested for
     * the first time for this instance, they will be queried from the database.
     *
     * @return The pre-defined tests from the puzzle.
     */
    public List<Test> getPuzzleTests() {
        if (tests == null) {
            tests = getTests(false);
        }
        return tests.stream().filter(t -> t.getPlayerId() == DUMMY_DEFENDER_USER_ID).collect(Collectors.toList());
    }

    /**
     * Returns the player-written mutants. If the mutants are requested for the
     * first time for this instance, they will be queried from the database.
     *
     * @return The player-written mutants.
     */
    public List<Mutant> getPlayerMutants() {
        if (mutants == null) {
            mutants = getMutants();
        }
        return mutants.stream().filter(m -> m.getPlayerId() != DUMMY_ATTACKER_USER_ID).collect(Collectors.toList());
    }

    /**
     * Returns the player-written mutants. If the tests are requested for the first
     * time for this instance, they will be queried from the database.
     *
     * @return The player-written mutants.
     */
    public List<Test> getPlayerTests() {
        if (tests == null) {
            tests = getTests(false);
        }
        return tests.stream().filter(t -> t.getPlayerId() != DUMMY_DEFENDER_USER_ID).collect(Collectors.toList());
    }

    /**
     * Returns the number of valid mutants and tests the player submitted in the
     * game. This counts every mutant that was successfully validated and compiled,
     * and every test that was successfully validated, compiled and tested against
     * the original class.
     *
     * @return Rhe number of valid mutants and tests the player submitted in the
     *         game.
     */
    public int getValidSubmissionCount() {
        return getPlayerTests().size() + getPlayerMutants().size();
    }

    /**
     * Returns the number of invalid mutants and tests the player submitted in the
     * game. This counts every mutant that couldn't be validated or compiled, and
     * every test that couldn't be validated or compiled, or failed against the
     * original class.
     *
     * @return Rhe number of valid mutants and tests the player submitted in the
     *         game.
     */
    public int getInvalidSubmissionCount() {
        return currentRound - getValidSubmissionCount() - 1;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public Role getActiveRole() {
        return switch (type) {
            case ATTACKER, EQUIVALENCE -> Role.ATTACKER;
            case DEFENDER -> Role.DEFENDER;
        };
    }

    public PuzzleType getType() {
        return type;
    }

    public int getPuzzleId() {
        return puzzleId;
    }

    /**
     * Returns the {@link Puzzle} this is an instance of. If the puzzle is requested
     * for the first time for this instance, it will be queried from the database.
     *
     * @return The puzzle this is an instance of.
     */
    public Puzzle getPuzzle() {
        if (puzzle == null) {
            var puzzleRepo = CDIUtil.getBeanFromCDI(PuzzleRepository.class);
            puzzle = puzzleRepo.getPuzzleForId(puzzleId);
        }
        return puzzle;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    /**
     * Increments the current round by one and returns the new current round.
     *
     * @return The new current round.
     */
    public int incrementCurrentRound() {
        return ++this.currentRound;
    }

    /**
     * Adds a player with the given user ID and {@link Role} to the game. The dummy
     * attacker and defender can only be added with their respective {@link Role}.
     * The actual player can only be added with the active role, which the puzzle
     * specifies.
     *
     * @param userId The user ID.
     * @param role   The {@link Role}.
     * @return {@code true} Id the player was successfully added to the puzzle,
     *         {@code false} otherwise.
     * @see Constants#DUMMY_ATTACKER_USER_ID
     * @see Constants#DUMMY_DEFENDER_USER_ID
     */
    @Override
    public boolean addPlayer(int userId, Role role) {
        GameRepository gameRepo = CDIUtil.getBeanFromCDI(GameRepository.class);

        switch (userId) {
            case DUMMY_ATTACKER_USER_ID:
                if (role != Role.ATTACKER) {
                    logger.warn("Tried adding dummy attacker to puzzle game with wrong role: " + role);
                    return false;
                }
                break;
            case DUMMY_DEFENDER_USER_ID:
                if (role != Role.DEFENDER) {
                    logger.warn("Tried adding dummy defender to puzzle game with wrong role: " + role);
                    return false;
                }
                break;
            default:
                if (role != getActiveRole()) {
                    logger.warn("Tried adding player to puzzle game with wrong role: " + role);
                    return false;
                }
        }

        return gameRepo.addPlayerToGame(id, userId, role);
    }

    @Override
    public boolean insert() {
        var puzzleRepo = CDIUtil.getBeanFromCDI(PuzzleRepository.class);
        id = puzzleRepo.storePuzzleGame(this);
        return id != -1;
    }

    @Override
    public boolean update() {
        var puzzleRepo = CDIUtil.getBeanFromCDI(PuzzleRepository.class);
        return puzzleRepo.updatePuzzleGame(this);
    }

    @Override
    public boolean isChatEnabled() {
        return false;
    }
}
