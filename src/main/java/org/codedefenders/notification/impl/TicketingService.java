/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
    public synchronized String generateTicketForOwner(Integer owner) {
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
            tickets.remove(ticket);
    }

}
