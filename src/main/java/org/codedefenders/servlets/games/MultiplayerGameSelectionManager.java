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
package org.codedefenders.servlets.games;

import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.KillmapDAO;
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
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
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

import static org.codedefenders.util.Constants.DUMMY_ATTACKER_USER_ID;
import static org.codedefenders.util.Constants.DUMMY_DEFENDER_USER_ID;

/**
 * This {@link HttpServlet} handles redirecting to multiplayer games for
 * a given identifier and creation of Battleground games.
 * <p>
 * Serves on path: `/multiplayer/games`.
 */
public class MultiplayerGameSelectionManager extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(MultiplayerGameSelectionManager.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String contextPath = request.getContextPath();
        final String gameIdString = request.getParameter("id");
        if (gameIdString == null) {
            logger.info("No gameId provided. Redirecting back to /games/user");
            response.sendRedirect(contextPath + "/games/user");
            return;
        }
        int gameId;
        try {
            gameId = Integer.parseInt(gameIdString);
        } catch (NumberFormatException e) {
            logger.error("Failed to format parameter id", e);
            response.sendRedirect(contextPath + "/games/user");
            return;
        }

        MultiplayerGame mg = DatabaseAccess.getMultiplayerGame(gameId);
        if (mg == null) {
            logger.warn("Could not find requested game: {}", gameId);
            response.sendRedirect(contextPath + "/games/user");
        } else {
            mg.notifyPlayers();
            String redirect = contextPath + "/multiplayer/play?id=" + gameId;
            if (request.getParameter("attacker") != null) {
                redirect += "&attacker=1";
            } else if (request.getParameter("defender") != null) {
                redirect += "&defender=1";
            }
            response.sendRedirect(redirect);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession();
        String contextPath = request.getContextPath();
        ArrayList<String> messages = new ArrayList<String>();
        session.setAttribute("messages", messages);
        // Get their user id from the session.
        int uid = (Integer) session.getAttribute("uid");

        // Get the identifying information required to create a game from the submitted form.
        final String action = request.getParameter("formType");
        switch (action) {
            case "createGame":
                try {
                    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
                    Validator validator = factory.getValidator();
                    Set<ConstraintViolation<MultiplayerGame>> validationResults = new HashSet<>();

                    /*
                     * Since JSR 303 works on Beans, and we have String input
                     * values, we need to manually run the validation for them
                     */
                    String startDate = new StringBuilder()
                            .append(request.getParameter("start_dateTime"))
                            .append(" ")
                            .append(request.getParameter("start_hours"))
                            .append(":")
                            .append(request.getParameter("start_minutes"))
                            .toString();

                    validationResults.addAll(validator.validateValue(MultiplayerGame.class, "startDateTime", startDate));

                    String finishDate = new StringBuilder()
                            .append(request.getParameter("finish_dateTime"))
                            .append(" ")
                            .append(request.getParameter("finish_hours"))
                            .append(":")
                            .append(request.getParameter("finish_minutes"))
                            .toString();
                    validationResults.addAll(validator.validateValue(MultiplayerGame.class, "finishDateTime", finishDate));

//                  At this point, if there's validation errors, report them to the user and abort.
                    if (!validationResults.isEmpty()) {
                        for (ConstraintViolation<MultiplayerGame> violation : validationResults) {
                            messages.add(violation.getMessage());
                        }
                        Redirect.redirectBack(request, response);
                        return;
                    }

                    int classId = Integer.parseInt(request.getParameter("class"));
                    GameLevel level = request.getParameter("level") == null ? GameLevel.HARD : GameLevel.EASY;
                    boolean withTests = request.getParameter("withTests") != null;
                    boolean withMutants = request.getParameter("withMutants") != null;
                    String lineCovGoal = request.getParameter("line_cov");
                    String mutCovGoal = request.getParameter("mutant_cov");
                    double lineCoverage = lineCovGoal == null ? 1.1 : Double.parseDouble(lineCovGoal);
                    double mutantCoverage = mutCovGoal == null ? 1.1 : Double.parseDouble(mutCovGoal);
                    boolean chatEnabled = request.getParameter("chatEnabled") != null;
                    boolean markUncovered = request.getParameter("markUncovered") != null;
                    final int minDefenders = Integer.parseInt(request.getParameter("minDefenders"));
                    final int defenderLimit = Integer.parseInt(request.getParameter("defenderLimit"));
                    final int minAttackers = Integer.parseInt(request.getParameter("minAttackers"));
                    final int attackerLimit = Integer.parseInt(request.getParameter("attackerLimit"));
                    final long startTime = new SimpleDateFormat("yyyy/MM/dd HH:mm").parse(startDate).getTime();
                    final long endTime = new SimpleDateFormat("yyyy/MM/dd HH:mm").parse(finishDate).getTime();
                    final int maxAssertionsPerTest = Integer.parseInt(request.getParameter("maxAssertionsPerTest"));
                    final CodeValidatorLevel mutantValidatorLevel = CodeValidatorLevel.valueOf(request.getParameter("mutantValidatorLevel"));

                    boolean capturePlayersIntention = request.getParameter("capturePlayersIntention") != null;

                    // TODO Not sure what it does, but this was false by default
                    boolean requiresValidation = false;

                    MultiplayerGame nGame = new MultiplayerGame(classId, uid, level, (float) lineCoverage,
                            (float) mutantCoverage, 1f, 100, 100, defenderLimit,
                            attackerLimit,
                            minDefenders,
                            minAttackers,
                            startTime,
                            endTime,
                            GameState.CREATED.name(),
                            requiresValidation,
                            maxAssertionsPerTest,
                            chatEnabled,
                            mutantValidatorLevel,
                            markUncovered,
                            capturePlayersIntention);

                    validationResults = validator.validate(nGame);
                    if (!validationResults.isEmpty()) {
                        for (ConstraintViolation<MultiplayerGame> violation : validationResults) {
                            messages.add(violation.getMessage());
                        }
                        Redirect.redirectBack(request, response);
                        break;
                    }

                    if (nGame.insert()) {
                        final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                        Event event = new Event(-1, nGame.getId(), uid, "Game Created",
                                EventType.GAME_CREATED, EventStatus.GAME, timestamp);
                        event.insert();
                    }

                    // Mutants and tests uploaded with the class are already stored in the DB
                    List<Mutant> uploadedMutants = GameClassDAO.getMappedMutantsForClassId(classId);
                    // This returns ALL the tests linked to the same class, meaning that all the games which use the same class are included here !
                    List<Test> uploadedTests = GameClassDAO.getMappedTestsForClassId(classId);
                    //
                    ListIterator<Test> iter = uploadedTests.listIterator();
                    while (iter.hasNext()) {
                        Test test = iter.next();
                        if (test.getPlayerId() != -1) {
                            iter.remove();
                        }
                    }

                    //
                    // Always add system player to send mutants and tests at runtime!
                    nGame.addPlayer(DUMMY_ATTACKER_USER_ID, Role.ATTACKER);
                    nGame.addPlayer(DUMMY_DEFENDER_USER_ID, Role.DEFENDER);

                    // this mutant map links the uploaded mutants and the once generated from them here
                    // This implements bookkeeping for killmap
                    Map<Mutant, Mutant> mutantMap = new HashMap<>();
                    Map<Test, Test> testMap = new HashMap<>();

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
                                    DUMMY_ATTACKER_USER_ID);
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
                                    test.getClassFile(), 0, 0, DUMMY_DEFENDER_USER_ID, test.getLineCoverage().getLinesCovered(),
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
                                        // This also update the DB
                                        mutantMap.get(uploadedMutant).kill();
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
                    response.sendRedirect(contextPath + "/games/user");
                } catch (Throwable e) {
                    logger.error("Unknown error during battleground creation.", e);
                    messages.add("Invalid Request");

                    response.setStatus(400);
                    Redirect.redirectBack(request, response);
                }
                break;
            case "leaveGame":
                int gameId;
                try {
                    gameId = Integer.parseInt(request.getParameter("game"));
                } catch (NumberFormatException e) {
                    response.setStatus(400);
                    Redirect.redirectBack(request, response);
                    return;
                }

                MultiplayerGame game = DatabaseAccess.getMultiplayerGame(gameId);
                if (game.removePlayer(uid)) {
                    messages.add("Game " + gameId + " left");
                    DatabaseAccess.removePlayerEventsForGame(gameId, uid);
                    final EventType notifType = EventType.GAME_PLAYER_LEFT;
                    final String message = "You successfully left the game.";
                    final EventStatus eventStatus = EventStatus.NEW;
                    final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    Event notif = new Event(-1, gameId, uid, message, notifType, eventStatus, timestamp);
                    notif.insert();
                } else {
                    messages.add("An error occured while leaving game " + gameId);
                }
                // Redirect to the game selection menu.
                response.sendRedirect(contextPath + "/games/user");
                break;
            default:
                logger.info("Action not recognised: {}", action);
                Redirect.redirectBack(request, response);
                break;
        }
    }
}
