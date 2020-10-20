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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.beans.admin.AdminCreateGamesBean;
import org.codedefenders.beans.admin.AdminCreateGamesBean.GameSettings;
import org.codedefenders.beans.admin.AdminCreateGamesBean.StagedGame;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.beans.user.LoginBean;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.User;
import org.codedefenders.model.UserInfo;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.text.MessageFormat.format;
import static org.codedefenders.beans.admin.AdminCreateGamesBean.GameSettings.GameType.MELEE;
import static org.codedefenders.beans.admin.AdminCreateGamesBean.GameSettings.GameType.MULTIPLAYER;
import static org.codedefenders.servlets.admin.AdminCreateGames.RoleAssignmentMethod.RANDOM;
import static org.codedefenders.servlets.util.ServletUtils.getIntParameter;
import static org.codedefenders.util.Constants.DUMMY_ATTACKER_USER_ID;
import static org.codedefenders.util.Constants.DUMMY_DEFENDER_USER_ID;

@WebServlet(urlPatterns = {Paths.ADMIN_PAGE, Paths.ADMIN_GAMES})
public class AdminCreateGames extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminCreateGames.class);

    @Inject
    private MessagesBean messages;

    @Inject
    private LoginBean login;

    @Inject
    private EventDAO eventDAO;

    @Inject
    private GameManagingUtils gameManagingUtils;

    @Inject
    private AdminCreateGamesBean adminCreateGamesBean;

    public enum RoleAssignmentMethod {
        RANDOM,
        OPPOSITE
    }

    public enum TeamAssignmentMethod {
        RANDOM,
        SCORE_DESCENDING
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        adminCreateGamesBean.updateUserInfos();

        request.setAttribute("adminCreateGamesBean", adminCreateGamesBean);
        request.getRequestDispatcher(Constants.ADMIN_GAMES_JSP).forward(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        adminCreateGamesBean.updateUserInfos();

        final String action = request.getParameter("formType");
        switch (action) {
            case "stageGames":
                stageGames(request);
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
            case "movePlayerBetweenStagedGames":
                movePlayerBetweenStagedGames(request);
                break;
            case "addPlayerToGame":
                addPlayerToGame(request);
                break;
            default:
                logger.error("Action not recognised: {}", action);
                Redirect.redirectBack(request, response);
                break;
        }

        response.sendRedirect(Constants.ADMIN_GAMES_JSP);
    }

    /**
     * Extract and validate POST parameters for
     * {@link AdminCreateGames#stageGames(Set, GameSettings, RoleAssignmentMethod, TeamAssignmentMethod, int, int)}.
     * @param request The HTTP request.
     */
    private void stageGames(HttpServletRequest request) {
        /* Extract user IDs from the table. */
        String userIdsStr = request.getParameter("userIds");
        Set<Integer> userIdsFromTable;
        try {
            userIdsFromTable = Arrays.stream(userIdsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::valueOf)
                    .collect(Collectors.toSet());
        } catch (NullPointerException e) {
            messages.add("ERROR: Missing parameter: userIds.");
            return;
        } catch (NumberFormatException e) {
            messages.add("ERROR: Invalid parameter: userIds.");
            return;
        }

        /* Extract user names/emails from the text field. */
        String userNamesStr = request.getParameter("userNames");
        Set<String> userNames;
        try {
            userNames = Arrays.stream(userNamesStr.split("\\R"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        } catch (NullPointerException e) {
            messages.add("ERROR: Missing parameter: userNames.");
            return;
        } catch (NumberFormatException e) {
            messages.add("ERROR: Invalid parameter: userNames.");
            return;
        }

        /* Extract game settings. */
        GameSettings gameSettings = new GameSettings();
        try {
            gameSettings.setGameType(GameSettings.GameType.valueOf(request.getParameter("gameType")));
            gameSettings.setCut(GameClassDAO.getClassForId(getIntParameter(request, "cut").get()));
            gameSettings.setWithMutants(request.getParameter("withMutants") != null);
            gameSettings.setWithTests(request.getParameter("withTests") != null);

            gameSettings.setMaxAssertionsPerTest(getIntParameter(request, "maxAssertionsPerTest").get());
            gameSettings.setMutantValidatorLevel(CodeValidatorLevel.valueOf(request.getParameter("mutantValidatorLevel")));
            gameSettings.setChatEnabled(request.getParameter("chatEnabled") != null);
            gameSettings.setCaptureIntentions(request.getParameter("captureIntentions") != null);
            gameSettings.setEquivalenceThreshold(getIntParameter(request, "automaticEquivalenceTrigger").get());
            gameSettings.setLevel(GameLevel.valueOf(request.getParameter("level")));

            gameSettings.setStartGame(request.getAttribute("startGame") != null);
        } catch (NullPointerException | NoSuchElementException e) {
            messages.add("ERROR: Missing game settings parameter.");
            return;
        } catch (IllegalArgumentException e) {
            messages.add("ERROR: Invalid game settings parameter.");
            return;
        }

        /* Extract game management settings settings. */
        RoleAssignmentMethod roleAssignmentMethod;
        TeamAssignmentMethod teamAssignmentMethod;
        int attackersPerGame;
        int defendersPerGame;
        try {
            roleAssignmentMethod = RoleAssignmentMethod.valueOf(request.getParameter("roleAssignmentMethod"));
            teamAssignmentMethod = TeamAssignmentMethod.valueOf(request.getParameter("teamAssignmentMethod"));
            attackersPerGame = getIntParameter(request, "attackers").get();
            defendersPerGame = getIntParameter(request, "defenders").get();
        } catch (NullPointerException | NoSuchElementException e) {
            messages.add("ERROR: Missing game management settings parameter.");
            return;
        } catch (IllegalArgumentException e) {
            messages.add("ERROR: Invalid game management settings parameter.");
            return;
        }

        /* Validate that all given user IDs exist. */
        if (!validateUserIds(userIdsFromTable)) {
            return;
        }

        /* Map given user names/emails to user IDs and validate that all exist. */
        Optional<Set<Integer>> userIdsFromTextarea = getUserIdsForNamesAndEmails(userNames);
        if (!userIdsFromTextarea.isPresent()) {
            return;
        }

        /* Map user IDs to user infos.*/
        Set<Integer> userIds = new HashSet<>();
        userIds.addAll(userIdsFromTable);
        userIds.addAll(userIdsFromTextarea.get());
        Set<UserInfo> users = userIds.stream()
                .map(adminCreateGamesBean.getUserInfos()::get)
                .collect(Collectors.toSet());

        /* Abort if no users are provided. */
        if (users.isEmpty()) {
            messages.add("Please select at least one user.");
            return;
        }

        /* Validate that no users are already assigned to other staged games. */
        Set<Integer> assignedUsers = adminCreateGamesBean.getAssignedUsers();
        for (UserInfo user : users) {
            if (assignedUsers.contains(user.getUser().getId())) {
                messages.add(format(
                        "Cannot create staged games with user {0}. User is already assigned to a staged game.",
                        user.getUser().getId()));
                return;
            }
        }

        stageGames(users, gameSettings, roleAssignmentMethod,
                teamAssignmentMethod, attackersPerGame, defendersPerGame);
    }

    /**
     * Assigns selected users to teams and adds staged games with these teams to the list.
     * See {@link AdminCreateGames#assignRoles(Collection, RoleAssignmentMethod, int, int, Collection, Collection)
     * assignRoles} and {@link AdminCreateGames#splitIntoTeams(Collection, int, TeamAssignmentMethod) splitIntoTeams}
     * for more information on how the roles and teams are assigned.
     *
     * @param users The players for the staged games.
     * @param gameSettings The game settings.
     * @param roleAssignmentMethod The method of assigning roles to users.
     * @param teamAssignmentMethod The method of assigning users to teams.
     * @param attackersPerGame The number of attackers per game.
     * @param defendersPerGame The number of defenders per game.
     */
    private void stageGames(Set<UserInfo> users, GameSettings gameSettings,
                            RoleAssignmentMethod roleAssignmentMethod, TeamAssignmentMethod teamAssignmentMethod,
                            int attackersPerGame, int defendersPerGame) {
        int numGames = users.size() / (attackersPerGame + defendersPerGame);

        /* Split users into attackers and defenders. */
        List<UserInfo> attackers = new ArrayList<>();
        List<UserInfo> defenders = new ArrayList<>();
        assignRoles(users, roleAssignmentMethod, attackersPerGame, defendersPerGame, attackers, defenders);

        /* Assign attackers and defenders to teams. */
        List<List<UserInfo>> attackerTeams = splitIntoTeams(attackers, numGames, teamAssignmentMethod);
        List<List<UserInfo>> defenderTeams = splitIntoTeams(defenders, numGames, teamAssignmentMethod);

        for (int i = 0; i < numGames; i++) {
            StagedGame stagedGame = adminCreateGamesBean.addStagedGame(gameSettings);
            List<UserInfo> attackerTeam = attackerTeams.get(i);
            List<UserInfo> defenderTeam = defenderTeams.get(i);
            for (UserInfo user : attackerTeam) {
                stagedGame.addAttacker(user.getUser().getId());
            }
            for (UserInfo user : defenderTeam) {
                stagedGame.addDefender(user.getUser().getId());
            }
        }

        messages.add(format("Staged {0} games.", numGames));
    }

    /**
     * Extract and validate POST parameters for
     * {@link AdminCreateGames#deleteStagedGames(List)}.
     * @param request The HTTP request.
     */
    private void deleteStagedGames(HttpServletRequest request) {
        /* Convert game IDs to ints. */
        String stagedGameIdsStr = request.getParameter("stagedGameIds");
        List<Integer> stagedGameIds;
        try {
            stagedGameIds = Arrays.stream(stagedGameIdsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::valueOf)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (NullPointerException e) {
            messages.add("ERROR: Missing parameter: stagedGameIds.");
            return;
        } catch (NumberFormatException e) {
            messages.add("ERROR: Invalid parameter: stagedGameIds.");
            return;
        }

        Map<Integer, StagedGame> existingStagedGames = adminCreateGamesBean.getStagedGames();

        /* Verify that all staged games exist. */
        if (!existingStagedGames.keySet().containsAll(stagedGameIds)) {
            messages.add("Cannot delete staged games. Not all selected staged games exist.");
            return;
        }

        List<StagedGame> stagedGames = stagedGameIds.stream()
                .map(existingStagedGames::get)
                .collect(Collectors.toList());

        deleteStagedGames(stagedGames);
    }

    /**
     * Deletes the given staged games from the list.
     * @param stagedGames The staged games to delete.
     */
    private void deleteStagedGames(List<StagedGame> stagedGames) {
        for (StagedGame stagedGame : stagedGames) {
            adminCreateGamesBean.removeStagedGame(stagedGame.getId());
        }

        messages.add(format("Deleted {0} games.", stagedGames.size()));
    }

    /**
     * Extract and validate POST parameters for
     * {@link AdminCreateGames#createStagedGames(List)}.
     * @param request The HTTP request.
     */
    private void createStagedGames(HttpServletRequest request) {
        /* Convert game IDs to ints. */
        String stagedGameIdsStr = request.getParameter("stagedGameIds");
        List<Integer> stagedGameIds;
        try {
            stagedGameIds = Arrays.stream(stagedGameIdsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::valueOf)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (NullPointerException e) {
            messages.add("ERROR: Missing parameter: stagedGameIds.");
            return;
        } catch (NumberFormatException e) {
            messages.add("ERROR: Invalid parameter: stagedGameIds.");
            return;
        }

        Map<Integer, StagedGame> existingStagedGames = adminCreateGamesBean.getStagedGames();

        /* Verify that all staged games exist. */
        if (!existingStagedGames.keySet().containsAll(stagedGameIds)) {
            messages.add("Cannot create staged games. Not all selected staged games exist.");
            return;
        }

        List<StagedGame> stagedGames = stagedGameIds.stream()
                .map(existingStagedGames::get)
                .collect(Collectors.toList());

        createStagedGames(stagedGames);
    }

    private void createStagedGames(List<StagedGame> stagedGames) {
        for (StagedGame stagedGame : stagedGames) {
            if (insertStagedGame(stagedGame)) {
                adminCreateGamesBean.removeStagedGame(stagedGame.getId());
            }
        }

        messages.add(format("Created {0} games.", stagedGames.size()));
    }

    /**
     * Extract and validate POST parameters for
     * {@link AdminCreateGames#removePlayerFromStagedGame(StagedGame, int)}.
     * @param request The HTTP request.
     */
    private void removePlayerFromStagedGame(HttpServletRequest request) {
        int userId;
        int gameId;
        try {
            userId = getIntParameter(request, "userId").get();
            gameId = getIntParameter(request, "gameId").get();
        } catch (NullPointerException e) {
            messages.add("ERROR: Missing parameter.");
            return;
        } catch (IllegalArgumentException e) {
            messages.add("ERROR: Invalid parameter.");
            return;
        }

        StagedGame stagedGame = adminCreateGamesBean.getStagedGame(gameId);
        if (stagedGame == null) {
            messages.add(format("ERROR: Cannot remove user {0} from staged game T{1}. Staged game does not exist.",
                    userId, gameId));
            return;
        }

        removePlayerFromStagedGame(stagedGame, userId);
    }

    /**
     * Extract and validate POST parameters for
     * {@link AdminCreateGames#movePlayerBetweenStagedGames(StagedGame, StagedGame, User, Role)}.
     * @param request The HTTP request.
     */
    private void movePlayerBetweenStagedGames(HttpServletRequest request) {
        int userId;
        int gameIdFrom;
        int gameIdTo;
        Role role;
        try {
            userId = getIntParameter(request, "userId").get();
            gameIdFrom = getIntParameter(request, "gameIdFrom").get();
            gameIdTo = getIntParameter(request, "gameIdTo").get();
            role = Role.valueOf(request.getParameter("role"));
        } catch (NullPointerException e) {
            messages.add("ERROR: Missing parameter.");
            return;
        } catch (IllegalArgumentException e) {
            messages.add("ERROR: Invalid parameter.");
            return;
        }

        StagedGame stagedGameFrom = adminCreateGamesBean.getStagedGame(gameIdFrom);
        if (stagedGameFrom == null) {
            messages.add(format("ERROR: Cannot move user {0} from staged game T{1}. Staged game does not exist.",
                    userId, gameIdFrom));
            return;
        }

        StagedGame stagedGameTo = adminCreateGamesBean.getStagedGame(gameIdTo);
        if (stagedGameTo == null) {
            messages.add(format("ERROR: Cannot move user {0} to staged game T{1}. Staged game does not exist.",
                    userId, gameIdTo));
            return;
        }

        UserInfo user = adminCreateGamesBean.getUserInfos().get(userId);
        if (user == null) {
            messages.add(format("ERROR: Cannot move user {0}. User does not exist.",
                    userId, gameIdTo));
            return;
        }

        movePlayerBetweenStagedGames(stagedGameFrom, stagedGameTo, user.getUser(), role);
    }

    private void movePlayerBetweenStagedGames(StagedGame stagedGameFrom, StagedGame stagedGameTo,
                                              User user, Role role) {
        if (removePlayerFromStagedGame(stagedGameFrom, user.getId())) {
            addPlayerToStagedGame(stagedGameTo, user, role);
        }
    }

    /**
     * Extract and validate POST parameters for
     * {@link AdminCreateGames#addPlayerToStagedGame(StagedGame, User, Role)} or
     * {@link AdminCreateGames#addPlayerToExistingGame(AbstractGame, User, Role)}.
     * @param request The HTTP request.
     */
    private void addPlayerToGame(HttpServletRequest request) {
        int userId;
        int gameId;
        Role role;
        boolean isStagedGame = false;
        try {
            userId = getIntParameter(request, "userId").get();
            String gameIdStr = request.getParameter("gameId");
            if (gameIdStr.startsWith("T")) {
                isStagedGame = true;
                gameIdStr = gameIdStr.substring(1);
            }
            gameId = Integer.parseInt(gameIdStr);
            role = Role.valueOf(request.getParameter("role"));
        } catch (NullPointerException e) {
            messages.add("ERROR: Missing parameter.");
            return;
        } catch (IllegalArgumentException e) {
            messages.add("ERROR: Invalid parameter.");
            return;
        }

        UserInfo user = adminCreateGamesBean.getUserInfos().get(userId);
        if (user == null) {
            if (isStagedGame) {
                messages.add(format("ERROR: Cannot add user {0} to staged game T{1}. User does not exist.",
                        userId, gameId));
            } else {
                messages.add(format("ERROR: Cannot add user {0} to existing game {1}. User does not exist.",
                        userId, gameId));
            }
            return;
        }

        if (isStagedGame) {
            StagedGame stagedGame = adminCreateGamesBean.getStagedGame(gameId);
            if (stagedGame == null) {
                messages.add(format("ERROR: Cannot add user {0} to staged game T{1}. Staged game does not exist.",
                        userId, gameId));
                return;
            }

            addPlayerToStagedGame(stagedGame, user.getUser(), role);
        } else {
            AbstractGame game = GameDAO.getGame(gameId);
            if (game == null) {
                messages.add(format("ERROR: Cannot add user {0} to existing game {1}. Game does not exist.",
                        userId, gameId));
                return;
            }

            addPlayerToExistingGame(game, user.getUser(), role);
        }
    }

    // =================================================================================================================

    private boolean validateUserIds(Collection<Integer> userIds) {
        Set<Integer> validUserIds = new HashSet<>(adminCreateGamesBean.getUserInfos().keySet());
        boolean success = true;

        for (int userId : userIds) {
            if (!validUserIds.contains(userId)) {
                messages.add(format("ERROR: No valid user with the ID {0} exists.", userId));
                success = false;
            }
        }

        return success;
    }

    private Optional<Set<Integer>> getUserIdsForNamesAndEmails(Collection<String> userNames) {
        Map<String, Integer> userIdByName = adminCreateGamesBean.getUserInfos().values().stream()
                .collect(Collectors.toMap(
                        userInfo -> userInfo.getUser().getUsername(),
                        userInfo -> userInfo.getUser().getId()));

        Map<String, Integer> userIdByEmail = adminCreateGamesBean.getUserInfos().values().stream()
                .collect(Collectors.toMap(
                        userInfo -> userInfo.getUser().getEmail(),
                        userInfo -> userInfo.getUser().getId()));

        boolean success = true;
        Set<Integer> userIds = new HashSet<>();

        for (String userNameOrEmail : userNames) {
            Integer userId = userIdByName.get(userNameOrEmail);
            if (userId != null) {
                userIds.add(userId);
                continue;
            }
            userId = userIdByEmail.get(userNameOrEmail);
            if (userId != null) {
                userIds.add(userId);
                continue;
            }
            messages.add(format("ERROR: No valid user with name/email {0} exists.", userNameOrEmail));
            success = false;
        }

        return success ? Optional.of(userIds) : Optional.empty();
    }

    private void assignRoles(Collection<UserInfo> userInfos, RoleAssignmentMethod method,
                             int attackersPerGame, int defendersPerGame,
                             Collection<UserInfo> attackers, Collection<UserInfo> defenders) {

        assert Stream.of(userInfos, attackers, defenders).flatMap(Collection::stream).distinct().count()
                == userInfos.size() + attackers.size() + defenders.size()
                : "User collections must be distinct and disjoint.";

        switch (method) {
            case RANDOM:
                /* Calculate the number of attackers to assign, while taking into account how users have previously been
                 * distributed. (This method can be called with non-empty attackers and defenders sets containing
                 * already assigned users.) */
                int numUsers = userInfos.size() + attackers.size() + defenders.size();
                int numAttackers = ((numUsers * attackersPerGame) + 1) / (attackersPerGame + defendersPerGame);
                int remainingNumAttackers = Math.max(0, numAttackers - attackers.size());

                List<UserInfo> shuffledUsers = new ArrayList<>(userInfos);
                Collections.shuffle(shuffledUsers);
                for (int i = 0; i < remainingNumAttackers; i++) {
                    attackers.add(shuffledUsers.get(i));
                }
                for (int i = remainingNumAttackers; i < shuffledUsers.size(); i++) {
                    defenders.add(shuffledUsers.get(i));
                }

                break;

            case OPPOSITE:
                List<UserInfo> remainingUsers = new ArrayList<>();

                for (UserInfo userInfo : userInfos) {
                    if (userInfo.getLastRole() == Role.ATTACKER) {
                        attackers.add(userInfo);
                    } else if (userInfo.getLastRole() == Role.DEFENDER) {
                        defenders.add(userInfo);
                    } else {
                        remainingUsers.add(userInfo);
                    }
                }

                /* Randomly assign remaining users (that were neither attacker or defender in their last game). */
                assignRoles(remainingUsers, RANDOM, attackersPerGame, defendersPerGame, attackers, defenders);

                break;

            default:
                throw new IllegalArgumentException(format("Unknown role assignment method: {0}.", method));
        }
    }

    private List<List<UserInfo>> splitIntoTeams(Collection<UserInfo> users, int numTeams, TeamAssignmentMethod method) {
        List<UserInfo> usersList = new ArrayList<>(users);

        switch (method) {
            case RANDOM:
                Collections.shuffle(usersList);
                break;
            case SCORE_DESCENDING:
                usersList.sort(Comparator.comparingInt(UserInfo::getTotalScore).reversed());
                break;
            default:
                throw new IllegalArgumentException(format("Unknown team assignment method: {0}.", method));
        }

        int numUsersPerTeam = usersList.size() / numTeams;
        int numRemainingUsers = usersList.size() % numTeams;

        List<List<UserInfo>> teams = new ArrayList<>();

        int index = 0;
        for (int i = 0; i < numTeams; i++) {
            List<UserInfo> subList;
            if (i < numRemainingUsers) {
                subList = usersList.subList(index, index + numUsersPerTeam + 1);
                index += numUsersPerTeam + 1;
            } else {
                subList = usersList.subList(index, index + numUsersPerTeam);
                index += numUsersPerTeam;
            }
            teams.add(new ArrayList<>(subList));
        }

        return teams;
    }

    private boolean insertStagedGame(StagedGame stagedGame) {
        GameSettings gameSettings = stagedGame.getGameSettings();

        /* Create the game. */
        AbstractGame game;
        if (gameSettings.getGameType() == MULTIPLAYER) {
            game = new MultiplayerGame.Builder(gameSettings.getCut().getId(),
                    login.getUserId(),
                    gameSettings.getMaxAssertionsPerTest())
                    .cut(gameSettings.getCut())
                    .mutantValidatorLevel(gameSettings.getMutantValidatorLevel())
                    .chatEnabled(gameSettings.isChatEnabled())
                    .capturePlayersIntention(gameSettings.isCaptureIntentions())
                    .automaticMutantEquivalenceThreshold(gameSettings.getEquivalenceThreshold())
                    .level(gameSettings.getLevel())
                    .build();
        } else if (gameSettings.getGameType() == MELEE) {
            game = new MeleeGame.Builder(gameSettings.getCut().getId(),
                    login.getUserId(),
                    gameSettings.getMaxAssertionsPerTest())
                    .cut(gameSettings.getCut())
                    .mutantValidatorLevel(gameSettings.getMutantValidatorLevel())
                    .chatEnabled(gameSettings.isChatEnabled())
                    .capturePlayersIntention(gameSettings.isCaptureIntentions())
                    .automaticMutantEquivalenceThreshold(gameSettings.getEquivalenceThreshold())
                    .level(gameSettings.getLevel())
                    .build();
        } else {
            messages.add(format("ERROR: Could not create staged game T{0}. Invalid game type: {1}.",
                    stagedGame.getId(), gameSettings.getGameType().getName()));
            return false;
        }

        /* Insert the game. */
        game.setEventDAO(eventDAO);
        if (!game.insert()) {
            messages.add(format("ERROR: Could not create staged game T{0}. Could not insert into the database.",
                    stagedGame.getId()));
            return false;
        }

        /* Add system users and predefined mutants/tests. */
        if (!game.addPlayer(DUMMY_ATTACKER_USER_ID, Role.ATTACKER)
                || !game.addPlayer(DUMMY_DEFENDER_USER_ID, Role.DEFENDER)) {
            messages.add(format("ERROR: Could not add system players to game T{0}.", stagedGame.getId()));
            return false;
        }
        if (gameSettings.isWithMutants() || gameSettings.isWithTests()) {
            gameManagingUtils.addPredefinedMutantsAndTests(game,
                    gameSettings.isWithMutants(), gameSettings.isWithTests());
        }

        Map<Integer, UserInfo> userInfos = adminCreateGamesBean.getUserInfos();

        /* Add users to the game. */
        if (gameSettings.getGameType() == MULTIPLAYER) {
            for (int userId : stagedGame.getAttackers()) {
                UserInfo user = userInfos.get(userId);
                if (user != null) {
                    addPlayerToExistingGame(game, user.getUser(), Role.ATTACKER);
                } else {
                    messages.add(format("ERROR: Cannot add user {0} to existing game {1} as {2}. User does not exist.",
                            userId, game.getId(), Role.ATTACKER.getFormattedString()));
                }
            }
            for (int userId : stagedGame.getDefenders()) {
                UserInfo user = userInfos.get(userId);
                if (user != null) {
                    addPlayerToExistingGame(game, user.getUser(), Role.DEFENDER);
                } else {
                    messages.add(format("ERROR: Cannot add user {0} to existing game {1} as {2}. User does not exist.",
                            userId, game.getId(), Role.DEFENDER.getFormattedString()));
                }
            }
        } else if (gameSettings.getGameType() == MELEE) {
            for (int userId : stagedGame.getDefenders()) {
                UserInfo user = userInfos.get(userId);
                if (user != null) {
                    addPlayerToExistingGame(game, user.getUser(), Role.PLAYER);
                } else {
                    messages.add(format("ERROR: Cannot add user {0} to existing game {1} as {2}. User does not exist.",
                            userId, game.getId(), Role.PLAYER.getFormattedString()));
                }
            }
        }

        /* Start game if configured to. */
        if (gameSettings.isStartGame()) {
            game.setState(GameState.ACTIVE);
            game.update();
        }

        return true;
    }

    private boolean removePlayerFromStagedGame(StagedGame stagedGame, int userId) {
        if (stagedGame.removePlayer(userId)) {
            messages.add(format(
                    "Removed user {0} from staged game T{1}.",
                    userId, stagedGame.getId())
            );
            return true;
        } else {
            messages.add(format(
                    "ERROR: Cannot remove user {0} from staged game T{1}. "
                            + "User is not assigned to the the staged game.",
                    userId, stagedGame.getId()));
            return false;
        }
    }

    private boolean addPlayerToStagedGame(StagedGame stagedGame, User user, Role role) {
        boolean success;
        switch (role) {
            case PLAYER:
            case ATTACKER:
                success = stagedGame.addAttacker(user.getId());
                break;
            case DEFENDER:
                success = stagedGame.addDefender(user.getId());
                break;
            default:
                messages.add(format("Cannot add player with role {0}. Invalid role.", role));
                return false;
        }

        if (success) {
            messages.add(format("Added user {0} to staged game T{1} as {2}.",
                    user.getId(), stagedGame.getId(), role.getFormattedString()));
        } else {
            messages.add(format("ERROR: Cannot add user {0} to staged game T{1}. "
                    + "User is already assigned to a different staged game.",
                    user.getId(), stagedGame.getId()));
        }

        return success;
    }

    private boolean addPlayerToExistingGame(AbstractGame game, User user, Role role) {
        if (!game.addPlayer(user.getId(), role)) {
            messages.add(format("ERROR: Cannot add user {0} to existing game {1} as {2}.",
                    user.getId(), game.getId(), role));
            return false;
        }
        messages.add(format("Added user {0} to existing game {1} as {2}.", user.getId(), game.getId(), role));
        return true;
    }
}
