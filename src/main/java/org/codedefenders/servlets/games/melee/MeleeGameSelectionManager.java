/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.EventDAO;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.ClassroomMember;
import org.codedefenders.model.ClassroomRole;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.server.game.GameJoinedEvent;
import org.codedefenders.notification.events.server.game.GameLeftEvent;
import org.codedefenders.notification.events.server.game.GameStartedEvent;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.ClassroomService;
import org.codedefenders.service.game.MeleeGameService;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.game.GameState.ACTIVE;
import static org.codedefenders.servlets.util.ServletUtils.formType;
import static org.codedefenders.servlets.util.ServletUtils.getFloatParameter;
import static org.codedefenders.servlets.util.ServletUtils.getIntParameter;
import static org.codedefenders.servlets.util.ServletUtils.getStringParameter;
import static org.codedefenders.servlets.util.ServletUtils.parameterThenOrOther;

/**
 * This {@link HttpServlet} handles selection of {@link MeleeGame games}.
 *
 * <p>{@code GET} requests redirect to the game overview page and {@code POST}
 * requests handle creating, joining and entering {@link MultiplayerGame
 * battleground games}.
 *
 * <p>Serves under {@code /melee/games}.
 *
 * @see org.codedefenders.util.Paths#MELEE_SELECTION
 */
@WebServlet(Paths.MELEE_SELECTION)
public class MeleeGameSelectionManager extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(MeleeGameSelectionManager.class);

    @Inject
    private MessagesBean messages;

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private INotificationService notificationService;

    @Inject
    private EventDAO eventDAO;

    @Inject
    private UserRepository userRepo;

    @Inject
    private GameManagingUtils gameManagingUtils;

    @Inject
    private GameProducer gameProducer;

    @Inject
    private MeleeGameService gameService;

    @Inject
    private URLUtils url;

    @Inject
    private ClassroomService classroomService;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final String action = formType(request);

        if (!action.equals("createGame")) {
            MeleeGame game = gameProducer.getMeleeGame();
            if (game == null) {
                logger.error("No game or wrong type of game found. Aborting request.");
                Redirect.redirectBack(request, response);
                return;
            }
        }

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
            case "rematch":
                rematch(request, response);
                return;
            case "durationChange":
                changeDuration(request, response);
                return;
            default:
                logger.info("Action not recognised: {}", action);
                Redirect.redirectBack(request, response);
                break;
        }
    }

    private void createGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int classId;
        int maxAssertionsPerTest;
        int automaticEquivalenceTrigger;
        CodeValidatorLevel mutantValidatorLevel;
        Role selectedRole;
        int duration;

        try {
            classId = getIntParameter(request, "class").get();
            maxAssertionsPerTest = getIntParameter(request, "maxAssertionsPerTest").get();
            automaticEquivalenceTrigger = getIntParameter(request, "automaticEquivalenceTrigger").get();
            mutantValidatorLevel = getStringParameter(request, "mutantValidatorLevel")
                    .map(CodeValidatorLevel::valueOrNull).get();
            // If we select "player in the UI this should not result in a null value
            selectedRole = getStringParameter(request, "roleSelection").map(Role::valueOrNull).orElse(Role.NONE);
            duration = getIntParameter(request, "gameDurationMinutes").get();
        } catch (NoSuchElementException e) {
            logger.error("At least one request parameter was missing or was no valid integer value.", e);
            Redirect.redirectBack(request, response);
            return;
        }

        GameLevel level = GameLevel.valueOf(getStringParameter(request, "level").orElse(GameLevel.HARD.name()));
        float lineCoverage = getFloatParameter(request, "line_cov").orElse(1.1f);
        float mutantCoverage = getFloatParameter(request, "mutant_cov").orElse(1.1f);
        boolean chatEnabled = parameterThenOrOther(request, "chatEnabled", true, false);
        boolean capturePlayersIntention = parameterThenOrOther(request, "capturePlayersIntention", true, false);

        MeleeGame nGame = new MeleeGame.Builder(classId, login.getUserId(), maxAssertionsPerTest)
                .level(level)
                .chatEnabled(chatEnabled)
                .capturePlayersIntention(capturePlayersIntention)
                .lineCoverage(lineCoverage)
                .mutantCoverage(mutantCoverage)
                .mutantValidatorLevel(mutantValidatorLevel)
                .automaticMutantEquivalenceThreshold(automaticEquivalenceTrigger)
                .gameDurationMinutes(duration)
                .build();

        boolean withTests = parameterThenOrOther(request, "withTests", true, false);
        boolean withMutants = parameterThenOrOther(request, "withMutants", true, false);

        gameService.createGame(nGame, withMutants, withTests, selectedRole);

        // Redirect to admin interface
        if (request.getParameter("fromAdmin").equals("true")) {
            response.sendRedirect(url.forPath("/admin"));
            return;
        }

        // Redirect to the game selection menu.
        response.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
    }

    private void joinGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final boolean canJoinGames = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.GAME_JOINING)
                .getBoolValue();
        if (!canJoinGames) {
            logger.warn("User {} tried to join a melee game, but joining games is not permitted.", login.getUserId());
            Redirect.redirectBack(request, response);
            return;
        }

        MeleeGame game = gameProducer.getMeleeGame();
        int gameId = game.getId();

        if (game.hasUserJoined(login.getUserId())) {
            logger.info("User {} already in the requested game.", login.getUserId());
            response.sendRedirect(url.forPath(Paths.MELEE_GAME) + "?gameId=" + gameId);
            return;
        }

        boolean observerParamExists = ServletUtils.parameterThenOrOther(request, "observer", true, false);

        // Create the event, publish if successfully joined
        GameJoinedEvent gje = new GameJoinedEvent();
        gje.setGameId(game.getId());
        gje.setUserId(login.getUserId());
        gje.setUserName(login.getSimpleUser().getName());

        if (observerParamExists) {
            if (login.isAdmin() || isClassroomModeratorForGame(game)) {
                if (game.addPlayer(login.getUserId(), Role.OBSERVER)) {
                    logger.info("User {} joined game {} as observer.", login.getUserId(), gameId);
                    notificationService.post(gje);
                    response.sendRedirect(url.forPath(Paths.MELEE_GAME) + "?gameId=" + gameId);
                } else {
                    logger.info("User {} failed to join game {} as observer.", login.getUserId(), gameId);
                    response.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
                }
            } else {
                logger.info("User {} tried to join game {} as an observer, but is not an admin.", login.getUserId(),
                        gameId);
                response.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
            }
        } else {
            if (game.addPlayer(login.getUserId(), Role.PLAYER)) {
                logger.info("User {} joined game {} as player.", login.getUserId(), gameId);
                notificationService.post(gje);
                response.sendRedirect(url.forPath(Paths.MELEE_GAME) + "?gameId=" + gameId);
            } else {
                logger.info("User {} failed to join game {}.", login.getUserId(), gameId);
                response.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
            }
        }
    }

    /**
     * Checks if the user is a moderator or owner of the classroom the game is associated with.
     *
     * @param game The game to check for.
     * @return True if the user is a moderator or higher, false otherwise.
     */
    private boolean isClassroomModeratorForGame(MeleeGame game) {
        return game.getClassroomId()
                .map(classroomService::getMembersForClassroom)
                .orElse(List.of())
                .stream()
                .filter(member -> member.getUserId() == login.getUserId())
                .map(ClassroomMember::getRole)
                .anyMatch(role -> role == ClassroomRole.MODERATOR || role == ClassroomRole.OWNER);
    }

    private void leaveGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        MeleeGame game = gameProducer.getMeleeGame();

        if (game == null || !game.hasUserJoined(login.getUserId())) {
            messages.add("You cannot leave a game you are not part of.");
            Redirect.redirectBack(request, response);
            return;
        }

        int gameId = game.getId();

        final boolean removalSuccess = game.removePlayer(login.getUserId());
        if (!removalSuccess) {
            messages.add("An error occurred while leaving game " + gameId);
            response.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
            return;
        }

        messages.add("You left game " + gameId);
        eventDAO.removePlayerEventsForGame(gameId, login.getUserId());

        final EventType notifType = EventType.GAME_PLAYER_LEFT;
        final String message = "You successfully left the game.";
        final EventStatus eventStatus = EventStatus.NEW;
        final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Event notif = new Event(-1, gameId, login.getUserId(), message, notifType, eventStatus, timestamp);
        eventDAO.insert(notif);

        logger.info("User {} successfully left game {}", login.getUserId(), gameId);

        /*
         * Publish the event about the user
         */
        GameLeftEvent gle = new GameLeftEvent();
        gle.setGameId(game.getId());
        gle.setUserId(login.getUserId());
        gle.setUserName(login.getSimpleUser().getName());

        notificationService.post(gle);

        response.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
    }

    private void startGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        MeleeGame game = gameProducer.getMeleeGame();

        if (game.getCreatorId() != login.getUserId() && game.getRole(login.getUserId()) != Role.OBSERVER) {
            messages.add("Only the game's creator or an observer can start the game.");
            Redirect.redirectBack(request, response);
            return;
        }

        int gameId = game.getId();

        if (game.getState() == GameState.CREATED) {
            logger.info("Starting melee game {} (Setting state to ACTIVE)", gameId);
            gameService.startGame(game);
        }

        /*
         * Publish the event about the user
         */
        GameStartedEvent gse = new GameStartedEvent();
        gse.setGameId(game.getId());
        notificationService.post(gse);

        response.sendRedirect(url.forPath(Paths.MELEE_GAME) + "?gameId=" + gameId);
    }

    private void endGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        MeleeGame game = gameProducer.getMeleeGame();

        if (game.getCreatorId() != login.getUserId() && game.getRole(login.getUserId()) != Role.OBSERVER) {
            messages.add("Only the game's creator or an observer can end the game.");
            Redirect.redirectBack(request, response);
            return;
        }

        int gameId = game.getId();

        if (game.getState() == GameState.ACTIVE) {
            gameService.closeGame(game);

            response.sendRedirect(url.forPath(Paths.MELEE_SELECTION));
        } else {
            // TODO Update this later !
            response.sendRedirect(url.forPath(Paths.MELEE_GAME) + "?gameId=" + gameId);
        }
    }

    private void rematch(HttpServletRequest request, HttpServletResponse response) throws IOException {
        MeleeGame oldGame = gameProducer.getMeleeGame();

        if (login.getUser().getId() != oldGame.getCreatorId() && oldGame.getRole(login.getUserId()) != Role.OBSERVER) {
            messages.add("Only the creator of this game can call a rematch.");
            Redirect.redirectBack(request, response);
            return;
        }

        Optional<MeleeGame> newGame = gameService.rematch(oldGame);
        if (!newGame.isPresent()) {
            Redirect.redirectBack(request, response);
            return;
        }

        response.sendRedirect(url.forPath(Paths.MELEE_GAME) + "?gameId=" + newGame.get().getId());
    }

    private void changeDuration(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final MeleeGame game = gameProducer.getMeleeGame();

        if (game == null || game.getState() == GameState.FINISHED) {
            messages.add("The game is already over. You cannot change its duration.");
            Redirect.redirectBack(request, response);
            return;
        }

        if (login.getUser().getId() != game.getCreatorId() && game.getRole(login.getUserId()) != Role.OBSERVER) {
            messages.add("Only the creator or an observer of this game can change its duration.");
            Redirect.redirectBack(request, response);
            return;
        }

        Optional<Integer> newDuration = getIntParameter(request, "newDuration");
        if (newDuration.isEmpty()) {
            logger.debug("No duration value supplied.");
            Redirect.redirectBack(request, response);
            return;
        }

        final int maxDuration = AdminDAO.getSystemSetting(
                AdminSystemSettings.SETTING_NAME.GAME_DURATION_MINUTES_MAX).getIntValue();
        final int minDuration = 0;
        final int remainingMinutes = newDuration.get();

        if (remainingMinutes < minDuration) {
            messages.add("The remaining time cannot be below " + minDuration + " minutes.");
            Redirect.redirectBack(request, response);
            return;
        }

        if (remainingMinutes > maxDuration) {
            messages.add("The new remaining duration must be at most " + maxDuration + " minutes.");
            Redirect.redirectBack(request, response);
            return;
        }

        int elapsedTimeMinutes = 0;
        if (game.getState() == ACTIVE) {
            final long startTime = game.getStartTimeUnixSeconds();
            final long now = Instant.now().getEpochSecond();
            elapsedTimeMinutes = (int) TimeUnit.SECONDS.toMinutes(now - startTime);
        }

        game.setGameDurationMinutes(remainingMinutes + elapsedTimeMinutes);
        game.update();
        Redirect.redirectBack(request, response);
    }
}
