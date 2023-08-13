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
package org.codedefenders.servlets.admin;

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
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.beans.creategames.AdminCreateGamesBean;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameType;
import org.codedefenders.game.Role;
import org.codedefenders.model.UserEntity;
import org.codedefenders.model.UserInfo;
import org.codedefenders.model.creategames.GameSettings;
import org.codedefenders.model.creategames.StagedGameList;
import org.codedefenders.model.creategames.StagedGameList.StagedGame;
import org.codedefenders.model.creategames.roleassignment.RoleAssignmentMethod;
import org.codedefenders.model.creategames.teamassignment.TeamAssignmentMethod;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.text.MessageFormat.format;
import static org.codedefenders.game.GameType.MELEE;
import static org.codedefenders.servlets.util.ServletUtils.getEnumParameter;
import static org.codedefenders.servlets.util.ServletUtils.getIntParameter;
import static org.codedefenders.servlets.util.ServletUtils.getStringParameter;

/**
 * Handles extraction and verification of parameters for the admin create games page.
 * @see AdminCreateGamesBean
 */
@WebServlet(urlPatterns = {Paths.ADMIN_PAGE, Paths.ADMIN_GAMES})
public class AdminCreateGames extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminCreateGames.class);

    @Inject
    private MessagesBean messages;

    @Inject
    private AdminCreateGamesBean adminCreateGamesBean;

    @Inject
    private URLUtils url;

    private StagedGameList stagedGameList;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        synchronized (adminCreateGamesBean.getSynchronizer()) {
            adminCreateGamesBean.update();
            stagedGameList = adminCreateGamesBean.getStagedGameList();

            request.getRequestDispatcher(Constants.ADMIN_GAMES_JSP).forward(request, response);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        synchronized (adminCreateGamesBean.getSynchronizer()) {
            adminCreateGamesBean.update();
            stagedGameList = adminCreateGamesBean.getStagedGameList();

            final Optional<String> action = getStringParameter(request, "formType");
            if (!action.isPresent()) {
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

            response.sendRedirect(url.forPath(Paths.ADMIN_GAMES));
        }
    }

    /**
     * Extract game settings from a request.
     * @param request The HTTP request.
     * @return The extracted game settings.
     */
    private GameSettings extractGameSettings(HttpServletRequest request) {
        GameSettings.Builder builder = new GameSettings.Builder();

        builder.setGameType(
                getEnumParameter(request, GameType.class, "gameType").get());

        builder.setClassId(
                getIntParameter(request, "cut").get());

        builder.setWithMutants(
                request.getParameter("withMutants") != null);

        builder.setWithTests(
                request.getParameter("withTests") != null);

        builder.setMaxAssertionsPerTest(
                getIntParameter(request, "maxAssertionsPerTest").get());

        builder.setMutantValidatorLevel(
                getEnumParameter(request, CodeValidatorLevel.class, "mutantValidatorLevel").get());

        builder.setCreatorRole(
                getEnumParameter(request, Role.class, "creatorRole").get());

        builder.setChatEnabled(
                request.getParameter("chatEnabled") != null);

        builder.setCaptureIntentions(
                request.getParameter("captureIntentions") != null);

        builder.setEquivalenceThreshold(
                getIntParameter(request, "automaticEquivalenceTrigger").get());

        builder.setLevel(
                getEnumParameter(request, GameLevel.class, "level").get());

        builder.setLevel(
                getEnumParameter(request, GameLevel.class, "level").get());

        builder.setStartGame(
                request.getParameter("startGames") != null);

        builder.setGameDurationMinutes(
                clampGameDuration(getIntParameter(request, "gameDurationMinutes").get()));

        return builder.build();
    }

    /**
     * Clamps the game duration to the valid range defined by the system settings.
     * If the value is out of bounds, an info message is returned.
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
        Optional<Set<UserInfo>> usersFromTable = adminCreateGamesBean.getUserInfosForIds(userIdsFromTable);
        if (!usersFromTable.isPresent()) {
            return Optional.empty();
        }

        /* Map given usernames/emails to users and validate that all exist. */
        Optional<Set<UserInfo>> usersFromTextArea = adminCreateGamesBean.getUserInfosForNamesAndEmails(userNames);
        if (!usersFromTextArea.isPresent()) {
            return Optional.empty();
        }

        players.addAll(usersFromTable.get());
        players.addAll(usersFromTextArea.get());
        return Optional.of(players);
    }

    /**
     * Extract and validate POST parameters and forward them to {@link AdminCreateGamesBean#stageGamesWithUsers(Set,
     * GameSettings, RoleAssignmentMethod, TeamAssignmentMethod, int, int, int) AdminCreateGamesBean#stageGames()}.
     * @param request The HTTP request.
     */
    private void stageGamesWithUsers(HttpServletRequest request) {
        GameSettings gameSettings = extractGameSettings(request);

        /* Extract game management settings settings. */
        RoleAssignmentMethod roleAssignmentMethod
                = RoleAssignmentMethod.valueOf(getStringParameter(request, "roleAssignmentMethod").get());
        TeamAssignmentMethod teamAssignmentMethod
                = TeamAssignmentMethod.valueOf(getStringParameter(request, "teamAssignmentMethod").get());
        int attackersPerGame = getIntParameter(request, "attackersPerGame").get();
        int defendersPerGame = getIntParameter(request, "defendersPerGame").get();
        int playersPerGame = getIntParameter(request, "playersPerGame").get();

        Optional<Set<UserInfo>> players = extractPlayers(request);
        if (!players.isPresent()) {
            return;
        }

        /* Validate that no users are already assigned to other staged games. */
        Set<Integer> assignedUsers = stagedGameList.getAssignedUsers();
        for (UserInfo user : players.get()) {
            if (assignedUsers.contains(user.getUser().getId())) {
                messages.add(format("ERROR: Cannot create staged games with user {0}. "
                        + "User is already assigned to a staged game.",
                        user.getUser().getId()));
                return;
            }
        }

        /* Validate team sizes. */
        if (gameSettings.getGameType() == MELEE) {
            if (playersPerGame < 0) {
                messages.add(format("Invalid team sizes. Players per game: {0}.", playersPerGame));
            }
            attackersPerGame = playersPerGame;
        } else {
            if (attackersPerGame < 0 || defendersPerGame < 0 || attackersPerGame + defendersPerGame == 0) {
                messages.add(format("Invalid team sizes. Attackers per game: {0}, defenders per game: {1}.",
                        attackersPerGame, defendersPerGame));
                return;
            }
        }

        adminCreateGamesBean.stageGamesWithUsers(players.get(), gameSettings, roleAssignmentMethod,
                teamAssignmentMethod, attackersPerGame, defendersPerGame);
    }

    /**
     * Extract and validate POST parameters and forward them to {@link AdminCreateGamesBean#stageEmptyGames(
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

        adminCreateGamesBean.stageEmptyGames(gameSettings, numGames);
    }

    /**
     * Extract and validate POST parameters and forward them to {@link AdminCreateGamesBean#deleteStagedGames(List)
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

        Map<Integer, StagedGame> existingStagedGames = stagedGameList.getStagedGames();

        /* Verify that all staged games exist. */
        if (!existingStagedGames.keySet().containsAll(stagedGameIds)) {
            messages.add("ERROR: Cannot delete staged games. Not all selected staged games exist.");
            return;
        }

        List<StagedGame> stagedGames = stagedGameIds.stream()
                .map(existingStagedGames::get)
                .collect(Collectors.toList());

        adminCreateGamesBean.deleteStagedGames(stagedGames);
    }

    /**
     * Extract and validate POST parameters and forward them to {@link AdminCreateGamesBean#createStagedGames(List)
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

        Map<Integer, StagedGame> existingStagedGames = stagedGameList.getStagedGames();

        /* Verify that all staged games exist. */
        if (!existingStagedGames.keySet().containsAll(stagedGameIds)) {
            messages.add("ERROR: Cannot create staged games. Not all selected staged games exist.");
            return;
        }

        List<StagedGame> stagedGames = stagedGameIds.stream()
                .map(existingStagedGames::get)
                .collect(Collectors.toList());

        adminCreateGamesBean.createStagedGames(stagedGames);
    }

    /**
     * Extract and validate POST parameters and forward them to {@link AdminCreateGamesBean#removePlayerFromStagedGame(
     * StagedGame, int) AdminCreateGamesBean#removePlayerFromStagedGame()}.
     * @param request The HTTP request.
     */
    private void removePlayerFromStagedGame(HttpServletRequest request) {
        int userId = getIntParameter(request, "userId").get();
        int gameId = getStringParameter(request, "gameId")
                .flatMap(StagedGameList::formattedToNumericGameId).get();

        StagedGame stagedGame = stagedGameList.getStagedGame(gameId);
        if (stagedGame == null) {
            messages.add(format("ERROR: Cannot remove user {0} from staged game {1}. Staged game does not exist.",
                    userId, StagedGameList.numericToFormattedGameId(gameId)));
            return;
        }

        adminCreateGamesBean.removePlayerFromStagedGame(stagedGame, userId);
    }

    /**
     * Extract and validate POST parameters and forward them to
     * {@link AdminCreateGamesBean#removeCreatorFromStagedGame(StagedGame)}
     * AdminCreateGamesBean#removeCreatorFromStagedGame()}.
     * @param request The HTTP request.
     */
    private void removeCreatorFromStagedGame(HttpServletRequest request) {
        int gameId = getStringParameter(request, "gameId")
                .flatMap(StagedGameList::formattedToNumericGameId).get();

        StagedGame stagedGame = stagedGameList.getStagedGame(gameId);
        if (stagedGame == null) {
            messages.add(format("ERROR: Cannot remove you from staged game {1}. Staged game does not exist.",
                    StagedGameList.numericToFormattedGameId(gameId)));
            return;
        }

        adminCreateGamesBean.removeCreatorFromStagedGame(stagedGame);
    }

    /**
     * Extract and validate POST parameters for {@link AdminCreateGamesBean#switchRole(StagedGame, UserEntity)
     * AdminCreateGamesBean#switchRole()}.
     * @param request The HTTP request.
     */
    private void switchRole(HttpServletRequest request) {
        int userId = getIntParameter(request, "userId").get();
        int gameId = getStringParameter(request, "gameId")
                .flatMap(StagedGameList::formattedToNumericGameId).get();

        UserInfo user = adminCreateGamesBean.getUserInfos().get(userId);
        if (user == null) {
            messages.add(format("ERROR: Cannot switch role of user {0}. User does not exist.", userId));
            return;
        }

        StagedGame stagedGame = stagedGameList.getStagedGame(gameId);
        if (stagedGame == null) {
            messages.add(format("ERROR: Cannot switch role of user {0} in staged game {1}. Staged game does not exist.",
                    userId, StagedGameList.numericToFormattedGameId(gameId)));
            return;
        }

        adminCreateGamesBean.switchRole(stagedGame, user.getUser());
    }

    /**
     * Extract and validate POST parameters for {@link AdminCreateGamesBean#switchCreatorRole(StagedGame)
     * AdminCreateGamesBean#switchCreatorRole()}.
     * @param request The HTTP request.
     */
    private void switchCreatorRole(HttpServletRequest request) {
        int gameId = getStringParameter(request, "gameId")
                .flatMap(StagedGameList::formattedToNumericGameId).get();

        StagedGame stagedGame = stagedGameList.getStagedGame(gameId);
        if (stagedGame == null) {
            messages.add(format("ERROR: Cannot switch creator role in staged game {1}. Staged game does not exist.",
                    StagedGameList.numericToFormattedGameId(gameId)));
            return;
        }

        adminCreateGamesBean.switchCreatorRole(stagedGame);
    }

    /**
     * Extract and validate POST parameters for {@link AdminCreateGamesBean#movePlayerBetweenStagedGames(StagedGame,
     * StagedGame, UserEntity, Role) AdminCreateGamesBean#movePlayerBetweenStagedGames()}.
     * @param request The HTTP request.
     */
    private void movePlayerBetweenStagedGames(HttpServletRequest request) {
        int userId = getIntParameter(request, "userId").get();
        int gameIdFrom = getStringParameter(request, "gameIdFrom")
                .flatMap(StagedGameList::formattedToNumericGameId).get();
        int gameIdTo = getStringParameter(request, "gameIdTo")
                .flatMap(StagedGameList::formattedToNumericGameId).get();
        Role role = getEnumParameter(request, Role.class, "role").get();

        StagedGame stagedGameFrom = stagedGameList.getStagedGame(gameIdFrom);
        if (stagedGameFrom == null) {
            messages.add(format("ERROR: Cannot move user {0} from staged game {1}. Staged game does not exist.",
                    userId, StagedGameList.numericToFormattedGameId(gameIdFrom)));
            return;
        }

        StagedGame stagedGameTo = stagedGameList.getStagedGame(gameIdTo);
        if (stagedGameTo == null) {
            messages.add(format("ERROR: Cannot move user {0} to staged game {1}. Staged game does not exist.",
                    userId, StagedGameList.numericToFormattedGameId(gameIdTo)));
            return;
        }

        UserInfo user = adminCreateGamesBean.getUserInfos().get(userId);
        if (user == null) {
            messages.add(format("ERROR: Cannot move user {0}. User does not exist.",
                    userId, gameIdTo));
            return;
        }

        adminCreateGamesBean.movePlayerBetweenStagedGames(stagedGameFrom, stagedGameTo, user.getUser(), role);
    }

    /**
     * Extract and validate POST parameters for {@link AdminCreateGamesBean#addPlayerToStagedGame(StagedGame, UserEntity,
     * Role) AdminCreateGamesBean#addPlayerToStagedGame()} or {@link AdminCreateGamesBean#addPlayerToExistingGame(
     * AbstractGame, UserEntity, Role) AdminCreateGamesBean#addPlayerToExistingGame()}.
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

        UserInfo user = adminCreateGamesBean.getUserInfos().get(userId);
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
            StagedGame stagedGame = stagedGameList.getStagedGame(gameId);
            if (stagedGame == null) {
                messages.add(format("ERROR: Cannot add user {0} to staged game {1}. Staged game does not exist.",
                        userId, StagedGameList.numericToFormattedGameId(gameId)));
                return;
            }
            adminCreateGamesBean.addPlayerToStagedGame(stagedGame, user.getUser(), role);
        } else {
            AbstractGame game = GameDAO.getGame(gameId);
            if (game == null) {
                messages.add(format("ERROR: Cannot add user {0} to existing game {1}. Game does not exist.",
                        userId, gameId));
                return;
            }
            adminCreateGamesBean.addPlayerToExistingGame(game, user.getUser(), role);
        }
    }
}
