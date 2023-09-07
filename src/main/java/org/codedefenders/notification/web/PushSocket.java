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
package org.codedefenders.notification.web;

import java.io.IOException;
import java.util.Optional;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.codedefenders.dto.SimpleUser;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.ITicketingService;
import org.codedefenders.notification.events.EventNames;
import org.codedefenders.notification.events.client.ClientEvent;
import org.codedefenders.notification.events.server.ServerEvent;
import org.codedefenders.notification.handling.ClientEventHandler;
import org.codedefenders.notification.handling.ServerEventHandlerContainer;
import org.codedefenders.service.UserService;
import org.codedefenders.util.CDIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Communicates events with clients through a WebSocket.
 * </p>
 *
 * <p>
 * <h2>Event format and types</h2>
 * Sent and received messages follow the following JSON format:
 * <pre>
 * {
 *     type: &lt;event name based on the class, given by {@link EventNames}&gt;,
 *     data: &lt;json representation of event object&gt;
 * }
 * </pre>
 * </p>
 *
 * <p>
 * <h2>Server-to-client events</h2>
 * Server-to-client events are located in the package events/server.
 * </p>
 *
 * <p>
 * Filtering of server-to-client events (i.e. deciding which sessions to send the events to)
 * is done by the event handlers using information (e.g. user ids) stored in the event.
 * </p>
 * <br/>
 *
 * <p>
 * <h2>Client-to-server events</h2>
 * Client-to-server events are located in the package events/client.
 * </p>
 */
@ServerEndpoint(
        value = "/notifications/{ticket}/{userId}",
        encoders = {EventEncoder.class},
        decoders = {EventDecoder.class})
public class PushSocket {
    private static final Logger logger = LoggerFactory.getLogger(PushSocket.class);

    private final INotificationService notificationService;
    private final ITicketingService ticketingServices;

    private final UserService userService;

    // Authorization
    private SimpleUser user;
    private String ticket;
    private boolean open;

    // Event handler
    private ClientEventHandler clientEventHandler;
    private ServerEventHandlerContainer serverEventHandlerContainer;

    private Session session;

    public PushSocket() {
        // Since @Inject does not work with WebSocket ...

        notificationService = CDIUtil.getBeanFromCDI(INotificationService.class);
        ticketingServices = CDIUtil.getBeanFromCDI(ITicketingService.class);

        userService = CDIUtil.getBeanFromCDI(UserService.class);

        open = false;
    }

    @OnOpen
    public synchronized void open(Session session,
            @PathParam("ticket") String ticket,
            @PathParam("userId") Integer userId) throws IOException {

        if (!ticketingServices.validateTicket(ticket, userId)) {
            logger.info("Invalid ticket " + ticket + " for session " + session);
            session.close(new CloseReason(CloseCodes.CANNOT_ACCEPT, "Invalid ticket"));
            return;
        }

        Optional<SimpleUser> user = userService.getSimpleUserById(userId);

        if (user.isEmpty()) {
            logger.info("Invalid user id for session " + session);
            session.close(new CloseReason(CloseCodes.CANNOT_ACCEPT, "Invalid user id"));
            return;
        }

        this.user = user.get();
        this.ticket = ticket;
        this.serverEventHandlerContainer = new ServerEventHandlerContainer(notificationService, this, user.get(), ticket);
        this.clientEventHandler = new ClientEventHandler(notificationService, serverEventHandlerContainer, user.get(), ticket);
        this.session = session;

        open = true;
    }

    @OnClose
    public synchronized void close(Session session) {
        /* Close is called before open when the connection is denied. */
        if (open) {
            logger.info("Closing session for user: " + user.getId() + " (ticket: " + ticket + ")");
            /* Don't invalidate tickets on close, because the connection is opened multiple times
               for progress-bars on Firefox. */
            // ticketingServices.invalidateTicket(this.ticket);
            serverEventHandlerContainer.unregisterAll();
        }
    }

    @OnMessage
    public synchronized void onMessage(ClientEvent event, Session session) {
        event.accept(clientEventHandler);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.error("Session " + session + " caused an error. Cause: ", throwable);
    }

    // TODO: error handling?
    public synchronized void sendEvent(ServerEvent event) {
        try {
            // TODO: asyncRemote?
            session.getBasicRemote().sendObject(event);
        } catch (IOException e) {
            logger.error("Exception while sending event.", e);
        } catch (EncodeException e) {
            logger.error("Exception while encoding event.", e);
        }
    }
}
