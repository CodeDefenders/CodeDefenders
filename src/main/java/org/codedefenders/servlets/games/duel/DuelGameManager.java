/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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
package org.codedefenders.servlets.games.duel;

import org.apache.commons.lang.StringEscapeUtils;
import org.codedefenders.database.DuelGameDAO;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.execution.AntRunner;
import org.codedefenders.execution.MutationTester;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.duel.DuelGame;
import org.codedefenders.game.singleplayer.SinglePlayerGame;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.codedefenders.validation.code.CodeValidator;
import org.codedefenders.validation.code.CodeValidatorException;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.codedefenders.validation.code.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.codedefenders.util.Constants.MODE_DUEL_DIR;
import static org.codedefenders.util.Constants.MUTANT_ACCEPTED_EQUIVALENT_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_CLAIMED_EQUIVALENT_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_COMPILED_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_CREATION_ERROR_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_DUPLICATED_MESSAGE;
import static org.codedefenders.util.Constants.MUTANT_UNCOMPILABLE_MESSAGE;
import static org.codedefenders.util.Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT;
import static org.codedefenders.util.Constants.SESSION_ATTRIBUTE_PREVIOUS_TEST;
import static org.codedefenders.util.Constants.TEST_DID_NOT_COMPILE_MESSAGE;
import static org.codedefenders.util.Constants.TEST_DID_NOT_KILL_CLAIMED_MUTANT_MESSAGE;
import static org.codedefenders.util.Constants.TEST_DID_NOT_PASS_ON_CUT_MESSAGE;
import static org.codedefenders.util.Constants.TEST_GENERIC_ERROR_MESSAGE;
import static org.codedefenders.util.Constants.TEST_INVALID_MESSAGE;
import static org.codedefenders.util.Constants.TEST_KILLED_CLAIMED_MUTANT_MESSAGE;
import static org.codedefenders.util.Constants.TEST_PASSED_ON_CUT_MESSAGE;
import static org.codedefenders.validation.code.CodeValidator.DEFAULT_NB_ASSERTIONS;

/**
 * This {@link HttpServlet} handles retrieval and in-game management for {@link DuelGame DuelGames}.
 * <p>
 * {@code GET} requests allow accessing duel games and {@code POST} requests handle starting and ending games,
 * creation of tests, mutants and resolving equivalences.
 * <p>
 * Serves under {@code /duelgame}.
 * @see Paths#DUEL_GAME
 */
public class DuelGameManager extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(DuelGameManager.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        int userId = ServletUtils.userId(request);

        final Optional<Integer> gameIdOpt = ServletUtils.gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.error("No gameId parameter. Redirecting to games overview.");
            response.sendRedirect(ServletUtils.ctx(request) + Paths.GAMES_OVERVIEW);
            return;
        }
        int gameId = gameIdOpt.get();

        final DuelGame game = DuelGameDAO.getDuelGameForId(gameId);
        if (game == null) {
            logger.error("No game found for gameId={}. Aborting request.", gameId);
            response.sendRedirect(ServletUtils.ctx(request) + Paths.GAMES_OVERVIEW);
            return;
        }

        if (game.getDefenderId() == userId) {
            request.setAttribute("game", game);
            request.getRequestDispatcher(Constants.DUEL_DEFENDER_VIEW_JSP).forward(request, response);
            return;
        }
        if (game.getAttackerId() == userId) {
            request.setAttribute("game", game);

            List<Mutant> equivMutants = game.getMutantsMarkedEquivalentPending();
            if (equivMutants.isEmpty()) {
                List<Mutant> aliveMutants = game.getAliveMutants();
                if (aliveMutants.isEmpty()) {
                    logger.info("No Mutants Alive, only attacker can play.");
                    game.setActiveRole(Role.ATTACKER);
                    game.update();
                }
                // If no mutants needed to be proved non-equivalent, direct to the Attacker Page.
                request.getRequestDispatcher(Constants.DUEL_ATTACKER_VIEW_JSP).forward(request, response);
            } else {
                request.setAttribute("equivMutant", equivMutants.get(0));
                request.getRequestDispatcher(Constants.DUEL_RESOLVE_EQUIVALENCE_JSP).forward(request, response);
            }
            return;
        }
        logger.info("User {} is not participating in game {}", userId, gameId);
        Redirect.redirectBack(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession();
        ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        final String action = ServletUtils.formType(request);
        switch (action) {
            case "resolveEquivalence":
                resolveEquivalence(request, response);
                return;
            case "claimEquivalent":
                claimEquivalent(request, response);
                return;
            case "whoseTurn":
                whoseTurn(request, response);
                return;
            case "createMutant":
                createMutant(request, response);
                return;
            case "createTest":
                createTest(request, response);
                return;
            default:
                logger.info("Action not recognised: {}", action);
                Redirect.redirectBack(request, response);
                break;
        }
    }

    @SuppressWarnings("Duplicates")
    private void resolveEquivalence(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        int userId = ServletUtils.userId(request);

        final Optional<Integer> gameIdOpt = ServletUtils.gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.error("No gameId parameter. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }
        int gameId = gameIdOpt.get();

        final DuelGame game = DuelGameDAO.getDuelGameForId(gameId);
        if (game == null) {
            logger.error("No game found for gameId={}. Aborting request.", gameId);
            Redirect.redirectBack(request, response);
            return;
        }

        final Optional<Integer> equivMutantIdOpt = ServletUtils.getIntParameter(request, "equivMutantId");
        if (!equivMutantIdOpt.isPresent()) {
            logger.error("No 'equivMutantId' parameter. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }
        int equivMutantId = equivMutantIdOpt.get();

        Mutant mutant = game.getMutantByID(equivMutantId);

        // Check type of equivalence response.
        if (request.getParameter("rejectEquivalent") != null) { // If user wanted to supply a test
            logger.debug("Equivalence rejected for mutant {}, processing killing test", equivMutantId);

            // Get the text submitted by the user.
            String testText = request.getParameter("test");

            // If it can be written to file and compiled, end turn. Otherwise, dont.

            Test newTest;

            try {
                newTest = GameManagingUtils.createTest(game.getId(), game.getClassId(), testText, userId, MODE_DUEL_DIR);
            } catch (CodeValidatorException e) {
                logger.warn("Handled CodeValidator error.", e);
                messages.add(TEST_GENERIC_ERROR_MESSAGE);
                session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
                response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
                return;
            }
            if (newTest == null) {
                messages.add(String.format(TEST_INVALID_MESSAGE, DEFAULT_NB_ASSERTIONS));
                session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
                response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
                return;
            }

            TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);

            if (compileTestTarget == null || compileTestTarget.status != TargetExecution.Status.SUCCESS) {
                logger.debug("compileTestTarget: " + compileTestTarget);
                messages.add(TEST_DID_NOT_COMPILE_MESSAGE);
                if (compileTestTarget != null) {
                    messages.add(compileTestTarget.message);
                }
                session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
                response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
                return;
            }
            TargetExecution testOriginalTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TargetExecution.Target.TEST_ORIGINAL);
            if (testOriginalTarget.status != TargetExecution.Status.SUCCESS) {
                //  (testOriginalTarget.state.equals(TargetExecution.Status.FAIL) || testOriginalTarget.state.equals(TargetExecution.Status.ERROR)
                logger.debug("testOriginalTarget: " + testOriginalTarget);
                messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE);
                messages.add(testOriginalTarget.message);
                session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
                response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
                return;
            }
            logger.info(TEST_PASSED_ON_CUT_MESSAGE);
            if (!mutant.isAlive() || mutant.getEquivalent() != Mutant.Equivalence.PENDING_TEST) {
                game.endRound();
                game.update();
                messages.add(TEST_KILLED_CLAIMED_MUTANT_MESSAGE);
                response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
                return;
            }
            // TODO: Allow multiple trials?
            // TODO: Doesnt differentiate between failing because the test didn't run and failing because it detected the mutant
            MutationTester.runEquivalenceTest(newTest, mutant);
            game.endRound();
            game.update();
            Mutant mutantAfterTest = game.getMutantByID(equivMutantId);
            if (mutantAfterTest.getEquivalent() == Mutant.Equivalence.PROVEN_NO) {
                logger.info("Test {} killed mutant {}, hence NOT equivalent", newTest.getId(), mutant.getId());
                messages.add(TEST_KILLED_CLAIMED_MUTANT_MESSAGE);
            } else {
                // test did not kill the mutant, lost duel, kill mutant
                mutantAfterTest.kill(Mutant.Equivalence.ASSUMED_YES);

                logger.info("Test {} failed to kill mutant {}", newTest.getId(), mutant.getId());
                messages.add(TEST_DID_NOT_KILL_CLAIMED_MUTANT_MESSAGE);
            }
            response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
        } else if (request.getParameter("acceptEquivalent") != null) { // If the user didnt want to supply a test
            logger.info("Equivalence accepted for mutant {}", mutant.getId());
            if (!mutant.isAlive() || mutant.getEquivalent() != Mutant.Equivalence.PENDING_TEST) {
                response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
                return;
            }
            mutant.kill(Mutant.Equivalence.DECLARED_YES);

            messages.add(MUTANT_ACCEPTED_EQUIVALENT_MESSAGE);
            game.endRound();
            game.update();
            response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
        }
    }

    private void claimEquivalent(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        final Optional<Integer> gameIdOpt = ServletUtils.gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.error("No gameId parameter. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }
        int gameId = gameIdOpt.get();

        final DuelGame game = DuelGameDAO.getDuelGameForId(gameId);
        if (game == null) {
            logger.error("No game found for gameId={}. Aborting request.", gameId);
            Redirect.redirectBack(request, response);
            return;
        }

        Optional<String> equivLinesParam = ServletUtils.getStringParameter(request, "equivLines");
        if (!equivLinesParam.isPresent()) {
            logger.debug("Missing 'equivLines' parameter.");
            Redirect.redirectBack(request, response);
            return;
        }

        AtomicInteger claimedMutants = new AtomicInteger();
        AtomicBoolean noneCovered = new AtomicBoolean(true);
        List<Mutant> mutantsAlive = game.getAliveMutants();

        Arrays.stream(equivLinesParam.get().split(","))
                .map(Integer::parseInt)
                .forEach(line -> {
                    noneCovered.set(false);
                    mutantsAlive.stream()
                            .filter(m -> m.getLines().contains(line) && m.getCreatorId() != Constants.DUMMY_ATTACKER_USER_ID)
                            .forEach(m -> {
                                if (game.getMode() == GameMode.SINGLE) {
                                    // TODO: Why is this not handled in the single player game but here?
                                    //Singleplayer - use automatic system.
                                    if (AntRunner.potentialEquivalent(m)) {
                                        //Is potentially equiv - accept as equivalent
                                        m.kill(Mutant.Equivalence.DECLARED_YES);
                                        messages.add("The AI has accepted the mutant as equivalent.");
                                    } else {
                                        m.kill(Mutant.Equivalence.PROVEN_NO);
                                        messages.add("The AI has submitted a test that kills the mutant and proves it non-equivalent!");
                                    }
                                    game.endTurn();
                                    if (game.getState() != GameState.FINISHED) {
                                        //The ai should make another move if the game isn't over
                                        SinglePlayerGame spg = (SinglePlayerGame) game;
                                        if (spg.getAi().makeTurn()) {
                                            messages.addAll(spg.getAi().getMessagesLastTurn());
                                        }
                                    }
                                } else {
                                    m.setEquivalent(Mutant.Equivalence.PENDING_TEST);
                                    m.update();
                                    messages.add(MUTANT_CLAIMED_EQUIVALENT_MESSAGE);
                                    claimedMutants.incrementAndGet();
                                }
                            });
                });

        int nClaimed = claimedMutants.get();
        String flaggingMessage = nClaimed == 0
                ? "Mutant has already been claimed as equivalent or killed!"
                : String.format("Flagged %d mutant%s as equivalent", nClaimed,
                (nClaimed == 1 ? "" : 's'));
        messages.add(flaggingMessage);
        game.passPriority();
        game.update();
        response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
    }

    private void whoseTurn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final Optional<Integer> gameIdOpt = ServletUtils.gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.error("No gameId parameter. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }
        int gameId = gameIdOpt.get();

        final DuelGame game = DuelGameDAO.getDuelGameForId(gameId);
        if (game == null) {
            logger.error("No game found for gameId={}. Aborting request.", gameId);
            Redirect.redirectBack(request, response);
            return;
        }

        String turn = game.getActiveRole() == Role.ATTACKER ? "attacker" : "defender";
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(turn);
    }

    private void createMutant(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        int userId = ServletUtils.userId(request);

        final Optional<Integer> gameIdOpt = ServletUtils.gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.error("No gameId parameter. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }
        int gameId = gameIdOpt.get();

        final DuelGame game = DuelGameDAO.getDuelGameForId(gameId);
        if (game == null) {
            logger.error("No game found for gameId={}. Aborting request.", gameId);
            Redirect.redirectBack(request, response);
            return;
        }

        // Get the text submitted by the user.
        final Optional<String> mutant = ServletUtils.getStringParameter(request, "mutant");
        if (!mutant.isPresent()) {
            session.removeAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
            response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
            return;
        }
        final String mutantText = mutant.get();

        // Duels are always 'strict'
        ValidationMessage validationMessage = CodeValidator.validateMutantGetMessage(game.getCUT().getSourceCode(), mutantText, CodeValidatorLevel.STRICT);
        if (validationMessage != ValidationMessage.MUTANT_VALIDATION_SUCCESS) {
            // Mutant is either the same as the CUT or it contains invalid code
            // Do not restore mutated code
            messages.add(validationMessage.get());
            response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
            return;
        }
        Mutant existingMutant = GameManagingUtils.existingMutant(gameId, mutantText);
        if (existingMutant != null) {
            messages.add(MUTANT_DUPLICATED_MESSAGE);
            TargetExecution existingMutantTarget = TargetExecutionDAO.getTargetExecutionForMutant(existingMutant, TargetExecution.Target.COMPILE_MUTANT);
            if (existingMutantTarget != null && existingMutantTarget.status != TargetExecution.Status.SUCCESS
                    && existingMutantTarget.message != null && !existingMutantTarget.message.isEmpty()) {
                messages.add(existingMutantTarget.message);
            }
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, StringEscapeUtils.escapeHtml(mutantText));
            response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
            return;
        }
        Mutant newMutant = GameManagingUtils.createMutant(gameId, game.getClassId(), mutantText, userId, MODE_DUEL_DIR);
        if (newMutant == null) {
            messages.add(MUTANT_CREATION_ERROR_MESSAGE);
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, StringEscapeUtils.escapeHtml(mutantText));
            logger.error("Error creating mutant. Game: {}, Class: {}, User: {}", gameId, game.getClassId(), userId, mutantText);
            response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
            return;
        }
        TargetExecution compileMutantTarget = TargetExecutionDAO.getTargetExecutionForMutant(newMutant, TargetExecution.Target.COMPILE_MUTANT);
        if (compileMutantTarget == null || compileMutantTarget.status != TargetExecution.Status.SUCCESS) {
            messages.add(MUTANT_UNCOMPILABLE_MESSAGE);
            if (compileMutantTarget != null && compileMutantTarget.message != null && !compileMutantTarget.message.isEmpty()) {
                messages.add(compileMutantTarget.message);
            }
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, StringEscapeUtils.escapeHtml(mutantText));
            response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
            return;
        }
        messages.add(MUTANT_COMPILED_MESSAGE);
        MutationTester.runAllTestsOnMutant(game, newMutant, messages);

        if (game.getMode() != GameMode.SINGLE) {
            game.endTurn();
        } else {
            // TODO: Why doesnt that happen in SinglePlayerGame.endTurn()?
            //Singleplayer - check for potential equivalent.
            if (AntRunner.potentialEquivalent(newMutant)) {
                //Is potentially equiv - mark as equivalent and update.
                messages.add("The AI has started an equivalence challenge on your last mutant.");
                newMutant.setEquivalent(Mutant.Equivalence.PENDING_TEST);
                newMutant.update();
                game.update();
            } else {
                game.endTurn();
                SinglePlayerGame g = (SinglePlayerGame) game;
                if (g.getAi().makeTurn()) {
                    messages.addAll(g.getAi().getMessagesLastTurn());
                }
            }
        }
        game.update();
        response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
    }

    @SuppressWarnings("Duplicates")
    private void createTest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        int userId = ServletUtils.userId(request);

        final Optional<Integer> gameIdOpt = ServletUtils.gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.error("No gameId parameter. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }
        int gameId = gameIdOpt.get();

        final DuelGame game = DuelGameDAO.getDuelGameForId(gameId);
        if (game == null) {
            logger.error("No game found for gameId={}. Aborting request.", gameId);
            Redirect.redirectBack(request, response);
            return;
        }

        // Get the text submitted by the user.
        // Get the text submitted by the user.
        final Optional<String> test = ServletUtils.getStringParameter(request, "test");
        if (!test.isPresent()) {
            session.removeAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST);
            response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
            return;
        }
        final String testText = test.get();

        Test newTest;
        try {
            newTest = GameManagingUtils.createTest(gameId, game.getClassId(), testText, userId, MODE_DUEL_DIR);
        } catch (CodeValidatorException e) {
            logger.warn("Handled CodeValidator error.", e);
            messages.add(TEST_GENERIC_ERROR_MESSAGE);
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
            response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
            return;
        }

        if (newTest == null) {
            messages.add(String.format(TEST_INVALID_MESSAGE, DEFAULT_NB_ASSERTIONS));
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
            response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
            return;
        }
        logger.debug("New Test " + newTest.getId());

        TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);
        if (compileTestTarget == null || compileTestTarget.status != TargetExecution.Status.SUCCESS) {
            messages.add(TEST_DID_NOT_COMPILE_MESSAGE);
            if (compileTestTarget != null) {
                messages.add(compileTestTarget.message);
            }
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
            response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
            return;
        }
        TargetExecution testOriginalTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TargetExecution.Target.TEST_ORIGINAL);
        if (testOriginalTarget.status != TargetExecution.Status.SUCCESS) {
            // testOriginalTarget.state.equals(TargetExecution.Status.FAIL) || testOriginalTarget.state.equals(TargetExecution.Status.ERROR)
            messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE);
            messages.add(testOriginalTarget.message);
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
            response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
            return;
        }
        messages.add(TEST_PASSED_ON_CUT_MESSAGE);
        MutationTester.runTestOnAllMutants(game, newTest, messages);

        game.endTurn();
        game.update();

        // TODO: Why doesn't that simply happen in SinglePlayerGame.endTurn?
        // if single-player game is not finished, make a move
        if (game.getState() != GameState.FINISHED && game.getMode() == GameMode.SINGLE) {
            SinglePlayerGame g = (SinglePlayerGame) game;
            if (g.getAi().makeTurn()) {
                messages.addAll(g.getAi().getMessagesLastTurn());
            }
        }
        response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
    }
}