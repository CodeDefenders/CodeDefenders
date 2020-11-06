package org.codedefenders.beans.notification;

import javax.annotation.ManagedBean;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.codedefenders.notification.events.EventNames;
import org.codedefenders.notification.events.client.chat.ClientGameChatEvent;
import org.codedefenders.notification.events.client.registration.GameChatRegistrationEvent;
import org.codedefenders.notification.events.server.chat.ServerGameChatEvent;
import org.codedefenders.notification.events.server.chat.ServerSystemChatEvent;

@ApplicationScoped
@ManagedBean
@Named("eventNames")
public class EventNamesBean {
    public String getGameChatRegistrationEventName() {
        return EventNames.toClientEventName(GameChatRegistrationEvent.class);
    }

    public String getServerChatEventName() {
        return EventNames.toServerEventName(ServerGameChatEvent.class);
    }

    public String getServerSystemChatEventName() {
        return EventNames.toServerEventName(ServerSystemChatEvent.class);
    }

    public String getClientChatEventName() {
        return EventNames.toClientEventName(ClientGameChatEvent.class);
    }
}
