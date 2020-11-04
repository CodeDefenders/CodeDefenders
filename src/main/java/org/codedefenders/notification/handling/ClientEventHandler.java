package org.codedefenders.notification.handling;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.GameChatDAO;
import org.codedefenders.game.Role;
import org.codedefenders.model.User;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.client.chat.ClientGameChatEvent;
import org.codedefenders.notification.events.client.registration.GameChatRegistrationEvent;
import org.codedefenders.notification.events.client.registration.GameLifecycleRegistrationEvent;
import org.codedefenders.notification.events.client.registration.MutantProgressBarRegistrationEvent;
import org.codedefenders.notification.events.client.registration.TestProgressBarRegistrationEvent;
import org.codedefenders.notification.events.server.chat.ServerGameChatEvent;
import org.codedefenders.notification.web.PushSocket;
import org.codedefenders.util.CDIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles incoming client events from a {@link PushSocket}.
 */
public class ClientEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(ClientEventHandler.class);
    private static final Pattern chatCommandPattern = Pattern.compile("^/([a-zA-Z]+)");

    private INotificationService notificationService;
    private ServerEventHandlerContainer serverEventHandlerContainer;
    private User user;
    private GameChatDAO gameChatDAO;

    public ClientEventHandler(
            INotificationService notificationService,
            ServerEventHandlerContainer serverEventHandlerContainer,
            User user) {
        this.notificationService = notificationService;
        this.serverEventHandlerContainer = serverEventHandlerContainer;
        this.user = user;
        gameChatDAO = CDIUtil.getBeanFromCDI(GameChatDAO.class);
    }

    public void visit(ClientGameChatEvent event) {
        Role role = DatabaseAccess.getRole(user.getId(), event.getGameId());

        if (role == null) {
            logger.warn("User {} tried to send chat message to game {}, which the user is not playing in.",
                    user.getId(), event.getGameId());
            return;
        }

        String message = event.getMessage().trim();
        boolean isAllChat = event.isAllChat();

        Matcher matcher = chatCommandPattern.matcher(message);
        if (matcher.find()) {
            final String command = matcher.group(1);
            switch (command) {
                case "all":
                    isAllChat = true;
                    break;
                case "team":
                    isAllChat = false;
                    break;
                default:
                    // Simply ignore invalid commands for now.
                    return;
            }
            message = message.substring(command.length() + 1).trim();
            if (message.isEmpty()) {
                return;
            }
        }

        ServerGameChatEvent serverEvent = new ServerGameChatEvent();
        serverEvent.setMessage(message);
        serverEvent.setSenderId(user.getId());
        serverEvent.setSenderName(user.getUsername());
        serverEvent.setAllChat(isAllChat);
        serverEvent.setGameId(event.getGameId());
        serverEvent.setRole(role);

        gameChatDAO.insertMessage(serverEvent);
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
}
