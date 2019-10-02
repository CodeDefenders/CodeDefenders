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
    private ServerEventHandler serverEventHandler;
    private User user;

    public ClientEventHandler(
            INotificationService notificationService,
            ServerEventHandler serverEventHandler,
            User user) {
        this.notificationService = notificationService;
        this.serverEventHandler = serverEventHandler;
        this.user = user;
    }

    public void visit(ClientGameChatEvent event) {
        Role role = DatabaseAccess.getRole(user.getId(), event.getGameId());

        if (role == null || role == Role.NONE) {
            logger.warn("Tried to send chat message to game user is not playing in.");
            return;
        }

        ServerGameChatEvent serverEvent = new ServerGameChatEvent(
                user,
                event.getMessage(),
                event.getGameId(),
                role,
                event.isAllChat());
        notificationService.post(serverEvent);
    }

    public void visit(GameChatRegistrationEvent event) {
        serverEventHandler.handleRegistrationEvent(event);
    }

    public void visit(MutantProgressBarRegistrationEvent event) {
        serverEventHandler.handleRegistrationEvent(event);
    }

    public void visit(TestProgressBarRegistrationEvent event) {
        serverEventHandler.handleRegistrationEvent(event);
    }

    public void visit(GameLifecycleRegistrationEvent event) {
        serverEventHandler.handleRegistrationEvent(event);
    }
}
