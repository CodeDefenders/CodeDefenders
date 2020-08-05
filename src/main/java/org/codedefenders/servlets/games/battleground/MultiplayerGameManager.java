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
package org.codedefenders.servlets.games.battleground;

import org.apache.commons.lang.StringEscapeUtils;
import org.codedefenders.beans.game.PreviousSubmissionBean;
import org.codedefenders.beans.user.LoginBean;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.IntentionDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.database.PlayerDAO;
import org.codedefenders.database.TargetExecutionDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.database.TestSmellsDAO;
import org.codedefenders.database.UserDAO;
import org.codedefenders.execution.IMutationTester;
import org.codedefenders.execution.KillMap;
import org.codedefenders.execution.KillMap.KillMapEntry;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.tcs.ITestCaseSelector;
import org.codedefenders.model.AttackerIntention;
import org.codedefenders.model.DefenderIntention;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.model.User;
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
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.codedefenders.execution.TargetExecution.Target.COMPILE_MUTANT;
import static org.codedefenders.execution.TargetExecution.Target.COMPILE_TEST;
import static org.codedefenders.execution.TargetExecution.Target.TEST_ORIGINAL;
import static org.codedefenders.game.Mutant.Equivalence.ASSUMED_YES;
import static org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME.FAILED_DUEL_VALIDATION_THRESHOLD;
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

/**
 * This {@link HttpServlet} handles retrieval and in-game management for {@link MultiplayerGame battleground games}.
 *
 * <p>{@code GET} requests allow accessing battleground games and {@code POST} requests handle starting and ending
 * games, creation of tests, mutants and resolving equivalences.
 *
 * <p>Serves under {@code /multiplayergame}.
 *
 * @see org.codedefenders.util.Paths#BATTLEGROUND_GAME
 */
@WebServlet(org.codedefenders.util.Paths.BATTLEGROUND_GAME)
public class MultiplayerGameManager extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(MultiplayerGameManager.class);

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

    @Inject
    private EventDAO eventDAO;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        Optional<Integer> gameIdOpt = ServletUtils.gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.info("No gameId parameter. Aborting request.");
            response.sendRedirect(ctx(request) + Paths.GAMES_OVERVIEW);
            return;
        }
        int gameId = gameIdOpt.get();

        MultiplayerGame game = MultiplayerGameDAO.getMultiplayerGame(gameId);

        if (game == null) {
            logger.error("Could not find multiplayer game {}", gameId);
            response.sendRedirect(request.getContextPath() + Paths.GAMES_OVERVIEW);
            return;
        } else {
            // TODO Shall we make MultiplayerGameDAO ensure dependencies are set?
            game.setEventDAO(eventDAO);
        }
        int playerId = PlayerDAO.getPlayerIdForUserAndGame(login.getUserId(), gameId);

        if (playerId == -1 && game.getCreatorId() != login.getUserId()) {
            logger.info("User {} not part of game {}. Aborting request.", login.getUserId(), gameId);
            response.sendRedirect(ctx(request) + Paths.GAMES_OVERVIEW);
            return;
        }

        // check is there is a pending equivalence duel for the user.
        game.getMutantsMarkedEquivalentPending()
                .stream()
                .filter(m -> m.getPlayerId() == playerId)
                .findFirst()
                .ifPresent(mutant -> {
                    int defenderId = DatabaseAccess.getEquivalentDefenderId(mutant);
                    User defender = UserDAO.getUserForPlayer(defenderId);

                    request.setAttribute("equivDefender", defender);
                    request.setAttribute("equivMutant", mutant);
                    request.setAttribute("openEquivalenceDuel", true);
                });

        request.setAttribute("game", game);
        request.setAttribute("playerId", playerId);

        RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.BATTLEGROUND_GAME_VIEW_JSP);
        dispatcher.forward(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final Optional<Integer> gameIdOpt = ServletUtils.gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.debug("No gameId parameter. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }
        final int gameId = gameIdOpt.get();

        final MultiplayerGame game = MultiplayerGameDAO.getMultiplayerGame(gameId);

        if (game == null) {
            logger.debug("Could not retrieve game from database for gameId: {}", gameId);
            Redirect.redirectBack(request, response);
            return;
        } else {
            // TODO Shall we make MultiplayerGameDAO ensure dependencies are set?
            game.setEventDAO(eventDAO);
        }

        final String action = ServletUtils.formType(request);
        switch (action) {
            case "createMutant": {
                createMutant(request, response, gameId, game);
                // After creating a mutant, there's the chance that the mutant already survived enough tests
                triggerAutomaticMutantEquivalenceForGame(game);
                return;
            }
            case "createTest": {
                createTest(request, response, gameId, game);
                // After a test is submitted, there's the chance that one or more mutants already survived enough tests
                triggerAutomaticMutantEquivalenceForGame(game);
                return;
            }
            case "reset": {
                previousSubmission.clear();
                response.sendRedirect(ctx(request) + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
                return;
            }
            case "resolveEquivalence": {
                resolveEquivalence(request, response, gameId, game);
                return;
            }
            case "claimEquivalent": {
                claimEquivalent(request, response, gameId, game);
                return;
            }
            default:
                logger.info("Action not recognised: {}", action);
                Redirect.redirectBack(request, response);
        }
    }

    // This is package protected to enable testing
    void triggerAutomaticMutantEquivalenceForGame(MultiplayerGame game) {
        int threshold = game.getAutomaticMutantEquivalenceThreshold();
        if (threshold < 1) {
            // No need to check as this feature is disabled
            return;
        }
        // Get all the live mutants in the game
        for (Mutant aliveMutant : game.getAliveMutants()) {
            /*
             * If the mutant is covered by enough tests trigger the automatic
             * equivalence duel. Consider ONLY the coveringTests submitted after the mutant was created
             */
            Set<Integer> allCoveringTests = aliveMutant.getCoveringTests().stream()
                        .map(Test::getId)
                        .collect(Collectors.toSet());

            boolean considerOnlydefenders = false;
            Set<Integer> testSubmittedAfterMutant =
                    TestDAO.getValidTestsForGameSubmittedAfterMutant(game.getId(), considerOnlydefenders, aliveMutant)
                        .stream()
                        .map(Test::getId)
                        .collect(Collectors.toSet());

            allCoveringTests.retainAll(testSubmittedAfterMutant);

            int numberOfCoveringTestsSubmittedAfterMutant = allCoveringTests.size();

            if (numberOfCoveringTestsSubmittedAfterMutant  >= threshold) {
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
                eventDAO.insert(event);
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
                eventDAO.insert(gameEvent);
            }
        }
    }

    @SuppressWarnings("Duplicates")
    private void createTest(HttpServletRequest request, HttpServletResponse response, int gameId, MultiplayerGame game)
            throws IOException {
        final String contextPath = ctx(request);
        final HttpSession session = request.getSession();

        if (game.getRole(login.getUserId()) != Role.DEFENDER) {
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
            previousSubmission.clear();
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }
        final String testText = test.get();

        TestSubmittedEvent tse = new TestSubmittedEvent();
        tse.setGameId(gameId);
        tse.setUserId(login.getUserId());
        notificationService.post(tse);

        // Do the validation even before creating the mutant
        // TODO Here we need to account for #495
        List<String> validationMessage = CodeValidator.validateTestCodeGetMessage(
                testText,
                game.getMaxAssertionsPerTest(),
                game.isForceHamcrest()
        );
        boolean validationSuccess = validationMessage.isEmpty();

        TestValidatedEvent tve = new TestValidatedEvent();
        tve.setGameId(gameId);
        tve.setUserId(login.getUserId());
        tve.setSuccess(validationSuccess);
        tve.setValidationMessage(validationSuccess ? null : String.join("\n", validationMessage));
        notificationService.post(tve);

        if (!validationSuccess) {
            messages.getBridge().addAll(validationMessage);
            previousSubmission.setTestCode(testText);
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }

        // From this point on we assume that test is valid according to the rules (but it might still not compile)
        Test newTest;
        try {
            newTest = gameManagingUtils.createTest(gameId, game.getClassId(), testText, login.getUserId(), MODE_BATTLEGROUND_DIR);
        } catch (IOException io) {
            messages.add(TEST_GENERIC_ERROR_MESSAGE);
            previousSubmission.setTestCode(testText);
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
            // boolean validatedKilledMutants = true;

            // Prepare the validation message
            StringBuilder userIntentionsValidationMessage = new StringBuilder();
            userIntentionsValidationMessage.append("Cheeky! You cannot submit a test without specifying");

            final String selected_lines = request.getParameter("selected_lines");
            if (selected_lines != null) {
                Set<Integer> selectLinesSet =
                        DefenderIntention.parseIntentionFromCommaSeparatedValueString(selected_lines);
                selectedLines.addAll(selectLinesSet);
            }

            if (selectedLines.isEmpty()) {
                validatedCoveredLines = false;
                userIntentionsValidationMessage.append(" a line to cover");
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
            userIntentionsValidationMessage.append(".");

            if (!validatedCoveredLines) { // || !validatedKilledMutants
                messages.add(userIntentionsValidationMessage.toString());
                // Keep the test around
                previousSubmission.setTestCode(testText);
                response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
                return;
            }
        }

        logger.debug("New Test {} by user {}", newTest.getId(), login.getUserId());
        TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, COMPILE_TEST);

        if (game.isCapturePlayersIntention()) {
            collectDefenderIntentions(newTest, selectedLines, selectedMutants);
            // Store intentions in the session in case tests is broken we automatically re-select the same line
            // TODO At the moment, there is only and only one line
            session.setAttribute("selected_lines", selectedLines.iterator().next());
        }

        if (compileTestTarget.status != TargetExecution.Status.SUCCESS) {
            messages.add(TEST_DID_NOT_COMPILE_MESSAGE).fadeOut(false);
            // We escape the content of the message for new tests since user can embed there anything
            String escapedHtml = StringEscapeUtils.escapeHtml(compileTestTarget.message);
            // Extract the line numbers of the errors
            List<Integer> errorLines = extractErrorLines(compileTestTarget.message);
            // Store them in the session so they can be picked up later
            previousSubmission.setErrorLines(errorLines);
            // We introduce our decoration
            String decorate = decorateWithLinksToCode(escapedHtml, true, false);
            messages.add(decorate).escape(false);
            //
            previousSubmission.setTestCode(testText);
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }
        TargetExecution testOriginalTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TEST_ORIGINAL);
        if (testOriginalTarget.status != TargetExecution.Status.SUCCESS) {
            // testOriginalTarget.state.equals(TargetExecution.Status.FAIL)
            //     || testOriginalTarget.state.equals(TargetExecution.Status.ERROR)
            messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE).fadeOut(false);
            messages.add(StringEscapeUtils.escapeHtml(testOriginalTarget.message));
            previousSubmission.setTestCode(testText);
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }

        messages.add(TEST_PASSED_ON_CUT_MESSAGE);

        // Include Test Smells in the messages back to user
        includeDetectTestSmellsInMessages(newTest);

        final String message = login.getUser().getUsername() + " created a test";
        final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        final Event notif = new Event(-1, gameId, login.getUserId(), message, EventType.DEFENDER_TEST_CREATED,
                EventStatus.GAME, timestamp);
        eventDAO.insert(notif);

        mutationTester.runTestOnAllMultiplayerMutants(game, newTest, messages.getBridge());
        game.update();
        logger.info("Successfully created test {} ", newTest.getId());

        TestTestedMutantsEvent ttme = new TestTestedMutantsEvent();
        ttme.setGameId(gameId);
        ttme.setUserId(login.getUserId());
        notificationService.post(ttme);

        // Clean up the session
        previousSubmission.clear();
        session.removeAttribute("selected_lines");
        response.sendRedirect(ctx(request) + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
    }

    /**
     * Returns the line numbers mentioned in the error message of the compiler.
     *
     * @param compilerOutput The compiler output.
     * @return A list of (1-indexed) line numbers.
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

    private void createMutant(HttpServletRequest request,
                              HttpServletResponse response,
                              int gameId,
                              MultiplayerGame game) throws IOException {
        final String contextPath = ctx(request);
        final HttpSession session = request.getSession();

        if (game.getRole(login.getUserId()) != Role.ATTACKER) {
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
            previousSubmission.clear();
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }
        final String mutantText = mutant.get();

        int attackerId = PlayerDAO.getPlayerIdForUserAndGame(login.getUserId(), gameId);

        // If the user has pending duels we cannot accept the mutant, but we keep it around
        // so students do not lose mutants once the duel is solved.
        if (gameManagingUtils.hasAttackerPendingMutantsInGame(gameId, attackerId)
                && (session.getAttribute(Constants.BLOCK_ATTACKER) != null)
                && ((Boolean) session.getAttribute(Constants.BLOCK_ATTACKER))) {
            messages.add(Constants.ATTACKER_HAS_PENDING_DUELS);
            // Keep the mutant code in the view for later
            previousSubmission.setMutantCode(mutantText);
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }

        MutantSubmittedEvent mse = new MutantSubmittedEvent();
        mse.setGameId(gameId);
        mse.setUserId(login.getUserId());
        notificationService.post(mse);

        // Do the validation even before creating the mutant
        CodeValidatorLevel codeValidatorLevel = game.getMutantValidatorLevel();
        ValidationMessage validationMessage =
                CodeValidator.validateMutantGetMessage(game.getCUT().getSourceCode(), mutantText, codeValidatorLevel);
        boolean validationSuccess = validationMessage == ValidationMessage.MUTANT_VALIDATION_SUCCESS;

        MutantValidatedEvent mve = new MutantValidatedEvent();
        mve.setGameId(gameId);
        mve.setUserId(login.getUserId());
        mve.setSuccess(validationSuccess);
        notificationService.post(mve);

        if (!validationSuccess) {
            // Mutant is either the same as the CUT or it contains invalid code
            messages.add(validationMessage.get());
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }

        Mutant existingMutant = gameManagingUtils.existingMutant(gameId, mutantText);
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
            if (existingMutantTarget != null && existingMutantTarget.status != TargetExecution.Status.SUCCESS
                    && existingMutantTarget.message != null && !existingMutantTarget.message.isEmpty()) {
                messages.add(existingMutantTarget.message);
            }
            previousSubmission.setMutantCode(mutantText);
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }

        Mutant newMutant = gameManagingUtils.createMutant(gameId, game.getClassId(), mutantText,
                login.getUserId(), MODE_BATTLEGROUND_DIR);
        if (newMutant == null) {
            messages.add(MUTANT_CREATION_ERROR_MESSAGE);
            previousSubmission.setMutantCode(mutantText);
            logger.debug("Error creating mutant. Game: {}, Class: {}, User: {}, Mutant: {}",
                    gameId, game.getClassId(), login.getUserId(), mutantText);
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }

        TargetExecution compileMutantTarget = TargetExecutionDAO.getTargetExecutionForMutant(newMutant,
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

        if (!compileSuccess) {
            messages.add(MUTANT_UNCOMPILABLE_MESSAGE).fadeOut(false);
            // There's a ton of defensive programming here...
            if (errorMessage != null) {
                // We escape the content of the message for new tests since user can embed there anything
                String escapedHtml = StringEscapeUtils.escapeHtml(errorMessage);
                // Extract the line numbers of the errors
                List<Integer> errorLines = extractErrorLines(errorMessage);
                // Store them in the session so they can be picked up later
                previousSubmission.setErrorLines(errorLines);
                // We introduce our decoration
                String decorate = decorateWithLinksToCode(escapedHtml, false, true);
                messages.add(decorate).escape(false);
            }
            previousSubmission.setMutantCode(mutantText);
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }

        messages.add(MUTANT_COMPILED_MESSAGE);
        final String notificationMsg = login.getUser().getUsername() + " created a mutant.";
        Event notif = new Event(-1, gameId, login.getUserId(), notificationMsg, EventType.ATTACKER_MUTANT_CREATED,
                EventStatus.GAME, new Timestamp(System.currentTimeMillis() - 1000));
        eventDAO.insert(notif);

        mutationTester.runAllTestsOnMutant(game, newMutant, messages.getBridge());
        game.update();

        MutantTestedEvent mte = new MutantTestedEvent();
        mte.setGameId(gameId);
        mte.setUserId(login.getUserId());
        mte.setMutantId(newMutant.getId());
        notificationService.post(mte);

        if (game.isCapturePlayersIntention()) {
            AttackerIntention intention = AttackerIntention.fromString(request.getParameter("attacker_intention"));
            // This parameter is required !
            if (intention == null) {
                messages.add(ValidationMessage.MUTANT_MISSING_INTENTION.toString());
                previousSubmission.setMutantCode(mutantText);
                response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
                return;
            }
            collectAttackerIntentions(newMutant, intention);
        }
        // Clean the mutated code only if mutant is accepted
        previousSubmission.clear();
        logger.info("Successfully created mutant {} ", newMutant.getId());
        response.sendRedirect(ctx(request) + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
    }

    @SuppressWarnings("Duplicates")
    private void resolveEquivalence(HttpServletRequest request,
                                    HttpServletResponse response,
                                    int gameId,
                                    MultiplayerGame game) throws IOException {
        final String contextPath = ctx(request);

        if (game.getRole(login.getUserId()) != Role.ATTACKER) {
            messages.add("Can only resolve equivalence duels if you are an Attacker!");
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }
        if (game.getState() == GameState.FINISHED) {
            messages.add(String.format("Game %d has finished.", gameId));
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_SELECTION);
            return;
        }

        boolean handleAcceptEquivalence = ServletUtils.parameterThenOrOther(request, "acceptEquivalent", true, false);
        boolean handleRejectEquivalence = ServletUtils.parameterThenOrOther(request, "rejectEquivalent", true, false);

        if (handleAcceptEquivalence) {
            // Accepting equivalence
            final Optional<Integer> equivMutantId = ServletUtils.getIntParameter(request, "equivMutantId");
            if (!equivMutantId.isPresent()) {
                logger.debug("Missing equivMutantId parameter.");
                response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
                return;
            }
            int mutantId = equivMutantId.get();
            int playerId = PlayerDAO.getPlayerIdForUserAndGame(login.getUserId(), gameId);
            List<Mutant> mutantsPending = game.getMutantsMarkedEquivalentPending();

            for (Mutant m : mutantsPending) {
                if (m.getId() == mutantId && m.getPlayerId() == playerId) {
                    // Here we check if the accepted equivalence is "possibly" equivalent
                    boolean isMutantKillable = isMutantKillableByOtherTests(m);

                    String message = Constants.MUTANT_ACCEPTED_EQUIVALENT_MESSAGE;
                    String notification = String.format("%s accepts that their mutant %d is equivalent",
                            login.getUser().getUsername(), m.getId());
                    if (isMutantKillable) {
                        logger.warn("Mutant {} was accepted as equivalence but it is killable", m);
                        message = message + " " + " However, the mutant was killable!";
                        notification = notification + " " + " However, the mutant was killable!";
                    }

                    // At this point we where not able to kill the mutant will all the covering
                    // tests on the same class from different games
                    m.kill(Mutant.Equivalence.DECLARED_YES);

                    PlayerDAO.increasePlayerPoints(1, DatabaseAccess.getEquivalentDefenderId(m));
                    messages.add(message);

                    // Notify the attacker
                    Event notifEquiv = new Event(-1, game.getId(),
                            login.getUserId(),
                            notification,
                            EventType.DEFENDER_MUTANT_EQUIVALENT, EventStatus.GAME,
                            new Timestamp(System.currentTimeMillis()));
                    eventDAO.insert(notifEquiv);

                    // Notify the defender which triggered the duel about it !
                    if (isMutantKillable) {
                        int defenderId = DatabaseAccess.getEquivalentDefenderId(m);
                        notification = login.getUser().getUsername() + " accepts that the mutant " + m.getId()
                                + "that you claimed equivalent is equivalent, but that mutant was killable.";
                        Event notifDefenderEquiv = new Event(-1, game.getId(), defenderId, notification,
                                EventType.GAME_MESSAGE_DEFENDER, EventStatus.GAME,
                                new Timestamp(System.currentTimeMillis()));
                        eventDAO.insert(notifDefenderEquiv);
                    }


                    response.sendRedirect(request.getContextPath() + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
                    return;
                }
            }

            logger.info("User {} tried to accept equivalence for mutant {}, but mutant has no pending equivalences.",
                    login.getUserId(), mutantId);
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
        } else if (handleRejectEquivalence) {
            // Reject equivalence and submit killing test case
            final Optional<String> test = ServletUtils.getStringParameter(request, "test");
            if (!test.isPresent()) {
                previousSubmission.clear();
                response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
                return;
            }
            final String testText = test.get();

            TestSubmittedEvent tse = new TestSubmittedEvent();
            tse.setGameId(gameId);
            tse.setUserId(login.getUserId());
            notificationService.post(tse);

            // TODO Duplicate code here !
            // If it can be written to file and compiled, end turn. Otherwise, dont.
            // Do the validation even before creating the mutant
            // TODO Here we need to account for #495
            List<String> validationMessage = CodeValidator.validateTestCodeGetMessage(
                    testText,
                    game.getMaxAssertionsPerTest(),
                    game.isForceHamcrest()
            );
            boolean validationSuccess = validationMessage.isEmpty();

            TestValidatedEvent tve = new TestValidatedEvent();
            tve.setGameId(gameId);
            tve.setUserId(login.getUserId());
            tve.setSuccess(validationSuccess);
            tve.setValidationMessage(validationSuccess ? null : String.join("\n", validationMessage));
            notificationService.post(tve);

            if (!validationSuccess) {
                messages.getBridge().addAll(validationMessage);
                previousSubmission.setTestCode(testText);
                response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
                return;
            }

            // If it can be written to file and compiled, end turn. Otherwise, dont.
            Test newTest;
            try {
                newTest = gameManagingUtils.createTest(gameId, game.getClassId(),
                        testText, login.getUserId(), MODE_BATTLEGROUND_DIR);
            } catch (IOException io) {
                messages.add(TEST_GENERIC_ERROR_MESSAGE);
                previousSubmission.setTestCode(testText);
                response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
                return;
            }

            /* TODO: Why not check this in the beginning? */
            final Optional<Integer> equivMutantId = ServletUtils.getIntParameter(request, "equivMutantId");
            if (!equivMutantId.isPresent()) {
                logger.info("Missing equivMutantId parameter.");
                previousSubmission.setTestCode(testText);
                response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
                return;
            }
            int mutantId = equivMutantId.get();

            logger.debug("Executing Action resolveEquivalence for mutant {} and test {}", mutantId, newTest.getId());
            TargetExecution compileTestTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, COMPILE_TEST);

            if (compileTestTarget == null || compileTestTarget.status != TargetExecution.Status.SUCCESS) {
                logger.debug("compileTestTarget: " + compileTestTarget);
                messages.add(TEST_DID_NOT_COMPILE_MESSAGE).fadeOut(false);

                if (compileTestTarget != null) {
                    String escapedHtml = StringEscapeUtils.escapeHtml(compileTestTarget.message);
                    // Extract the line numbers of the errors
                    List<Integer> errorLines = extractErrorLines(compileTestTarget.message);
                    // Store them in the session so they can be picked up later
                    previousSubmission.setErrorLines(errorLines);
                    // We introduce our decoration
                    String decorate = decorateWithLinksToCode(escapedHtml, true, false);
                    messages.add(decorate);
                }

                previousSubmission.setTestCode(testText);
                response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
                return;
            }
            TargetExecution testOriginalTarget = TargetExecutionDAO.getTargetExecutionForTest(newTest, TEST_ORIGINAL);
            if (testOriginalTarget.status != TargetExecution.Status.SUCCESS) {
                // (testOriginalTarget.state.equals(TargetExecution.Status.FAIL)
                //   || testOriginalTarget.state.equals(TargetExecution.Status.ERROR)
                logger.debug("testOriginalTarget: " + testOriginalTarget);
                messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE).fadeOut(false);
                messages.add(testOriginalTarget.message);
                previousSubmission.setTestCode(testText);
                response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
                return;
            }
            logger.debug("Test {} passed on the CUT", newTest.getId());

            // Instead of running equivalence on only one mutant, let's try with all mutants pending resolution
            List<Mutant> mutantsPendingTests = game.getMutantsMarkedEquivalentPending();
            boolean killedClaimed = false;
            int killedOthers = 0;
            boolean isMutantKillable = false;

            for (Mutant mutPending : mutantsPendingTests) {
                // TODO: Doesnt distinguish between failing because the test didnt run at all and failing
                //  because it detected the mutant
                mutationTester.runEquivalenceTest(newTest, mutPending); // updates mutPending

                if (mutPending.getEquivalent() == Mutant.Equivalence.PROVEN_NO) {
                    logger.debug("Test {} killed mutant {} and proved it non-equivalent",
                            newTest.getId(), mutPending.getId());
                    final String message = login.getUser().getUsername() + " killed mutant "
                            + mutPending.getId() + " in an equivalence duel.";
                    Event notif = new Event(-1, gameId, login.getUserId(), message,
                            EventType.ATTACKER_MUTANT_KILLED_EQUIVALENT,EventStatus.GAME,
                            new Timestamp(System.currentTimeMillis())
                    );
                    eventDAO.insert(notif);
                    if (mutPending.getId() == mutantId) {
                        killedClaimed = true;
                    } else {
                        killedOthers++;
                    }
                } else { // ASSUMED_YES

                    if (mutPending.getId() == mutantId) {
                        // Here we check if the accepted equivalence is "possibly" equivalent
                        isMutantKillable = isMutantKillableByOtherTests(mutPending);
                        String notification = login.getUser().getUsername()
                                + " lost an equivalence duel. Mutant " + mutPending.getId()
                                + " is assumed equivalent.";

                        if (isMutantKillable) {
                            notification = notification + " " + "However, the mutant was killable!";
                        }

                        // only kill the one mutant that was claimed
                        mutPending.kill(ASSUMED_YES);

                        Event notif = new Event(-1, gameId, login.getUserId(), notification,
                                EventType.DEFENDER_MUTANT_EQUIVALENT, EventStatus.GAME,
                                new Timestamp(System.currentTimeMillis()));
                        eventDAO.insert(notif);

                    }
                    logger.debug("Test {} failed to kill mutant {}, hence mutant is assumed equivalent",
                            newTest.getId(), mutPending.getId());

                }
            }

            if (killedClaimed) {
                messages.add(TEST_KILLED_CLAIMED_MUTANT_MESSAGE);
            } else {
                String message = TEST_DID_NOT_KILL_CLAIMED_MUTANT_MESSAGE;
                if (isMutantKillable) {
                    message = message + " " + "Unfortunately, the mutant was killable!";
                }
                messages.add(message);
            }

            if (killedOthers == 1) {
                messages.add("Additionally, your test did kill another claimed mutant!");
            } else if (killedOthers > 1) {
                messages.add(String.format("Additionally, your test killed other %d claimed mutants!", killedOthers));
            }

            TestTestedMutantsEvent ttme = new TestTestedMutantsEvent();
            ttme.setGameId(gameId);
            ttme.setUserId(login.getUserId());
            ttme.setTestId(newTest.getId());
            notificationService.post(ttme);

            newTest.update();
            game.update();
            logger.info("Resolving equivalence was handled successfully");
            response.sendRedirect(ctx(request) + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
        } else {
            logger.info("Rejecting resolving equivalence request. Missing parameters"
                    + "'acceptEquivalent' or 'rejectEquivalent'.");
            Redirect.redirectBack(request, response);
        }
    }

    private void claimEquivalent(HttpServletRequest request,
                                 HttpServletResponse response,
                                 int gameId,
                                 MultiplayerGame game) throws IOException {
        final String contextPath = ctx(request);

        Role role = game.getRole(login.getUserId());

        if (role != Role.DEFENDER) {
            messages.add("Can only claim mutant as equivalent if you are a Defender!");
            logger.info("Non defender (role={}) tried to claim mutant as equivalent.", role);
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }

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

        int playerId = PlayerDAO.getPlayerIdForUserAndGame(login.getUserId(), gameId);
        AtomicInteger claimedMutants = new AtomicInteger();
        AtomicBoolean noneCovered = new AtomicBoolean(true);
        List<Mutant> mutantsAlive = game.getAliveMutants();

        Arrays.stream(equivLinesParam.get().split(","))
                .map(Integer::parseInt)
                .filter(game::isLineCovered)
                .forEach(line -> {
                    noneCovered.set(false);
                    mutantsAlive.stream()
                            .filter(m -> m.getLines().contains(line))
                            .filter(m -> m.getCreatorId() != Constants.DUMMY_ATTACKER_USER_ID)
                            .forEach(m -> {
                                m.setEquivalent(Mutant.Equivalence.PENDING_TEST);
                                m.update();

                                User mutantOwner = UserDAO.getUserForPlayer(m.getPlayerId());

                                Event event = new Event(-1, gameId, mutantOwner.getId(),
                                        "One or more of your mutants is flagged equivalent.",
                                        EventType.DEFENDER_MUTANT_EQUIVALENT, EventStatus.NEW,
                                        new Timestamp(System.currentTimeMillis()));
                                eventDAO.insert(event);

                                DatabaseAccess.insertEquivalence(m, playerId);
                                claimedMutants.incrementAndGet();
                            });
                });

        if (noneCovered.get()) {
            messages.add(Constants.MUTANT_CANT_BE_CLAIMED_EQUIVALENT_MESSAGE);
            response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            return;
        }

        int numClaimed = claimedMutants.get();
        if (numClaimed > 0) {
            String flaggingChatMessage = login.getUser().getUsername() + " flagged "
                    + numClaimed + " mutant" + (numClaimed == 1 ? "" : "s") + " as equivalent.";
            Event event = new Event(-1, gameId, login.getUserId(), flaggingChatMessage,
                    EventType.DEFENDER_MUTANT_CLAIMED_EQUIVALENT, EventStatus.GAME,
                    new Timestamp(System.currentTimeMillis()));
            eventDAO.insert(event);
        }

        String flaggingMessage = numClaimed == 0
                ? "Mutant has already been claimed as equivalent or killed!"
                : String.format("Flagged %d mutant%s as equivalent", numClaimed,
                (numClaimed == 1 ? "" : 's'));
        messages.add(flaggingMessage);
        response.sendRedirect(contextPath + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
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

    private void includeDetectTestSmellsInMessages(Test newTest) {
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

    /**
     * Selects a max of AdminSystemSettings.SETTING_NAME.FAILED_DUEL_VALIDATION_THRESHOLD tests randomly sampled
     * which cover the mutant but belongs to other games and executes them against the mutant.
     *
     * @param mutantToValidate The mutant why try to find a killing test for
     * @return whether the mutant is killable or not/cannot be validated
     */
    boolean isMutantKillableByOtherTests(Mutant mutantToValidate) {
        int validationThreshold = AdminDAO.getSystemSetting(FAILED_DUEL_VALIDATION_THRESHOLD).getIntValue();
        if (validationThreshold <= 0) {
            return false;
        }

        // Get all the covering tests of this mutant which do not belong to this game
        int classId = mutantToValidate.getClassId();
        List<Test> tests = TestDAO.getValidTestsForClass(classId);

        // Remove tests which belong to the same game as the mutant
        tests.removeIf(test -> test.getGameId() == mutantToValidate.getGameId());

        List<Test> selectedTests = regressionTestCaseSelector.select(tests, validationThreshold);
        logger.debug("Validating the mutant with {} selected tests:\n{}", selectedTests.size(), selectedTests);

        // At the moment this is purposely blocking.
        // This is the dumbest, but safest way to deal with it while we design a better solution.
        KillMap killmap = KillMap.forMutantValidation(selectedTests, mutantToValidate, classId);

        if (killmap == null) {
            // There was an error we cannot empirically prove the mutant was killable.
            logger.warn("An error prevents validation of mutant {}", mutantToValidate);
            return false;
        } else {
            for (KillMapEntry killMapEntry : killmap.getEntries()) {
                if (killMapEntry.status.equals(KillMapEntry.Status.KILL)
                        || killMapEntry.status.equals(KillMapEntry.Status.ERROR)) {
                    return true;
                }
            }
        }
        return false;
    }
}
