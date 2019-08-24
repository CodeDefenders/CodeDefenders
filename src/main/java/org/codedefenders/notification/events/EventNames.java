package org.codedefenders.notification.events;

import org.codedefenders.notification.events.client.ClientEvent;
import org.codedefenders.notification.events.server.ServerEvent;

public class EventNames {
    public static String toServerEventName(Class<? extends ServerEvent> eventClass) {
        String fullName = eventClass.getName();
        return fullName.substring("org.codedefenders.notification.events.server.".length());
    }

    public static String toClientEventName(Class<? extends ClientEvent> eventClass) {
        String fullName = eventClass.getName();
        return fullName.substring("org.codedefenders.notification.events.client.".length());
    }

    @SuppressWarnings("unchecked")
    public static Class<ClientEvent> toClientEvent(String eventName) throws ClassNotFoundException, ClassCastException {
        return (Class<ClientEvent>) Class.forName("org.codedefenders.notification.events.client." + eventName);
    }
}
