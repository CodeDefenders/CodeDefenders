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

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.CloseReason.CloseCode;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.ITicketingService;
import org.codedefenders.notification.model.ChatEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

// Since we manage here different message types we cannot have a single decoder
// @RequestScoped -> TODO What's this?
@ServerEndpoint(value = "/notifications/{ticket}/{userId}", encoders = { NotificationEncoder.class })
public class PushSocket {

    // @Inject
    private INotificationService notificationService;

    // @Inject
    private ITicketingService ticketingServices;

    // TODO Make an Inject for this
    private static final Logger logger = LoggerFactory.getLogger(PushSocket.class);

    // Authorization
    private int userId = -1;
    private String ticket;
    private boolean validSession;

    // Events Handlers
    private ChatEventHandler chatEventHandler;
    private GameEventHandler gameEventHandler;
    private ProgressBarEventHandler progressBarEventHandler;

    public PushSocket() {
        try {
            // Since @Inject does not work with WebSocket ...
            InitialContext initialContext = new InitialContext();
            BeanManager bm = (BeanManager) initialContext.lookup("java:comp/env/BeanManager");
            Bean bean;
            CreationalContext ctx;

            bean = bm.getBeans(INotificationService.class).iterator().next();
            ctx = bm.createCreationalContext(bean);
            notificationService = (INotificationService) bm.getReference(bean, INotificationService.class, ctx);

            bean = bm.getBeans(ITicketingService.class).iterator().next();
            ctx = bm.createCreationalContext(bean);
            ticketingServices = (ITicketingService) bm.getReference(bean, ITicketingService.class, ctx);

        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    private boolean validate(Session session, String ticket, Integer owner) throws IOException{
        if (! ticketingServices.validateTicket(ticket, owner)){
            logger.info("Invalid ticket for session " + session );
            session.close( new CloseReason( CloseCodes.CANNOT_ACCEPT, "Invalid ticket"));
            return false;
        } else {
            return true;
        }
    }

    @OnOpen
    public void open(Session session, @PathParam("ticket") String ticket, @PathParam("userId") Integer userId)
            throws IOException {

        if (!validate(session, ticket, userId)) {
            return;
        }

        this.userId = userId;
        this.ticket = ticket;
    }

    @OnClose
    public void close(Session session) {

        // Invalidate the ticket
        ticketingServices.invalidateTicket(this.ticket);

        if (this.gameEventHandler != null) {
            notificationService.unregister(this.gameEventHandler);
        }
        if (this.chatEventHandler != null) {
            notificationService.unregister(this.chatEventHandler);
        }
        if (this.progressBarEventHandler != null) {
            logger.info("Unregistering Progress Bar " + session);
            notificationService.unregister(this.progressBarEventHandler);
        }
    }

    @OnMessage
    public void onMessage(String json, Session session) {
        // TODO Create a typeAdapterFactory:
        // https://stackoverflow.com/questions/22307382/how-do-i-implement-typeadapterfactory-in-gson

        // Registration for event types
        // PushSocketRegistrationEvent registration = new Gson().fromJson(json, PushSocketRegistrationEvent.class);
        // this.gameEventHandler = new GameEventHandler(registration.getPlayerID(), registration.getGameID(), session);
        // notificationService.register(gameEventHandler);

        // Chat messages
        // ChatEvent chatMessage = new Gson().fromJson(json, ChatEvent.class);
        // notificationService.post(chatMessage);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.error("Session " + session + " is on error. Cause: ", throwable);
    }

}
