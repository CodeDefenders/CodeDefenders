package org.codedefenders.notification;

public interface ITicketingService {
    public String generateTicketForOwner(int owner);

    public boolean validateTicket(String ticket, int owner);

    public void invalidateTicket(String ticket);
}
