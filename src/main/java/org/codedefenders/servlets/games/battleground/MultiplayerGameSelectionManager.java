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

import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.database.PlayerDAO;
import org.codedefenders.database.UserDAO;
import org.codedefenders.execution.KillMap;
import org.codedefenders.execution.KillMap.KillMapEntry;
import org.codedefenders.execution.KillMapProcessor;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.model.User;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.model.GameCreatedEvent;
import org.codedefenders.notification.model.GameJoinedEvent;
import org.codedefenders.notification.model.GameLeftEvent;
import org.codedefenders.notification.model.GameStartedEvent;
import org.codedefenders.notification.model.GameStoppedEvent;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Paths;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import static org.codedefenders.servlets.util.ServletUtils.ctx;
import static org.codedefenders.servlets.util.ServletUtils.formType;
import static org.codedefenders.servlets.util.ServletUtils.gameId;
import static org.codedefenders.servlets.util.ServletUtils.getFloatParameter;
import static org.codedefenders.servlets.util.ServletUtils.getIntParameter;
import static org.codedefenders.servlets.util.ServletUtils.getStringParameter;
import static org.codedefenders.servlets.util.ServletUtils.parameterThenOrOther;
import static org.codedefenders.servlets.util.ServletUtils.userId;
import static org.codedefenders.util.Constants.DUMMY_ATTACKER_USER_ID;
import static org.codedefenders.util.Constants.DUMMY_DEFENDER_USER_ID;

/**
 * This {@link HttpServlet} handles selection of {@link MultiplayerGame battleground games}.
 * <p>
 * {@code GET} requests redirect to the game overview page and {@code POST} requests handle creating, joining
 * and entering {@link MultiplayerGame battleground games}.
 * <p>
 * Serves under {@code /multiplayer/games}.
 *
 * @see org.codedefenders.util.Paths#BATTLEGROUND_SELECTION
 */
public class MultiplayerGameSelectionManager extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(MultiplayerGameSelectionManager.class);

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    @Inject
    private INotificationService notificationService;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect(ctx(request) + Paths.GAMES_OVERVIEW);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final String action = formType(request);
        switch (action) {
            case "createGame":
                createGame(request, response);
                return;
            case "joinGame":
                joinGame(request, response);
                return;
            case "leaveGame":
                leaveGame(request, response);
                return;
            case "startGame":
                startGame(request, response);
                return;
            case "endGame":
                endGame(request, response);
                return;
            default:
                logger.info("Action not recognised: {}", action);
                Redirect.redirectBack(request, response);
                break;
        }
    }

    private void createGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final int userId = userId(request);

        final boolean canCreateGames = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.GAME_CREATION).getBoolValue();
        if (!canCreateGames) {
            logger.warn("User {} tried to create a battleground game, but creating games is not permitted.", userId);
            Redirect.redirectBack(request, response);
            return;
        }

        String contextPath = request.getContextPath();

        HttpSession session = request.getSession();
        ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        int classId;
        int maxAssertionsPerTest;
        CodeValidatorLevel mutantValidatorLevel;
        Role selectedRole;

        try {
            classId = getIntParameter(request, "class").get();
            maxAssertionsPerTest = getIntParameter(request, "maxAssertionsPerTest").get();
            mutantValidatorLevel = getStringParameter(request, "mutantValidatorLevel").map(CodeValidatorLevel::valueOrNull).get();
            selectedRole = getStringParameter(request, "roleSelection").map(Role::valueOrNull).get();
        } catch (NoSuchElementException e) {
            logger.error("At least one request parameter was missing or was no valid integer value.", e);
            Redirect.redirectBack(request, response);
            return;
        }

        GameLevel level = parameterThenOrOther(request, "level", GameLevel.EASY, GameLevel.HARD);
        float lineCoverage = getFloatParameter(request, "line_cov").orElse(1.1f);
        float mutantCoverage = getFloatParameter(request, "mutant_cov").orElse(1.1f);
        boolean chatEnabled = parameterThenOrOther(request, "chatEnabled", true, false);
        boolean capturePlayersIntention = parameterThenOrOther(request, "capturePlayersIntention", true, false);

        MultiplayerGame nGame = new MultiplayerGame.Builder(classId, userId, maxAssertionsPerTest)
                .level(level)
                .chatEnabled(chatEnabled)
                .capturePlayersIntention(capturePlayersIntention)
                .lineCoverage(lineCoverage)
                .mutantCoverage(mutantCoverage)
                .mutantValidatorLevel(mutantValidatorLevel)
                .build();

        if (nGame.insert()) {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            Event event = new Event(-1, nGame.getId(), userId, "Game Created",
                    EventType.GAME_CREATED, EventStatus.GAME, timestamp);
            event.insert();
        }

        // Mutants and tests uploaded with the class are already stored in the DB
        List<Mutant> uploadedMutants = GameClassDAO.getMappedMutantsForClassId(classId);
        List<Test> uploadedTests = GameClassDAO.getMappedTestsForClassId(classId);

        // Always add system player to send mutants and tests at runtime!
        nGame.addPlayer(DUMMY_ATTACKER_USER_ID, Role.ATTACKER);
        nGame.addPlayer(DUMMY_DEFENDER_USER_ID, Role.DEFENDER);

        // Add selected role to game if the creator participates as attacker/defender
        if (selectedRole.equals(Role.ATTACKER) || selectedRole.equals(Role.DEFENDER)) {
            nGame.addPlayer(userId, selectedRole);
        }

        int dummyAttackerPlayerId = PlayerDAO.getPlayerIdForUserAndGame(DUMMY_ATTACKER_USER_ID, nGame.getId());
        int dummyDefenderPlayerId = PlayerDAO.getPlayerIdForUserAndGame(DUMMY_DEFENDER_USER_ID, nGame.getId());

        // this mutant map links the uploaded mutants and the once generated from them here
        // This implements bookkeeping for killmap
        Map<Mutant, Mutant> mutantMap = new HashMap<>();
        Map<Test, Test> testMap = new HashMap<>();

        boolean withTests = parameterThenOrOther(request, "withTests", true, false);
        boolean withMutants = parameterThenOrOther(request, "withMutants", true, false);
        // Register Valid Mutants.
        if (withMutants) {
            // Validate uploaded mutants from the list
            // TODO
            // Link the mutants to the game
            for (Mutant mutant : uploadedMutants) {
//                          final String mutantCode = new String(Files.readAllBytes();
                Mutant newMutant = new Mutant(nGame.getId(), classId,
                        mutant.getJavaFile(),
                        mutant.getClassFile(),
                        // Alive be default
                        true,
                        //
                        dummyAttackerPlayerId);
                // insert this into the DB and link the mutant to the game
                newMutant.insert();
                // BookKeeping
                mutantMap.put(mutant, newMutant);
            }
        }
        // Register Valid Tests
        if (withTests) {
            // Validate the tests from the list
            // TODO
            for (Test test : uploadedTests) {
                // At this point we need to fill in all the details
                Test newTest = new Test(-1, classId, nGame.getId(), test.getJavaFile(),
                        test.getClassFile(), 0, 0, dummyDefenderPlayerId, test.getLineCoverage().getLinesCovered(),
                        test.getLineCoverage().getLinesUncovered(), 0);
                newTest.insert();
                testMap.put(test, newTest);
            }
        }

        if (withMutants && withTests) {
            List<KillMapEntry> killmap = KillmapDAO.getKillMapEntriesForClass(classId);
            // Filter the killmap and keep only the one created during the upload ...

            for (Mutant uploadedMutant : uploadedMutants) {
                boolean alive = true;
                for (Test uploadedTest : uploadedTests) {
                    // Does the test kill the mutant?
                    for (KillMapEntry entry : killmap) {
                        if (entry.mutant.getId() == uploadedMutant.getId() &&
                                entry.test.getId() == uploadedTest.getId() &&
                                entry.status.equals(KillMapEntry.Status.KILL)) {

                            // If the mutant was not yet killed by some other test increment the kill count for the test
                            // and kill the mutant, otherwise continue
                            // We need this because the killmap gives us all the possible combinations !
                            if( mutantMap.get(uploadedMutant).isAlive() ){
                                testMap.get( uploadedTest).killMutant();
                                mutantMap.get(uploadedMutant).kill();
                            }
                            alive = false;
                            break;
                        }
                    }
                    if (!alive) {
                        break;
                    }
                }
            }
        }

        /*
         * Publish the event that a new game started
         */
        notificationService.post(new GameCreatedEvent(nGame));


        // Redirect to admin interface
        if (request.getParameter("fromAdmin").equals("true")) {
            response.sendRedirect(contextPath + "/admin");
            return;
        }
        // Redirect to the game selection menu.
        response.sendRedirect(contextPath + Paths.GAMES_OVERVIEW);
    }

    private void joinGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final int userId = userId(request);

        final boolean canJoinGames = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.GAME_JOINING).getBoolValue();
        if (!canJoinGames) {
            logger.warn("User {} tried to join a battleground game, but joining games is not permitted.", userId);
            Redirect.redirectBack(request, response);
            return;
        }

        HttpSession session = request.getSession();
        ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        final Optional<Integer> gameIdOpt = gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.error("No gameId parameter. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }
        final int gameId = gameIdOpt.get();

        MultiplayerGame game = MultiplayerGameDAO.getMultiplayerGame(gameId);
        if (game == null) {
            logger.error("No game found for gameId={}. Aborting request.", gameId);
            Redirect.redirectBack(request, response);
            return;
        }

        Role role = game.getRole(userId);

        if (role != Role.NONE) {
            logger.info("User {} already in the requested game. Has role {}", userId, role);
            return;
        }
        boolean defenderParamExists = ServletUtils.parameterThenOrOther(request, "defender", true, false);
        boolean attackerParamExists = ServletUtils.parameterThenOrOther(request, "attacker", true, false);

        User user = UserDAO.getUserById(userId);

        if (defenderParamExists) {
            if (game.addPlayer(userId, Role.DEFENDER)) {
                logger.info("User {} joined game {} as a defender.", userId, gameId);

                /*
                 * Publish the event about the user
                 */
                notificationService.post(new GameJoinedEvent(game, user));

                response.sendRedirect(ctx(request) + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            } else {
                logger.info("User {} failed to join game {} as a defender.", userId, gameId);
                response.sendRedirect(ctx(request) + Paths.GAMES_OVERVIEW);
            }
        } else if (attackerParamExists) {
            if (game.addPlayer(userId, Role.ATTACKER)) {
                logger.info("User {} joined game {} as an attacker.", userId, gameId);

                /*
                 * Publish the event about the user
                 */
                notificationService.post(new GameJoinedEvent(game, user));

                response.sendRedirect(ctx(request) + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            } else {
                logger.info("User {} failed to join game {} as an attacker.", userId, gameId);
                response.sendRedirect(ctx(request) + Paths.GAMES_OVERVIEW);
            }
        } else {
            logger.debug("No 'defender' or 'attacker' request parameter found. Abort request.");
            response.sendRedirect(ctx(request) + Paths.GAMES_OVERVIEW);
        }
    }

    private void leaveGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final int userId = userId(request);

        HttpSession session = request.getSession();
        String contextPath = request.getContextPath();
        ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        final Optional<Integer> gameIdOpt = gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.error("No gameId parameter. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }
        final int gameId = gameIdOpt.get();

        MultiplayerGame game = MultiplayerGameDAO.getMultiplayerGame(gameId);
        if (game == null) {
            logger.error("No game found for gameId={}. Aborting request.", gameId);
            Redirect.redirectBack(request, response);
            return;
        }
        final boolean removalSuccess = game.removePlayer(userId);
        if (!removalSuccess) {
            messages.add("An error occurred while leaving game " + gameId);
            response.sendRedirect(contextPath + Paths.GAMES_OVERVIEW);
            return;
        }

        messages.add("Game " + gameId + " left");
        DatabaseAccess.removePlayerEventsForGame(gameId, userId);

        final EventType notifType = EventType.GAME_PLAYER_LEFT;
        final String message = "You successfully left the game.";
        final EventStatus eventStatus = EventStatus.NEW;
        final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Event notif = new Event(-1, gameId, userId, message, notifType, eventStatus, timestamp);
        notif.insert();

        logger.info("User {} successfully left game {}", userId, gameId);

        /*
         * Publish the event about the user
         */
        User user = UserDAO.getUserById(userId);
        notificationService.post(new GameLeftEvent(game, user));


        response.sendRedirect(contextPath + Paths.GAMES_OVERVIEW);
    }

    private void startGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final Optional<Integer> gameIdOpt = gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.error("No gameId parameter. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }
        final int gameId = gameIdOpt.get();

        MultiplayerGame game = MultiplayerGameDAO.getMultiplayerGame(gameId);
        if (game == null) {
            logger.error("No game found for gameId={}. Aborting request.", gameId);
            Redirect.redirectBack(request, response);
            return;
        }
        if (game.getState() == GameState.CREATED) {
            logger.info("Starting multiplayer game {} (Setting state to ACTIVE)", gameId);
            game.setState(GameState.ACTIVE);
            game.update();
        }

        /*
         * Publish the event about the user
         */
        notificationService.post(new GameStartedEvent(game));

        response.sendRedirect(ctx(request) + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
    }

    private void endGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final Optional<Integer> gameIdOpt = gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.error("No gameId parameter. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }
        final int gameId = gameIdOpt.get();

        MultiplayerGame game = MultiplayerGameDAO.getMultiplayerGame(gameId);
        if (game == null) {
            logger.error("No game found for gameId={}. Aborting request.", gameId);
            Redirect.redirectBack(request, response);
            return;
        }
        if (game.getState() == GameState.ACTIVE) {
            logger.info("Ending multiplayer game {} (Setting state to FINISHED)", gameId);
            game.setState(GameState.FINISHED);
            boolean updated = game.update();
            if (updated) {
                KillmapDAO.enqueueJob(new KillMapProcessor.KillMapJob(KillMap.KillMapType.GAME, gameId));
            }

            /*
             * Publish the event about the user
             */
            notificationService.post(new GameStoppedEvent(game));

            response.sendRedirect(ctx(request) + Paths.BATTLEGROUND_SELECTION);
        } else {
            response.sendRedirect(ctx(request) + Paths.BATTLEGROUND_HISTORY + "?gameId=" + gameId);
        }
    }
}
