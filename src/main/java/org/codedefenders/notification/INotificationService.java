package org.codedefenders.notification;

/**
 * Interface for the Notification Service. 
 * 
 * Modeled following the Guava Event Bus generic interface.
 * 
 *  TODO Introduce a bit more logic here for filtering, tagging, authentication, etc
 * 
 * @author gambi
 *
 */
public interface INotificationService {

    public void register(Object eventHandler);
    
    public void unregister(Object eventHandler);
    
    public void post(Object message);
}
