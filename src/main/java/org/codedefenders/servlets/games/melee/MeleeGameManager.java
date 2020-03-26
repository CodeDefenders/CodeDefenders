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
package org.codedefenders.servlets.games.melee;

import org.apache.commons.lang.StringEscapeUtils;
import org.codedefenders.beans.game.PreviousSubmissionBean;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.beans.user.LoginBean;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.IntentionDAO;
import org.codedefenders.database.MeleeGameDAO;
import org.codedefenders.database.PlayerDAO;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.database.TestSmellsDAO;
import org.codedefenders.database.UserDAO;
import org.codedefenders.execution.IMutationTester;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.tcs.ITestCaseSelector;
import org.codedefenders.model.AttackerIntention;
import org.codedefenders.model.DefenderIntention;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.model.User;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.codedefenders.validation.code.CodeValidator;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.codedefenders.validation.code.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
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
import static org.codedefenders.util.Constants.TEST_DID_NOT_COMPILE_MESSAGE;
import static org.codedefenders.util.Constants.TEST_DID_NOT_KILL_CLAIMED_MUTANT_MESSAGE;
import static org.codedefenders.util.Constants.TEST_DID_NOT_PASS_ON_CUT_MESSAGE;
import static org.codedefenders.util.Constants.TEST_GENERIC_ERROR_MESSAGE;
import static org.codedefenders.util.Constants.TEST_KILLED_CLAIMED_MUTANT_MESSAGE;
import static org.codedefenders.util.Constants.TEST_PASSED_ON_CUT_MESSAGE;

// TODO Alessio 18/02/2020: Differentiate between errorLines in the mutants and errorLines in the tests in the UI.
//  See: https://gitlab.infosun.fim.uni-passau.de/se2/codedefenders/CodeDefenders/merge_requests/505#note_17170

/**
 * This {@link HttpServlet} handles retrieval and in-game management for
 * {@link MeleeGame} games.
 *
 * <p>{@code GET} requests allow accessing melee games and {@code POST} requests
 * handle starting and ending games, creation of tests, mutants and resolving
 * equivalences.
 *
 * <p>Serves under {@code /meleegame}.
 *
 * @see org.codedefenders.util.Paths#MELEE_GAME
 */
@WebServlet(Paths.MELEE_GAME)
public class MeleeGameManager extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(MeleeGameManager.class);

    @Inject
    private GameManagingUtils gameManagingUtils;

    @Inject
    private IMutationTester mutationTester;

    @Inject
    private TestSmellsDAO testSmellsDAO;

    @Inject
    private ITestCaseSelector regressionTestCaseSelector;

    @Inject
    private INotificationService notificationService;

    @Inject
    private MessagesBean messages;

    @Inject
    private LoginBean login;

    @Inject
    private PreviousSubmissionBean previousSubmission;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Optional<Integer> gameIdOpt = ServletUtils.gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.info("No gameId parameter. Aborting request.");
            response.sendRedirect(ctx(request) + Paths.GAMES_OVERVIEW);
            return;
        }
        int gameId = gameIdOpt.get();

        MeleeGame game = MeleeGameDAO.getMeleeGame(gameId);
        if (game == null) {
            logger.error("Could not find melee game {}", gameId);
            response.sendRedirect(request.getContextPath() + Paths.GAMES_OVERVIEW);
            return;
        }
        int userId = login.getUserId();

        if (!game.hasUserJoined(userId) && game.getCreatorId() != userId) {
            logger.info("User {} not part of game {}. Aborting request.", userId, gameId);
            response.sendRedirect(ctx(request) + Paths.GAMES_OVERVIEW);
            return;
        }

        final int playerId = PlayerDAO.getPlayerIdForUserAndGame(userId, gameId);

        if (game.getCreatorId() != userId && playerId == -1) {
            // Something odd with the registration - TODO
            logger.warn("Wrong registration with the User {} in Melee Game {}", userId, gameId);
            response.sendRedirect(ctx(request) + Paths.GAMES_OVERVIEW);
            return;
        }

        // Check is there is a pending equivalence duel for this user.
        game.getMutantsMarkedEquivalentPending()
                .stream()
                .filter(m -> m.getPlayerId() == playerId)
                .findFirst()
                .ifPresent(mutant -> {
                    // TODO Check if this is really based on role...
                    int defenderId = DatabaseAccess.getEquivalentDefenderId(mutant);
                    User defender = UserDAO.getUserForPlayer(defenderId);
                    // TODO This should be a better name
                    request.setAttribute("equivDefender", defender);
                    request.setAttribute("equivMutant", mutant);
                    request.setAttribute("openEquivalenceDuel", true);
                });

        request.setAttribute("game", game);
        request.setAttribute("playerId", playerId);

        RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.MELEE_GAME_VIEW_JSP);
        dispatcher.forward(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final Optional<Integer> gameIdOpt = ServletUtils.gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.warn("No gameId parameter. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }
        final int gameId = gameIdOpt.get();

        final MeleeGame game = MeleeGameDAO.getMeleeGame(gameId);

        if (game == null) {
            logger.warn("Could not retrieve game from database for gameId: {}", gameId);
            Redirect.redirectBack(request, response);
            return;
        }

        if (!game.hasUserJoined(login.getUserId())) {
            logger.warn("User {} has not yet joined the game : {}", login.getUserId(), gameId);
            Redirect.redirectBack(request, response);
            return;
        }

        final String action = ServletUtils.formType(request);
        final User user = UserDAO.getUserById(login.getUserId());

        final int playerId = PlayerDAO.getPlayerIdForUserAndGame(login.getUserId(), gameId);

        if (playerId == -1) {
            // Something odd with the registration - TODO
            logger.warn("Wrong registration with the User {} in Melee Game {}", login.getUserId(), gameId);
            Redirect.redirectBack(request, response);
            return;
        }

        switch (action) {
            case "createMutant": {
                createMutant(request, response, user, game, playerId);
                triggerAutomaticMutantEquivalenceForGame(game);
                return;
            }
            case "createTest": {
                createTest(request, response, user, game);
                // After a test is submitted, there's the chance that one or more mutants
                // already survived enough tests
                triggerAutomaticMutantEquivalenceForGame(game);
                return;
            }
            case "reset": {
                final HttpSession session = request.getSession();
                // TODO Why those are commented out?
    //            session.removeAttribute(Constants.SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
                response.sendRedirect(ctx(request) + Paths.MELEE_GAME + "?gameId=" + game.getId());
                return;
            }
            case "claimEquivalent": {
                claimEquivalent(request, response, gameId, game, playerId);
                return;
            }
            case "resolveEquivalence": {
                resolveEquivalence(request, response, gameId, game, playerId);
                return;
            }
            default:
                logger.info("Action not recognised: {}", action);
                Redirect.redirectBack(request, response);
        }
    }

    // This is package protected to enable testing
    void triggerAutomaticMutantEquivalenceForGame(MeleeGame game) {
        int threshold = game.getAutomaticMutantEquivalenceThreshold();
        if (threshold < 1) {
            // No need to check as this feature is disabled
            return;
        }
        // Get all the live mutants in the game
        for (Mutant aliveMutant : game.getAliveMutants()) {
            /*
             * If the mutant is covered by enough tests trigger the automatic equivalence
             * duel
             */
            int coveringTests = aliveMutant.getCoveringTests().size();
            if (coveringTests >= threshold) {
                // Flag the mutant as possibly equivalent
                aliveMutant.setEquivalent(Mutant.Equivalence.PENDING_TEST);
                aliveMutant.update();
                // Send the notification about the flagged mutant to attacker
                int mutantOwnerId = aliveMutant.getPlayerId();
                Event event = new Event(-1, game.getId(), mutantOwnerId,
                        "One of your mutants survived "
                                + (threshold == aliveMutant.getCoveringTests().size() ? "" : "more than ") + threshold
                                + "tests so it was automatically claimed as equivalent.",
                        // TODO it might make sense to specify a new event type?
                        EventType.DEFENDER_MUTANT_EQUIVALENT, EventStatus.NEW,
                        new Timestamp(System.currentTimeMillis()));
                event.insert();
                /*
                 * Register the event to DB
                 */
                DatabaseAccess.insertEquivalence(aliveMutant, Constants.DUMMY_CREATOR_USER_ID);
                /*
                 * Send the notification about the flagged mutant to the game channel
                 */
                String flaggingChatMessage = "Code Defenders automatically flagged mutant " + aliveMutant.getId()
                        + " as equivalent.";
                Event gameEvent = new Event(-1, game.getId(), -1, flaggingChatMessage,
                        EventType.DEFENDER_MUTANT_CLAIMED_EQUIVALENT, EventStatus.GAME,
                        new Timestamp(System.currentTimeMillis()));
                gameEvent.insert();
            }
        }
    }

    @SuppressWarnings("Duplicates")
    private void createTest(HttpServletRequest request, HttpServletResponse response, User user, MeleeGame game)
            throws IOException {

        final String contextPath = ctx(request);
        final HttpSession session = request.getSession();

        if (game.getState() != GameState.ACTIVE) {
            messages.add(GRACE_PERIOD_MESSAGE);
            response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
            return;
        }

        // Get the text submitted by the user.
        final Optional<String> test = ServletUtils.getStringParameter(request, "test");
        if (!test.isPresent()) {
//            session.removeAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST);
            response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
            return;
        }

        final String testText = test.get();

        // TODO Where do we check that the test is not a duplicate ?!

        // Do the validation even before creating the mutant
        List<String> validationMessages = CodeValidator.validateTestCodeGetMessage(testText,
                game.getMaxAssertionsPerTest(), game.isForceHamcrest());
        if (!validationMessages.isEmpty()) {
            for (String validationMessage : validationMessages) {
                messages.add(validationMessage);
            }
//            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
            response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
            return;
        }

        // From this point on we assume that test is valid according to the rules (but
        // it might still not compile)
        Test newTest;
        try {
            // TODO Mistmatch ? We pass USER_ID as creator/owner but we will get back
            // PLAYER_ID for getCreatorId() ?
            newTest = gameManagingUtils.createTest(game.getId(), game.getClassId(), testText, user.getId(),
                    MODE_BATTLEGROUND_DIR);
        } catch (IOException io) {
            messages.add(TEST_GENERIC_ERROR_MESSAGE);
//            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
            response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
            return;
        }

        /*
         * Validation of Players Intention: if intentions must be collected but none are
         * specified in the user request we fail the request, but keep the test code in
         * the session
         */
        Set<Integer> selectedLines = new HashSet<>();
        Set<Integer> selectedMutants = new HashSet<>();

        if (game.isCapturePlayersIntention()) {
            boolean validatedCoveredLines = true;
            // Prepare the validation message
            StringBuilder userIntentionsValidationMessage = new StringBuilder();
            userIntentionsValidationMessage.append("Cheeky! You cannot submit a test without specifying");

            final String selected_lines = request.getParameter("selected_lines");
            if (selected_lines != null) {
                Set<Integer> selectLinesSet = DefenderIntention
                        .parseIntentionFromCommaSeparatedValueString(selected_lines);
                selectedLines.addAll(selectLinesSet);
            }

            if (selectedLines.isEmpty()) {
                validatedCoveredLines = false;
                userIntentionsValidationMessage.append(" a line to cover");
            }

            userIntentionsValidationMessage.append(".");

            if (!validatedCoveredLines) {
                messages.add(userIntentionsValidationMessage.toString());
//                session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
                response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
                return;
            }
        }

        logger.debug("New Test {} by user {}", newTest.getId(), user.getId());

        TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest,
                TargetExecution.Target.COMPILE_TEST);

        if (game.isCapturePlayersIntention()) {
            collectDefenderIntentions(newTest, selectedLines, selectedMutants);
            session.setAttribute("selected_lines", selectedLines.iterator().next());
        }

        if (compileTestTarget.status != TargetExecution.Status.SUCCESS) {
            messages.add(TEST_DID_NOT_COMPILE_MESSAGE);
            // We escape the content of the message for new tests since user can embed there
            // anything
            String escapedHtml = StringEscapeUtils.escapeHtml(compileTestTarget.message);
            // Extract the line numbers of the errors
            List<Integer> errorLines = extractErrorLines(compileTestTarget.message);
            // Store them in the session so they can be picked up later
//            session.setAttribute(SESSION_ATTRIBUTE_ERROR_LINES_IN_TEST, errorLines);
            // We introduce our decoration
            String decorate = decorateWithLinksToCode(escapedHtml, true, false);
            messages.add(decorate);
            //
//            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
            response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
            return;
        }
        TargetExecution testOriginalTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest,
                TargetExecution.Target.TEST_ORIGINAL);
        if (testOriginalTarget.status != TargetExecution.Status.SUCCESS) {
            messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE);
            messages.add(StringEscapeUtils.escapeHtml(testOriginalTarget.message));
//            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
            response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
            return;
        }

        messages.add(TEST_PASSED_ON_CUT_MESSAGE);

        // Include Test Smells in the messages back to user
        includeDetectTestSmellsInMessages(newTest, messages.getBridge());

        final String message = user.getUsername() + " created a test";
        final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        final Event notif = new Event(-1, game.getId(), user.getId(), message, EventType.DEFENDER_TEST_CREATED,
                EventStatus.GAME, timestamp);
        notif.insert();

        //
        mutationTester.runTestOnAllMeleeMutants(game, newTest, messages.getBridge());

        game.update();
        logger.info("Successfully created test {} ", newTest.getId());

        // Clean up the session
        session.removeAttribute("selected_lines");
        response.sendRedirect(ctx(request) + Paths.MELEE_GAME + "?gameId=" + game.getId());
    }

    /**
     * Return the line numbers mentioned in the error message of the compiler
     *
     * @param message
     * @return
     */
    List<Integer> extractErrorLines(String compilerOutput) {
        List<Integer> errorLines = new ArrayList<>();
        Pattern p = Pattern.compile("\\[javac\\].*\\.java:([0-9]+): error:.*");
        for (String line : compilerOutput.split("\n")) {
            Matcher m = p.matcher(line);
            if (m.find()) {
                // TODO may be not robust
                errorLines.add(Integer.parseInt(m.group(1)));
            }
        }
        return errorLines;
    }

    /**
     * Add links that points to line for errors. Not sure that invoking a JS
     * function suing a link in this way is 100% safe ! XXX Consider to move the
     * decoration utility, and possibly the sanitize methods to some other
     * components.
     */
    String decorateWithLinksToCode(String compilerOutput, boolean forTest, boolean forMutant) {
        String jumpFunction = "";
        if (forTest) {
            jumpFunction = "jumpToTestLine";
        } else if (forMutant) {
            jumpFunction = "jumpToMutantLine";
        }

        StringBuffer decorated = new StringBuffer();
        Pattern p = Pattern.compile("\\[javac\\].*\\.java:([0-9]+): error:.*");
        for (String line : compilerOutput.split("\n")) {
            Matcher m = p.matcher(line);
            if (m.find()) {
                // Replace the entire line with a link to the source code
                String replacedLine = "<a onclick=\"" + jumpFunction + "(" + m.group(1)
                        + ")\" href=\"javascript:void(0);\">" + line + "</a>";
                decorated.append(replacedLine).append("\n");
            } else {
                decorated.append(line).append("\n");
            }
        }
        return decorated.toString();
    }

    private void createMutant(HttpServletRequest request, HttpServletResponse response, User user, MeleeGame game,
            int playerId) throws IOException {

        final String contextPath = ctx(request);
        final HttpSession session = request.getSession();

        if (game.getState() != GameState.ACTIVE) {
            messages.add(GRACE_PERIOD_MESSAGE);
            response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
            return;
        }

        // Get the text submitted by the user.
        final Optional<String> mutant = ServletUtils.getStringParameter(request, "mutant");
        if (!mutant.isPresent()) {
            response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
            return;
        }

        final String mutantText = mutant.get();

        /*
         * If the user has pending duels we cannot accept this mutant, but we keep it
         * around so the user does not lose it once the duel is solved. TODO I guess we
         * need to store it somewhere...
         * session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT,
         * StringEscapeUtils.escapeHtml(mutantText));
         */
        if (gameManagingUtils.hasAttackerPendingMutantsInGame(game.getId(), playerId)
                && (session.getAttribute(Constants.BLOCK_ATTACKER) != null)
                && ((Boolean) session.getAttribute(Constants.BLOCK_ATTACKER))) {
            messages.add(Constants.ATTACKER_HAS_PENDING_DUELS);
            response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
            return;
        }

        // Do the validation even before creating the mutant
        CodeValidatorLevel codeValidatorLevel = game.getMutantValidatorLevel();
        ValidationMessage validationMessage = CodeValidator.validateMutantGetMessage(game.getCUT().getSourceCode(),
                mutantText, codeValidatorLevel);

        if (validationMessage != ValidationMessage.MUTANT_VALIDATION_SUCCESS) {
            // Mutant is either the same as the CUT or it contains invalid code
            messages.add(validationMessage.get());
            response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
            return;
        }
        Mutant existingMutant = gameManagingUtils.existingMutant(game.getId(), mutantText);
        //
        if (existingMutant != null && existingMutant.getCreatorId() == playerId) {
            messages.add(MUTANT_DUPLICATED_MESSAGE);
            TargetExecution existingMutantTarget = TargetExecutionDAO.getTargetExecutionForMutant(existingMutant,
                    TargetExecution.Target.COMPILE_MUTANT);
            if (existingMutantTarget != null && existingMutantTarget.status != TargetExecution.Status.SUCCESS
                    && existingMutantTarget.message != null && !existingMutantTarget.message.isEmpty()) {
                messages.add(existingMutantTarget.message);
            }
            response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
            return;
        }
        // TODO There is a mistmatch. We pass the USER_ID while creating a mutant, but
        // then we get the PLAYER_ID when we get id of the mutants' creator?
        Mutant newMutant = gameManagingUtils.createMutant(game.getId(), game.getClassId(), mutantText, user.getId(),
                // TODO Should we use a different directory structure for MELEE GAMES?
                MODE_BATTLEGROUND_DIR);
        if (newMutant == null) {
            messages.add(MUTANT_CREATION_ERROR_MESSAGE);
//            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, StringEscapeUtils.escapeHtml(mutantText));
            logger.debug("Error creating mutant. Game: {}, Class: {}, User: {}", game.getId(), game.getClassId(),
                    user.getId(), mutantText);
            response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
            return;
        }
        TargetExecution compileMutantTarget = TargetExecutionDAO.getTargetExecutionForMutant(newMutant,
                TargetExecution.Target.COMPILE_MUTANT);
        if (compileMutantTarget == null || compileMutantTarget.status != TargetExecution.Status.SUCCESS) {
            messages.add(MUTANT_UNCOMPILABLE_MESSAGE);
            // There's a ton of defensive programming here...
            if (compileMutantTarget != null && compileMutantTarget.message != null
                    && !compileMutantTarget.message.isEmpty()) {
                // We escape the content of the message for new tests since user can embed there
                // anything
                String escapedHtml = StringEscapeUtils.escapeHtml(compileMutantTarget.message);
                // Extract the line numbers of the errors
                List<Integer> errorLines = extractErrorLines(compileMutantTarget.message);
                // Store them in the session so they can be picked up later
//                session.setAttribute(SESSION_ATTRIBUTE_ERROR_LINES_IN_MUTANT, errorLines);
                // We introduce our decoration
                String decorate = decorateWithLinksToCode(escapedHtml, false, true);
                messages.add(decorate);

            }
//            session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, StringEscapeUtils.escapeHtml(mutantText));
            response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
            return;
        }

        messages.add(MUTANT_COMPILED_MESSAGE);
        final String notificationMsg = UserDAO.getUserById(user.getId()).getUsername() + " created a mutant.";
        // TODO Do we need to create a special message: PLAYER_MUTANT_CREATED?
        Event notif = new Event(-1, game.getId(), user.getId(), notificationMsg, EventType.ATTACKER_MUTANT_CREATED,
                EventStatus.GAME, new Timestamp(System.currentTimeMillis() - 1000));
        notif.insert();

        mutationTester.runAllTestsOnMeleeMutant(game, newMutant, messages.getBridge());

        game.update();

        if (game.isCapturePlayersIntention()) {
            AttackerIntention intention = AttackerIntention.fromString(request.getParameter("attacker_intention"));
            // This parameter is required !
            if (intention == null) {
                messages.add(ValidationMessage.MUTANT_MISSING_INTENTION.toString());
//                session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT, StringEscapeUtils.escapeHtml(mutantText));
                response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
                return;
            }
            collectAttackerIntentions(newMutant, intention);
        }
        // Clean the mutated code only if mutant is accepted
//        session.removeAttribute(SESSION_ATTRIBUTE_PREVIOUS_MUTANT);
        logger.info("Successfully created mutant {} ", newMutant.getId());
        response.sendRedirect(ctx(request) + Paths.MELEE_GAME + "?gameId=" + game.getId());
    }

    @SuppressWarnings("Duplicates")
    private void resolveEquivalence(HttpServletRequest request, HttpServletResponse response, //
            int gameId, MeleeGame game, //
            int playerId) throws IOException {

        final String contextPath = ctx(request);
        final HttpSession session = request.getSession();
        final ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        if (game.getState() == GameState.FINISHED) {
            messages.add(String.format("Game %d has finished.", gameId));
            response.sendRedirect(contextPath + Paths.MELEE_SELECTION);
            return;
        }

        boolean handleAcceptEquivalence = ServletUtils.parameterThenOrOther(request, "acceptEquivalent", true, false);
        boolean handleRejectEquivalence = ServletUtils.parameterThenOrOther(request, "rejectEquivalent", true, false);

        if (handleAcceptEquivalence) {
            // Accepting equivalence
            final Optional<Integer> equivMutantId = ServletUtils.getIntParameter(request, "equivMutantId");
            if (!equivMutantId.isPresent()) {
                logger.debug("Missing equivMutantId parameter.");
                response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
                return;
            }
            int mutantId = equivMutantId.get();
            List<Mutant> mutantsPending = game.getMutantsMarkedEquivalentPending();

            for (Mutant m : mutantsPending) {
                // This might be replaced by m.getCreatorId() == userId
                if (m.getId() == mutantId && m.getPlayerId() == playerId) {
                    m.kill(Mutant.Equivalence.DECLARED_YES);
                    DatabaseAccess.increasePlayerPoints(1, DatabaseAccess.getEquivalentDefenderId(m));
                    messages.add(Constants.MUTANT_ACCEPTED_EQUIVALENT_MESSAGE);

                    User eventUser = UserDAO.getUserById(login.getUserId());

                    Event notifEquiv = new Event(-1, game.getId(), login.getUserId(),
                            eventUser.getUsername() + " accepts that their mutant " + m.getId() + " is equivalent.",
                            EventType.DEFENDER_MUTANT_EQUIVALENT, EventStatus.GAME,
                            new Timestamp(System.currentTimeMillis()));
                    notifEquiv.insert();

                    response.sendRedirect(request.getContextPath() + Paths.MELEE_GAME + "?gameId=" + game.getId());
                    return;
                }
            }

            logger.info("User {} tried to accept equivalence for mutant {}, but mutant has no pending equivalences.",
                    login.getUserId(), mutantId);
            response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
        } else if (handleRejectEquivalence) {
            // Reject equivalence and submit killing test case
            final Optional<String> test = ServletUtils.getStringParameter(request, "test");
            if (!test.isPresent()) {
//                session.removeAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST);
                response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
                return;
            }
            final String testText = test.get();

            // TODO Duplicate code here !
            // If it can be written to file and compiled, end turn. Otherwise, dont.
            // Do the validation even before creating the mutant
            // TODO Here we need to account for #495
            List<String> validationMessage = CodeValidator.validateTestCodeGetMessage(testText,
                    game.getMaxAssertionsPerTest(), game.isForceHamcrest());
            if (!validationMessage.isEmpty()) {
                messages.addAll(validationMessage);
//                session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
                response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
                return;
            }

            // If it can be written to file and compiled, end turn. Otherwise, dont.
            Test newTest;
            try {
                newTest = gameManagingUtils.createTest(gameId, game.getClassId(), testText, login.getUserId(),
                        MODE_BATTLEGROUND_DIR);
            } catch (IOException io) {
                messages.add(TEST_GENERIC_ERROR_MESSAGE);
//                session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
                response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
                return;
            }

            final Optional<Integer> equivMutantId = ServletUtils.getIntParameter(request, "equivMutantId");
            if (!equivMutantId.isPresent()) {
                logger.info("Missing equivMutantId parameter.");
//                session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
                response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
                return;
            }
            int mutantId = equivMutantId.get();

            logger.debug("Executing Action resolveEquivalence for mutant {} and test {}", mutantId, newTest.getId());
            TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest,
                    TargetExecution.Target.COMPILE_TEST);

            if (compileTestTarget == null || compileTestTarget.status != TargetExecution.Status.SUCCESS) {
                logger.debug("compileTestTarget: " + compileTestTarget);
                messages.add(TEST_DID_NOT_COMPILE_MESSAGE);
                if (compileTestTarget != null) {
                    messages.add(compileTestTarget.message);
                }
//                session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
                response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
                return;
            }
            TargetExecution testOriginalTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest,
                    TargetExecution.Target.TEST_ORIGINAL);
            if (testOriginalTarget.status != TargetExecution.Status.SUCCESS) {
                // (testOriginalTarget.state.equals(TargetExecution.Status.FAIL) ||
                // testOriginalTarget.state.equals(TargetExecution.Status.ERROR)
                logger.debug("testOriginalTarget: " + testOriginalTarget);
                messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE);
                messages.add(testOriginalTarget.message);
//                session.setAttribute(SESSION_ATTRIBUTE_PREVIOUS_TEST, StringEscapeUtils.escapeHtml(testText));
                response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
                return;
            }

            logger.debug("Test {} passed on the CUT", newTest.getId());

            // TODO I still do not believe this is completely correct, but open for debate.
            // The user does not want to kill all his/her mutants !
            // Instead of running equivalence on only one mutant, let's try with all mutants
            // pending resolution
            List<Mutant> mutantsPendingTests = game.getMutantsMarkedEquivalentPending();
            boolean killedClaimed = false;
            int killedOthers = 0;
            for (Mutant mPending : mutantsPendingTests) {

                // TODO Consider only the flagged mutant in this round !
                if (mPending.getId() != mutantId) {
                    logger.info("Skip pending mutant {} as it is not the one to deal with in this request ({})",
                            mPending, mutantId);
                    continue;
                }

                // TODO: Doesnt distinguish between failing because the test didnt run at all
                // and failing because it detected the mutant
                mutationTester.runEquivalenceTest(newTest, mPending); // updates mPending
                if (mPending.getEquivalent() == Mutant.Equivalence.PROVEN_NO) {
                    logger.debug("Test {} killed mutant {} and proved it non-equivalent", newTest.getId(),
                            mPending.getId());
                    // TODO Phil 23/09/18: comment below doesn't make sense, literally 0 points
                    // added.
                    newTest.updateScore(0); // score 2 points for proving a mutant non-equivalent
                    final String message = UserDAO.getUserById(login.getUserId()).getUsername() + " killed mutant "
                            + mPending.getId() + " in an equivalence duel.";
                    Event notif = new Event(-1, gameId, login.getUserId(), message,
                            EventType.ATTACKER_MUTANT_KILLED_EQUIVALENT, EventStatus.GAME,
                            new Timestamp(System.currentTimeMillis()));
                    notif.insert();
                    if (mPending.getId() == mutantId) {
                        killedClaimed = true;
                    } else {
                        killedOthers++;
                    }
                } else { // ASSUMED_YES
                    if (mPending.getId() == mutantId) {
                        // only kill the one mutant that was claimed
                        mPending.kill(ASSUMED_YES);
                        final String message = UserDAO.getUserById(login.getUserId()).getUsername()
                                + " lost an equivalence duel. Mutant " + mPending.getId() + " is assumed equivalent.";
                        Event notif = new Event(-1, gameId, login.getUserId(), message,
                                EventType.DEFENDER_MUTANT_EQUIVALENT, EventStatus.GAME,
                                new Timestamp(System.currentTimeMillis()));
                        notif.insert();
                    }
                    logger.debug("Test {} failed to kill mutant {}, hence mutant is assumed equivalent",
                            newTest.getId(), mPending.getId());
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
            response.sendRedirect(ctx(request) + Paths.MELEE_GAME + "?gameId=" + game.getId());
        } else {
            logger.info(
                    "Rejecting resolving equivalence request. Missing parameters 'acceptEquivalent' or 'rejectEquivalent'.");
            Redirect.redirectBack(request, response);
        }
    }

    private void claimEquivalent(HttpServletRequest request, HttpServletResponse response, //
            int gameId, MeleeGame game, //
            int playerId) throws IOException {

        final String contextPath = ctx(request);

        if (game.getState() != GameState.ACTIVE && game.getState() != GameState.GRACE_ONE) {
            messages.add("You cannot claim mutants as equivalent in this game anymore.");
            logger.info("Mutant claimed for non-active game.");
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

        Arrays.stream(equivLinesParam.get().split(",")).map(Integer::parseInt).filter(game::isLineCovered)
                .forEach(line -> {
                    noneCovered.set(false);
                    mutantsAlive.stream()
                            // Keep only the mutants which containts the claimed line
                            .filter(m -> m.getLines().contains(line)
                                    && m.getCreatorId() != Constants.DUMMY_ATTACKER_USER_ID)
                            // Keep only the mutant that do not belong to the user. TODO Sometimes creator refers to player and sometimes to user?
                            .filter(m -> m.getCreatorId() != login.getUserId()).forEach(m -> {
                                m.setEquivalent(Mutant.Equivalence.PENDING_TEST);
                                m.update();

                                User mutantOwner = UserDAO.getUserForPlayer(m.getPlayerId());

                                Event event = new Event(-1, gameId, mutantOwner.getId(),
                                        "One or more of your mutants is flagged equivalent.",
                                        EventType.DEFENDER_MUTANT_EQUIVALENT, EventStatus.NEW,
                                        new Timestamp(System.currentTimeMillis()));
                                event.insert();
                                // Register this user in the Role.DEFENDER as the one claiming the equivalence
                                DatabaseAccess.insertEquivalence(m, playerId);
                                claimedMutants.incrementAndGet();
                            });
                });

        if (noneCovered.get()) {
            messages.add(Constants.MUTANT_CANT_BE_CLAIMED_EQUIVALENT_MESSAGE);
            response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
            return;
        }

        int nClaimed = claimedMutants.get();
        if (nClaimed > 0) {
            String flaggingChatMessage = UserDAO.getUserById(login.getUserId()).getUsername() + " flagged " + nClaimed
                    + " mutant" + (nClaimed == 1 ? "" : "s") + " as equivalent.";
            Event event = new Event(-1, gameId, login.getUserId(), flaggingChatMessage,
                    EventType.DEFENDER_MUTANT_CLAIMED_EQUIVALENT, EventStatus.GAME,
                    new Timestamp(System.currentTimeMillis()));
            event.insert();
        }

        String flaggingMessage = nClaimed == 0 ? "Mutant has already been claimed as equivalent or killed!"
                : String.format("Flagged %d mutant%s as equivalent", nClaimed, (nClaimed == 1 ? "" : 's'));
        messages.add(flaggingMessage);
        response.sendRedirect(contextPath + Paths.MELEE_GAME + "?gameId=" + game.getId());
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
        List<String> detectedTestSmells = testSmellsDAO.getDetectedTestSmellsForTest(newTest);
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
