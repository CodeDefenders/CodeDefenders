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
package org.codedefenders.servlets;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.server.game.GameJoinedEvent;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.PlayerRepository;
import org.codedefenders.persistence.database.WhitelistRepository;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@WebServlet({Paths.INVITE})
public class InvitePage extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(InvitePage.class);

    @Inject
    private MessagesBean messages;

    @Inject
    private PlayerRepository playerRepo;

    @Inject
    private GameRepository gameRepository;

    @Inject
    private URLUtils url;

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private INotificationService notificationService;

    @Inject
    private WhitelistRepository whitelistRepo;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        logger.debug("Invite page requested");
        AbstractGame game;

        String inviteId = req.getParameter("inviteId");
        if (inviteId == null) {
            logger.warn("No invite ID in invite link.");
            messages.add("Your link is malformed, you could not join the game.").alert();
            return;
        } else {
            try {
                game = gameRepository.getGameForInviteId(Integer.parseInt(inviteId));
            } catch (NumberFormatException e) {
                logger.warn("Invalid invite ID: {}", inviteId);
                messages.add("Your link is malformed, you could not join the game.").alert();
                return;
            }
        }

        String roleParameter = req.getParameter("role");
        Role wantedRole;
        if (roleParameter != null) {
            if (roleParameter.equals("attacker")) {
                wantedRole = Role.ATTACKER;
            } else if (roleParameter.equals("defender")) {
                wantedRole = Role.DEFENDER;
            } else {
                logger.warn("Invalid role parameter in invite link: {}", roleParameter);
                messages.add("Your invite link was malformed: Your role may not be " + roleParameter)
                        .alert();
                resp.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
                return;
            }
        } else {
            wantedRole = null; // No role specified
        }

        if (game == null) {
            logger.warn("User {} tried to join game with invite link {}, but the game does not exist.",
                    login.getUserId(), req.getParameter("inviteId"));
            messages.add("The game you were invited to no longer exists, or it has not been created yet.").alert();
            resp.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
            return;
        }
        if (game.isFinished()) {
            logger.warn("User {} tried to join game {}, but it is already finished.", login.getUserId(), game.getId());
            messages.add("The game you were invited to has already finished!").alert();
        }
        int gameId = game.getId();
        int userId = login.getUserId();

        int playerId = playerRepo.getPlayerIdForUserAndGame(userId, gameId);

        if (playerId == -1 && game.getCreatorId() != login.getUserId()) {
            if (!AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.GAME_JOINING).getBoolValue()) {
                logger.warn("User {} tried to join game {}, but game joining is disabled.", userId, gameId);
                messages.add("Joining games is disabled.").alert();
            }

            GameJoinedEvent event = new GameJoinedEvent();
            event.setUserId(userId);
            event.setGameId(gameId);
            event.setUserName(login.getSimpleUser().getName());

            boolean success;
            if (game instanceof MultiplayerGame multiplayerGame) {
                success = multiplayerGame.addPlayer(userId, wantedRole);
            } else if (game instanceof MeleeGame) {
                success = game.addPlayer(userId, Role.PLAYER);
            } else {
                logger.warn("User {} tried to join puzzle game {}.", userId, gameId);
                messages.add("You cannot join puzzle games with an invite link.").alert();
                resp.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
                return;
            }
            if (success) {
                notificationService.post(event);
                logger.info("User {} joined game {}.", userId, gameId);
            } else {
                if (game.isInviteOnly() && !whitelistRepo.isWhitelisted(gameId, userId)) {
                    logger.info("User {} tried to join the game {} he was not whitelisted for" +
                            " with in invite link.", userId, gameId);
                    messages.add("You could not join the game because you have not been added to the whitelist.")
                            .alert();
                    resp.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
                    return;
                } else {
                    logger.warn("User {} tried to join game {}, but was rejected for an unknown reason.",
                            userId, gameId);
                    messages.add("You could not join the game.").alert();
                    resp.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
                    return;
                }
            }
        } else {
            logger.warn("User {} tried to join game {}, but is already in the game.", userId, gameId);
            messages.add("You had already joined this game.");
        }
        String redirectPath;
        if (game instanceof MultiplayerGame) {
            redirectPath = Paths.BATTLEGROUND_GAME;
        } else if (game instanceof MeleeGame) {
            redirectPath = Paths.MELEE_GAME;
        } else {
            throw new IllegalStateException("Game type not supported: " + game.getClass().getName());
        }
        resp.sendRedirect(url.forPath(redirectPath) + "?gameId=" + gameId);
    }


}
