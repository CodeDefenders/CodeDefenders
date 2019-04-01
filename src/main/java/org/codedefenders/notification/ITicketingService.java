package org.codedefenders.notification;

public interface ITicketingService {

    public String generatedTicketForOwner(Integer owner);
    
    public boolean validateTicket(String ticket, Integer owner);

    // TODO Is owner necessary here?
    public void invalidateTicket(String ticket);
}
