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
package org.codedefenders.servlets.games.battleground;

import org.apache.commons.lang.StringEscapeUtils;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.IntentionDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.database.TestSmellsDAO;
import org.codedefenders.database.UserDAO;
import org.codedefenders.execution.MutationTester;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.AttackerIntention;
import org.codedefenders.model.DefenderIntention;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.codedefenders.game.Mutant.Equivalence.ASSUMED_YES;
import static org.codedefenders.servlets.util.ServletUtils.ctx;
import static org.codedefenders.util.Constants.GRACE_PERIOD_MESSAGE;
import static org.codedefenders.util.Constants.MODE_BATTLEGROUND_DIR;
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

/**
 * This {@link HttpServlet} handles retrieval and in-game management for {@link MultiplayerGame battleground games}.
 * <p>
 * {@code GET} requests allow accessing battleground games and {@code POST} requests handle starting and ending games,
 * creation of tests, mutants and resolving equivalences.
 * <p>
 * Serves under {@code /multiplayergame}.
 *
 * @see org.codedefenders.util.Paths#BATTLEGROUND_GAME
 */
public class MultiplayerGameManager extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(MultiplayerGameManager.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final Optional<Integer> gameIdOpt = ServletUtils.gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.error("No gameId parameter. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }
        request.setAttribute("gameId", gameIdOpt.get());

        RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.BATTLEGROUND_GAME_VIEW_JSP);
        dispatcher.forward(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final Optional<Integer> gameIdOpt = ServletUtils.gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.error("No gameId parameter. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }
        final int gameId = gameIdOpt.get();

        final MultiplayerGame game = MultiplayerGameDAO.getMultiplayerGame(gameId);
        if (game == null) {
            logger.error("Could not retrieve game from database for gameId: {}", gameId);
            Redirect.redirectBack(request, response);
            return;
        }

        final String action = ServletUtils.formType(request);
        switch (action) {
            case "createMutant": {
                createMutant(request, response, gameId, game);
                return;
            }
            case "createTest": {
                createTest(request, response, gameId, game);
                return;
            }
            case "reset": {
                final HttpSession session = request.getSession();
                session.removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
                return;
            }
            case "resolveEquivalence": {
                resolveEquivalence(request, response, gameId, game);
                return;
            }
            default:
                logger.info("Action not recognised: {}", action);
                Redirect.redirectBack(request, response);
        }
    }

    @SuppressWarnings("Duplicates")
    private void createTest(HttpServletRequest request, HttpServletResponse response, int gameId, MultiplayerGame game) throws IOException {
        final int userId = ServletUtils.userId(request);

        final String contextPath = ctx(request);
        final HttpSession session = request.getSession();
        final ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        if (game.getRole(userId) != Role.DEFENDER) {
            messages.add("Can only submit tests if you are an Defender!");
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }
        if (game.getState() != GameState.ACTIVE) {
            messages.add(GRACE_PERIOD_MESSAGE);
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }
        // Get the text submitted by the user.
        final Optional<String> test = ServletUtils.getStringParameter(request, "test");
        if (!test.isPresent()) {
            session.removeAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST);
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }
        final String testText = test.get();

        // If it can be written to file and compiled, end turn. Otherwise, dont.
        Test newTest;
        try {
            newTest = GameManagingUtils.createTest(gameId, game.getClassId(), testText, userId, MODE_BATTLEGROUND_DIR, game.getMaxAssertionsPerTest());
        } catch (CodeValidatorException cve) {
            messages.add(TEST_GENERIC_ERROR_MESSAGE);
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }

        // If test is null, then test did compile but codevalidator triggered
        if (newTest == null) {
            messages.add(String.format(TEST_INVALID_MESSAGE, game.getMaxAssertionsPerTest()));
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }

        /*
         * Validation of Players Intention: if intentions must be
         * collected but none are specified in the user request we fail
         * the request, but keep the test code in the session
         */
        Set<Integer> selectedLines = new HashSet<>();
        Set<Integer> selectedMutants = new HashSet<>();

        if (game.isCapturePlayersIntention()) {
            boolean validatedCoveredLines = true;
//                        boolean validatedKilledMutants = true;

            // Prepare the validation message
            StringBuilder validationMessage = new StringBuilder();
            validationMessage.append("Cheeky! You cannot submit a test without specifying");

            final String selected_lines = request.getParameter("selected_lines");
            if (selected_lines != null) {
                Set<Integer> selectLinesSet = DefenderIntention.parseIntentionFromCommaSeparatedValueString(selected_lines);
                selectedLines.addAll(selectLinesSet);
            }

            if (selectedLines.isEmpty()) {
                validatedCoveredLines = false;
                validationMessage.append(" a line to cover");
            }
            // NOTE: We consider only covering lines at the moment
            // if (request.getParameter("selected_mutants") != null) {
            // selectedMutants.addAll(DefenderIntention
            // .parseIntentionFromCommaSeparatedValueString(request.getParameter("selected_mutants")));
            // }
            // if( selectedMutants.isEmpty() &&
            // game.isDeclareKilledMutants()) {
            // validatedKilledMutants = false;
            //
            // if( selectedLines.isEmpty() &&
            // game.isCapturePlayersIntention() ){
            // validationMessage.append(" or");
            // }
            //
            // validationMessage.append(" a mutant to kill");
            // }
            validationMessage.append(".");

            if (!validatedCoveredLines) { // || !validatedKilledMutants
                messages.add(validationMessage.toString());
                // Keep the test around
                session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
                response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
                return;
            }
        }
        logger.debug("New Test {} by user {}", newTest.getId(), userId);
        TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);

        if (game.isCapturePlayersIntention()) {
            collectDefenderIntentions(newTest, selectedLines, selectedMutants);
        }

        if (compileTestTarget.status != TargetExecution.Status.SUCCESS) {
            messages.add(TEST_DID_NOT_COMPILE_MESSAGE);
            messages.add(StringEscapeUtils.escapeHtml(compileTestTarget.message));
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }
        TargetExecution testOriginalTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TargetExecution.Target.TEST_ORIGINAL);
        if (testOriginalTarget.status != TargetExecution.Status.SUCCESS) {
            // testOriginalTarget.state.equals(TargetExecution.Status.FAIL) || testOriginalTarget.state.equals(TargetExecution.Status.ERROR)
            messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE);
            messages.add(StringEscapeUtils.escapeHtml(testOriginalTarget.message));
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }

        messages.add(TEST_PASSED_ON_CUT_MESSAGE);

        // Include Test Smells in the messages back to user
        includeDetectTestSmellsInMessages(newTest, messages);

        final String message = UserDAO.getUserById(userId).getUsername() + " created a test";
        final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        final Event notif = new Event(-1, gameId, userId, message, EventType.DEFENDER_TEST_CREATED, EventStatus.GAME, timestamp);
        notif.insert();

        MutationTester.runTestOnAllMultiplayerMutants(game, newTest, messages);
        game.update();
        logger.info("Successfully created test {} ", newTest.getId());
        response.sendRedirect(ctx(request) + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
    }

    private void createMutant(HttpServletRequest request, HttpServletResponse response, int gameId, MultiplayerGame game) throws IOException {
        final int userId = ServletUtils.userId(request);

        final String contextPath = ctx(request);
        final HttpSession session = request.getSession();
        final ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        if (game.getRole(userId) != Role.ATTACKER) {
            messages.add("Can only submit mutants if you are an Attacker!");
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }

        if (game.getState() != GameState.ACTIVE) {
            messages.add(GRACE_PERIOD_MESSAGE);
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }
        // Get the text submitted by the user.
        final Optional<String> mutant = ServletUtils.getStringParameter(request, "mutant");
        if (!mutant.isPresent()) {
            session.removeAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }
        final String mutantText = mutant.get();

        int attackerID = DatabaseAccess.getPlayerIdForMultiplayerGame(userId, gameId);

        // If the user has pending duels we cannot accept the mutant, but we keep it around
        // so students do not lose mutants once the duel is solved.
        if (GameManagingUtils.hasAttackerPendingMutantsInGame(gameId, attackerID)
                && (session.getAttribute(Constants.BLOCK_ATTACKER) != null) && ((Boolean) session.getAttribute(Constants.BLOCK_ATTACKER))) {
            messages.add(Constants.ATTACKER_HAS_PENDING_DUELS);
            // Keep the mutant code in the view for later
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, StringEscapeUtils.escapeHtml(mutantText));
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }

        CodeValidatorLevel codeValidatorLevel = game.getMutantValidatorLevel();

        ValidationMessage validationMessage = CodeValidator.validateMutantGetMessage(game.getCUT().getSourceCode(), mutantText, codeValidatorLevel);

        if (validationMessage != ValidationMessage.MUTANT_VALIDATION_SUCCESS) {
            // Mutant is either the same as the CUT or it contains invalid code
            messages.add(validationMessage.get());
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
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
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }
        Mutant newMutant = GameManagingUtils.createMutant(gameId, game.getClassId(), mutantText, userId, MODE_BATTLEGROUND_DIR);
        if (newMutant == null) {
            messages.add(MUTANT_CREATION_ERROR_MESSAGE);
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, StringEscapeUtils.escapeHtml(mutantText));
            logger.debug("Error creating mutant. Game: {}, Class: {}, User: {}", gameId, game.getClassId(), userId, mutantText);
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }
        TargetExecution compileMutantTarget = TargetExecutionDAO.getTargetExecutionForMutant(newMutant, TargetExecution.Target.COMPILE_MUTANT);
        if (compileMutantTarget == null || compileMutantTarget.status != TargetExecution.Status.SUCCESS) {
            messages.add(MUTANT_UNCOMPILABLE_MESSAGE);
            if (compileMutantTarget != null && compileMutantTarget.message != null && !compileMutantTarget.message.isEmpty()) {
                // Add compile output
                messages.add(compileMutantTarget.message);
            }
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, StringEscapeUtils.escapeHtml(mutantText));
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }

        messages.add(MUTANT_COMPILED_MESSAGE);
        final String notificationMsg = UserDAO.getUserById(userId).getUsername() + " created a mutant.";
        Event notif = new Event(-1, gameId, userId, notificationMsg, EventType.ATTACKER_MUTANT_CREATED, EventStatus.GAME,
                new Timestamp(System.currentTimeMillis() - 1000));
        notif.insert();
        MutationTester.runAllTestsOnMutant(game, newMutant, messages);
        game.update();

        if (game.isCapturePlayersIntention()) {
            AttackerIntention intention = AttackerIntention.fromString(request.getParameter("attacker_intention"));
            // This parameter is required !
            if (intention == null) {
                messages.add(ValidationMessage.MUTANT_MISSING_INTENTION.toString());
                session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, StringEscapeUtils.escapeHtml(mutantText));
                response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
                return;
            }
            collectAttackerIntentions(newMutant, intention);
        }
        // Clean the mutated code only if mutant is accepted
        session.removeAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
        logger.info("Successfully created mutant {} ", newMutant.getId());
        response.sendRedirect(ctx(request) + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
    }

    @SuppressWarnings("Duplicates")
    private void resolveEquivalence(HttpServletRequest request, HttpServletResponse response, int gameId, MultiplayerGame game) throws IOException {
        final int userId = ServletUtils.userId(request);

        final String contextPath = ctx(request);
        final HttpSession session = request.getSession();
        final ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        if (game.getRole(userId) != Role.ATTACKER) {
            messages.add("Can only resolve equivalence duels if you are an Attacker!");
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }
        if (game.getState() == GameState.FINISHED) {
            messages.add(String.format("Game %d has finished.", gameId));
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_SELECTION);
            return;
        }

        // Get the text submitted by the user.
        final Optional<String> test = ServletUtils.getStringParameter(request, "test");
        if (!test.isPresent()) {
            session.removeAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST);
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }
        final String testText = test.get();

        // If it can be written to file and compiled, end turn. Otherwise, dont.

        Test newTest;
        try {
            newTest = GameManagingUtils.createTest(gameId, game.getClassId(), testText, userId, MODE_BATTLEGROUND_DIR, game.getMaxAssertionsPerTest());
        } catch (CodeValidatorException cve) {
            messages.add(TEST_GENERIC_ERROR_MESSAGE);
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }

        // If test is null, it compiled but codevalidator triggered
        if (newTest == null) {
            messages.add(String.format(TEST_INVALID_MESSAGE, game.getMaxAssertionsPerTest()));
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }

        final Optional<Integer> currentEquivMutant = ServletUtils.getIntParameter(request, "currentEquivMutant");
        if (!currentEquivMutant.isPresent()) {
            logger.info("Missing currentEquivMutant parameter.");
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }
        int currentEquivMutantId = currentEquivMutant.orElse(-1);

        logger.debug("Executing Action resolveEquivalence for mutant {} and test {}", currentEquivMutantId, newTest.getId());
        TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TargetExecution.Target.COMPILE_TEST);

        if (compileTestTarget == null || compileTestTarget.status != TargetExecution.Status.SUCCESS) {
            logger.debug("compileTestTarget: " + compileTestTarget);
            messages.add(TEST_DID_NOT_COMPILE_MESSAGE);
            if (compileTestTarget != null) {
                messages.add(compileTestTarget.message);
            }
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }
        TargetExecution testOriginalTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TargetExecution.Target.TEST_ORIGINAL);
        if (testOriginalTarget.status != TargetExecution.Status.SUCCESS) {
            //  (testOriginalTarget.state.equals(TargetExecution.Status.FAIL) || testOriginalTarget.state.equals(TargetExecution.Status.ERROR)
            logger.debug("testOriginalTarget: " + testOriginalTarget);
            messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE);
            messages.add(testOriginalTarget.message);
            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }
        logger.debug("Test {} passed on the CUT", newTest.getId());

        // Instead of running equivalence on only one mutant, let's try with all mutants pending resolution
        List<Mutant> mutantsPendingTests = game.getMutantsMarkedEquivalentPending();
        boolean killedClaimed = false;
        int killedOthers = 0;
        for (Mutant mPending : mutantsPendingTests) {
            // TODO: Doesnt distinguish between failing because the test didnt run at all and failing because it detected the mutant
            MutationTester.runEquivalenceTest(newTest, mPending); // updates mPending
            if (mPending.getEquivalent() == Mutant.Equivalence.PROVEN_NO) {
                logger.debug("Test {} killed mutant {} and proved it non-equivalent", newTest.getId(), mPending.getId());
                // TODO Phil 23/09/18: comment below doesn't make sense, literally 0 points added.
                newTest.updateScore(0); // score 2 points for proving a mutant non-equivalent
                final String message = UserDAO.getUserById(userId).getUsername() + " killed mutant " + mPending.getId() + " in an equivalence duel.";
                Event notif = new Event(-1, gameId, userId, message, EventType.ATTACKER_MUTANT_KILLED_EQUIVALENT, EventStatus.GAME,
                        new Timestamp(System.currentTimeMillis()));
                notif.insert();
                if (mPending.getId() == currentEquivMutantId) {
                    killedClaimed = true;
                } else {
                    killedOthers++;
                }
            } else { // ASSUMED_YES
                if (mPending.getId() == currentEquivMutantId) {
                    // only kill the one mutant that was claimed
                    mPending.kill(ASSUMED_YES);
                    final String message = UserDAO.getUserById(userId).getUsername() +
                            " lost an equivalence duel. Mutant " + mPending.getId() +
                            " is assumed equivalent.";
                    Event notif = new Event(-1, gameId, userId, message,
                            EventType.DEFENDER_MUTANT_EQUIVALENT, EventStatus.GAME,
                            new Timestamp(System.currentTimeMillis()));
                    notif.insert();
                }
                logger.debug("Test {} failed to kill mutant {}, hence mutant is assumed equivalent", newTest.getId(), mPending.getId());
            }
        }
        if (killedClaimed) {
            messages.add(TEST_KILLED_CLAIMED_MUTANT_MESSAGE);
            if (killedOthers == 1) {
                messages.add("...and it also killed another claimed mutant!");
            } else if (killedOthers > 1) {
                messages.add(String.format("...and it also killed other %d claimed mutants!", killedOthers));
            }
        } else {
            messages.add(TEST_DID_NOT_KILL_CLAIMED_MUTANT_MESSAGE);
            if (killedOthers == 1) {
                messages.add("...however, your test did kill another claimed mutant!");
            } else if (killedOthers > 1) {
                messages.add(String.format("...however, your test killed other %d claimed mutants!", killedOthers));
            }
        }
        newTest.update();
        game.update();
        logger.info("Resolving equivalence was handled successfully");
        response.sendRedirect(ctx(request) + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
    }

    private void collectDefenderIntentions(Test newTest, Set<Integer> selectedLines, Set<Integer> selectedMutants) {
        try {
            DefenderIntention intention = new DefenderIntention(selectedLines, selectedMutants);
            IntentionDAO.storeIntentionForTest(newTest, intention);
        } catch (Exception e) {
            logger.error("Cannot store intention to database.", e);
        }
    }

    private void collectAttackerIntentions(Mutant newMutant, AttackerIntention intention) {
        try {
            IntentionDAO.storeIntentionForMutant(newMutant, intention);
        } catch (Exception e) {
            logger.error("Cannot store intention to database.", e);
        }
    }

    private void includeDetectTestSmellsInMessages(Test newTest, ArrayList<String> messages) {
        List<String> detectedTestSmells = TestSmellsDAO.getDetectedTestSmellsForTest(newTest);
        if (!detectedTestSmells.isEmpty()) {
            if (detectedTestSmells.size() == 1) {
                messages.add("Your test has the following smell: " + detectedTestSmells.get(0));
            } else {
                String join = String.join(", ", detectedTestSmells);
                messages.add("Your test has the following smells: " + join);
            }
        }
    }
}
