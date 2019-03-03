package org.codedefenders.notification.impl;

import javax.annotation.ManagedBean;

import org.codedefenders.notification.INotificationService;

import com.google.common.eventbus.EventBus;

/**
 * Notification Service implementation.

 * @author gambi
 *
 */
@ManagedBean 
public class NotificationService implements INotificationService {

    // TODO Maybe there's some better instantiation options
    private EventBus eventBus = new EventBus();
    
    // TODO Ensures that event bus is defined in a Tomcat System Listener !
//    public NotificationService(EventBus eventBus) {
//        this.eventBus = eventBus;
//    }
    
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
