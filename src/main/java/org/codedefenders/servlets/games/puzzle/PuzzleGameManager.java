package org.codedefenders.servlets.games.puzzle;

import org.apache.commons.lang.StringEscapeUtils;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.PuzzleDAO;
import org.codedefenders.execution.MutationTester;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.game.puzzle.solving.MutantSolvingStrategy;
import org.codedefenders.game.puzzle.solving.TestSolvingStrategy;
import org.codedefenders.servlets.games.GameManager;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.validation.CodeValidator;
import org.codedefenders.validation.CodeValidatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.codedefenders.servlets.util.GameServletUtils.getGameId;
import static org.codedefenders.util.Constants.MODE_PUZZLE_DIR;
import static org.codedefenders.util.Constants.MUTANT_COMPILED_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_CREATION_ERROR_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_DUPLICATED_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_UNCOMPILABLE_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_VALIDATION_SUCCESS_MESSAGE;
import static org.codedefenders.util.Constants.PUZZLEGAME_ATTACKER_VIEW_JSP;
import static org.codedefenders.util.Constants.PUZZLEGAME_DEFENDER_VIEW_JSP;
import static org.codedefenders.util.Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT;
import static org.codedefenders.util.Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST;
import static org.codedefenders.util.Constants.REQUEST_ATTRIBUTE_PUZZLE_GAME;
import static org.codedefenders.util.Constants.TEST_DID_NOT_COMPILE_MESSAGE;
import static org.codedefenders.util.Constants.TEST_DID_NOT_PASS_ON_CUT_MESSAGE;
import static org.codedefenders.util.Constants.TEST_GENERIC_ERROR_MESSAGE;
import static org.codedefenders.util.Constants.TEST_INVALID_MESSAGE;
import static org.codedefenders.util.Constants.TEST_PASSED_ON_CUT_MESSAGE;

/**
 * This {@link HttpServlet} handles retrieval and in-game management for {@link PuzzleGame}s.
 * <p>
 * {@code GET} requests allow accessing puzzle games and {@code POST} requests all creating of tests or mutants.
 * <p>
 * Serves under {@code /puzzlegame}.
 *
 * @author <a href=https://github.com/werli>Phil Werli<a/>
 * @see PuzzleGameSelectionManager
 * @see PuzzleGame
 */
public class PuzzleGameManager extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(PuzzleGameManager.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final HttpSession session = request.getSession();

        final Integer gameId = getGameId(request);
        if (gameId == null) {
            logger.error("Cannot retrieve puzzle game page. Failed to retrieve gameId from request.");
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }

        final PuzzleGame game = PuzzleDAO.getPuzzleGameForId(gameId);
        if (game == null) {
            logger.error("Cannot retrieve puzzle game page. Failed to retrieve puzzle game from database for gameId: {}.", gameId);
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }

        final int userId = ((Integer) session.getAttribute("uid"));
        if (game.getCreatorId() != userId) {
            logger.error("Cannot retrieve puzzle game page. User {} is not creator of the requested game: {}.", userId, gameId);
            response.setStatus(SC_FORBIDDEN);
            Redirect.redirectBack(request, response);
            return;
        }

        request.setAttribute(REQUEST_ATTRIBUTE_PUZZLE_GAME, game);

        final Role role = game.getActiveRole();
        final String subPath;
        switch (role) {
            case ATTACKER:
                subPath = PUZZLEGAME_ATTACKER_VIEW_JSP;
                break;
            case DEFENDER:
                subPath = PUZZLEGAME_DEFENDER_VIEW_JSP;
                break;
            default:
                logger.error("Trying to enter puzzle game with illegal role {}", role);
                response.setStatus(SC_INTERNAL_SERVER_ERROR);
                Redirect.redirectBack(request, response);
                return;
        }

        final String path = request.getContextPath() + subPath + "?gameId=" + gameId;
        request.getRequestDispatcher(path).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final HttpSession session = request.getSession();
        final String action = request.getParameter("formType");
        switch (action) {
            case "reset":
                session.removeAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
                Redirect.redirectBack(request, response);
                break;
            case "createTest":
                createTest(request, response, session);
                break;
            case "createMutant":
                createMutant(request, response, session);
                break;
            default:
                logger.info("Action not recognised: {}", action);
                Redirect.redirectBack(request, response);
                break;
        }
    }

    /**
     * Creates a test for an active puzzle game for a given request with the required parameters.
     * <p>
     * If the requesting user is not a defender or the test is not valid, the request will abort
     * and return a {@code 400} status code.
     * <p>
     * If the submitted test solves the puzzle, the puzzle game is finished. Otherwise, the number
     * of submits is incremented by one.
     * <p>
     * After request handling the user is redirected back to the game page.
     *
     * @param request  the request to create a test.
     * @param response the response to the request.
     * @param session  the session of the requesting user.
     * @throws IOException when redirecting fails.
     */
    @SuppressWarnings("Duplicates")
    private void createTest(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws IOException {
        final int userId = ((Integer) session.getAttribute("uid"));
        final Integer gameId = getGameId(request);
        if (gameId == null) {
            logger.error("Cannot create test for this puzzle. Failed to retrieve gameId from request.");
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }

        final PuzzleGame game = PuzzleDAO.getPuzzleGameForId(gameId);
        if (game == null) {
            logger.error("Failed to retrieve puzzle game from database for gameId: {}.", gameId);
            Redirect.redirectBack(request, response);
            return;
        }
        if (game.getMode() != GameMode.PUZZLE) {
            logger.error("Trying to submit test to non-puzzle game {}.", gameId);
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }
        if (game.getActiveRole() != Role.DEFENDER) {
            logger.error("Cannot create tests for this puzzle. GameId={}", gameId);
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }
        if (game.getState() != GameState.ACTIVE) {
            logger.error("Cannot create test for puzzle game {}. Game is not active.", gameId);
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }

        String testText = request.getParameter("test");
        if (testText == null) {
            logger.error("Cannot create test for puzzle game {}. Provided test is empty.", gameId);
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }

        final ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        final Test newTest;
        try {
            newTest = GameManager.createTest(gameId, game.getClassId(), testText, userId, MODE_PUZZLE_DIR, game.getMaxAssertionsPerTest());
        } catch (CodeValidatorException e) {
            messages.add(TEST_GENERIC_ERROR_MESSAGE);
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, testText);
            Redirect.redirectBack(request, response);
            return;
        }
        if (newTest == null) {
            messages.add(String.format(TEST_INVALID_MESSAGE, game.getMaxAssertionsPerTest()));
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, testText);
            Redirect.redirectBack(request, response);
            return;
        }

        final TargetExecution compileTestTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);
        if (!compileTestTarget.status.equals("SUCCESS")) {
            messages.add(TEST_DID_NOT_COMPILE_MESSAGE);
            messages.add(StringEscapeUtils.escapeHtml(compileTestTarget.message));
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, testText);
            Redirect.redirectBack(request, response);
            return;
        }

        final TargetExecution testOriginalTarget = DatabaseAccess.getTargetExecutionForTest(newTest, TargetExecution.Target.TEST_ORIGINAL);
        if (!testOriginalTarget.status.equals("SUCCESS")) {
            messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE);
            messages.add(StringEscapeUtils.escapeHtml(testOriginalTarget.message));
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, testText);
            Redirect.redirectBack(request, response);
            return;
        }

        messages.add(TEST_PASSED_ON_CUT_MESSAGE);
        session.removeAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST);

        MutationTester.runTestOnAllMutants(game, newTest, messages);

        // may be // final TestSolvingStrategy solving = Testgame.getTestSolver();
        final TestSolvingStrategy solver = TestSolvingStrategy.get("KILLED_ALL_MUTANTS");
        if (solver == null) {
            throw new IllegalStateException("Test solving strategy not found. That shouldn't happen.");
        }

        if (!solver.solve(game, newTest)) {
            messages.add("Your test did not solve the puzzle. Try another one...");
            game.incrementCurrentRound();
        } else {
            game.setState(GameState.SOLVED);
        }
        PuzzleDAO.updatePuzzleGame(game);
        Redirect.redirectBack(request, response);
    }

    /**
     * Creates a mutant for an active puzzle game for a given request with the required parameters.
     * <p>
     * If the requesting user is not an attacker or the mutant is not valid, the request will abort
     * and return a {@code 400} status code.
     * <p>
     * If the submitted mutant solves the puzzle, the puzzle game is finished. Otherwise, the number
     * of submits is incremented by one.
     * <p>
     * After request handling the user is redirected back to the game page.
     *
     * @param request  the request to create a mutant.
     * @param response the response to the request.
     * @param session  the session of the requesting user.
     * @throws IOException when redirecting fails.
     */
    private void createMutant(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws IOException {
        final int userId = ((Integer) session.getAttribute("uid"));
        final Integer gameId = getGameId(request);
        if (gameId == null) {
            logger.error("Cannot create mutant for this puzzle. Failed to retrieve gameId from request.");
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }

        final PuzzleGame game = PuzzleDAO.getPuzzleGameForId(gameId);
        if (game == null) {
            logger.error("Failed to retrieve puzzle game from database for gameId: {}. Aborting.", gameId);
            Redirect.redirectBack(request, response);
            return;
        }
        if (game.getMode() != GameMode.PUZZLE) {
            logger.error("Trying to submit mutant to non-puzzle game {}. Aborting.", gameId);
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }
        if (game.getActiveRole() != Role.ATTACKER) {
            logger.error("Cannot create mutants for this puzzle (gameId={}). Aborting.", gameId);
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }
        if (game.getState() != GameState.ACTIVE) {
            logger.error("Cannot create mutant for puzzle game {}. Game is not active. Aborting.", gameId);
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }

        String mutantText = request.getParameter("mutant");
        if (mutantText == null) {
            logger.error("Cannot create mutant for puzzle game {}. Provided test is empty. Aborting.", gameId);
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }

        final CodeValidator.CodeValidatorLevel mutantValidatorLevel = game.getMutantValidatorLevel();

        final ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        final String validityMessage = GameManager.getMutantValidityMessage(game.getClassId(), mutantText, mutantValidatorLevel);
        if (!validityMessage.equals(MUTANT_VALIDATION_SUCCESS_MESSAGE)) {
            // Mutant is either the same as the CUT or it contains invalid code
            messages.add(validityMessage);
            Redirect.redirectBack(request, response);
            return;
        }
        final Mutant existingMutant = GameManager.existingMutant(gameId, mutantText);
        if (existingMutant != null) {
            messages.add(MUTANT_DUPLICATED_MESSAGE);
            TargetExecution existingMutantTarget = DatabaseAccess.getTargetExecutionForMutant(existingMutant, TargetExecution.Target.COMPILE_MUTANT);
            if (existingMutantTarget != null && !existingMutantTarget.status.equals("SUCCESS")
                    && existingMutantTarget.message != null && !existingMutantTarget.message.isEmpty()) {
                messages.add(existingMutantTarget.message);
            }
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, mutantText);
            Redirect.redirectBack(request, response);
            return;
        }
        final Mutant newMutant = GameManager.createMutant(gameId, game.getClassId(), mutantText, userId, MODE_PUZZLE_DIR);
        if (newMutant == null) {
            messages.add(MUTANT_CREATION_ERROR_MESSAGE);
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, mutantText);
            logger.error("Error creating mutant for puzzle game. Game: {}, Class: {}, User: {}. Aborting.", gameId, game.getClassId(), userId, mutantText);
            Redirect.redirectBack(request, response);
            return;
        }

        final TargetExecution compileMutantTarget = DatabaseAccess.getTargetExecutionForMutant(newMutant, TargetExecution.Target.COMPILE_MUTANT);
        if (compileMutantTarget == null || !compileMutantTarget.status.equals("SUCCESS")) {
            messages.add(MUTANT_UNCOMPILABLE_MESSAGE);
            if (compileMutantTarget != null && compileMutantTarget.message != null && !compileMutantTarget.message.isEmpty()) {
                messages.add(compileMutantTarget.message);
            }
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, mutantText);
            Redirect.redirectBack(request, response);
            return;
        }

        messages.add(MUTANT_COMPILED_MESSAGE);
        session.removeAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
        MutationTester.runAllTestsOnMutant(game, newMutant, messages);

        // may be // final MutantSolvingStrategy solving = game.getMutantSolver();
        final MutantSolvingStrategy solver = MutantSolvingStrategy.get(MutantSolvingStrategy.Types.SURVIVED_ALL_MUTANTS.name());
        if (solver == null) {
            throw new IllegalStateException("Mutant solving strategy not found. That shouldn't happen.");
        }

        if (!solver.solve(game, newMutant)) {
            messages.add("Your mutant did not solve the puzzle. Try another one...");
            game.incrementCurrentRound();
        } else {
            game.setState(GameState.SOLVED);
        }
        PuzzleDAO.updatePuzzleGame(game);
        Redirect.redirectBack(request, response);
    }
}
