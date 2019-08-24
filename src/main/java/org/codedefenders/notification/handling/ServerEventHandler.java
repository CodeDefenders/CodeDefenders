package org.codedefenders.notification.handling;

import org.apache.commons.lang.NotImplementedException;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.PlayerDAO;
import org.codedefenders.game.Role;
import org.codedefenders.model.User;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.client.registration.*;
import org.codedefenders.notification.handling.server.GameChatEventHandler;
import org.codedefenders.notification.handling.server.MutantProgressBarEventHandler;
import org.codedefenders.notification.handling.server.TestProgressBarEventHandler;
import org.codedefenders.notification.web.PushSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(ServerEventHandler.class);

    private INotificationService notificationService;
    private PushSocket socket;
    private User user;

    private GameChatEventHandler gameChatEventHandler;
    private MutantProgressBarEventHandler mutantProgressBarEventHandler;
    private TestProgressBarEventHandler testProgressBarEventHandler;

    public ServerEventHandler(INotificationService notificationService, PushSocket socket, User user) {
        this.notificationService = notificationService;
        this.socket = socket;
        this.user = user;
    }

    public void handleRegistrationEvent(GameChatRegistrationEvent event) {
        Role role = DatabaseAccess.getRole(user.getId(), event.getGameId());
        if (role == null) {
            logger.warn("Tried to register/unregister for events for a game the user is not playing in.");
            return;
        }

        if (event.getAction() == RegistrationEvent.Action.REGISTER) {
            if (gameChatEventHandler != null) {
                logger.warn("Tried to register event handler when handler for this type already registered.");
                return;
            }
            gameChatEventHandler = new GameChatEventHandler(socket, event.getGameId(), role);
            notificationService.register(gameChatEventHandler);

        } else if (event.getAction() == RegistrationEvent.Action.UNREGISTER) {
            if (gameChatEventHandler == null) {
                logger.warn("Tried to unregister event handler that is not registered.");
                return;
            }
            if (event.getGameId() != gameChatEventHandler.getGameId()
                    || role != gameChatEventHandler.getRole()) {
                logger.warn("Unregistered event handler with different values as registered.");
            }
            notificationService.unregister(gameChatEventHandler);
            gameChatEventHandler = null;
        }
    }

    public void handleRegistrationEvent(MutantProgressBarRegistrationEvent event) {
        int playerId = PlayerDAO.getPlayerIdForUserAndGame(user.getId(), event.getGameId());
        if (playerId == -1) {
            logger.warn("Tried to register/unregister for events for a game the user is not playing in.");
            return;
        }

        if (event.getAction() == RegistrationEvent.Action.REGISTER) {
            if (mutantProgressBarEventHandler != null) {
                logger.warn("Tried to register event handler when handler for this type already registered.");
                return;
            }
            mutantProgressBarEventHandler = new MutantProgressBarEventHandler(socket, playerId);
            notificationService.register(mutantProgressBarEventHandler);

        } else if (event.getAction() == RegistrationEvent.Action.UNREGISTER) {
            if (mutantProgressBarEventHandler == null) {
                logger.warn("Tried to unregister event handler that is not registered.");
                return;
            }
            if (playerId != mutantProgressBarEventHandler.getPlayerId()) {
                logger.warn("Unregistered event handler with different values as registered.");
            }
            notificationService.unregister(mutantProgressBarEventHandler);
            mutantProgressBarEventHandler = null;
        }
    }

    public void handleRegistrationEvent(TestProgressBarRegistrationEvent event) {
        int playerId = PlayerDAO.getPlayerIdForUserAndGame(user.getId(), event.getGameId());
        if (playerId == -1) {
            logger.warn("Tried to register/unregister for events for a game the user is not playing in.");
            return;
        }

        if (event.getAction() == RegistrationEvent.Action.REGISTER) {
            if (testProgressBarEventHandler != null) {
                logger.warn("Tried to register event handler when handler for this type already registered.");
                return;
            }
            testProgressBarEventHandler = new TestProgressBarEventHandler(socket, playerId);
            notificationService.register(testProgressBarEventHandler);

        } else if (event.getAction() == RegistrationEvent.Action.UNREGISTER) {
            if (testProgressBarEventHandler == null) {
                logger.warn("Tried to unregister event handler that is not registered.");
                return;
            }
            if (playerId != testProgressBarEventHandler.getPlayerId()) {
                logger.warn("Unregistered event handler with different values as registered.");
            }
            notificationService.unregister(testProgressBarEventHandler);
            testProgressBarEventHandler = null;
        }
    }

    public void handleRegistrationEvent(GameLifecycleRegistrationEvent event) {
        // TODO: Not sure how game lifecycle events are going to be handled yet
        throw new NotImplementedException();
    }

    public void unregisterAll() {
        if (this.gameChatEventHandler != null) {
            notificationService.unregister(this.gameChatEventHandler);
        }
        if (this.mutantProgressBarEventHandler != null) {
            notificationService.unregister(this.mutantProgressBarEventHandler);
        }
        if (this.testProgressBarEventHandler != null) {
            notificationService.unregister(this.testProgressBarEventHandler);
        }
    }
}
