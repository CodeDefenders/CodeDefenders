package org.codedefenders.notification.impl;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.ManagedBean;
import javax.enterprise.context.ApplicationScoped;

import org.codedefenders.notification.ITicketingService;

@ManagedBean
@ApplicationScoped
public class TicketingService implements ITicketingService {

    // Add duration for validity ?
    private ConcurrentHashMap<String, Integer> tickets = new ConcurrentHashMap<String, Integer>();
    
    @Override
    public synchronized String generatedTicketForOwner(Integer owner) {
        // TODO Validate. Owner cannot be null !
        String ticket = UUID.randomUUID().toString();
        tickets.put(ticket, owner);
        return ticket;
    }

    @Override
    public boolean validateTicket(String ticket, Integer owner) {
        if (ticket == null || owner == null) return false;
        return owner.equals(tickets.get(ticket));
    }

    @Override
    public void invalidateTicket(String ticket) {
        if (ticket != null)
            tickets.remove( ticket );
    }

}
