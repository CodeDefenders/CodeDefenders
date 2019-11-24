package org.codedefenders.notification.events;

import org.codedefenders.notification.events.client.ClientEvent;
import org.codedefenders.notification.events.server.ServerEvent;

/**
 * Provides mapping between sever and client event names / types (used to send them via JSON) and their classes.
 * To avoid security risks, only part of the package name is usd for the event names.
 */
public class EventNames {
    /**
     * Returns the name of a server event.
     *
     * @param eventClass The class of the server event.
     * @return The name of the server event.
     */
    public static String toServerEventName(Class<? extends ServerEvent> eventClass) {
        String fullName = eventClass.getName();
        return fullName.substring("org.codedefenders.notification.events.server.".length());
    }

    /**
     * Returns the name of a client event.
     *
     * @param eventClass The class of the client event.
     * @return The name of the client event.
     */
    public static String toClientEventName(Class<? extends ClientEvent> eventClass) {
        String fullName = eventClass.getName();
        return fullName.substring("org.codedefenders.notification.events.client.".length());
    }

    /**
     * Returns the client event class for the given event name.
     *
     * @param eventName The event name.
     * @return The client event class for the given event name.
     * @throws ClassNotFoundException If no client event for the name exists.
     * @throws ClassCastException If the class with that name is no client event class.
     */
    @SuppressWarnings("unchecked")
    public static Class<ClientEvent> toClientEvent(String eventName) throws ClassNotFoundException, ClassCastException {
        return (Class<ClientEvent>) Class.forName("org.codedefenders.notification.events.client." + eventName);
    }
}
