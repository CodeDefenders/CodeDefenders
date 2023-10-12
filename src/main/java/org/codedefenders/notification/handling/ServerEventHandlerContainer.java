package org.codedefenders.notification.handling;

import java.util.HashMap;
import java.util.Map;

import org.codedefenders.database.GameDAO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.Role;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.client.registration.AchievementRegistrationEvent;
import org.codedefenders.notification.events.client.registration.GameChatRegistrationEvent;
import org.codedefenders.notification.events.client.registration.GameLifecycleRegistrationEvent;
import org.codedefenders.notification.events.client.registration.MutantProgressBarRegistrationEvent;
import org.codedefenders.notification.events.client.registration.RegistrationEvent;
import org.codedefenders.notification.events.client.registration.TestProgressBarRegistrationEvent;
import org.codedefenders.notification.handling.server.AchievementEventHandler;
import org.codedefenders.notification.handling.server.GameChatEventHandler;
import org.codedefenders.notification.handling.server.GameLifecycleEventHandler;
import org.codedefenders.notification.handling.server.MutantProgressBarEventHandler;
import org.codedefenders.notification.handling.server.ServerEventHandler;
import org.codedefenders.notification.handling.server.TestProgressBarEventHandler;
import org.codedefenders.notification.impl.NotificationService;
import org.codedefenders.notification.web.PushSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container for {@link ServerEventHandler ServerEventHandlers},
 * that handle server events form a {@link NotificationService}
 * and send outgoing events with a {@link PushSocket}.
 */
public class ServerEventHandlerContainer {
    private static final Logger logger = LoggerFactory.getLogger(ServerEventHandlerContainer.class);

    private INotificationService notificationService;
    private PushSocket socket;
    private SimpleUser user;
    private String ticket;

    private Map<ServerEventHandler, ServerEventHandler> handlers;

    public ServerEventHandlerContainer(INotificationService notificationService, PushSocket socket, SimpleUser user,
            String ticket) {
        this.notificationService = notificationService;
        this.socket = socket;
        this.user = user;
        this.ticket = ticket;
        this.handlers = new HashMap<>();
    }

    private synchronized boolean addHandler(ServerEventHandler handler) {
        if (handlers.containsKey(handler)) {
            logger.warn("User {} tried to register {} that is already registered.",
                    user.getId(), handler.getClass().getSimpleName());
            return false;
        } else {
            handlers.put(handler, handler);
            notificationService.register(handler);
            return true;
        }
    }

    private synchronized boolean removeHandler(ServerEventHandler handler) {
        if (!handlers.containsKey(handler)) {
            logger.warn("User {} tried to remove {} that is not registered.",
                    user.getId(), handler.getClass().getSimpleName());
            return false;
        } else {
            ServerEventHandler realHandler = handlers.get(handler);
            handlers.remove(handler);
            notificationService.unregister(realHandler);
            return true;
        }
    }

    public synchronized void unregisterAll() {
        for (ServerEventHandler handler : handlers.values()) {
            notificationService.unregister(handler);
        }
        handlers.clear();
    }

    public void handleRegistrationEvent(GameChatRegistrationEvent event) {
        Role role = GameDAO.getRole(user.getId(), event.getGameId());
        if (role == null) {
            logger.warn("User {} tried to register/unregister {} for game {}, which they are not playing in.",
                    user.getId(), GameChatEventHandler.class.getSimpleName(), event.getGameId());
            return;
        }

        if (event.getAction() == RegistrationEvent.Action.REGISTER) {
            addHandler(new GameChatEventHandler(socket, event.getGameId(), role, ticket));

        } else if (event.getAction() == RegistrationEvent.Action.UNREGISTER) {
            removeHandler(new GameChatEventHandler(socket, event.getGameId(), role, ticket));
        }
    }

    public void handleRegistrationEvent(MutantProgressBarRegistrationEvent event) {
        Role role = GameDAO.getRole(user.getId(), event.getGameId());
        if (role == null) {
            logger.warn("User {} tried to register/unregister {} for game {}, which they are not playing in.",
                    user.getId(), MutantProgressBarEventHandler.class.getSimpleName(), event.getGameId());
            return;
        }

        if (event.getAction() == RegistrationEvent.Action.REGISTER) {
            addHandler(new MutantProgressBarEventHandler(socket, event.getGameId(), user.getId()));

        } else if (event.getAction() == RegistrationEvent.Action.UNREGISTER) {
            removeHandler(new MutantProgressBarEventHandler(socket, event.getGameId(), user.getId()));
        }
    }

    public void handleRegistrationEvent(TestProgressBarRegistrationEvent event) {
        Role role = GameDAO.getRole(user.getId(), event.getGameId());
        if (role == null) {
            logger.warn("User {} tried to register/unregister {} for game {}, which they are not playing in.",
                    user.getId(), TestProgressBarEventHandler.class.getSimpleName(), event.getGameId());
            return;
        }

        if (event.getAction() == RegistrationEvent.Action.REGISTER) {
            addHandler(new TestProgressBarEventHandler(socket, event.getGameId(), user.getId()));

        } else if (event.getAction() == RegistrationEvent.Action.UNREGISTER) {
            removeHandler(new TestProgressBarEventHandler(socket, event.getGameId(), user.getId()));
        }
    }

    public void handleRegistrationEvent(GameLifecycleRegistrationEvent event) {
        Role role = GameDAO.getRole(user.getId(), event.getGameId());
        if (role == null) {
            logger.warn("User {} tried to register/unregister {} for game {}, which they are not playing in.",
                    user.getId(), GameLifecycleEventHandler.class.getSimpleName(), event.getGameId());
            return;
        }

        if (event.getAction() == RegistrationEvent.Action.REGISTER) {
            addHandler(new GameLifecycleEventHandler(socket, event.getGameId(), user.getId()));

        } else if (event.getAction() == RegistrationEvent.Action.UNREGISTER) {
            removeHandler(new GameLifecycleEventHandler(socket, event.getGameId(), user.getId()));
        }
    }

    public void handleRegistrationEvent(AchievementRegistrationEvent achievementRegistrationEvent) {
        if (achievementRegistrationEvent.getAction() == RegistrationEvent.Action.REGISTER) {
            addHandler(new AchievementEventHandler(socket, user.getId()));

        } else if (achievementRegistrationEvent.getAction() == RegistrationEvent.Action.UNREGISTER) {
            removeHandler(new AchievementEventHandler(socket, user.getId()));
        }
    }
}
