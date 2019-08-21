package org.codedefenders.notification.web;

import java.io.IOException;
import java.util.Arrays;

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
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.ITicketingService;
import org.codedefenders.notification.events.client.ClientChatEvent;
import org.codedefenders.notification.events.client.ClientEvent;
import org.codedefenders.notification.events.client.RegistrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Communicates notifications and (game) events with clients through a WebSocket.
 *
 * <p></p>
 *
 * <h2>Event format and types</h2>
 * Sent and received messages follow the following JSON format:
 * <pre>
 * {
 *     type: &lt;simple classname of the event&gt;,
 *     data: &lt;json representation of event class&gt;
 * }
 * </pre>
 * Here type is the simple class name of the class the event belongs to,
 * and data is the data of the event, represented by objects of the class.
 *
 * <p></p>
 *
 * <h2>Server-to-client events</h2>
 * Server-to-client events are located in the package events/server.
 * They are serialized to JSON by the event's {@code toJson()} method.
 * Because of this, server-to-client events may store additional information,
 * which can be omitted when sending the event to the client.
 * <p></p>
 * Filtering of server-to-client events is done by the event handlers.
 * Therefore, server-to-client events may need to store information to filter
 * which users will receive the message (e.g. user ids, game id, etc.).
 *
 * <p></p>
 *
 * <h2>Client-to-server events</h2>
 * Client-to-server events are located in the package events/client.
 * They are deserialized from JSON automatically based on their type.
 * <p></p>
 * Some of the client-to-server events may have optional attributes
 * (e.g. {@link RegistrationEvent}).
 */
// @RequestScoped -> TODO What's this?
@ServerEndpoint(
        value = "/notifications/{ticket}/{userId}",
        encoders = { NotificationEncoder.class },
        decoders = { NotificationDecoder.class })
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

    @OnOpen
    public void open(Session session, @PathParam("ticket") String ticket, @PathParam("userId") Integer userId)
            throws IOException {

        if (! ticketingServices.validateTicket(ticket, userId)) {
            logger.info("Invalid ticket for session " + session);
            session.close(new CloseReason(CloseCodes.CANNOT_ACCEPT, "Invalid ticket"));
            return;
        }

        this.userId = userId;
        this.ticket = ticket;
    }

    @OnClose
    public void close(Session session) {
        logger.info("Closing session for user: " + userId + " (ticket: " + ticket + ")");

        // Invalidate the ticket
        ticketingServices.invalidateTicket(this.ticket);

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
    public void onMessage(ClientEvent event, Session session) {

        if (event instanceof RegistrationEvent) {
            RegistrationEvent e2 = (RegistrationEvent) event;
            logger.info("Client registered for " + e2.getType());
        } else if (event instanceof ClientChatEvent) {
            ClientChatEvent e2 = (ClientChatEvent) event;
            logger.info("Client sent message: " + e2.getMessage());
            // TODO
        } else {
            logger.info("Unknown event");
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.error("Session " + session + " caused an error. Cause: ", throwable);
    }

    enum EventType {
        GAME,
        CHAT,
        PROGRESSBAR
    }
}
