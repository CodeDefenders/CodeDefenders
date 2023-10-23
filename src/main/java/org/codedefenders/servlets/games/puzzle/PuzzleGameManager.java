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

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.text.StringEscapeUtils;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.game.PreviousSubmissionBean;
import org.codedefenders.beans.message.Message;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.PuzzleDAO;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.execution.IMutationTester;
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
import org.codedefenders.notification.events.server.game.GameSolvedEvent;
import org.codedefenders.notification.events.server.mutant.MutantCompiledEvent;
import org.codedefenders.notification.events.server.mutant.MutantDuplicateCheckedEvent;
import org.codedefenders.notification.events.server.mutant.MutantSubmittedEvent;
import org.codedefenders.notification.events.server.mutant.MutantTestedEvent;
import org.codedefenders.notification.events.server.mutant.MutantValidatedEvent;
import org.codedefenders.notification.events.server.test.TestSubmittedEvent;
import org.codedefenders.notification.events.server.test.TestTestedMutantsEvent;
import org.codedefenders.notification.events.server.test.TestValidatedEvent;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.codedefenders.validation.code.CodeValidator;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.codedefenders.validation.code.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.codedefenders.execution.TargetExecution.Target.COMPILE_MUTANT;
import static org.codedefenders.execution.TargetExecution.Target.COMPILE_TEST;
import static org.codedefenders.execution.TargetExecution.Target.TEST_ORIGINAL;
import static org.codedefenders.game.puzzle.solving.MutantSolvingStrategy.Types.SURVIVED_ALL_MUTANTS;
import static org.codedefenders.servlets.util.ServletUtils.gameId;
import static org.codedefenders.servlets.util.ServletUtils.getIntParameter;
import static org.codedefenders.util.Constants.DUMMY_ATTACKER_USER_ID;
import static org.codedefenders.util.Constants.DUMMY_DEFENDER_USER_ID;
import static org.codedefenders.util.Constants.MODE_PUZZLE_DIR;
import static org.codedefenders.util.Constants.MUTANT_COMPILED_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_CREATION_ERROR_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_DUPLICATED_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_UNCOMPILABLE_MESSAGE;
import static org.codedefenders.util.Constants.PUZZLE_GAME_ATTACKER_VIEW_JSP;
import static org.codedefenders.util.Constants.PUZZLE_GAME_DEFENDER_VIEW_JSP;
import static org.codedefenders.util.Constants.REQUEST_ATTRIBUTE_PUZZLE_GAME;
import static org.codedefenders.util.Constants.TEST_DID_NOT_COMPILE_MESSAGE;
import static org.codedefenders.util.Constants.TEST_DID_NOT_PASS_ON_CUT_MESSAGE;
import static org.codedefenders.util.Constants.TEST_GENERIC_ERROR_MESSAGE;
import static org.codedefenders.util.Constants.TEST_INVALID_MESSAGE;
import static org.codedefenders.util.Constants.TEST_PASSED_ON_CUT_MESSAGE;


/**
 * This {@link HttpServlet} handles retrieval and in-game management for {@link PuzzleGame PuzzleGames}.
 *
 * <p>{@code GET} requests allow accessing puzzle games and {@code POST} requests handle creating of tests or mutants.
 *
 * <p>Serves under {@code /puzzlegame}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 * @see PuzzleGame
 */
@WebServlet(org.codedefenders.util.Paths.PUZZLE_GAME)
public class PuzzleGameManager extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(PuzzleGameManager.class);

    @Inject
    private GameManagingUtils gameManagingUtils;

    @Inject
    private IMutationTester mutationTester;

    @Inject
    private INotificationService notificationService;

    @Inject
    private MessagesBean messages;

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private PreviousSubmissionBean previousSubmission;

    @Inject
    private EventDAO eventDAO;

    @Inject
    private GameProducer gameProducer;

    @Inject
    private UserRepository userRepo;

    @Inject
    private GameService gameService;

    @Inject
    private URLUtils url;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        if (!checkEnabled()) {
            // Send users to the home page
            response.sendRedirect(url.forPath("/"));
            return;
        }
        final PuzzleGame game;

        final Optional<Integer> gameIdOpt = gameId(request);
        boolean fromGameId = gameIdOpt.isPresent(); // else from puzzleId
        if (fromGameId) {
            final int gameId = gameIdOpt.get();
            gameProducer.setGameId(gameId);
            game = PuzzleDAO.getPuzzleGameForId(gameId);

            if (game == null) {
                logger.error("Cannot retrieve puzzle game page. Failed to retrieve puzzle game from database"
                        + "for gameId: {}.", gameId);
                response.sendRedirect(url.forPath(Paths.PUZZLE_OVERVIEW));
                return;
            } else {
                // TODO Should he make PuzzleDAO inject dependencies instead
                game.setEventDAO(eventDAO);
                game.setUserRepository(userRepo);
            }
            if (game.getCreatorId() != login.getUserId()) {
                logger.error("Cannot retrieve puzzle game page. User {} is not creator of the requested game: {}.",
                        login.getUserId(), gameId);
                response.sendRedirect(url.forPath(Paths.PUZZLE_OVERVIEW));
                return;
            }
        } else {
            final Optional<Integer> puzzleIdOpt = getIntParameter(request, "puzzleId");
            if (puzzleIdOpt.isEmpty()) {
                logger.error("Cannot retrieve puzzle game page. Failed to retrieve gameId and puzzleId from request.");
                response.sendRedirect(url.forPath(Paths.PUZZLE_OVERVIEW));
                return;
            }
            final int puzzleId = puzzleIdOpt.get();
            game = PuzzleDAO.getLatestPuzzleGameForPuzzleAndUser(puzzleId, login.getUserId());
            if (game == null) {
                logger.info("Failed to retrieve puzzle game from database. Creating game for puzzleId {} and userId {}",
                        puzzleId, login.getUserId());
                createGame(login.getUserId(), request, response);
                return;
            } else {
                gameProducer.setGameId(game.getId());
                // TODO Should he make PuzzleDAO inject dependencies instead
                game.setEventDAO(eventDAO);
                game.setUserRepository(userRepo);
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
                response.sendRedirect(url.forPath(Paths.PUZZLE_OVERVIEW));
        }
    }

    /**
     * Checks whether users can play puzzles.
     *
     * @return {@code true} when users can play puzzles, {@code false} otherwise.
     */
    public static boolean checkEnabled() {
        return AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.ALLOW_PUZZLE_SECTION).getBoolValue() &&
                !PuzzleDAO.getPuzzles().isEmpty();
    }

    /**
     * Creates a puzzle game for a given request for the required parameter {@code puzzleId}.
     *
     * <p>If the provided parameter is not valid, the request will abort and return a {@code 400} status code.
     *
     * <p>If a puzzle game can be created, the game is started and the user is redirected to the game page.
     *
     * @param request  the request to create a test.
     * @param response the response to the request.
     * @throws IOException when redirecting fails.
     */
    public void createGame(int userId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        final Optional<Integer> puzzleId = getIntParameter(request, "puzzleId");
        if (puzzleId.isEmpty()) {
            logger.error("Failed to retrieve puzzleId from request.");
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }

        final Puzzle puzzle = PuzzleDAO.getPuzzleForId(puzzleId.get());
        if (puzzle == null) {
            logger.error("Failed to retrieve puzzle from database for puzzleId: {}.", puzzleId);
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        }

        // check if puzzle is locked
        Optional<Puzzle> nextPuzzle = getNextPuzzleForUser(userId);
        if (nextPuzzle.isPresent() && nextPuzzle.get().getPuzzleId() != puzzle.getPuzzleId()) {
            logger.error("Cannot create puzzle game for puzzleId: {}. Puzzle is locked.", puzzleId);
            response.setStatus(SC_FORBIDDEN);
            Redirect.redirectBack(request, response);
            return;
        }

        final PuzzleGame game = createPuzzleGame(puzzle, userId);

        if (game == null) {
            logger.error("Failed to create puzzle game for puzzleId: {} and userId: {}.", puzzleId, userId);
            response.setStatus(SC_BAD_REQUEST);
            Redirect.redirectBack(request, response);
            return;
        } else {
            // TODO How we should handle dependency injection here?
            game.setEventDAO(eventDAO);
            game.setUserRepository(userRepo);
        }

        request.setAttribute(REQUEST_ATTRIBUTE_PUZZLE_GAME, game);

        String path = url.forPath(Paths.PUZZLE_GAME) + "?gameId=" + game.getId();
        response.sendRedirect(path);
    }

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
    public PuzzleGame createPuzzleGame(Puzzle puzzle, int uid) {
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

        gameManagingUtils.addPredefinedMutantsAndTests(game, true, true);

        if (!game.addPlayer(uid, game.getActiveRole())) {
            logger.error(errorMsg + " Could not add player to the game.");
            return null;
        }

        if (!gameService.startGame(game)) {
            logger.error(errorMsg + " Could not update game state.");
            return null;
        }

        return game;
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
        final HttpSession session = request.getSession();
        final String action = ServletUtils.formType(request);
        switch (action) {
            case "reset":
                previousSubmission.clear();
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
     *
     * <p>If the requesting user is not a defender or the test is not valid, the request will abort
     * and return a {@code 400} status code.
     *
     * <p>If the submitted test solves the puzzle, the puzzle game is finished. Otherwise, the number
     * of submits is incremented by one.
     *
     * <p>After request handling the user is redirected back to the game page.
     *
     * @param request  the request to create a test.
     * @param response the response to the request.
     * @param session  the session of the requesting user.
     * @throws IOException when redirecting fails.
     */
    @SuppressWarnings("Duplicates")
    private void createTest(HttpServletRequest request,
                            HttpServletResponse response,
                            HttpSession session) throws IOException {
        final Optional<Integer> gameIdOpt = gameId(request);
        if (gameIdOpt.isEmpty()) {
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
        } else {
            // TODO Should he make PuzzleDAO inject dependencies instead
            game.setEventDAO(eventDAO);
            game.setUserRepository(userRepo);
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
        tse.setUserId(login.getUserId());
        notificationService.post(tse);

        // TODO Why we have testText and not escaped(testText)?
        // Validate the test
        // Do the validation even before creating the mutant
        List<String> validationMessage = CodeValidator.validateTestCodeGetMessage(
                testText,
                game.getMaxAssertionsPerTest(),
                game.getCUT().getAssertionLibrary());
        boolean validationSuccess = validationMessage.isEmpty();

        TestValidatedEvent tve = new TestValidatedEvent();
        tve.setGameId(gameId);
        tve.setUserId(login.getUserId());
        tve.setSuccess(validationSuccess);
        tve.setValidationMessage(validationSuccess ? null : String.join("\n", validationMessage));
        notificationService.post(tve);

        if (!validationSuccess) {
            messages.addAll(validationMessage);
            previousSubmission.setTestCode(testText);
            Redirect.redirectBack(request, response);
            return;
        }

        final Test newTest;
        try {
            newTest = gameManagingUtils.createTest(gameId, game.getClassId(), testText, login.getUserId(),
                    MODE_PUZZLE_DIR);
        } catch (IOException e) {
            messages.add(TEST_GENERIC_ERROR_MESSAGE);
            previousSubmission.setTestCode(testText);
            Redirect.redirectBack(request, response);
            return;
        }
        if (newTest == null) {
            messages.add(String.format(TEST_INVALID_MESSAGE, game.getMaxAssertionsPerTest()));
            previousSubmission.setTestCode(testText);
            Redirect.redirectBack(request, response);
            return;
        }

        final TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, COMPILE_TEST);
        if (!compileTestTarget.status.equals(TargetExecution.Status.SUCCESS)) {
            messages.add(TEST_DID_NOT_COMPILE_MESSAGE).fadeOut(false);
            // We escape the content of the message for new tests since user can embed there anything
            String escapedHtml = StringEscapeUtils.escapeHtml4(compileTestTarget.message);
            // Extract the line numbers of the errors
            List<Integer> errorLines = GameManagingUtils.extractErrorLines(compileTestTarget.message);
            // Store them in the session so they can be picked up later
            previousSubmission.setErrorLines(errorLines);
            // We introduce our decoration
            String decorate = GameManagingUtils.decorateWithLinksToCode(escapedHtml, true, false);
            messages.add(decorate).escape(false).fadeOut(false);
            previousSubmission.setTestCode(testText);
            Redirect.redirectBack(request, response);
            return;
        }

        final TargetExecution testOriginalTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TEST_ORIGINAL);
        if (!testOriginalTarget.status.equals(TargetExecution.Status.SUCCESS)) {
            messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE).fadeOut(false);
            messages.add(testOriginalTarget.message).fadeOut(false);
            previousSubmission.setTestCode(testText);
            Redirect.redirectBack(request, response);
            return;
        }

        messages.add(TEST_PASSED_ON_CUT_MESSAGE);
        previousSubmission.clear();

        messages.add(mutationTester.runTestOnAllMutants(game, newTest));

        TestTestedMutantsEvent ttme = new TestTestedMutantsEvent();
        ttme.setGameId(gameId);
        ttme.setUserId(login.getUserId());
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
            Message message = messages.add(generateWinningMessage(request, game, isAnAttackGame))
                    .escape(false).fadeOut(false);

            GameSolvedEvent gse = new GameSolvedEvent();
            gse.setGameId(gameId);
            gse.setAttackPuzzle(isAnAttackGame);
            notificationService.post(gse);
        }
        PuzzleDAO.updatePuzzleGame(game);
        Redirect.redirectBack(request, response);
    }

    /**
     * Creates a mutant for an active puzzle game for a given request with the required parameters.
     *
     * <p>If the requesting user is not an attacker or the mutant is not valid, the request will abort
     * and return a {@code 400} status code.
     *
     * <p>If the submitted mutant solves the puzzle, the puzzle game is finished. Otherwise, the number
     * of submits is incremented by one.
     *
     * <p>After request handling the user is redirected back to the game page.
     *
     * @param request  the request to create a mutant.
     * @param response the response to the request.
     * @param session  the session of the requesting user.
     * @throws IOException when redirecting fails.
     */
    private void createMutant(HttpServletRequest request,
                              HttpServletResponse response,
                              HttpSession session) throws IOException {
        final Optional<Integer> gameIdOpt = gameId(request);
        if (gameIdOpt.isEmpty()) {
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
        } else {
            // TODO Should he make PuzzleDAO inject dependencies instead
            game.setEventDAO(eventDAO);
            game.setUserRepository(userRepo);
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
        mse.setUserId(login.getUserId());
        notificationService.post(mse);

        final CodeValidatorLevel mutantValidatorLevel = game.getMutantValidatorLevel();

        ValidationMessage validationMessage =
                CodeValidator.validateMutantGetMessage(game.getCUT().getSourceCode(), mutantText, mutantValidatorLevel);
        boolean validationSuccess = validationMessage == ValidationMessage.MUTANT_VALIDATION_SUCCESS;

        MutantValidatedEvent mve = new MutantValidatedEvent();
        mve.setGameId(gameId);
        mve.setUserId(login.getUserId());
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
        mdce.setUserId(login.getUserId());
        mdce.setSuccess(duplicateCheckSuccess);
        mdce.setDuplicateId(duplicateCheckSuccess ? null : existingMutant.getId());
        notificationService.post(mdce);

        if (!duplicateCheckSuccess) {
            messages.add(MUTANT_DUPLICATED_MESSAGE);
            TargetExecution existingMutantTarget =
                    TargetExecutionDAO.getTargetExecutionForMutant(existingMutant, COMPILE_MUTANT);
            if (existingMutantTarget != null && !existingMutantTarget.status.equals(TargetExecution.Status.SUCCESS)
                    && existingMutantTarget.message != null && !existingMutantTarget.message.isEmpty()) {
                // We escape the content of the message for new tests since user can embed there
                // anything
                String escapedHtml = StringEscapeUtils.escapeHtml4(existingMutantTarget.message);
                // Extract the line numbers of the errors
                List<Integer> errorLines = GameManagingUtils.extractErrorLines(existingMutantTarget.message);
                // Store them in the session so they can be picked up later
                previousSubmission.setErrorLines(errorLines);
                // We introduce our decoration
                String decorate = GameManagingUtils.decorateWithLinksToCode(escapedHtml, false, true);
                messages.add(decorate).escape(false);
            }
            previousSubmission.setMutantCode(mutantText);
            Redirect.redirectBack(request, response);
            return;
        }
        final Mutant newMutant =
                gameManagingUtils.createMutant(gameId, game.getClassId(), mutantText, login.getUserId(),
                        MODE_PUZZLE_DIR);
        if (newMutant == null) {
            messages.add(MUTANT_CREATION_ERROR_MESSAGE);
            previousSubmission.setMutantCode(mutantText);
            logger.error("Error creating mutant for puzzle game. Game: {}, Class: {}, User: {}. Aborting.",
                    gameId, game.getClassId(), login.getUserId());
            Redirect.redirectBack(request, response);
            return;
        }

        final TargetExecution compileMutantTarget = TargetExecutionDAO.getTargetExecutionForMutant(newMutant,
                COMPILE_MUTANT);
        boolean compileSuccess = compileMutantTarget != null
                && compileMutantTarget.status == TargetExecution.Status.SUCCESS;
        String errorMessage = (compileMutantTarget != null
                && compileMutantTarget.message != null
                && !compileMutantTarget.message.isEmpty())
                ? compileMutantTarget.message : null;

        MutantCompiledEvent mce = new MutantCompiledEvent();
        mce.setGameId(gameId);
        mce.setUserId(login.getUserId());
        mce.setMutantId(newMutant.getId());
        mce.setSuccess(compileSuccess);
        mce.setErrorMessage(errorMessage);
        notificationService.post(mce);

        if (!compileSuccess) {
            messages.add(MUTANT_UNCOMPILABLE_MESSAGE).fadeOut(false);
            if (errorMessage != null) {
                // We escape the content of the message for new tests since user can embed there anything
                String escapedHtml = StringEscapeUtils.escapeHtml4(errorMessage);
                // Extract the line numbers of the errors
                List<Integer> errorLines = GameManagingUtils.extractErrorLines(errorMessage);
                // Store them in the session so they can be picked up later
                previousSubmission.setErrorLines(errorLines);
                // We introduce our decoration
                String decorate = GameManagingUtils.decorateWithLinksToCode(escapedHtml, false, true);
                messages.add(decorate).escape(false).fadeOut(false);
            }
            previousSubmission.setMutantCode(mutantText);
            Redirect.redirectBack(request, response);
            return;
        }

        messages.add(MUTANT_COMPILED_MESSAGE);
        previousSubmission.clear();
        messages.add(mutationTester.runAllTestsOnMutant(game, newMutant));

        MutantTestedEvent mte = new MutantTestedEvent();
        mte.setGameId(gameId);
        mte.setUserId(login.getUserId());
        mte.setMutantId(newMutant.getId());
        notificationService.post(mte);

        // may be // final MutantSolvingStrategy solving = game.getMutantSolver();
        final MutantSolvingStrategy solver = MutantSolvingStrategy.get(SURVIVED_ALL_MUTANTS.name());
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
            messages.add(generateWinningMessage(request, game, isAnAttackGame))
                    .escape(false).fadeOut(false);

            GameSolvedEvent gse = new GameSolvedEvent();
            gse.setGameId(gameId);
            gse.setAttackPuzzle(isAnAttackGame);
            notificationService.post(gse);
        }
        PuzzleDAO.updatePuzzleGame(game);
        Redirect.redirectBack(request, response);
    }

    /**
     * Retrieves the first puzzle that has not been solved yet by the given user.
     *
     * @param userId The user to retrieve the next puzzle for.
     * @return The next puzzle for the given user, or an empty Optional if no puzzle is available / all puzzles were
     * solved.
     */
    private Optional<Puzzle> getNextPuzzleForUser(int userId) {
        for (PuzzleChapter puzzleChapter : PuzzleDAO.getPuzzleChapters()) {
            /*
             * This returns the puzzles ordered by position and (hopefully)
             * an empty, not-null list if there's no puzzles
             */
            for (Puzzle puzzle : PuzzleDAO.getPuzzlesForChapterId(puzzleChapter.getChapterId())) {
                // TODO Should he make PuzzleDAO inject dependencies instead
                PuzzleGame playedGame = PuzzleDAO.getLatestPuzzleGameForPuzzleAndUser(
                        puzzle.getPuzzleId(),
                        login.getUserId()
                );

                if (playedGame == null // Not yet played this puzzle
                        || (playedGame.getState() != GameState.SOLVED) // played but not yet solved.
                ) {
                    // first not solved puzzle
                    return Optional.of(puzzle);
                }
            }
        }
        return Optional.empty();
    }

    private String generateWinningMessage(HttpServletRequest request, PuzzleGame game, boolean isAnAttackGame) {
        StringBuilder message = new StringBuilder();
        message.append("Congratulations, your ")
                .append(isAnAttackGame ? "mutant" : "test")
                .append(" solved the puzzle!");

        int currentChapter = game.getPuzzle().getChapterId();
        int currentPositionInChapter = game.getPuzzle().getPosition();

        /*
         * Find the next puzzle in the same chapter or the first puzzle in the next not empty chapters.
         *
         * {@link PuzzleGameManager#getNextPuzzleForUser(int)} cannot be used here, because the puzzle is marked as
         * solved only after this message is generated.
         */
        for (PuzzleChapter puzzleChapter : PuzzleDAO.getPuzzleChapters()) {
            // Skip chapters before this one
            if (puzzleChapter.getChapterId() < currentChapter) {
                continue;
            } else if (puzzleChapter.getChapterId() >= currentChapter) {
                // Check in current and next chapters
                /*
                 * This returns the puzzles ordered by position and (hopefully)
                 * empty, not-null list if there are no puzzles
                 */
                for (Puzzle puzzle : PuzzleDAO.getPuzzlesForChapterId(puzzleChapter.getChapterId())) {
                    if (puzzleChapter.getChapterId() == currentChapter
                            && puzzle.getPosition() <= currentPositionInChapter) {
                        // Skip past and current puzzles in the same chapter
                        continue;
                    }
                    // Skip already solved puzzles
                    // TODO Should he make PuzzleDAO inject dependencies instead
                    PuzzleGame playedGame = PuzzleDAO.getLatestPuzzleGameForPuzzleAndUser(puzzle.getPuzzleId(),
                            login.getUserId());

                    if (playedGame == null // Not yet played this puzzle
                            || (playedGame.getState() != GameState.SOLVED) // played but not yet solved.
                    ) {
                        message.append(" ")
                                .append("Try to solve the <a href=")
                                .append(url.forPath(Paths.PUZZLE_GAME))
                                .append("?puzzleId=")
                                .append(puzzle.getPuzzleId())
                                .append(">next Puzzle</a>, or go back to the <a href=")
                                .append(url.forPath(Paths.PUZZLE_GAME))
                                .append(">Puzzle Overview</a>.");
                        return message.toString();
                    }
                    playedGame.setEventDAO(eventDAO);
                    playedGame.setUserRepository(userRepo);
                }
            }
        }

        /*
         * If we got here, the user has solved all the puzzles ?
         */
        message.append(" ")
                .append("You solved all the puzzles, go back to the <a href=")
                .append(url.forPath(Paths.PUZZLE_GAME))
                .append(">Puzzle Overview</a>.");
        return message.toString();
    }
}
