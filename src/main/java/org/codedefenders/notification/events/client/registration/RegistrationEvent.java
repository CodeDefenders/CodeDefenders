package org.codedefenders.notification.events.client.registration;

import org.codedefenders.notification.events.client.ClientEvent;

import com.google.gson.annotations.Expose;

/**
 * A message used to register for certain types of notifications events.
 */
public abstract class RegistrationEvent extends ClientEvent {
    @Expose private Action action;

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public enum Action {
        REGISTER,
        UNREGISTER
    }
}
