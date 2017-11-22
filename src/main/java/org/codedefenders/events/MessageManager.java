package org.codedefenders.events;

import org.codedefenders.Role;
import org.codedefenders.User;
import org.codedefenders.util.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.ArrayList;

public class MessageManager extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(MessageManager.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String result = "{'status':'Error'}";
        response.setContentType("text/json");
        PrintWriter out = response.getWriter();
        try {
            if (canAccess(request)) {


                ArrayList<Event> events = null;

                int gameId =
                        Integer.parseInt(request.getParameter("gameId"));
                int userId = (int) request.getSession().getAttribute("uid");

                Role role = DatabaseAccess.getRole(userId, gameId);

                EventType eventType = EventType.valueOf(request.getParameter
                        ("target"));

                if ((eventType == EventType.ATTACKER_MESSAGE && role.equals
                        (Role.ATTACKER)) ||
                        (eventType == EventType.DEFENDER_MESSAGE && role
                                .equals(Role.DEFENDER))
                        || eventType == EventType.GAME_MESSAGE) {

                    if (eventType == EventType.GAME_MESSAGE) {
                        if (role.equals(Role.DEFENDER)) {
                            eventType = EventType.GAME_MESSAGE_DEFENDER;
                        } else if (role.equals(Role.ATTACKER)) {
                            eventType = EventType.GAME_MESSAGE_ATTACKER;
                        }
                    }

                    EventStatus es = EventStatus.GAME;

                    User u = DatabaseAccess.getUser(userId);

                    String message = request.getParameter("message");

                    Event e = new Event(0, gameId, userId,
                            u.getUsername() + ": " + message,
                            eventType, es, new Timestamp(System.currentTimeMillis()));

                    e.setChatMessage(DatabaseAccess.sanitise(message));

                    if (e.insert()) {
                        logger.info(String.format("Event %s saved in game %d.", eventType.toString(), gameId));
                        result = "{'status':'Success'}";
                    } else
                        logger.error(String.format("Problem saving event %s in game %d", eventType.toString(), gameId));
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        out.print(result);
        out.flush();
    }

    public boolean canAccess(HttpServletRequest request) {
        //TODO: Implement heavy load/DDOS handling
        if (request.getParameter("gameId") != null
                && request.getParameter("message") != null
                && request.getParameter("target") != null) {
            return true;
        }
        return false;
    }

}