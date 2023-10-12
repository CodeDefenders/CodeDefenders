package org.codedefenders.notification.events.client.registration;

import org.codedefenders.notification.handling.ClientEventHandler;

/**
 * A message used to register for the achievement unlocked event.
 */
public class AchievementRegistrationEvent extends RegistrationEvent {

    @Override
    public void accept(ClientEventHandler visitor) {
        visitor.visit(this);
    }
}
