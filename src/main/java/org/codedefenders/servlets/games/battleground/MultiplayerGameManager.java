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

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.text.StringEscapeUtils;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.game.PreviousSubmissionBean;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.EventDAO;
import org.codedefenders.dto.SimpleUser;
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
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.IntentionRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.persistence.database.PlayerRepository;
import org.codedefenders.persistence.database.TestRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.codedefenders.validation.code.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;

import static org.codedefenders.game.Mutant.State.KILLED;
import static org.codedefenders.util.Constants.GRACE_PERIOD_MESSAGE;
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
import static org.codedefenders.util.Constants.TITLE_SUCCESS;

/**
 * This {@link HttpServlet} handles retrieval and in-game management for {@link MultiplayerGame battleground games}.
 *
 * <p>{@code GET} requests allow accessing battleground games and {@code POST} requests handle starting and ending
 * games, creation of tests, mutants and resolving equivalences.
 *
 * <p>Serves under {@code /multiplayergame}.
 *
 * @see Paths#BATTLEGROUND_GAME
 */
@WebServlet({Paths.BATTLEGROUND_GAME, Paths.BATTLEGROUND_HISTORY})
public class MultiplayerGameManager extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(MultiplayerGameManager.class);

    private static final Histogram.Child automaticEquivalenceDuelTrigger =
            GameManagingUtils.automaticEquivalenceDuelTrigger
                    .labels("multiplayer");

    private static final Counter.Child automaticEquivalenceDuelsTriggered =
            GameManagingUtils.automaticEquivalenceDuelsTriggered
                    .labels("multiplayer");

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private Configuration config;

    @Inject
    private GameManagingUtils gameManagingUtils;

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
    private UserService userService;

    @Inject
    private IntentionRepository intentionRepository;

    @Inject
    private URLUtils url;

    @Inject
    private TestRepository testRepo;

    @Inject
    private MutantRepository mutantRepo;

    @Inject
    private GameRepository gameRepo;

    @Inject
    private PlayerRepository playerRepo;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        MultiplayerGame game = gameProducer.getMultiplayerGame();
        if (game == null) {
            logger.error("No game found. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }

        int gameId = game.getId();

        int playerId = playerRepo.getPlayerIdForUserAndGame(login.getUserId(), gameId);

        if (playerId == -1 && game.getCreatorId() != login.getUserId()) {
            logger.info("User {} not part of game {}. Aborting request.", login.getUserId(), gameId);
            response.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
            return;
        }

        // check is there is a pending equivalence duel for the user.
        game.getMutantsMarkedEquivalentPending()
                .stream()
                .filter(m -> m.getPlayerId() == playerId)
                .findFirst()
                .ifPresent(mutant -> {
                    int defenderId = mutantRepo.getEquivalentDefenderId(mutant);
                    Optional<SimpleUser> defender = userService.getSimpleUserByPlayerId(defenderId);

                    // TODO
                    request.setAttribute("equivDefender", defender.orElse(null));
                    request.setAttribute("equivMutant", mutant);
                    request.setAttribute("openEquivalenceDuel", true);
                });

        request.setAttribute("game", game);
        request.setAttribute("playerId", playerId);

        final boolean isGameClosed = game.getState() == GameState.FINISHED
                || (game.getState() == GameState.ACTIVE && gameRepo.isGameExpired(gameId));
        final String jspPath = isGameClosed
                ? Constants.BATTLEGROUND_DETAILS_VIEW_JSP
                : Constants.BATTLEGROUND_GAME_VIEW_JSP;

        if (!isGameClosed && game.getRole(login.getUserId()) == Role.DEFENDER) {
            Test prevTest = testRepo.getLatestTestForGameAndUser(gameId, login.getUserId());
            request.setAttribute("previousTest", prevTest);
        }

        request.getRequestDispatcher(jspPath).forward(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        MultiplayerGame game = gameProducer.getMultiplayerGame();
        if (game == null) {
            logger.error("No game found. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }

        int gameId = game.getId();

        final String action = ServletUtils.formType(request);
        switch (action) {
            case "createMutant": {
                createMutant(request, response, game);
                // After creating a mutant, there's the chance that the mutant already survived enough tests
                checkAutomaticMutantEquivalenceForGame(game);
                return;
            }
            case "createTest": {
                createTest(request, response, game);
                // After a test is submitted, there's the chance that one or more mutants already survived enough tests
                checkAutomaticMutantEquivalenceForGame(game);
                return;
            }
            case "reset": {
                previousSubmission.clear();
                response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + gameId);
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

    void checkAutomaticMutantEquivalenceForGame(MultiplayerGame game) {
        int threshold = game.getAutomaticMutantEquivalenceThreshold();
        if (threshold > 0) { // Feature is disabled if threshold <= 0
            try (Histogram.Timer ignored = automaticEquivalenceDuelTrigger.startTimer()) {
                triggerAutomaticMutantEquivalenceForGame(game);
            }
        }
    }

    // This is package protected to enable testing
    void triggerAutomaticMutantEquivalenceForGame(MultiplayerGame game) {
        int threshold = game.getAutomaticMutantEquivalenceThreshold();
        // Get all the live mutants in the game
        for (Mutant aliveMutant : game.getAliveMutants()) {
            /*
             * If the mutant is covered by enough tests trigger the automatic
             * equivalence duel. Consider ONLY the coveringTests submitted after the mutant was created
             */
            Set<Integer> allCoveringTests = testRepo.getCoveringTestsForMutant(aliveMutant).stream()
                    .map(Test::getId)
                    .collect(Collectors.toSet());

            Set<Integer> testSubmittedAfterMutant =
                    testRepo.getValidTestsForGameSubmittedAfterMutant(game.getId(), aliveMutant)
                            .stream()
                            .map(Test::getId)
                            .collect(Collectors.toSet());

            allCoveringTests.retainAll(testSubmittedAfterMutant);

            int numberOfCoveringTestsSubmittedAfterMutant = allCoveringTests.size();

            if (numberOfCoveringTestsSubmittedAfterMutant >= threshold) {
                automaticEquivalenceDuelsTriggered.inc();
                // Flag the mutant as possibly equivalent
                aliveMutant.setEquivalent(Mutant.Equivalence.PENDING_TEST);
                mutantRepo.updateMutant(aliveMutant);
                // Send the notification about the flagged mutant to attacker
                Optional<Integer> mutantOwnerId = userRepo.getUserIdForPlayerId(aliveMutant.getPlayerId());
                Event event = new Event(-1, game.getId(), mutantOwnerId.orElse(0),
                        "One of your mutants survived "
                                + (threshold == numberOfCoveringTestsSubmittedAfterMutant ? "" : "more than ") + threshold
                                + "tests so it was automatically claimed as equivalent.",
                        // TODO it might make sense to specify a new event type?
                        EventType.DEFENDER_MUTANT_EQUIVALENT, EventStatus.NEW,
                        new Timestamp(System.currentTimeMillis()));
                eventDAO.insert(event);
                /*
                 * Register the event to DB
                 */
                mutantRepo.insertEquivalence(aliveMutant, Constants.DUMMY_CREATOR_USER_ID);
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
    private void createTest(HttpServletRequest request,
                            HttpServletResponse response,
                            MultiplayerGame game) throws IOException {
        // Get the text submitted by the user.
        final Optional<String> testText = ServletUtils.getStringParameter(request, "test");
        if (testText.isEmpty()) {
            previousSubmission.clear();
            messages.add("Parameter 'test' is missing.");
            response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + game.getId());
            return;
        }

        previousSubmission.setMutantCode(testText.get());

        var selectedLines = ServletUtils.getStringParameter(request, "selected_lines")
                .map(DefenderIntention::parseIntentionFromCommaSeparatedValueString);
        if (game.isCapturePlayersIntention() && (selectedLines.isEmpty() || selectedLines.get().isEmpty())) {
            messages.add("You cannot submit a test without specifying a line to cover.");
            response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + game.getId());
            return;
        }

        switch (gameManagingUtils.canUserSubmitTest(game, login.getUserId())) {
            case USER_NOT_PART_OF_THE_GAME -> {
                messages.add("User is not a player in the game.");
                logger.info("User {} not part of game {}. Aborting request.", login.getUserId(), game.getId());
                response.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
                return;
            }
            case USER_NOT_A_DEFENDER -> {
                messages.add("Can only submit tests if you are an Defender!");
                response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + game.getId());
                return;
            }
            case GAME_NOT_ACTIVE -> {
                messages.add(GRACE_PERIOD_MESSAGE);
                response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + game.getId());
                return;
            }
            case YES -> {
            }
        }

        GameManagingUtils.CreateBattlegroundTestResult result;
        try {
            result = gameManagingUtils.createBattlegroundTest(game, login.getUserId(), testText.get());
        } catch (IOException e) {
            messages.add(TEST_GENERIC_ERROR_MESSAGE);
            response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + game.getId());
            return;
        }

        if (result.isSuccess()) {
            var test = result.test().orElseThrow();

            messages.add(TEST_PASSED_ON_CUT_MESSAGE);
            gameManagingUtils.getTestSmellsMessage(test).ifPresent(messages::add);
            result.mutationTesterMessage().ifPresent(messages::add);

            // Clear the mutant code only if mutant is accepted
            previousSubmission.clear();

        } else {
            switch (result.failureReason().orElseThrow()) {
                case VALIDATION_FAILED -> {
                    result.validationErrorMessages().ifPresent(errors -> {
                        for (var error : errors) {
                            messages.add(error).fadeOut(false);
                        }
                    });
                }
                case COMPILATION_FAILED -> {
                    messages.add(TEST_DID_NOT_COMPILE_MESSAGE).fadeOut(false);
                    result.compilationError().ifPresent(this::handleCompilationError);
                }
                case TEST_DID_NOT_PASS_ON_CUT -> {
                    messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE).fadeOut(false);
                    result.testCutError().ifPresent(error -> messages.add(error).fadeOut(false));
                }
            }

            if (game.isCapturePlayersIntention() && result.test().isPresent()) {
                collectDefenderIntentions(result.test().get(), selectedLines.get(), Set.of());
                // Store intentions in the session in case tests is broken we automatically re-select the same line
                // TODO At the moment, there is one and only one line
                var firstLine = selectedLines.get().stream().findFirst().get();
                previousSubmission.setSelectedLine(firstLine);
            }
        }

        response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + game.getId());
    }


    private void handleCompilationError(String errorMessage) {
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

    private void createMutant(HttpServletRequest request,
                              HttpServletResponse response,
                              MultiplayerGame game) throws IOException {
        // Get the text submitted by the user.
        final Optional<String> mutantText = ServletUtils.getStringParameter(request, "mutant");
        if (mutantText.isEmpty()) {
            previousSubmission.clear();
            messages.add("Parameter 'mutant' is missing.");
            response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + game.getId());
            return;
        }

        previousSubmission.setMutantCode(mutantText.get());

        var intention = ServletUtils.getStringParameter(request, "attacker_intention").map(AttackerIntention::fromString);
        if (game.isCapturePlayersIntention() && intention.isEmpty()) {
            messages.add(ValidationMessage.MUTANT_MISSING_INTENTION.get());
            response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + game.getId());
            return;
        }

        switch (gameManagingUtils.canUserSubmitMutant(game, login.getUserId(), config.isBlockAttacker())) {
            case USER_NOT_PART_OF_THE_GAME -> {
                messages.add("User is not a player in the game.");
                logger.info("User {} not part of game {}. Aborting request.", login.getUserId(), game.getId());
                response.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
                return;
            }
            case USER_NOT_AN_ATTACKER -> {
                messages.add("Can only submit mutants if you are an Attacker.");
                response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + game.getId());
                return;
            }
            case GAME_NOT_ACTIVE -> {
                messages.add(GRACE_PERIOD_MESSAGE);
                response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + game.getId());
                return;
            }
            case USER_HAS_PENDING_EQUIVALENCE_DUELS -> {
                messages.add(Constants.ATTACKER_HAS_PENDING_DUELS);
                // Keep the submitted Mutant around so students do not lose mutants once the duel is solved.
                response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + game.getId());
                return;
            }
            case YES -> {
            }
        }

        GameManagingUtils.CreateBattlegroundMutantResult result;
        try {
            result = gameManagingUtils.createBattlegroundMutant(
                    game, login.getUserId(), mutantText.get());
        } catch (GameManagingUtils.MutantCreationException e) {
            messages.add(MUTANT_CREATION_ERROR_MESSAGE);
            logger.debug("Error creating mutant. Game: {}, Class: {}, User: {}, Mutant: {}",
                    game.getId(), game.getClassId(), login.getUserId(), mutantText);
            response.sendRedirect(url.forPath(org.codedefenders.util.Paths.BATTLEGROUND_GAME) + "?gameId=" + game.getId());
            return;
        }

        if (result.isSuccess()) {
            var mutant = result.mutant().orElseThrow();
            if (game.isCapturePlayersIntention()) {
                collectAttackerIntentions(mutant, intention.orElseThrow());
            }

            // Clear the mutant code only if mutant is accepted
            previousSubmission.clear();

            messages.add(MUTANT_COMPILED_MESSAGE, TITLE_SUCCESS);
            result.mutationTesterMessage().ifPresent(messages::add);
            logger.info("Successfully created mutant {} ", mutant.getId());

        } else {
            switch (result.failureReason().orElseThrow()) {
                case VALIDATION_FAILED -> {
                    // Mutant is either the same as the CUT or it contains invalid code
                    result.validationErrorMessage().ifPresent(error -> messages.add(error.get()).fadeOut(false));
                }
                case DUPLICATE_MUTANT_FOUND -> {
                    messages.add(MUTANT_DUPLICATED_MESSAGE);
                    result.compilationError().ifPresent(this::handleCompilationError);
                }
                case COMPILATION_FAILED -> {
                    messages.add(MUTANT_UNCOMPILABLE_MESSAGE, "FAILURE").fadeOut(false)
                            .setSecondary("Could not compile");//TODO Just for demo purposes, remove before final merge
                    result.compilationError().ifPresent(this::handleCompilationError);
                }
            }
        }

        response.sendRedirect(url.forPath(org.codedefenders.util.Paths.BATTLEGROUND_GAME) + "?gameId=" + game.getId());
    }

    @SuppressWarnings("Duplicates")
    private void resolveEquivalence(HttpServletRequest request,
                                    HttpServletResponse response,
                                    int gameId,
                                    MultiplayerGame game) throws IOException {
        final Optional<Integer> equivMutantId = ServletUtils.getIntParameter(request, "equivMutantId");
        if (equivMutantId.isEmpty()) {
            logger.debug("Missing equivMutantId parameter.");
            response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + gameId);
            return;
        }
        String resolveAction = request.getParameter("resolveAction");
        Mutant equivMutant = mutantRepo.getMutantById(equivMutantId.get());

        switch (gameManagingUtils.canUserResolveEquivalence(game, login.getUserId(), equivMutantId.get())) {
            case USER_NOT_PART_OF_THE_GAME -> {
                messages.add("User is not a player in the game.");
                logger.info("User {} not part of game {}. Aborting request.", login.getUserId(), game.getId());
                response.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
            }
            case USER_NOT_AN_ATTACKER -> {
                messages.add("Can only resolve equivalence duels if you are an Attacker!");
                response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + gameId);
                return;
            }
            case GAME_NOT_ACTIVE -> {
                messages.add(String.format("Game %d has finished.", gameId));
                response.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
                return;
            }
            case MUTANT_DOES_NOT_EXIST -> {
                messages.add("Mutant does not exist.");
                response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + gameId);
                return;
            }
            case USER_DID_NOT_CREATE_MUTANT -> {
                logger.info("User {} tried to accept equivalence for mutant {}, but mutant is written by another player.",
                        login.getUserId(), equivMutantId.get());
                response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + gameId);
                return;
            }
            case MUTANT_IS_NOT_PENDING -> {
                logger.info("User {} tried to accept equivalence for mutant {}, but mutant has no pending equivalences.",
                        login.getUserId(), equivMutantId.get());
                if (equivMutant.getState() == KILLED) {
                    messages.add("Too late. The mutant was already killed and therefore proven to be not equivalent.");
                    // TODO: Continue with the resolution to give them the option to win other duels? (only if action=="reject")
                }
                response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + gameId);
                return;
            }
            case YES -> {}
        }


        if ("accept".equals(resolveAction)) {
            var result = gameManagingUtils.acceptBattlegroundEquivalence(game, login.getUserId(), equivMutant);
            if (result.mutantKillable()) {
                messages.add(Constants.MUTANT_ACCEPTED_EQUIVALENT_MESSAGE + " " + "However, the mutant was killable!");
            } else {
                messages.add(Constants.MUTANT_ACCEPTED_EQUIVALENT_MESSAGE);
            }
            response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + gameId);

        } else if ("reject".equals(resolveAction)) {
            final Optional<String> test = ServletUtils.getStringParameter(request, "test");
            if (test.isEmpty()) {
                previousSubmission.clear();
                response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + gameId);
                return;
            }

            GameManagingUtils.RejectBattlegroundEquivalenceResult result;
            try {
                result = gameManagingUtils.rejectBattlegroundEquivalence(game, login.getUserId(), equivMutant, test.get());
            } catch (IOException e) {
                messages.add(TEST_GENERIC_ERROR_MESSAGE);
                response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + game.getId());
                return;
            }

            if (result.testValid()) {
                if (result.killedPendingMutant().orElseThrow()) {
                    messages.add(TEST_KILLED_CLAIMED_MUTANT_MESSAGE);
                } else {
                    String message = TEST_DID_NOT_KILL_CLAIMED_MUTANT_MESSAGE;
                    if (result.isMutantKillable().orElseThrow()) {
                        message = message + " " + "Unfortunately, the mutant was killable!";
                    }
                    messages.add(message);
                }

                int numKilledOthers = result.numOtherPendingMutantsKilled().orElseThrow();
                if (numKilledOthers == 1) {
                    messages.add("Additionally, your test did kill another claimed mutant!");
                } else if (numKilledOthers > 1) {
                    messages.add(String.format("Additionally, your test killed other %d claimed mutants!", numKilledOthers));
                }
                previousSubmission.clear();

            } else {
                switch (result.failureReason().orElseThrow()) {
                    case VALIDATION_FAILED -> {
                        result.validationErrorMessages().ifPresent(errors -> {
                            for (var error : errors) {
                                messages.add(error).fadeOut(false);
                            }
                        });
                    }
                    case COMPILATION_FAILED -> {
                        messages.add(TEST_DID_NOT_COMPILE_MESSAGE).fadeOut(false);
                        result.compilationError().ifPresent(this::handleCompilationError);
                    }
                    case TEST_DID_NOT_PASS_ON_CUT -> {
                        messages.add(TEST_DID_NOT_PASS_ON_CUT_MESSAGE).fadeOut(false);
                        result.testCutError().ifPresent(error -> messages.add(error).fadeOut(false));
                    }
                }
            }
            response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + gameId);
        }
    }

    private void claimEquivalent(HttpServletRequest request,
                                 HttpServletResponse response,
                                 int gameId,
                                 MultiplayerGame game) throws IOException {
        switch (gameManagingUtils.canUserClaimEquivalence(game, login.getUserId())) {
            case USER_NOT_PART_OF_THE_GAME -> {
                messages.add("User is not a player in the game.");
                logger.info("User {} not part of game {}. Aborting request.", login.getUserId(), game.getId());
                response.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
                return;
            }
            case USER_NOT_A_DEFENDER -> {
                Role role = game.getRole(login.getUserId());
                messages.add("Can only claim mutant as equivalent if you are a Defender!");
                logger.info("Non defender (role={}) tried to claim mutant as equivalent.", role);
                response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + gameId);
                return;
            }
            case GAME_NOT_ACTIVE -> {
                messages.add("You cannot claim mutants as equivalent in this game anymore.");
                logger.info("Mutant claimed for non-active game.");
                Redirect.redirectBack(request, response);
                return;
            }
            case YES -> {}
        }

        Optional<String> equivLinesParam = ServletUtils.getStringParameter(request, "equivLines");
        if (equivLinesParam.isEmpty()) {
            logger.debug("Missing 'equivLines' parameter.");
            Redirect.redirectBack(request, response);
            return;
        }

        List<Integer> equivLines;
        try {
            equivLines = Arrays.stream(equivLinesParam.get().split(","))
                    .map(Integer::valueOf)
                    .toList();
        } catch (NumberFormatException e) {
            logger.debug("Invalid 'equivLines' parameter.");
            Redirect.redirectBack(request, response);
            return;
        }

        var result = gameManagingUtils.claimBattlegroundEquivalence(game, login.getUserId(), equivLines);
        messages.addAll(result.messages());
        response.sendRedirect(url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + gameId);
    }

    private void collectDefenderIntentions(Test newTest, Set<Integer> selectedLines, Set<Integer> selectedMutants) {
        DefenderIntention intention = new DefenderIntention(selectedLines, selectedMutants);
        if (intentionRepository.storeIntentionForTest(newTest, intention).isEmpty()) {
            logger.error("Could not store defender intention to database.");
        }
    }

    private void collectAttackerIntentions(Mutant newMutant, AttackerIntention intention) {
        if (intentionRepository.storeIntentionForMutant(newMutant, intention).isEmpty()) {
            logger.error("Could not store attacker intention to database.");
        }
    }
}
