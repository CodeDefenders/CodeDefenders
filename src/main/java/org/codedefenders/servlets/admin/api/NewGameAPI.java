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
package org.codedefenders.servlets.admin.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.admin.AdminCreateGamesBean;
import org.codedefenders.beans.admin.StagedGameList;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.MeleeGameDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.dto.api.NewGameRequest;
import org.codedefenders.dto.api.Team;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.model.UserEntity;
import org.codedefenders.persistence.database.SettingsRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.util.APITransformers;
import org.codedefenders.servlets.util.APIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 * This {@link HttpServlet} offers an API for {@link Test tests}.
 *
 * <p>A {@code GET} request with the {@code testId} parameter results in a JSON string containing
 * test information, including the source code.
 *
 * <p>Serves on path: {@code /api/test}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
@WebServlet("/admin/api/game")
public class NewGameAPI extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(NewGameAPI.class);
    @Inject
    CodeDefendersAuth login;
    @Inject
    GameService gameService;
    @Inject
    SettingsRepository settingsRepository;
    @Inject
    UserRepository userRepository;
    @Inject
    UserService userService;
    @Inject
    AdminCreateGamesBean adminCreateGamesBean;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final NewGameRequest game;
        try {
            game = (NewGameRequest) APIUtils.parsePostOrRespondJsonError(request, response, NewGameRequest.class);
        } catch (JsonParseException e) {
            return;
        }
        if (GameClassDAO.getClassForId(game.getClassId()) == null) {
            APIUtils.respondJsonError(response, "Class with ID " + game.getClassId() + " not found", HttpServletResponse.SC_NOT_FOUND);
        } else if (game.getTeams().size() != 2) {
            APIUtils.respondJsonError(response, game.getTeams().size() + " teams specified, expected 2", HttpServletResponse.SC_BAD_REQUEST);
        } else if (game.getTeams().stream().anyMatch(t -> t.getUserIds().isEmpty())) {
            APIUtils.respondJsonError(response, "Teams must not be empty", HttpServletResponse.SC_BAD_REQUEST);
        } else if (game.getSettings().getGameType().equals(StagedGameList.GameSettings.GameType.MELEE) &&
                !game.getTeams().stream().allMatch(t -> t.getRole().equals(Role.PLAYER))) {
            APIUtils.respondJsonError(response, "All teams must have the player role for melee match", HttpServletResponse.SC_BAD_REQUEST);
        } else if (game.getSettings().getGameType().equals(StagedGameList.GameSettings.GameType.MULTIPLAYER) &&
                (game.getTeams().stream().noneMatch(t -> t.getRole().equals(org.codedefenders.game.Role.ATTACKER)) ||
                        game.getTeams().stream().noneMatch(t -> t.getRole().equals(org.codedefenders.game.Role.DEFENDER)))) {
            APIUtils.respondJsonError(response, "Need one attacker team and one defender team for battleground match", HttpServletResponse.SC_BAD_REQUEST);
        } else if (game.getTeams().get(0).getUserIds().stream().anyMatch(game.getTeams().get(1).getUserIds()::contains)) {
            APIUtils.respondJsonError(response, "Teams must be disjointed", HttpServletResponse.SC_BAD_REQUEST);
        } else {
            synchronized (adminCreateGamesBean.getSynchronizer()) {
                adminCreateGamesBean.update();
                adminCreateGamesBean.stageEmptyGames(APITransformers.NewGameRequestToGameSettings(game), 1);
                Map<Integer, StagedGameList.StagedGame> stagedGames = adminCreateGamesBean.getStagedGameList().getStagedGames();
                StagedGameList.StagedGame stagedGame = stagedGames.get(new ArrayList<>(stagedGames.keySet()).get(stagedGames.size() - 1));
                try {
                    for (Team t : game.getTeams()) {
                        for (Integer userId : t.getUserIds()) {
                            Optional<UserEntity> user = userRepository.getUserById(userId);
                            if (user.isPresent()) {
                                adminCreateGamesBean.addPlayerToStagedGame(stagedGame, user.get(), t.getRole());
                            } else {
                                throw new NoSuchElementException("User with ID " + userId + " not found");
                            }
                        }
                    }
                    Supplier<List<? extends AbstractGame>> availableGames;
                    if (game.getSettings().getGameType().equals(StagedGameList.GameSettings.GameType.MELEE)) {
                        availableGames = MeleeGameDAO::getAvailableMeleeGames;
                    } else {
                        availableGames = MultiplayerGameDAO::getAvailableMultiplayerGames;
                    }
                    List<Integer> oldGameIds = availableGames.get().stream().map(AbstractGame::getId).collect(Collectors.toList());
                    adminCreateGamesBean.insertStagedGame(stagedGame, game.getReturnUrl());
                    List<Integer> newGameIds = availableGames.get().stream().map(AbstractGame::getId).collect(Collectors.toList());
                    List<Integer> newGameId = newGameIds.stream().filter(id -> !oldGameIds.contains(id)).collect(Collectors.toList());
                    if (newGameId.size() == 1) {
                        response.setContentType("application/json");
                        PrintWriter out = response.getWriter();
                        out.print(new Gson().toJson(Collections.singletonMap("gameId", newGameId.get(0))));
                        out.flush();
                    } else {
                        APIUtils.respondJsonError(response, "Expected to create 1 game, created " + newGameId.size());
                    }
                } catch (NoSuchElementException e) {
                    APIUtils.respondJsonError(response, e.getMessage(), HttpServletResponse.SC_NOT_FOUND);
                }
            }
        }
    }
}
