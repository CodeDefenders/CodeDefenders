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
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.ITicketingService;
import org.codedefenders.notification.events.client.ClientEvent;
import org.codedefenders.notification.events.client.RegistrationEvent;
import org.codedefenders.notification.handling.client.ClientEventHandler;
import org.codedefenders.notification.handling.server.ChatEventHandler;
import org.codedefenders.notification.handling.server.GameEventHandler;
import org.codedefenders.notification.handling.server.ProgressBarEventHandler;
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
        encoders = { EventEncoder.class },
        decoders = { EventDecoder.class })
public class PushSocket {
    // TODO Make an Inject for this
    private static final Logger logger = LoggerFactory.getLogger(PushSocket.class);

    // @Inject
    private INotificationService notificationService;

    // @Inject
    private ITicketingService ticketingServices;

    // Authorization
    private int userId = -1;
    private String ticket;

    // Server events handlers
    private ChatEventHandler chatEventHandler;
    private GameEventHandler gameEventHandler;
    private ProgressBarEventHandler progressBarEventHandler;

    // Client event handler
    private ClientEventHandler clientEventHandler;

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
            handleRegistrationEvent((RegistrationEvent) event, session);
        } else {
            event.accept(clientEventHandler);
        }
    }

    // TODO: create separate events for the different registrations?
    private void handleRegistrationEvent(RegistrationEvent event, Session session) {
        switch (event.getType()) {
            case CHAT:
                if (event.getUserId() != null) {
                    chatEventHandler = new ChatEventHandler(event.getUserId(), session);
                    notificationService.register(chatEventHandler);
                } else {
                    logger.warn("RegistrationEvent for progressbar is missing user id.");
                }
                break;
            case GAME:
                if (event.getPlayerId() != null && event.getGameId() != null) {
                    gameEventHandler = new GameEventHandler(event.getPlayerId(), event.getGameId(), session);
                    notificationService.register(chatEventHandler);
                } else {
                    logger.warn("RegistrationEvent for progressbar is missing game id or player id.");
                }
                break;
            case PROGRESSBAR:
                if (event.getPlayerId() != null) {
                    progressBarEventHandler = new ProgressBarEventHandler(event.getPlayerId(), session);
                    notificationService.register(chatEventHandler);
                } else {
                    logger.warn("RegistrationEvent for progressbar is missing player id.");
                }
                break;
            default:
                logger.error("Unknown enum entry.");
                break;
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.error("Session " + session + " caused an error. Cause: ", throwable);
    }
}
