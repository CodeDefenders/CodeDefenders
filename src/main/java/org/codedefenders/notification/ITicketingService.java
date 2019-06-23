package org.codedefenders.notification;

public interface ITicketingService {
    public String generateTicketForOwner(Integer owner);

    public boolean validateTicket(String ticket, Integer owner);

    public void invalidateTicket(String ticket);
}
