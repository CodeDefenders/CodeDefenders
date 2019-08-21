package org.codedefenders.notification.impl;

import javax.annotation.ManagedBean;
import javax.enterprise.context.ApplicationScoped;

import com.google.common.eventbus.SubscriberExceptionHandler;
import com.google.gson.Gson;
import org.codedefenders.notification.INotificationService;

import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Notification Service implementation.
 * This service behaves like a singleton in the app.
 * See https://docs.oracle.com/javaee/6/api/javax/enterprise/context/ApplicationScoped.html
 * @author gambi
 */
@ManagedBean
@ApplicationScoped
public class NotificationService implements INotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private SubscriberExceptionHandler exceptionHandler = (exception, context) -> {
        Gson gson = new Gson();
        logger.warn("Got " + exception.getClass().getSimpleName() + " while calling notification handler.", exception);
        logger.warn("Event was: " + gson.toJson(context.getEvent()));
    };

    @SuppressWarnings("UnstableApiUsage")
    private EventBus eventBus = new EventBus(exceptionHandler);

    // TODO Ensures that event bus is defined in a Tomcat System Listener !
    // public NotificationService(EventBus eventBus) {
    //     this.eventBus = eventBus;
    // }

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
