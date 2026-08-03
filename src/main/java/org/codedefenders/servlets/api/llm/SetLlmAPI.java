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
package org.codedefenders.servlets.api.llm;

import java.io.IOException;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.llm.LlmOrganizer;
import org.codedefenders.model.llm.LlModel;
import org.codedefenders.model.llm.LlmDefaultStrategies;
import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.model.llm.LlmType;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.servlets.util.ServletUtils;

import com.google.gson.Gson;


@WebServlet("/llm-api/battleground/set-llm")
public class SetLlmAPI extends APIServlet {

    @Inject
    GameProducer gameProducer;

    @Inject
    LlmOrganizer llmService;

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final Optional<Integer> gameId = ServletUtils.getIntParameter(request, "gameId");
        final Optional<String> modelName = ServletUtils.getStringParameter(request, "modelName");
        final Optional<LlmType> modelType = ServletUtils.getEnumParameter(request, LlmType.class, "modelType");
        final Optional<Role> role = ServletUtils.getStringParameter(request, "role")
                .map(String::toUpperCase)
                .map(Role::valueOrNull);
        final Optional<Action> action = ServletUtils.getEnumParameter(request, Action.class, "action");
        final LlmStrategy strategy = ServletUtils.getEnumParameter(request, LlmDefaultStrategies.class, "strategy")
                        .map(LlmStrategy::of).orElse(null);//TODO unschön
        if (gameId.isEmpty() || modelName.isEmpty() || modelType.isEmpty() || action.isEmpty() || role.isEmpty()
                || strategy == null) {
            if (gameId.isEmpty()) {
                writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        new Common.ErrorResponseDTO("Parameter 'gameId' missing."));
            } else if (modelName.isEmpty()) {
                writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        new Common.ErrorResponseDTO("Parameter 'modelName' missing."));
            } else if (modelType.isEmpty()) {
                writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        new Common.ErrorResponseDTO("Parameter 'modelType' missing."));
            } else if (action.isEmpty()) {
                writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        new Common.ErrorResponseDTO("Parameter 'action' missing or invalid."));
            } else if (role.isEmpty()) {
                writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        new Common.ErrorResponseDTO("Parameter 'role' is missing or invalid"));
            } else {
                writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                        new Common.ErrorResponseDTO("Parameter 'strategy' is missing or invalid"));
            }
            return;
        }
        gameProducer.setGameId(gameId.get());
        final MultiplayerGame game = gameProducer.getMultiplayerGame();
        if (game == null) {
            writeResponse(response, HttpServletResponse.SC_NOT_FOUND,
                    new Common.ErrorResponseDTO("Game not found."));
            return;
        }
        LlModel model = new LlModel(modelName.get(), modelType.get());
        //TODO test if this really works
        if (action.get() == Action.START) {
            if (role.get() == Role.DEFENDER) {
                llmService.setPlayerModel(game, role.get(), model, null, strategy, null);
            } else if (role.get() == Role.ATTACKER) {
                llmService.setPlayerModel(game, role.get(), null, model, null, strategy);
            } else {
                writeResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Melee games are not supported.");
            }
        } else {
            llmService.finishPlayer(gameId.get(), role.get());
        }
        //model.setActive(action.get() == Action.START);

        //llmRepo.setActive(model.getName(), model.getType(), model.isActive());
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final Optional<Integer> gameId = ServletUtils.getIntParameter(request, "gameId");
        if (gameId.isEmpty()) {
            writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    new Common.ErrorResponseDTO("Parameter 'gameId' missing."));
            return;
        }
        boolean active = llmService.getModelForGame(gameId.get(), Role.DEFENDER).isPresent()
                || llmService.getModelForGame(gameId.get(), Role.ATTACKER).isPresent();
        String attackerError = llmService.getErrorMessage(gameId.get(), Role.ATTACKER).orElse("");
        String defenderError = llmService.getErrorMessage(gameId.get(), Role.DEFENDER).orElse("");

        Gson gson = new Gson();
        CheckStatusDTO dto = new CheckStatusDTO(active, attackerError, defenderError);
        String json = gson.toJson(dto);
        writeResponse(response, HttpServletResponse.SC_OK, json);


    }

    private record CheckStatusDTO(boolean active, String attackerError, String defenderError) {
    }

    private enum Action {
        START, STOP
    }


}
