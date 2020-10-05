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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.PlayerDAO;
import org.codedefenders.database.PuzzleDAO;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
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
     * Maximum number of allowed assertions per submitted test.
     */
    private int maxAssertionsPerTest;

    /**
     * Validation level used to check submitted mutants.
     */
    private CodeValidatorLevel mutantValidatorLevel;

    /**
     * The current round of the puzzle. Every mutant or test (valid or invalid) the
     * player submits to the puzzle advances the current round.
     */
    private int currentRound;

    /**
     * The {@link Role} the player takes in the puzzle.
     */
    private Role activeRole;

    /**
     * The ID of the {@link Puzzle} this is an instance of.
     */
    private int puzzleId;

    /**
     * The {@link Puzzle} this is an instance of.
     */
    private Puzzle puzzle;

    /**
     * Creates a new {@link PuzzleGame} according to the given {@link Puzzle}. Adds
     * the mapped tests and mutants from the {@link Puzzle puzzle}'s class to the
     * game. Adds the user to the game as a player. If any error occurs during the
     * preparation, {@code null} will be returned.
     *
     * @param puzzle {@link Puzzle} to create a {@link PuzzleGame} for.
     * @param uid    User ID of the user who plays the puzzle.
     * @return A fully prepared {@link PuzzleGame} for the given puzzle and user, or
     *         {@code null} if any error occurred.
     */
    public static PuzzleGame createPuzzleGame(Puzzle puzzle, int uid) {
        String errorMsg = String.format("Error while preparing puzzle game for puzzle %d and user %d.",
                puzzle.getPuzzleId(), uid);

        PuzzleGame game = new PuzzleGame(puzzle, uid);
        if (!game.insert()) {
            logger.error(errorMsg + " Could not insert the puzzle game");
            return null;
        }

        if (!game.addPlayer(DUMMY_ATTACKER_USER_ID, Role.ATTACKER)) {
            logger.error(errorMsg + " Could not add dummy attacker to game.");
            return null;
        }
        if (!game.addPlayer(DUMMY_DEFENDER_USER_ID, Role.DEFENDER)) {
            logger.error(errorMsg + " Could not add dummy defender to game.");
            return null;
        }
        int dummyAttackerId = PlayerDAO.getPlayerIdForUserAndGame(DUMMY_ATTACKER_USER_ID, game.id);
        int dummyDefenderId = PlayerDAO.getPlayerIdForUserAndGame(DUMMY_DEFENDER_USER_ID, game.id);

        final List<Test> mappedTests = GameClassDAO.getMappedTestsForClassId(game.classId);
        final List<Mutant> mappedMutants = GameClassDAO.getMappedMutantsForClassId(game.classId);

        /* Add the tests from the puzzle. */
        Map<Integer, Test> testMap = new HashMap<>();

        for (Test test : mappedTests) {
            final Test newTest = Test.newTestForGameAndPlayerIds(game.id, dummyDefenderId, test);
            newTest.insert();
            testMap.put(test.getId(), newTest);
        }
        Map<Integer, Mutant> mutantMap = new HashMap<>();

        /* Add the mutants from the puzzle. */
        for (Mutant mutant : mappedMutants) {
            final Mutant newMutant = new Mutant(game.id, game.classId, mutant.getJavaFile(), mutant.getClassFile(),
                    true, dummyAttackerId, GameDAO.getCurrentRound(game.id));
            newMutant.insert();
            mutantMap.put(mutant.getId(), newMutant);
        }

        if (!mutantMap.isEmpty() && !testMap.isEmpty()) {
            for (TargetExecution targetExecution : TargetExecutionDAO
                    .getTargetExecutionsForUploadedWithClass(game.classId)) {
                Test test = testMap.get(targetExecution.testId);
                Mutant mutant = mutantMap.get(targetExecution.mutantId);

                targetExecution.testId = test.getId();
                targetExecution.mutantId = mutant.getId();

                if (targetExecution.status == TargetExecution.Status.FAIL) {
                    test.killMutant();
                    mutant.kill();
                    mutant.setKillMessage(targetExecution.message);
                    MutantDAO.updateMutantKillMessageForMutant(mutant);
                }

                targetExecution.insert();
            }
        }

        if (!game.addPlayer(uid, game.getActiveRole())) {
            logger.error(errorMsg + " Could not add player to the game.");
            return null;
        }

        game.setState(GameState.ACTIVE);
        if (!game.update()) {
            logger.error(errorMsg + " Could not update game state.");
            return null;
        }

        return game;
    }

    /**
     * Constructor for creating a new puzzle game (instantiating a puzzle).
     *
     * @param puzzle The {@link Puzzle} the {@link PuzzleGame} is for.
     * @param uid    The user id of the user who the puzzle is for.
     */
    private PuzzleGame(Puzzle puzzle, int uid) {
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
        this.activeRole = puzzle.getActiveRole();

        /* Own attributes */
        this.puzzleId = puzzle.getPuzzleId();
        this.puzzle = puzzle;
    }

    /**
     * Constructor for reading a puzzle game from the database.
     */
    public PuzzleGame(GameClass cut, int puzzleId, int id, int classId, GameLevel level, int creatorId,
            int maxAssertionsPerTest, CodeValidatorLevel mutantValidatorLevel, GameState state, int currentRound,
            Role activeRole) {
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
        this.activeRole = activeRole;

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

    public int getMaxAssertionsPerTest() {
        return maxAssertionsPerTest;
    }

    public CodeValidatorLevel getMutantValidatorLevel() {
        return mutantValidatorLevel;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public Role getActiveRole() {
        return activeRole;
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
            puzzle = PuzzleDAO.getPuzzleForId(puzzleId);
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
                if (role != activeRole) {
                    logger.warn("Tried adding player to puzzle game with wrong role: " + role);
                    return false;
                }
        }

        return GameDAO.addPlayerToGame(id, userId, role);
    }

    @Override
    public boolean insert() {
        id = PuzzleDAO.storePuzzleGame(this);
        return id != -1;
    }

    @Override
    public boolean update() {
        return PuzzleDAO.updatePuzzleGame(this);
    }

}
