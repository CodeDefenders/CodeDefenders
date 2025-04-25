/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.notification.impl;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Singleton;

import org.codedefenders.notification.ITicketingService;

import static java.time.temporal.ChronoUnit.HOURS;

/**
 * Authenticates users' WebSocket connections generating a ticket for users and validating it when opening the
 * connection.
 */
@Singleton
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
     * Invalidates tickets older than 1 hour to prevent tickets, that are not invalidated, from piling up.
     * (If a ticket is generated but the WebSocket for it is never opened, it will not get invalidated right away).
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
