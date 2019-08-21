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
 * <ul>
 *     <li>Server-to-client events are located in the package events/server.</li>
 *     <li>Client-to-server events are located in the package events/client.</li>
 * </ul>
 * Filtering is done by the event handlers. Therefore, server-to-client events may need to store information to filter
 * which users will receive the message (e.g. user ids, game id, etc.).
 * <p></p>
 * Sent and received messages follow the following JSON format:
 * <pre>
 *     {
 *         type: &lt;simple classname of the event&gt;,
 *         data: &lt;json representation of event class&gt;
 *     }
 * </pre>
 * Client-to-server events are converted to JSON automatically based on their type.
 * <p></p>
 * Server-to-client events are converted to JSON by the event's {@code toJson()} method.
 * Because of this, server-to-client events may store additional information, which is omitted when sending the event to
 * the client.
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
        // TODO
        if (event instanceof RegistrationEvent) {
            RegistrationEvent e2 = (RegistrationEvent) event;
            logger.info("Client registered for " + Arrays.toString(e2.getEvents()));
        } else if (event instanceof ClientChatEvent) {
            ClientChatEvent e2 = (ClientChatEvent) event;
            logger.info("Client sent message: " + e2.getMessage());
        } else {
            logger.info("Unknown message");
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.error("Session " + session + " caused an error. Cause: ", throwable);
    }
}
