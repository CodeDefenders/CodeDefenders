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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameRepository;
import org.codedefenders.database.MeleeGameRepository;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.database.MutantRepository;
import org.codedefenders.database.TestRepository;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.multiplayer.PlayerScore;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.service.game.GameService;
import org.codedefenders.service.game.MeleeGameService;
import org.codedefenders.service.game.MultiplayerGameService;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;

@WebServlet(Paths.ADMIN_MONITOR)
public class AdminMonitorGames extends HttpServlet {

    @Inject
    private MessagesBean messages;

    @Inject
    private EventDAO eventDAO;

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private UserRepository userRepo;

    @Inject
    private UserService userService;

    @Inject
    private GameService gameService;

    @Inject
    private MultiplayerGameService multiplayerGameService;

    @Inject
    private MeleeGameService meleeGameService;

    @Inject
    private URLUtils url;

    @Inject
    private TestRepository testRepo;

    @Inject
    private MutantRepository mutantRepo;

    @Inject
    private GameRepository gameRepo;

    @Inject
    private MeleeGameRepository meleeGameRepo;


    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        List<MultiplayerGame> multiplayerGames = MultiplayerGameDAO.getAvailableMultiplayerGames();

        Map<Integer, String> multiplayerGameCreatorNames = multiplayerGames.stream()
                .collect(Collectors.toMap(AbstractGame::getId,
                        game -> userService.getSimpleUserById(game.getCreatorId())
                                .map(SimpleUser::getName)
                                .orElse("Unknown user")));

        Map<Integer, List<List<String>>> multiplayerPlayersInfoForGame = multiplayerGames.stream()
                .map(AbstractGame::getId)
                .collect(Collectors.toMap(id -> id, AdminDAO::getPlayersInfo));

        Map<Integer, Integer> multiplayerUserIdForPlayerIds = multiplayerPlayersInfoForGame.values().stream()
                .flatMap(Collection::stream)
                .map(list -> list.get(0))
                .map(Integer::parseInt)
                .distinct()
                .collect(Collectors.toMap(pid -> pid, pid -> userRepo.getUserIdForPlayerId(pid).orElse(0)));

        request.setAttribute("multiplayerGames", multiplayerGames);
        request.setAttribute("multiplayerGameCreatorNames", multiplayerGameCreatorNames);
        request.setAttribute("multiplayerPlayersInfoForGame", multiplayerPlayersInfoForGame);
        request.setAttribute("multiplayerUserIdForPlayerIds", multiplayerUserIdForPlayerIds);

        List<MeleeGame> meleeGames = meleeGameRepo.getAvailableMeleeGames();
        Map<Integer, String> meleeGameCreatorNames = meleeGames.stream()
                .collect(Collectors.toMap(AbstractGame::getId,
                        game -> userService.getSimpleUserById(game.getCreatorId())
                                .map(SimpleUser::getName)
                                .orElse("Unknown user")));

        Map<Integer, List<List<String>>> meleePlayersInfoForGame = meleeGames.stream()
                .map(AbstractGame::getId)
                .collect(Collectors.toMap(id -> id, AdminDAO::getPlayersInfo));

        Map<Integer, Integer> meleeUserIdForPlayerIds = meleePlayersInfoForGame.values().stream()
                .flatMap(Collection::stream)
                .map(list -> list.get(0))
                .map(Integer::parseInt)
                .distinct()
                .collect(Collectors.toMap(pid -> pid, pid -> userRepo.getUserIdForPlayerId(pid).orElse(0)));

        request.setAttribute("meleeGames", meleeGames);
        request.setAttribute("meleeGameCreatorNames", meleeGameCreatorNames);
        request.setAttribute("meleePlayersInfoForGame", meleePlayersInfoForGame);
        request.setAttribute("meleeUserIdForPlayerIds", meleeUserIdForPlayerIds);

        request.getRequestDispatcher(Constants.ADMIN_MONITOR_JSP).forward(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        switch (request.getParameter("formType")) {

            case "startStopGame":
                startStopGame(request, response);
                break;
            default:
                System.err.println("Action not recognised");
                Redirect.redirectBack(request, response);
                break;
        }
    }


    private void startStopGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String playerToRemoveIdGameIdString = request.getParameter("activeGameUserRemoveButton");
        String playerToSwitchIdGameIdString = request.getParameter("activeGameUserSwitchButton");
        boolean switchUser = playerToSwitchIdGameIdString != null;
        AbstractGame game;
        if (playerToRemoveIdGameIdString != null || playerToSwitchIdGameIdString != null) { // admin is removing user from temp game
            int playerToRemoveId = Integer.parseInt((switchUser ? playerToSwitchIdGameIdString : playerToRemoveIdGameIdString).split("-")[0]);
            int gameToRemoveFromId = Integer.parseInt((switchUser ? playerToSwitchIdGameIdString : playerToRemoveIdGameIdString).split("-")[1]);
            Optional<Integer> userId = userRepo.getUserIdForPlayerId(playerToRemoveId);
            if (userId.isPresent() && !deletePlayer(playerToRemoveId, gameToRemoveFromId, userId.get())) {
                messages.add("Deleting player " + playerToRemoveId + " failed! \n Please check the logs!");
            } else if (switchUser && userId.isPresent()) {
                Role newRole = Role.valueOf(playerToSwitchIdGameIdString.split("-")[2]).equals(Role.ATTACKER)
                        ? Role.DEFENDER : Role.ATTACKER;
                game = gameRepo.getGame(gameToRemoveFromId);
                if (game != null) {
                    game.setEventDAO(eventDAO);
                    game.setUserRepository(userRepo);
                    if (!game.addPlayer(userId.get(), newRole)) {
                        messages.add("Changing role of user " + userId.get() + " failed! \n Please check the logs!");
                    } else {
                        messages.add("Successfully changed role of user " + userId.get() + ".");
                    }
                }
            }

        } else {  // admin is starting or stopping selected games

            String[] selectedGames = request.getParameterValues("selectedGames");
            String gameSelectedViaPlayButton = request.getParameter("start_stop_btn");
            String gameSelectedViaRematchButton = request.getParameter("rematch_btn");

            if (gameSelectedViaPlayButton != null) {
                // admin is starting or stopping a single game
                int gameId = -1;
                // Get the identifying information required to create a game from the submitted form.

                try {
                    gameId = Integer.parseInt(gameSelectedViaPlayButton);
                } catch (Exception e) {
                    messages.add("There was a problem with the form.");
                    response.sendRedirect(url.forPath("/admin"));
                    return;
                }

                startStopGame(gameId, null);

            } else if (gameSelectedViaRematchButton != null) {
                // rematch for a single game
                int gameId;
                try {
                    gameId = Integer.parseInt(gameSelectedViaRematchButton);
                } catch (Exception e) {
                    messages.add("There was a problem with the form.");
                    response.sendRedirect(url.forPath("/admin"));
                    return;
                }

                rematchGame(gameId);

            } else if (selectedGames != null) {
                GameState newState = request.getParameter("games_btn").equals("Start Games")
                        ? GameState.ACTIVE : GameState.FINISHED;
                for (String gameId : selectedGames) {
                    startStopGame(Integer.parseInt(gameId), newState);
                }
            }
        }
        response.sendRedirect(url.forPath(Paths.ADMIN_MONITOR));
    }

    private void startStopGame(int gameId, GameState pNewState) {
        AbstractGame game = gameRepo.getGame(gameId);
        boolean updated = false;

        if (game != null) {
            GameState invertedState = game.getState().equals(GameState.ACTIVE) ? GameState.FINISHED : GameState.ACTIVE;
            GameState newState = pNewState == null ? invertedState : pNewState;

            if (newState != invertedState) {
                // skip games that already have the desired state
                return;
            }

            if (newState == GameState.FINISHED) { // close game
                updated = gameService.closeGame(game);
            } else { // start game
                updated = gameService.startGame(game);
            }
        }

        if (!updated) {
            messages.add(String.format(
                    "ERROR trying to start or stop game %d.\nIf this problem persists, contact your administrator.",
                    gameId
            ));
        }
    }

    private void rematchGame(int gameId) {
        AbstractGame game = gameRepo.getGame(gameId);
        if (game instanceof MultiplayerGame) {
            multiplayerGameService.rematch((MultiplayerGame) game);
        } else if (game instanceof MeleeGame) {
            meleeGameService.rematch((MeleeGame) game);
        } else {
            messages.add("Couldn't create a rematch game.");
        }
    }

    private boolean deletePlayer(int playerId, int gameId, int userId) {
        for (Test t : testRepo.getTestsForGame(gameId)) {
            if (t.getPlayerId() == playerId) {
                AdminDAO.deleteTestTargetExecutions(t.getId());
            }
        }
        for (Mutant m : mutantRepo.getValidMutantsForGame(gameId)) {
            if (m.getPlayerId() == playerId) {
                AdminDAO.deleteMutantTargetExecutions(m.getId());
            }
        }
        eventDAO.removePlayerEventsForGame(gameId, userId);
        AdminDAO.deleteAttackerEquivalences(playerId);
        AdminDAO.deleteDefenderEquivalences(playerId);
        AdminDAO.deletePlayerTest(playerId);
        AdminDAO.deletePlayerMutants(playerId);
        return AdminDAO.deletePlayer(playerId);
    }

    public static int getPlayerScore(MultiplayerGame mg, int pid) {
        HashMap<Integer, PlayerScore> mutantScores = mg.getMutantScores();
        HashMap<Integer, PlayerScore> testScores = mg.getTestScores();
        if (mutantScores.containsKey(pid) && mutantScores.get(pid) != null) {
            return (mutantScores.get(pid)).getTotalScore();
        } else if (testScores.containsKey(pid) && testScores.get(pid) != null) {
            return (testScores.get(pid)).getTotalScore();
        }
        return 0;
    }

    public static int getPlayerScore(MeleeGame mg, int pid) {
        Map<Integer, PlayerScore> mutantScores = mg.getMutantScores();
        Map<Integer, PlayerScore> testScores = mg.getTestScores();
        if (mutantScores.containsKey(pid) && mutantScores.get(pid) != null) {
            return (mutantScores.get(pid)).getTotalScore();
        } else if (testScores.containsKey(pid) && testScores.get(pid) != null) {
            return (testScores.get(pid)).getTotalScore();
        }
        return 0;
    }
}
