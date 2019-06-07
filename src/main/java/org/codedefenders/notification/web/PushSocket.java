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
        /*
        try {
            session.getBasicRemote().sendText(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
        */

        // TODO Create a typeAdapterFactory:
        // https://stackoverflow.com/questions/22307382/how-do-i-implement-typeadapterfactory-in-gson
        if (json.contains("org.codedefenders.notification.web.PushSocketRegistrationEvent")) {
            try {
                PushSocketRegistrationEvent registration = new Gson().fromJson(json, PushSocketRegistrationEvent.class);

                System.out.println("PushSocket.onMessage() GOT " + json + " -> " + registration);

                if ("GAME_EVENT".equals(registration.getTarget())) {
                    this.gameEventHandler = new GameEventHandler(registration.getPlayerID(), registration.getGameID(),
                            session);
                    notificationService.register(gameEventHandler);
                } else if ("CHAT_EVENT".equals(registration.getTarget())) {
                    this.chatEventHandler = new ChatEventHandler(this.userId, session);
                    notificationService.register(this.chatEventHandler);
                } else if ("PROGRESSBAR_EVENT".equals(registration.getTarget())) {

                    if ("DEREGISTER".equalsIgnoreCase(registration.getAction())){
                        logger.info("Deregistering Progress Bar " + session );
                        if (this.progressBarEventHandler != null) {
                            notificationService.unregister(this.progressBarEventHandler);
                        }
                    } else{
                        this.progressBarEventHandler = new ProgressBarEventHandler(registration.getPlayerID(), session);
                        notificationService.register(this.progressBarEventHandler);
                        logger.info("Registering Progress Bar " + session );
                    }

                }
            } catch (Throwable e) {
                logger.error("Cannot parse registration event", e);
            }
        } else {
            try {
                ChatEvent chatMessage = new Gson().fromJson(json, ChatEvent.class);
                // TODO Add routing information here? e.g., direct message vs
                // team
                // vs game
                notificationService.post(chatMessage);
            } catch (Throwable e) {
                logger.error("Cannot parse chat event", e);
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.error("Session " + session + " is on error. Cause: ", throwable);
    }

}
