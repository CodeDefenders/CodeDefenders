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
package org.codedefenders.servlets.creategames;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.beans.creategames.CreateGamesBean;
import org.codedefenders.beans.creategames.CreateGamesBean.UserInfo;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameType;
import org.codedefenders.game.Role;
import org.codedefenders.model.creategames.GameSettings;
import org.codedefenders.model.creategames.StagedGameList;
import org.codedefenders.model.creategames.StagedGameList.StagedGame;
import org.codedefenders.model.creategames.gameassignment.GameAssignmentStrategy;
import org.codedefenders.model.creategames.roleassignment.RoleAssignmentStrategy;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.text.MessageFormat.format;
import static org.codedefenders.game.GameType.MELEE;
import static org.codedefenders.servlets.util.ServletUtils.getEnumParameter;
import static org.codedefenders.servlets.util.ServletUtils.getIntParameter;
import static org.codedefenders.servlets.util.ServletUtils.getStringParameter;

public abstract class CreateGamesServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(CreateGamesServlet.class);

    @Inject
    private MessagesBean messages;

    /**
     * Returns the CreateGamesBean representing the context for the create-games page.
     */
    protected abstract CreateGamesBean getContext();

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        synchronized (getContext().getSynchronizer()) {
            final Optional<String> action = getStringParameter(request, "formType");
            if (action.isEmpty()) {
                logger.error("Missing parameter: formType.");
                Redirect.redirectBack(request, response);
                return;
            }

            try {
                switch (action.get()) {
                    case "stageGamesWithUsers":
                        stageGamesWithUsers(request);
                        break;
                    case "stageEmptyGames":
                        stageEmptyGames(request);
                        break;
                    case "deleteStagedGames":
                        deleteStagedGames(request);
                        break;
                    case "createStagedGames":
                        createStagedGames(request);
                        break;
                    case "removePlayerFromStagedGame":
                        removePlayerFromStagedGame(request);
                        break;
                    case "removeCreatorFromStagedGame":
                        removeCreatorFromStagedGame(request);
                        break;
                    case "switchRole":
                        switchRole(request);
                        break;
                    case "switchCreatorRole":
                        switchCreatorRole(request);
                        break;
                    case "movePlayerBetweenStagedGames":
                        movePlayerBetweenStagedGames(request);
                        break;
                    case "addPlayerToGame":
                        addPlayerToGame(request);
                        break;
                    default:
                        logger.error("Unknown form type: {}", action.get());
                        Redirect.redirectBack(request, response);
                        break;
                }
            } catch (NoSuchElementException e) {
                messages.add("ERROR: Missing parameter");
            } catch (IllegalArgumentException e) {
                messages.add("ERROR: Invalid parameter");
            }
        }
        Redirect.redirectBack(request, response);
    }

    /**
     * Extract game settings from a request.
     * @param request The HTTP request.
     * @return The extracted game settings.
     */
    private GameSettings extractGameSettings(HttpServletRequest request) {
        GameType gameType = getEnumParameter(request, GameType.class, "gameType").get();

        int classId = getIntParameter(request, "cut").get();

        boolean withMutants = request.getParameter("withMutants") != null;

        boolean withTests = request.getParameter("withTests") != null;

        int maxAssertionsPerTest = getIntParameter(request, "maxAssertionsPerTest").get();

        CodeValidatorLevel mutantValidatorLevel = getEnumParameter(
                request, CodeValidatorLevel.class, "mutantValidatorLevel").get();

        boolean chatEnabled = request.getParameter("chatEnabled") != null;

        boolean captureIntentions = request.getParameter("captureIntentions") != null;

        int equivalenceThreshold = getIntParameter(request, "automaticEquivalenceTrigger").get();

        GameLevel level = getEnumParameter(request, GameLevel.class, "level").get();

        Role creatorRole = getEnumParameter(request, Role.class, "creatorRole").get();

        int gameDurationMinutes = clampGameDuration(getIntParameter(request, "gameDurationMinutes").get());

        boolean startGame = request.getParameter("startGames") != null;

        Integer classroomId = request.getParameter("classroomId") != null
                ? getIntParameter(request, "classroomId").get()
                : null;

        return new GameSettings(
                gameType,
                classId,
                withMutants,
                withTests,
                maxAssertionsPerTest,
                mutantValidatorLevel,
                chatEnabled,
                captureIntentions,
                equivalenceThreshold,
                level,
                creatorRole,
                gameDurationMinutes,
                startGame,
                classroomId);
    }

    /**
     * Clamps the game duration to the valid range defined by the system settings.
     * If the value is out of bounds, an info message shown to the user.
     *
     * @param duration The game duration in minutes
     */
    private int clampGameDuration(int duration) {
        final int maxDuration = AdminDAO.getSystemSetting(
                AdminSystemSettings.SETTING_NAME.GAME_DURATION_MINUTES_MAX).getIntValue();
        final int minDuration = 1;

        if (duration > maxDuration) {
            messages.add(String.format(
                    "INFO: The max. allowed duration is %d minutes.",
                    maxDuration
            ));
            return maxDuration;
        } else if (duration < minDuration) {
            messages.add(String.format(
                    "INFO: The min. allowed duration is %d minutes.",
                    minDuration
            ));
            return minDuration;
        } else {
            return duration;
        }
    }

    /**
     * Extracts players to be assigned to staged games from a request.
     */
    private Optional<Set<UserInfo>> extractPlayers(HttpServletRequest request) {
        /* Extract user IDs from the table. */
        String userIdsStr = Optional.ofNullable(request.getParameter("userIds")).get();
        Set<Integer> userIdsFromTable = Arrays.stream(userIdsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::valueOf)
                    .collect(Collectors.toSet());

        /* Extract usernames/emails from the text field. */
        String userNamesStr = Optional.ofNullable(request.getParameter("userNames")).get();
        Set<String> userNames = Arrays.stream(userNamesStr.split("\\R"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());

        Set<UserInfo> players = new HashSet<>();

        /* Map given user IDs to users and validate that all exist. */
        Optional<Set<UserInfo>> usersFromTable = getContext().getUserInfosForIds(userIdsFromTable);
        if (usersFromTable.isEmpty()) {
            return Optional.empty();
        }

        /* Map given usernames/emails to users and validate that all exist. */
        Optional<Set<UserInfo>> usersFromTextArea = getContext().getUserInfosForNamesAndEmails(userNames);
        if (usersFromTextArea.isEmpty()) {
            return Optional.empty();
        }

        players.addAll(usersFromTable.get());
        players.addAll(usersFromTextArea.get());
        return Optional.of(players);
    }

    /**
     * Extract and validate POST parameters and forward them to {@link CreateGamesBean#stageGamesWithUsers(Set,
     * GameSettings, RoleAssignmentStrategy.Type, GameAssignmentStrategy.Type, int, int)}.
     * @param request The HTTP request.
     */
    private void stageGamesWithUsers(HttpServletRequest request) {
        GameSettings gameSettings = extractGameSettings(request);

        /* Extract game management settings settings. */
        RoleAssignmentStrategy.Type roleAssignmentType = getEnumParameter(request, RoleAssignmentStrategy.Type.class,
                "roleAssignmentMethod").get();
        GameAssignmentStrategy.Type gameAssignmentType = getEnumParameter(request, GameAssignmentStrategy.Type.class,
                "gameAssignmentMethod").get();
        int attackersPerGame = getIntParameter(request, "attackersPerGame").get();
        int defendersPerGame = getIntParameter(request, "defendersPerGame").get();
        int playersPerGame = getIntParameter(request, "playersPerGame").get();

        Optional<Set<UserInfo>> players = extractPlayers(request);
        if (players.isEmpty()) {
            return;
        }

        /* Validate that no users are already assigned to other staged games. */
        Set<Integer> assignedUsers = getContext().getStagedGames().getAssignedUsers();
        for (UserInfo user : players.get()) {
            if (assignedUsers.contains(user.getId())) {
                messages.add(format("ERROR: Cannot create staged games with user {0}. "
                        + "User is already assigned to a staged game.",
                        user.getName()));
                return;
            }
        }

        /* Validate team sizes. */
        if (gameSettings.getGameType() == MELEE) {
            if (playersPerGame <= 0) {
                messages.add(format("Invalid team sizes. Players per game: {0}.", playersPerGame));
            }
            attackersPerGame = playersPerGame;
        } else {
            if (attackersPerGame < 0 || defendersPerGame < 0 || (attackersPerGame == 0 && defendersPerGame == 0)) {
                messages.add(format("Invalid team sizes. Attackers per game: {0}, defenders per game: {1}.",
                        attackersPerGame, defendersPerGame));
                return;
            }
        }

        Set<Integer> userIds = players.get().stream()
                .map(UserInfo::getId)
                .collect(Collectors.toSet());
        getContext().stageGamesWithUsers(userIds, gameSettings, roleAssignmentType,
                gameAssignmentType, attackersPerGame, defendersPerGame);
    }

    /**
     * Extract and validate POST parameters and forward them to {@link CreateGamesBean#stageEmptyGames(
     * GameSettings, int) AdminCreateGamesBean#stageEmptyGames()}.
     * @param request The HTTP request.
     */
    private void stageEmptyGames(HttpServletRequest request) {
        int numGames = getIntParameter(request, "numGames").get();
        GameSettings gameSettings = extractGameSettings(request);

        if (numGames > 100) {
            messages.add("ERROR: Won't create more than 100 staged games.");
            return;
        }

        getContext().stageEmptyGames(gameSettings, numGames);
    }

    /**
     * Extract and validate POST parameters and forward them to {@link CreateGamesBean#deleteStagedGames(List)
     * AdminCreateGamesBean#deleteStagedGames()}.
     * @param request The HTTP request.
     */
    private void deleteStagedGames(HttpServletRequest request) {
        /* Convert game IDs to ints. */
        String stagedGameIdsStr = getStringParameter(request, "stagedGameIds").get();
        List<Integer> stagedGameIds = Arrays.stream(stagedGameIdsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(StagedGameList::formattedToNumericGameId)
                .map(Optional::get)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, StagedGame> existingStagedGames = getContext().getStagedGames().getMap();

        /* Verify that all staged games exist. */
        if (!existingStagedGames.keySet().containsAll(stagedGameIds)) {
            messages.add("ERROR: Cannot delete staged games. Not all selected staged games exist.");
            return;
        }

        List<StagedGame> stagedGames = stagedGameIds.stream()
                .map(existingStagedGames::get)
                .collect(Collectors.toList());

        getContext().deleteStagedGames(stagedGames);
    }

    /**
     * Extract and validate POST parameters and forward them to {@link CreateGamesBean#createStagedGames(List)
     * AdminCreateGamesBean#createStagedGames()}.
     * @param request The HTTP request.
     */
    private void createStagedGames(HttpServletRequest request) {
        /* Convert game IDs to ints. */
        String stagedGameIdsStr = getStringParameter(request, "stagedGameIds").get();
        List<Integer> stagedGameIds = Arrays.stream(stagedGameIdsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(StagedGameList::formattedToNumericGameId)
                .map(Optional::get)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, StagedGame> existingStagedGames = getContext().getStagedGames().getMap();

        /* Verify that all staged games exist. */
        if (!existingStagedGames.keySet().containsAll(stagedGameIds)) {
            messages.add("ERROR: Cannot create staged games. Not all selected staged games exist.");
            return;
        }

        List<StagedGame> stagedGames = stagedGameIds.stream()
                .map(existingStagedGames::get)
                .collect(Collectors.toList());

        getContext().createStagedGames(stagedGames);
    }

    /**
     * Extract and validate POST parameters and forward them to {@link CreateGamesBean#removePlayerFromStagedGame(
     * StagedGame, int) AdminCreateGamesBean#removePlayerFromStagedGame()}.
     * @param request The HTTP request.
     */
    private void removePlayerFromStagedGame(HttpServletRequest request) {
        int userId = getIntParameter(request, "userId").get();
        int gameId = getStringParameter(request, "gameId")
                .flatMap(StagedGameList::formattedToNumericGameId).get();

        StagedGame stagedGame = getContext().getStagedGame(gameId);
        if (stagedGame == null) {
            messages.add(format("ERROR: Cannot remove user {0} from staged game {1}. Staged game does not exist.",
                    userId, StagedGameList.numericToFormattedGameId(gameId)));
            return;
        }

        getContext().removePlayerFromStagedGame(stagedGame, userId);
    }

    /**
     * Extract and validate POST parameters and forward them to
     * {@link CreateGamesBean#removeCreatorFromStagedGame(StagedGame)}
     * AdminCreateGamesBean#removeCreatorFromStagedGame()}.
     * @param request The HTTP request.
     */
    private void removeCreatorFromStagedGame(HttpServletRequest request) {
        int gameId = getStringParameter(request, "gameId")
                .flatMap(StagedGameList::formattedToNumericGameId).get();

        StagedGame stagedGame = getContext().getStagedGame(gameId);
        if (stagedGame == null) {
            messages.add(format("ERROR: Cannot remove you from staged game {1}. Staged game does not exist.",
                    StagedGameList.numericToFormattedGameId(gameId)));
            return;
        }

        getContext().removeCreatorFromStagedGame(stagedGame);
    }

    /**
     * Extract and validate POST parameters for {@link CreateGamesBean#switchRole(StagedGame, int)}
     * @param request The HTTP request.
     */
    private void switchRole(HttpServletRequest request) {
        int userId = getIntParameter(request, "userId").get();
        int gameId = getStringParameter(request, "gameId")
                .flatMap(StagedGameList::formattedToNumericGameId).get();

        UserInfo user = getContext().getUserInfo(userId);
        if (user == null) {
            messages.add(format("ERROR: Cannot switch role of user {0}. Invalid user.", userId));
            return;
        }

        StagedGame stagedGame = getContext().getStagedGame(gameId);
        if (stagedGame == null) {
            messages.add(format("ERROR: Cannot switch role of user {0} in staged game {1}. Staged game does not exist.",
                    userId, StagedGameList.numericToFormattedGameId(gameId)));
            return;
        }

        getContext().switchRole(stagedGame, user.getId());
    }

    /**
     * Extract and validate POST parameters for {@link CreateGamesBean#switchCreatorRole(StagedGame)
     * AdminCreateGamesBean#switchCreatorRole()}.
     * @param request The HTTP request.
     */
    private void switchCreatorRole(HttpServletRequest request) {
        int gameId = getStringParameter(request, "gameId")
                .flatMap(StagedGameList::formattedToNumericGameId).get();

        StagedGame stagedGame = getContext().getStagedGame(gameId);
        if (stagedGame == null) {
            messages.add(format("ERROR: Cannot switch creator role in staged game {1}. Staged game does not exist.",
                    StagedGameList.numericToFormattedGameId(gameId)));
            return;
        }

        getContext().switchCreatorRole(stagedGame);
    }

    /**
     * Extract and validate POST parameters for {@link CreateGamesBean#movePlayerBetweenStagedGames(StagedGame,
     * StagedGame, int, Role)}
     * @param request The HTTP request.
     */
    private void movePlayerBetweenStagedGames(HttpServletRequest request) {
        int userId = getIntParameter(request, "userId").get();
        int gameIdFrom = getStringParameter(request, "gameIdFrom")
                .flatMap(StagedGameList::formattedToNumericGameId).get();
        int gameIdTo = getStringParameter(request, "gameIdTo")
                .flatMap(StagedGameList::formattedToNumericGameId).get();
        Role role = getEnumParameter(request, Role.class, "role").get();

        StagedGame stagedGameFrom = getContext().getStagedGame(gameIdFrom);
        if (stagedGameFrom == null) {
            messages.add(format("ERROR: Cannot move user {0} from staged game {1}. Staged game does not exist.",
                    userId, StagedGameList.numericToFormattedGameId(gameIdFrom)));
            return;
        }

        StagedGame stagedGameTo = getContext().getStagedGame(gameIdTo);
        if (stagedGameTo == null) {
            messages.add(format("ERROR: Cannot move user {0} to staged game {1}. Staged game does not exist.",
                    userId, StagedGameList.numericToFormattedGameId(gameIdTo)));
            return;
        }

        UserInfo user = getContext().getUserInfo(userId);
        if (user == null) {
            messages.add(format("ERROR: Cannot move user {0}. User does not exist.",
                    userId, gameIdTo));
            return;
        }

        getContext().movePlayerBetweenStagedGames(stagedGameFrom, stagedGameTo, user.getId(), role);
    }

    /**
     * Extract and validate POST parameters for {@link CreateGamesBean#addPlayerToStagedGame(StagedGame, int, Role)}
     * or {@link CreateGamesBean#addPlayerToExistingGame(AbstractGame, int, Role)}
     * @param request The HTTP request.
     */
    private void addPlayerToGame(HttpServletRequest request) {
        String gameIdStr = getStringParameter(request, "gameId").get();
        Optional<Integer> stagedGameId = StagedGameList.formattedToNumericGameId(gameIdStr);

        int gameId;
        boolean isStagedGame;
        if (stagedGameId.isPresent()) {
            isStagedGame = true;
            gameId = stagedGameId.get();
        } else {
            isStagedGame = false;
            gameId = Integer.parseInt(gameIdStr);
        }

        int userId = getIntParameter(request, "userId").get();
        Role role = getEnumParameter(request, Role.class, "role").get();

        UserInfo user = getContext().getUserInfo(userId);
        if (user == null) {
            if (isStagedGame) {
                messages.add(format("ERROR: Cannot add user {0} to staged game {1}. User does not exist.",
                        userId, StagedGameList.numericToFormattedGameId(gameId)));
            } else {
                messages.add(format("ERROR: Cannot add user {0} to existing game {1}. User does not exist.",
                        userId, gameId));
            }
            return;
        }

        if (isStagedGame) {
            StagedGame stagedGame = getContext().getStagedGame(gameId);
            if (stagedGame == null) {
                messages.add(format("ERROR: Cannot add user {0} to staged game {1}. Staged game does not exist.",
                        userId, StagedGameList.numericToFormattedGameId(gameId)));
                return;
            }
            getContext().addPlayerToStagedGame(stagedGame, user.getId(), role);
        } else {
            if (!getContext().getAvailableMultiplayerGames().contains(gameId)
                    && !getContext().getAvailableMeleeGames().contains(gameId)) {
                messages.add(format(
                        "ERROR: Cannot add user {0} to existing game {1}. Game is not available for this action.",
                        userId, gameId));
                return;
            }

            AbstractGame game = GameDAO.getGame(gameId);
            if (game == null) {
                messages.add(format("ERROR: Cannot add user {0} to existing game {1}. Game does not exist.",
                        userId, gameId));
                return;
            }
            getContext().addPlayerToExistingGame(game, user.getId(), role);
        }
    }
}
