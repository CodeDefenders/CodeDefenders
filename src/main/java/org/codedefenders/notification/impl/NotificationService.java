package org.codedefenders.notification.impl;

import javax.annotation.ManagedBean;
import javax.enterprise.context.ApplicationScoped;

import org.codedefenders.notification.INotificationService;

import com.google.common.eventbus.EventBus;

/**
 * Notification Service implementation.
 * This service behaves like a singleton in the app.
 * See https://docs.oracle.com/javaee/6/api/javax/enterprise/context/ApplicationScoped.html
 * @author gambi
 */
@ManagedBean
@ApplicationScoped
public class NotificationService implements INotificationService {

    // TODO Maybe there's some better instantiation options
    private EventBus eventBus = new EventBus();

    // TODO Ensures that event bus is defined in a Tomcat System Listener !
    // public NotificationService(EventBus eventBus) {
    //     this.eventBus = eventBus;
    // }

    public NotificationService() {}

    @Override
    public void post(Object message) {
        eventBus.post(message);
    }

    @Override
    public void register(Object eventHandler) {
        eventBus.register(eventHandler);
    }

    @Override
    public void unregister(Object eventHandler) {
        eventBus.unregister(eventHandler);
    }

}
