package org.codedefenders.game.puzzle;

import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.PuzzleDAO;
import org.codedefenders.execution.MutationTester;
import org.codedefenders.game.*;
import org.codedefenders.servlets.games.GameManager;
import org.codedefenders.validation.CodeValidator.CodeValidatorLevel;
import org.codedefenders.validation.CodeValidatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.codedefenders.util.Constants.*;

/**
 * Represents an instance of a {@link Puzzle puzzle}, which is played by one player.
 * @see Puzzle
 */
public class PuzzleGame extends AbstractGame {
    protected static final Logger logger = LoggerFactory.getLogger(PuzzleGame.class);

    /* Attributes from AbstractGame
    protected int id;
    protected int classId;
    protected int creatorId;
    protected GameState state;
    protected GameLevel level;
    protected GameMode mode; */

    /**
     * Maximum number of allowed assertions per submitted test.
     */
    private int maxAssertionsPerTest;

    /**
     * Validation level used to check submitted mutants.
     */
    private CodeValidatorLevel codeValidatorLevel;

    /**
     * The current round of the puzzle.
     * Every mutant or test (valid or invalid) the player submits to the puzzle advances the current round.
     */
    private int currentRound;
    // TODO advance on every submission or only on valid submissions ?

    /**
     * The {@link Role} the player takes in the puzzle.
     */
    private Role activeRole;

    /**
     * The puzzle this is an instance of.
     */
    private Puzzle puzzle;

    /**
     * Creates a new {@link PuzzleGame} according to the given {@link Puzzle}.
     * Adds the mapped tests and mutants from the {@link Puzzle puzzle}'s class to the game.
     * Adds the user to the game as a player.
     *
     * @param puzzle {@link Puzzle} to create a {@link PuzzleGame} for.
     * @param uid User ID of the user who plays the puzzle.
     * @return A fully prepared {@link PuzzleGame} for the given puzzle and user.
     */
    public static PuzzleGame createPuzzleGame(Puzzle puzzle, int uid) {
        PuzzleGame game = new PuzzleGame(puzzle, uid);
        game.insert();

        final List<Test> mappedTests = GameClassDAO.getMappedTestsForClassId(game.classId);
        final List<Mutant> mappedMutants = GameClassDAO.getMappedMutantsForClassId(game.classId);

        game.addPlayer(DUMMY_DEFENDER_USER_ID, Role.DEFENDER);
        game.addPlayer(DUMMY_ATTACKER_USER_ID, Role.ATTACKER);

        try {

            for (Test test : mappedTests) {
                final String testCode = String.join("\n", Files.readAllLines(Paths.get(test.getJavaFile())));
                // GameManager#createTest() compiles, tests and stores the tests
                GameManager.createTest(game.id, game.classId, testCode, DUMMY_DEFENDER_USER_ID, "pz", game.maxAssertionsPerTest);
            }

            for (Mutant mutant : mappedMutants) {
                final String mutantCode = String.join("\n", Files.readAllLines(Paths.get(mutant.getJavaFile())));
                // GameManager#createMutant() compiles and stores the mutant
                GameManager.createMutant(game.id, game.classId, mutantCode, DUMMY_ATTACKER_USER_ID, "pz");
                MutationTester.runAllTestsOnMutant(game, mutant, new ArrayList<>());
            }

        } catch (IOException | CodeValidatorException e) {
            logger.error("An exception occurred while adding pre-defined mutants and tests to the puzzle game.", e);
            return null;
        }

        game.addPlayer(uid, game.getActiveRole());
        game.setState(GameState.ACTIVE);
        game.update();

        return game;
    }

    /**
     * Constructor for creating a new puzzle game (instantiating a puzzle).
     * @param puzzle The {@link Puzzle} the {@link PuzzleGame} is for.
     * @param uid The user id of the user who the puzzle is for.
     */
    private PuzzleGame(Puzzle puzzle, int uid) {
        /* AbstractGame attributes */
        this.classId = puzzle.getClassId();
        this.creatorId = uid;
        this.state = GameState.CREATED;
        this.level = puzzle.getLevel();
        this.mode = GameMode.PUZZLE;
        this.mutants = null;
        this.tests = null;

        /* Other game attributes */
        this.maxAssertionsPerTest = 2;                          // TODO
        this.codeValidatorLevel = CodeValidatorLevel.MODERATE;  // TODO
        this.currentRound = 1;
        this.activeRole = puzzle.getActiveRole();

        /* Own attributes */
        this.puzzle = puzzle;
    }

    /**
     * Constructor for reading a puzzle game from the database.
     */
    private PuzzleGame(Puzzle puzzle,
                       int id,
                       int classId,
                       GameLevel level,
                       int creatorId,
                       int maxAssertionsPerTest,
                       CodeValidatorLevel codeValidatorLevel,
                       GameState state,
                       int currentRound,
                       Role activeRole) {
        /* AbstractGame attributes */
        this.classId = classId;
        this.creatorId = creatorId;
        this.state = state;
        this.level = level;
        this.mode = GameMode.PUZZLE;
        this.mutants = null;
        this.tests = null;

        /* Other game attributes */
        this.maxAssertionsPerTest = maxAssertionsPerTest;
        this.codeValidatorLevel = codeValidatorLevel;
        this.currentRound = currentRound;
        this.activeRole = activeRole;

        /* Own attributes */
        this.puzzle = puzzle;
    }

    public List<Mutant> getPuzzleMutants() {
       if (mutants == null) {
           mutants = getMutants();
       }
       return mutants.stream().filter(m -> m.getPlayerId() == DUMMY_ATTACKER_USER_ID).collect(Collectors.toList());
    }

    public List<Test> getPuzzleTests() {
        if (tests == null) {
            tests = getTests(false);
        }
        return tests.stream().filter(t -> t.getPlayerId() == DUMMY_DEFENDER_USER_ID).collect(Collectors.toList());
    }

    public List<Mutant> getPlayerMutants() {
        if (mutants == null) {
            mutants = getMutants();
        }
        return mutants.stream().filter(m -> m.getPlayerId() != DUMMY_ATTACKER_USER_ID).collect(Collectors.toList());
    }

    public List<Test> getPlayerTests() {
        if (tests == null) {
            tests = getTests(false);
        }
        return tests.stream().filter(t -> t.getPlayerId() != DUMMY_DEFENDER_USER_ID).collect(Collectors.toList());
    }

    @Override
    public boolean addPlayer(int userId, Role role) {
        throw new Error("not implemented");
         // TODO Add some DAO class and method to add a player that can be used for all game classes?
    }

    @Override
    public boolean insert() {
        return PuzzleDAO.storePuzzleGame(this);
    }

    @Override
    public boolean update() {
        return PuzzleDAO.updatePuzzleGame(this);
    }

    public int getMaxAssertionsPerTest() {
        return maxAssertionsPerTest;
    }

    public void setMaxAssertionsPerTest(int maxAssertionsPerTest) {
        this.maxAssertionsPerTest = maxAssertionsPerTest;
    }

    public CodeValidatorLevel getCodeValidatorLevel() {
        return codeValidatorLevel;
    }

    public void setCodeValidatorLevel(CodeValidatorLevel codeValidatorLevel) {
        this.codeValidatorLevel = codeValidatorLevel;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public Role getActiveRole() {
        return activeRole;
    }

    public void setActiveRole(Role activeRole) {
        this.activeRole = activeRole;
    }

    public Puzzle getPuzzle() {
        return puzzle;
    }

    public void setPuzzle(Puzzle puzzle) {
        this.puzzle = puzzle;
    }
}
