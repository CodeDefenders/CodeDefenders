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

import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.execution.KillMap;
import org.codedefenders.execution.KillMap.KillMapEntry;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.model.Player;
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

        String contextPath = request.getContextPath();

        HttpSession session = request.getSession();
        ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        String startDateParam;
        String startHoursParam;
        String startMinutesParam;
        String endDateParam;
        String endHoursParam;
        String endMinutesParam;
        int classId;
        int minDefenders;
        int defenderLimit;
        int minAttackers;
        int attackerLimit;
        int maxAssertionsPerTest;
        CodeValidatorLevel mutantValidatorLevel;

        try {
            startDateParam = getStringParameter(request, "start_dateTime").get();
            startHoursParam = getStringParameter(request, "start_hours").get();
            startMinutesParam = getStringParameter(request, "start_minutes").get();
            endDateParam = getStringParameter(request, "finish_dateTime").get();
            endHoursParam = getStringParameter(request, "finish_hours").get();
            endMinutesParam = getStringParameter(request, "finish_minutes").get();
            classId = getIntParameter(request, "class").get();
            minDefenders = getIntParameter(request, "minDefenders").get();
            defenderLimit = getIntParameter(request, "defenderLimit").get();
            minAttackers = getIntParameter(request, "minAttackers").get();
            attackerLimit = getIntParameter(request, "attackerLimit").get();
            maxAssertionsPerTest = getIntParameter(request, "maxAssertionsPerTest").get();
            mutantValidatorLevel = getStringParameter(request, "mutantValidatorLevel").map(CodeValidatorLevel::valueOrNull).get();
        } catch (NoSuchElementException e) {
            logger.error("At least one request parameter was missing or was no valid integer value.", e);
            Redirect.redirectBack(request, response);
            return;
        }

        GameLevel level = parameterThenOrOther(request, "level", GameLevel.EASY, GameLevel.HARD);
        float lineCoverage = getFloatParameter(request, "line_cov").orElse(1.1f);
        float mutantCoverage = getFloatParameter(request, "mutant_cov").orElse(1.1f);
        boolean chatEnabled = parameterThenOrOther(request, "chatEnabled", true, false);
        boolean markUncovered = parameterThenOrOther(request, "markUncovered", true, false);
        boolean capturePlayersIntention = parameterThenOrOther(request, "capturePlayersIntention", true, false);

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<MultiplayerGame>> validationResults = new HashSet<>();

        /*
         * Since JSR 303 works on Beans, and we have String input
         * values, we need to manually run the validation for them
         */
        String startDate = startDateParam + " " + startHoursParam + ":" + startMinutesParam;
        String finishDate = endDateParam + " " + endHoursParam + ":" + endMinutesParam;
        validationResults.addAll(validator.validateValue(MultiplayerGame.class, "startDateTime", startDate));
        validationResults.addAll(validator.validateValue(MultiplayerGame.class, "finishDateTime", finishDate));
        final long startTime;
        final long endTime;
        try {
            startTime = simpleDateFormat.parse(startDate).getTime();
            endTime = simpleDateFormat.parse(finishDate).getTime();
        } catch (ParseException e) {
            Redirect.redirectBack(request, response);
            return;
        }

        validationResults.addAll(validator.validateValue(MultiplayerGame.class, "startDateTime", startDate));
        validationResults.addAll(validator.validateValue(MultiplayerGame.class, "finishDateTime", finishDate));

//        At this point, if there's validation errors, report them to the user and abort.
        if (!validationResults.isEmpty()) {
            for (ConstraintViolation<MultiplayerGame> violation : validationResults) {
                messages.add(violation.getMessage());
            }
            Redirect.redirectBack(request, response);
            return;
        }

        MultiplayerGame nGame = new MultiplayerGame.Builder(classId, userId, startTime, endTime, maxAssertionsPerTest, defenderLimit, attackerLimit, minDefenders, minAttackers)
                .level(level)
                .chatEnabled(chatEnabled)
                .markUncovered(markUncovered)
                .capturePlayersIntention(capturePlayersIntention)
                .lineCoverage(lineCoverage)
                .mutantCoverage(mutantCoverage)
                .mutantValidatorLevel(mutantValidatorLevel)
                .build();

        validator.validate(nGame);
        if (!validationResults.isEmpty()) {
            for (ConstraintViolation<MultiplayerGame> violation : validationResults) {
                messages.add(violation.getMessage());
            }
            Redirect.redirectBack(request, response);
            return;
        }

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

        int dummyAttackerPlayerId = -1;
        int dummyDefenderPlayerId = -1;
        for (Player player : GameDAO.getAllPlayersForGame(nGame.getId())) {
            if (player.getUser().getId() == DUMMY_ATTACKER_USER_ID) {
                dummyAttackerPlayerId = player.getId();
            } else if (player.getUser().getId() == DUMMY_DEFENDER_USER_ID) {
                dummyDefenderPlayerId = player.getId();
            }
        }

        assert dummyAttackerPlayerId != -1;
        assert dummyDefenderPlayerId != -1;

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

        if (defenderParamExists) {
            if (game.addPlayer(userId, Role.DEFENDER)) {
                logger.info("User {} joined game {} as a defender.", userId, gameId);
                response.sendRedirect(ctx(request) + Paths.BATTLEGROUND_GAME + "?gameId=" + gameId);
            } else {
                logger.info("User {} failed to join game {} as a defender.", userId, gameId);
                response.sendRedirect(ctx(request) + Paths.GAMES_OVERVIEW);
            }
        } else if (attackerParamExists) {
            if (game.addPlayer(userId, Role.ATTACKER)) {
                logger.info("User {} joined game {} as an attacker.", userId, gameId);
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
                KillmapDAO.enqueueJob(new KillMap.KillMapJob(KillMap.KillMapJob.Type.GAME, gameId));
            }
            response.sendRedirect(ctx(request) + Paths.BATTLEGROUND_SELECTION);
        } else {
            response.sendRedirect(ctx(request) + Paths.BATTLEGROUND_HISTORY + "?gameId=" + gameId);
        }
    }
}
