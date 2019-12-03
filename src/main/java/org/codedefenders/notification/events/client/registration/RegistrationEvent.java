package org.codedefenders.notification.events.client.registration;

import com.google.gson.annotations.Expose;
import org.codedefenders.notification.events.client.ClientEvent;

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
