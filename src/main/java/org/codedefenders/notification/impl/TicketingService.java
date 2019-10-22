package org.codedefenders.notification.impl;

import static java.time.temporal.ChronoUnit.HOURS;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.ManagedBean;
import javax.enterprise.context.ApplicationScoped;

import org.codedefenders.notification.ITicketingService;

/**
 * Authenticates users' WebSocket connections generating a ticket for users and validating it when opening the
 * connection.
 */
@ManagedBean
@ApplicationScoped
public class TicketingService implements ITicketingService {
    /**
     * Stores tickets by their UUID.
     */
    private ConcurrentHashMap<String, Ticket> tickets = new ConcurrentHashMap<>();

    /**
     * Stores tickets in the order they are generated.
     */
    private ConcurrentLinkedQueue<Ticket> ticketsQueue = new ConcurrentLinkedQueue<>();

    @Override
    public synchronized String generateTicketForOwner(int owner) {
        String ticketStr = UUID.randomUUID().toString();

        Ticket ticket = new Ticket(ticketStr, owner);
        tickets.put(ticketStr, ticket);
        ticketsQueue.offer(ticket);

        timeoutTickets();

        return ticketStr;
    }

    @Override
    public boolean validateTicket(String ticketStr, int owner) {
        Ticket ticket = tickets.get(ticketStr);
        return ticket != null && owner == ticket.owner;
    }

    @Override
    public void invalidateTicket(String ticketStr) {
        Ticket removed = tickets.remove(ticketStr);
        removed.valid = false;
    }

    /**
     * Invalidates tickets older than 1 hour.
     * This is to prevent tickets, that are not invalidated, from piling up.
     * If a ticket is generated but the WebSocket for it is never opened,
     * it will not get invalidated right away (at least the way it's currently implemented).
     */
    private void timeoutTickets() {
        Instant now = Instant.now();
        while (!ticketsQueue.isEmpty()) {
            Ticket ticket = ticketsQueue.peek();
            if (HOURS.between(ticket.timestamp, now) >= 1) {
                if (ticket.valid) {
                    invalidateTicket(ticket.ticket);
                }
                ticketsQueue.poll();
            } else {
                break;
            }
        }
    }

    private static class Ticket {
        private String ticket;
        private int owner;
        private boolean valid;
        private Instant timestamp;

        public Ticket(String ticket, int owner) {
            this.ticket = ticket;
            this.owner = owner;
            this.valid = true;
            this.timestamp = Instant.now();
        }
    }
}
