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
package org.codedefenders.servlets.games.puzzle;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.codedefenders.servlets.util.ServletUtils.ctx;
import static org.codedefenders.servlets.util.ServletUtils.gameId;
import static org.codedefenders.servlets.util.ServletUtils.getIntParameter;
import static org.codedefenders.util.Constants.MODE_PUZZLE_DIR;
import static org.codedefenders.util.Constants.MUTANT_COMPILED_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_CREATION_ERROR_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_DUPLICATED_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_UNCOMPILABLE_MESSAGE;
import static org.codedefenders.util.Constants.PUZZLE_GAME_ATTACKER_VIEW_JSP;
import static org.codedefenders.util.Constants.PUZZLE_GAME_DEFENDER_VIEW_JSP;
import static org.codedefenders.util.Constants.REQUEST_ATTRIBUTE_PUZZLE_GAME;
import static org.codedefenders.util.Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT;
import static org.codedefenders.util.Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST;
import static org.codedefenders.util.Constants.TEST_DID_NOT_COMPILE_MESSAGE;
import static org.codedefenders.util.Constants.TEST_DID_NOT_PASS_ON_CUT_MESSAGE;
import static org.codedefenders.util.Constants.TEST_GENERIC_ERROR_MESSAGE;
import static org.codedefenders.util.Constants.TEST_INVALID_MESSAGE;
import static org.codedefenders.util.Constants.TEST_PASSED_ON_CUT_MESSAGE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringEscapeUtils;
import org.codedefenders.database.PuzzleDAO;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.execution.IMutationTester;
import org.codedefenders.execution.MutationTester;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.game.puzzle.PuzzleChapter;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.game.puzzle.solving.MutantSolvingStrategy;
import org.codedefenders.game.puzzle.solving.TestSolvingStrategy;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.server.mutant.MutantCompiledEvent;
import org.codedefenders.notification.events.server.mutant.MutantDuplicateCheckedEvent;
import org.codedefenders.notification.events.server.mutant.MutantSubmittedEvent;
import org.codedefenders.notification.events.server.mutant.MutantTestedEvent;
import org.codedefenders.notification.events.server.mutant.MutantValidatedEvent;
import org.codedefenders.notification.events.server.test.TestSubmittedEvent;
import org.codedefenders.notification.events.server.test.TestTestedMutantsEvent;
import org.codedefenders.notification.events.server.test.TestValidatedEvent;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Paths;
import org.codedefenders.validation.code.CodeValidator;
import org.codedefenders.validation.code.CodeValidatorException;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.codedefenders.validation.code.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link HttpServlet} handles retrieval and in-game management for {@link PuzzleGame PuzzleGames}.
 * <p>
 * {@code GET} requests allow accessing puzzle games and {@code POST} requests handle creating of tests or mutants.
 * <p>
 * Serves under {@code /puzzlegame}.
 *
 * @author <a href=https://github.com/werli>Phil Werli<a/>
 * @see PuzzleGameSelectionManager
 * @see PuzzleGame
 * @see org.codedefenders.util.Paths#PUZZLE_GAME
 */
public class PuzzleGameManager extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(PuzzleGameManager.class);

    @Inject
    private GameManagingUtils gameManagingUtils;

    @Inject
    private IMutationTester mutationTester;

    @Inject
    INotificationService notificationService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final int userId = ServletUtils.userId(request);

        final PuzzleGame game;

        final Optional<Integer> gameIdOpt = gameId(request);
        boolean fromGameId = gameIdOpt.isPresent(); // else from puzzleId
        if (fromGameId) {
            final int gameId = gameIdOpt.get();
            game = PuzzleDAO.getPuzzleGameForId(gameId);

            if (game == null) {
                logger.error("Cannot retrieve puzzle game page. Failed to retrieve puzzle game from database for gameId: {}.", gameId);
                response.sendRedirect(ctx(request) + Paths.PUZZLE_OVERVIEW);
                return;
            }
            if (game.getCreatorId() != userId) {
                logger.error("Cannot retrieve puzzle game page. User {} is not creator of the requested game: {}.", userId, gameId);
                response.sendRedirect(ctx(request) + Paths.PUZZLE_OVERVIEW);
                return;
            }
        } else {
            final Optional<Integer> puzzleIdOpt = getIntParameter(request, "puzzleId");
            if (!puzzleIdOpt.isPresent()) {
                logger.error("Cannot retrieve puzzle game page. Failed to retrieve gameId and puzzleId from request.");
                response.sendRedirect(ctx(request) + Paths.PUZZLE_OVERVIEW);
                return;
            }
            final int puzzleId = puzzleIdOpt.get();
            game = PuzzleDAO.getLatestPuzzleGameForPuzzleAndUser(puzzleId, userId);

            if (game == null) {
                logger.info("Failed to retrieve puzzle game from database. Creating game for puzzleId {} and userId {}", puzzleId, userId);
                PuzzleGameSelectionManager.createGame(request, response);
                return;
            }
        }

        request.setAttribute(REQUEST_ATTRIBUTE_PUZZLE_GAME, game);

        final Role role = game.getActiveRole();
        switch (role) {
            case ATTACKER:
                request.getRequestDispatcher(PUZZLE_GAME_ATTACKER_VIEW_JSP).forward(request, response);
                break;
            case DEFENDER:
                request.getRequestDispatcher(PUZZLE_GAME_DEFENDER_VIEW_JSP).forward(request, response);
                break;
            default:
                logger.error("Trying to enter puzzle game with illegal role {}", role);
                response.sendRedirect(ctx(request) + Paths.PUZZLE_OVERVIEW);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final HttpSession session = request.getSession();
        final String action = ServletUtils.formType(request);
        switch (action) {
            case "reset":
                session.removeAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
                session.removeAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST);
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
        final Optional<Integer> gameIdOpt = gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.error("Cannot create test for this puzzle. Failed to retrieve gameId from request.");
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }
        final int gameId = gameIdOpt.get();

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

        TestSubmittedEvent tse = new TestSubmittedEvent();
        tse.setGameId(gameId);
        tse.setUserId(userId);
        notificationService.post(tse);

        final ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        // TODO Why we have testText and not escaped(testText)?
        // Validate the test
        // Do the validation even before creating the mutant
        List<String> validationMessage = CodeValidator.validateTestCodeGetMessage(testText, game.getMaxAssertionsPerTest(), game.isForceHamcrest());
        boolean validationSuccess = validationMessage.isEmpty();

        TestValidatedEvent tve = new TestValidatedEvent();
        tve.setGameId(gameId);
        tve.setUserId(userId);
        tve.setSuccess(validationSuccess);
        tve.setValidationMessage(validationSuccess ? null : String.join("\n", validationMessage));
        notificationService.post(tve);

        if (!validationSuccess) {
            messages.addAll(validationMessage);
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, testText);
            Redirect.redirectBack(request, response);
            return;
        }

        final Test newTest;
        try {
            newTest = gameManagingUtils.createTest(gameId, game.getClassId(), testText, userId, MODE_PUZZLE_DIR);
        } catch (IOException e) {
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

        final TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);
        if (!compileTestTarget.status.equals(TargetExecution.Status.SUCCESS)) {
            messages.add(TEST_DID_NOT_COMPILE_MESSAGE);
            messages.add(StringEscapeUtils.escapeHtml(compileTestTarget.message));
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, testText);
            Redirect.redirectBack(request, response);
            return;
        }

        final TargetExecution testOriginalTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TargetExecution.Target.TEST_ORIGINAL);
        if (!testOriginalTarget.status.equals(TargetExecution.Status.SUCCESS)) {
            messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE);
            messages.add(StringEscapeUtils.escapeHtml(testOriginalTarget.message));
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, testText);
            Redirect.redirectBack(request, response);
            return;
        }

        messages.add(TEST_PASSED_ON_CUT_MESSAGE);
        session.removeAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST);

        mutationTester.runTestOnAllMutants(game, newTest, messages);

        TestTestedMutantsEvent ttme = new TestTestedMutantsEvent();
        ttme.setGameId(gameId);
        ttme.setUserId(userId);
        notificationService.post(ttme);

        // may be // final TestSolvingStrategy solving = Testgame.getTestSolver();
        final TestSolvingStrategy solver = TestSolvingStrategy.get(TestSolvingStrategy.Types.KILLED_ALL_MUTANTS.name());
        if (solver == null) {
            throw new IllegalStateException("Test solving strategy not found. That shouldn't happen.");
        }

        if (!solver.solve(game, newTest)) {
            messages.add("Your test did not solve the puzzle. Try another one...");
            game.incrementCurrentRound();
        } else {
            game.setState(GameState.SOLVED);
            messages.clear();
            boolean isAnAttackGame = false;
            messages.add( generateWinningMessage(request, userId, game, isAnAttackGame));
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
        final Optional<Integer> gameIdOpt = gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.error("Cannot create mutant for this puzzle. Failed to retrieve gameId from request.");
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }
        final int gameId = gameIdOpt.get();

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

        MutantSubmittedEvent mse = new MutantSubmittedEvent();
        mse.setGameId(gameId);
        mse.setUserId(userId);
        notificationService.post(mse);

        final CodeValidatorLevel mutantValidatorLevel = game.getMutantValidatorLevel();

        final ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        ValidationMessage validationMessage = CodeValidator.validateMutantGetMessage(game.getCUT().getSourceCode(), mutantText, mutantValidatorLevel);
        boolean validationSuccess = validationMessage == ValidationMessage.MUTANT_VALIDATION_SUCCESS;

        MutantValidatedEvent mve = new MutantValidatedEvent();
        mve.setGameId(gameId);
        mve.setUserId(userId);
        mve.setSuccess(validationSuccess);
        mve.setValidationMessage(validationSuccess ? null : validationMessage.get());
        notificationService.post(mve);

        if (!validationSuccess) {
            // Mutant is either the same as the CUT or it contains invalid code
            messages.add(validationMessage.get());
            Redirect.redirectBack(request, response);
            return;
        }

        final Mutant existingMutant = gameManagingUtils.existingMutant(gameId, mutantText);
        boolean duplicateCheckSuccess = existingMutant == null;

        MutantDuplicateCheckedEvent mdce = new MutantDuplicateCheckedEvent();
        mdce.setGameId(gameId);
        mdce.setUserId(userId);
        mdce.setSuccess(duplicateCheckSuccess);
        mdce.setDuplicateId(duplicateCheckSuccess ? null : existingMutant.getId());
        notificationService.post(mdce);

        if (!duplicateCheckSuccess) {
            messages.add(MUTANT_DUPLICATED_MESSAGE);
            TargetExecution existingMutantTarget = TargetExecutionDAO.getTargetExecutionForMutant(existingMutant, TargetExecution.Target.COMPILE_MUTANT);
            if (existingMutantTarget != null && !existingMutantTarget.status.equals(TargetExecution.Status.SUCCESS)
                    && existingMutantTarget.message != null && !existingMutantTarget.message.isEmpty()) {
                messages.add(existingMutantTarget.message);
            }
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, mutantText);
            Redirect.redirectBack(request, response);
            return;
        }

        final Mutant newMutant = gameManagingUtils.createMutant(gameId, game.getClassId(), mutantText, userId, MODE_PUZZLE_DIR);
        if (newMutant == null) {
            messages.add(MUTANT_CREATION_ERROR_MESSAGE);
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, mutantText);
            logger.error("Error creating mutant for puzzle game. Game: {}, Class: {}, User: {}. Aborting.", gameId, game.getClassId(), userId, mutantText);
            Redirect.redirectBack(request, response);
            return;
        }

        final TargetExecution compileMutantTarget = TargetExecutionDAO.getTargetExecutionForMutant(newMutant,
                TargetExecution.Target.COMPILE_MUTANT);
        boolean compileSuccess = compileMutantTarget != null
                && compileMutantTarget.status == TargetExecution.Status.SUCCESS;
        String errorMessage = (compileMutantTarget != null
                && compileMutantTarget.message != null
                && !compileMutantTarget.message.isEmpty())
                ? compileMutantTarget.message : null;

        MutantCompiledEvent mce = new MutantCompiledEvent();
        mce.setGameId(gameId);
        mce.setUserId(userId);
        mce.setMutantId(newMutant.getId());
        mce.setSuccess(compileSuccess);
        mce.setErrorMessage(errorMessage);

        if (!compileSuccess) {
            messages.add(MUTANT_UNCOMPILABLE_MESSAGE);
            if (errorMessage != null) {
                messages.add(errorMessage);
            }
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, mutantText);
            Redirect.redirectBack(request, response);
            return;
        }

        messages.add(MUTANT_COMPILED_MESSAGE);
        session.removeAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
        mutationTester.runAllTestsOnMutant(game, newMutant, messages);

        MutantTestedEvent mte = new MutantTestedEvent();
        mte.setGameId(gameId);
        mte.setUserId(userId);
        mte.setMutantId(newMutant.getId());
        notificationService.post(mte);

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
            messages.clear();
            boolean isAnAttackGame = true;
            messages.add( generateWinningMessage(request, userId, game, isAnAttackGame));
        }
        PuzzleDAO.updatePuzzleGame(game);
        Redirect.redirectBack(request, response);
    }

    private String generateWinningMessage(HttpServletRequest request, int userId, PuzzleGame game, boolean isAnAttackGame) {
        StringBuffer message = new StringBuffer();
        message.append("Congratulations, your " + (isAnAttackGame ? "mutant" : "test") + " solved the puzzle!");

        /*
         * TODO: this code does not yet consider already solved and locked
         * puzzles.
         */
        int currentChapter = game.getPuzzle().getChapterId();
        int currentPositionInChapter = game.getPuzzle().getPosition();

        /*
         * Find the next puzzle in the same chapter or the first puzzle in the
         * next not empty chapters
         */
        for (PuzzleChapter puzzleChapter : PuzzleDAO.getPuzzleChapters()) {

            // Skip chapters before this one
            if (puzzleChapter.getChapterId() < currentChapter) {
                continue;
            }
            // Check in current and next chapters
            else if (puzzleChapter.getChapterId() >= currentChapter) {
                /*
                 * This returns the puzzles ordered by position and (hopefully)
                 * and empty, not-null list if there's not puzzles
                 */
                for (Puzzle puzzle : PuzzleDAO.getPuzzlesForChapterId(puzzleChapter.getChapterId())) {
                    if (puzzleChapter.getChapterId() == currentChapter
                            && puzzle.getPosition() <= currentPositionInChapter) {
                        // Skip past and current puzzles in the same chapter
                        continue;
                    }
                    // Skip already solved puzzles
                    PuzzleGame playedGame = PuzzleDAO.getLatestPuzzleGameForPuzzleAndUser(puzzle.getPuzzleId(), userId);
                    if (
                            playedGame == null || // Not yet played this puzzle
                            ( playedGame != null  && ! playedGame.getState().equals(GameState.SOLVED)) // played but not yet solved. Condition expressed to be readable.
                    ) {
                        message.append(" ")
                                .append("Try to solve the <a href=" + request.getContextPath() + Paths.PUZZLE_GAME
                                        + "?puzzleId=" + puzzle.getPuzzleId()
                                        + ">next Puzzle</a>, or go back to the <a href=" + request.getContextPath()
                                        + Paths.PUZZLE_GAME + ">Puzzle Overview</a>.");
                        return message.toString();
                    } else {
                        continue;
                    }
                }
            }
        }

        /*
         * If we got here, the user has solved all the puzzles ?
         */
        message.append(" ").append("You solved all the puzzles, go back to the <a href=" + request.getContextPath()
                + Paths.PUZZLE_GAME + ">Puzzle Overview</a>.");
        return message.toString();
    }
}
