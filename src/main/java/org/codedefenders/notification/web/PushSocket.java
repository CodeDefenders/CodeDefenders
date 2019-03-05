package org.codedefenders.notification.web;

import java.io.IOException;

import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.ITicketingService;
import org.codedefenders.notification.model.ChatEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

@ServerEndpoint(value = "/notifications/{ticket}/{userId}", //
        encoders = { NotificationEncoder.class })
// Since we manage here different message types we cannot have a single decoder
public class PushSocket {

    @Inject
    private INotificationService notificationService;

    @Inject
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

    @OnOpen
    public void open(Session session, //
            @PathParam("ticket") String ticket, 
            @PathParam("userId") Integer userId) 
            throws IOException {

        // Validate this WebSocket against UserID using the Request-Ticket
        if (ticketingServices.validateTicket(ticket, userId)) {
            logger.warn("Invalid ticket for user " + userId);
            validSession = false;
            return;
        }
        //
        this.userId = userId;
        this.ticket = ticket;
    }

    @OnClose
    public void close(Session session) {
        // Invalidate the ticket
        ticketingServices.invalidateTicket(this.ticket);

        if( ! validSession ) return;
        
        if (this.gameEventHandler != null) {
            notificationService.unregister(this.gameEventHandler);
        }
        if (this.chatEventHandler != null) {
            notificationService.unregister(this.chatEventHandler);
        }
        if (this.progressBarEventHandler != null) {
            notificationService.unregister(this.progressBarEventHandler);
        }
    }

    @OnMessage
    public void onMessage(String json, Session session) {
        if( ! validSession ) return;
        
        // TODO Create a typeAdapterFactory: https://stackoverflow.com/questions/22307382/how-do-i-implement-typeadapterfactory-in-gson
        try{
            ChatEvent chatMessage = (ChatEvent) new Gson().fromJson(json, ChatEvent.class);
            // TODO Add routing information here? e.g., direct message vs team vs game
            notificationService.post( chatMessage );
        } catch (Throwable e) {
            try{
            PushSocketRegistrationEvent registration = (PushSocketRegistrationEvent) new Gson().fromJson(json, PushSocketRegistrationEvent.class);
            if ("GAME_EVENT".equals(registration.getTarget())) {
                this.gameEventHandler = new GameEventHandler(registration.getPlayerID(), registration.getGameID(), session);
                notificationService.register( gameEventHandler );
            } else if ("CHAT_EVENT".equals(registration.getTarget())) {
                this.chatEventHandler = new ChatEventHandler(this.userId, session);
                notificationService.register(this.chatEventHandler);
            } else if ("PROGRESSBAR_EVENT".equals(registration.getTarget())) {
                // This is not reliable since all the clients will receive the update message if they are linked to the same
                // user, unless the "progress bar event" is not 
                this.progressBarEventHandler = new ProgressBarEventHandler(registration.getPlayerID(), session);
                notificationService.register(this.progressBarEventHandler);
            }
            } catch (Throwable e1) {
                logger.warn("Unrecognize message:" + json);
            }
        }
        
        
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        if( ! validSession ) return;
    }

}