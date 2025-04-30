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

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.ClassroomRole;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.server.game.GameJoinedEvent;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.service.ClassroomService;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.servlets.util.ServletUtils;

@WebServlet("/llm-api/battleground/join")
public class JoinGameAPI extends APIServlet {
    @Inject
    protected GameService gameService;

    @Inject
    protected GameRepository gameRepo;

    @Inject
    protected GameClassRepository gameClassRepo;

    @Inject
    protected ClassroomService classroomService;

    @Inject
    protected GameProducer gameProducer;

    @Inject
    protected CodeDefendersAuth login;

    @Inject
    private INotificationService notificationService;

    @Override
    protected void doPost(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        final var gameId = ServletUtils.getIntParameter(request, "gameId");
        final var role = ServletUtils.getStringParameter(request, "role")
                    .map(String::toUpperCase)
                    .map(Role::valueOrNull);

        if (gameId.isEmpty()) {
            writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    new Common.ErrorResponseDTO("Parameter 'gameId' missing."));
            return;
        }
        if (role.isEmpty()) {
            writeResponse(response, HttpServletResponse.SC_BAD_REQUEST,
                    new Common.ErrorResponseDTO("Parameter 'role' missing."));
            return;
        }

        gameProducer.setGameId(gameId.get());
        final MultiplayerGame game = gameProducer.getMultiplayerGame();

        if (game == null) {
            writeResponse(response, HttpServletResponse.SC_NOT_FOUND,
                    new Common.ErrorResponseDTO("Game not found."));
            return;
        }

        if (game.getRole(login.getUserId()) != Role.NONE) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            writeResponse(response, HttpServletResponse.SC_FORBIDDEN,
                    new Common.ErrorResponseDTO("User is already a player in the game."));
            return;
        }

        if (role.get() == Role.OBSERVER) {
            boolean isClassroomModerator = game.getClassroomId()
                    .flatMap(id -> classroomService.getMemberForClassroomAndUser(id, login.getUserId()))
                    .map(member -> member.getRole() == ClassroomRole.MODERATOR || member.getRole() == ClassroomRole.OWNER)
                    .orElse(false);
            if (!login.isAdmin() && !isClassroomModerator) {
                writeResponse(response, HttpServletResponse.SC_FORBIDDEN,
                        new Common.ErrorResponseDTO("Could not join the game as Observer. "
                            + "Only admins and classroom moderators/owners can do that."));
            }
        }

        if (joinGame(game, role.get())) {
            writeResponse(response, HttpServletResponse.SC_OK,
                    new JoinGameResponseDTO(role.get()));
        } else {
            writeResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    new Common.ErrorResponseDTO("Could not join the game as " + role.get().getFormattedString()));
        }
    }

    private boolean joinGame(MultiplayerGame game, Role role) throws IOException {
        if (game.addPlayer(login.getUserId(), role)) {
            GameJoinedEvent gje = new GameJoinedEvent();
            gje.setGameId(game.getId());
            gje.setUserId(login.getUserId());
            gje.setUserName(login.getSimpleUser().getName());
            notificationService.post(gje);
            return true;
        }
        return false;
    }

    public record JoinGameResponseDTO(Role role) {}
}
