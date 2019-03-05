package org.codedefenders.notification;

/**
 * Interface for the Notification Service. 
 * 
 * Modeled following the Guava Event Bus generic interface for message passing and registration.
 * 
 *  TODO Introduce a bit more logic here for filtering, tagging, authentication, etc
 *  TODO Introduce some logic for authentication authorization some interactions must be specific for client request
 *  so maybe some ticketing service or similar. Most likely this can be done using some filter or request listener
 * 
 * @author gambi
 *
 */
public interface INotificationService {

    public void register(Object eventHandler);
    
    public void unregister(Object eventHandler);
    
    public void post(Object message);
}
