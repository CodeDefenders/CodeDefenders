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
package org.codedefenders.notification.handling;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codedefenders.beans.game.GameChatBean;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Role;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.client.achievement.ClientAchievementNotificationShownEvent;
import org.codedefenders.notification.events.client.chat.ClientGameChatEvent;
import org.codedefenders.notification.events.client.registration.AchievementRegistrationEvent;
import org.codedefenders.notification.events.client.registration.GameChatRegistrationEvent;
import org.codedefenders.notification.events.client.registration.GameLifecycleRegistrationEvent;
import org.codedefenders.notification.events.client.registration.MutantProgressBarRegistrationEvent;
import org.codedefenders.notification.events.client.registration.TestProgressBarRegistrationEvent;
import org.codedefenders.notification.events.server.achievement.ServerAchievementNotificationShownEvent;
import org.codedefenders.notification.events.server.chat.ServerGameChatEvent;
import org.codedefenders.notification.events.server.chat.ServerSystemChatEvent;
import org.codedefenders.notification.web.PushSocket;
import org.codedefenders.persistence.database.GameChatRepository;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.PlayerRepository;
import org.codedefenders.util.CDIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.game.ChatCommand.ALL;
import static org.codedefenders.game.ChatCommand.TEAM;

/**
 * Handles incoming client events from a {@link PushSocket}.
 */
public class ClientEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(ClientEventHandler.class);
    private static final Pattern CHAT_COMMAND_PATTERN = Pattern.compile("^/([a-zA-Z]+)");

    private final INotificationService notificationService;
    private final ServerEventHandlerContainer serverEventHandlerContainer;
    private final GameRepository gameRepo;
    private final PlayerRepository playerRepo;
    private final SimpleUser user;
    private final String ticket;

    public ClientEventHandler(
            INotificationService notificationService,
            ServerEventHandlerContainer serverEventHandlerContainer,
            GameRepository gameRepo,
            PlayerRepository playerRepo,
            SimpleUser user,
            String ticket) {
        this.notificationService = notificationService;
        this.serverEventHandlerContainer = serverEventHandlerContainer;
        this.gameRepo = gameRepo;
        this.playerRepo = playerRepo;
        this.user = user;
        this.ticket = ticket;
    }

    /**
     * Sends a system message to the chat instance.
     * @param message The message to send.
     */
    private void sendSystemMessage(String message) {
        ServerSystemChatEvent systemMessage = new ServerSystemChatEvent();
        systemMessage.setMessage(message);
        systemMessage.setTicket(ticket);
        notificationService.post(systemMessage);
    }

    public void visit(ClientGameChatEvent event) {
        GameChatRepository gameChatRepo = CDIUtil.getBeanFromCDI(GameChatRepository.class);
        Role role = playerRepo.getRole(user.getId(), event.getGameId());

        AbstractGame game = gameRepo.getGame(event.getGameId());
        if (game == null) {
            logger.warn("User {} tried to send chat message to game {}, which does not exist.",
                    user.getId(), event.getGameId());
            return;
        }
        if (!game.isChatEnabled()) {
            logger.warn("User {} tried to send chat message to game {}, which does not have chat enabled.",
                    user.getId(), event.getGameId());
            return;
        }
        if (role == null) {
            logger.warn("User {} tried to send chat message to game {}, which the user is not playing in.",
                    user.getId(), event.getGameId());
            return;
        }

        String message = event.getMessage().trim();
        boolean isAllChat = event.isAllChat();

        /* Handle chat commands. */
        Matcher matcher = CHAT_COMMAND_PATTERN.matcher(message);
        if (matcher.find()) {
            final String command = matcher.group(1);
            if (command.equals(ALL.getCommandString())) {
                isAllChat = true;
            } else if (command.equals(TEAM.getCommandString())) {
                isAllChat = false;
            } else {
                sendSystemMessage("/" + command + " is not a valid chat command.");
                return;
            }
            message = message.substring(command.length() + 1).trim();
            if (message.isEmpty()) {
                sendSystemMessage("Your message cannot be empty.");
                return;
            }
        }

        /* Trim message to maximum size. */
        message = message.substring(0, Math.min(GameChatBean.MAX_MESSAGE_LENGTH, message.length()));

        /* Check if message is empty. */
        if (message.isEmpty()) {
            return;
        }

        /* Ignore all chat for roles that can be viewed by everyone anyways. */
        if (role == Role.PLAYER || role == Role.OBSERVER) {
            isAllChat = false;
        }

        ServerGameChatEvent serverEvent = new ServerGameChatEvent();
        serverEvent.setMessage(message);
        serverEvent.setSenderId(user.getId());
        serverEvent.setSenderName(user.getName());
        serverEvent.setAllChat(isAllChat);
        serverEvent.setGameId(event.getGameId());
        serverEvent.setRole(role);

        gameChatRepo.insertMessage(serverEvent);
        notificationService.post(serverEvent);
    }

    public void visit(ClientAchievementNotificationShownEvent clientEvent) {
        ServerAchievementNotificationShownEvent serverEvent = new ServerAchievementNotificationShownEvent();
        serverEvent.setAchievementId(clientEvent.getAchievementId());
        serverEvent.setUserId(user.getId());
        notificationService.post(serverEvent);
    }

    public void visit(GameChatRegistrationEvent event) {
        serverEventHandlerContainer.handleRegistrationEvent(event);
    }

    public void visit(MutantProgressBarRegistrationEvent event) {
        serverEventHandlerContainer.handleRegistrationEvent(event);
    }

    public void visit(TestProgressBarRegistrationEvent event) {
        serverEventHandlerContainer.handleRegistrationEvent(event);
    }

    public void visit(GameLifecycleRegistrationEvent event) {
        serverEventHandlerContainer.handleRegistrationEvent(event);
    }

    public void visit(AchievementRegistrationEvent achievementRegistrationEvent) {
        serverEventHandlerContainer.handleRegistrationEvent(achievementRegistrationEvent);
    }
}
