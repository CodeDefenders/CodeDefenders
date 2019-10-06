package org.codedefenders.notification.handling;

import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.game.Role;
import org.codedefenders.model.User;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.client.chat.ClientGameChatEvent;
import org.codedefenders.notification.events.client.registration.GameChatRegistrationEvent;
import org.codedefenders.notification.events.client.registration.GameLifecycleRegistrationEvent;
import org.codedefenders.notification.events.client.registration.MutantProgressBarRegistrationEvent;
import org.codedefenders.notification.events.client.registration.TestProgressBarRegistrationEvent;
import org.codedefenders.notification.events.server.chat.ServerGameChatEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(ClientEventHandler.class);

    private INotificationService notificationService;
    private ServerEventHandlerContainer serverEventHandlerContainer;
    private User user;

    public ClientEventHandler(
            INotificationService notificationService,
            ServerEventHandlerContainer serverEventHandlerContainer,
            User user) {
        this.notificationService = notificationService;
        this.serverEventHandlerContainer = serverEventHandlerContainer;
        this.user = user;
    }

    public void visit(ClientGameChatEvent event) {
        Role role = DatabaseAccess.getRole(user.getId(), event.getGameId());

        if (role == null) {
            logger.warn("User {} tried to send chat message to game {}, which the user is not playing in.",
                    user.getId(), event.getGameId());
            return;
        }

        ServerGameChatEvent serverEvent = new ServerGameChatEvent();
        serverEvent.setMessage(event.getMessage());
        serverEvent.setSenderId(user.getId());
        serverEvent.setSenderName(user.getUsername());
        serverEvent.setAllChat(event.isAllChat());
        serverEvent.setGameId(event.getGameId());
        serverEvent.setRole(role);

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
