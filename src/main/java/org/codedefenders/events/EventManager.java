package org.codedefenders.events;

import com.google.gson.Gson;
import org.codedefenders.util.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class EventManager extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(EventManager.class);

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		try {
			if (!canAccess(request)) {
				logger.debug("Access denied");
				response.sendRedirect("/games/user");
			} else {
				logger.debug("Access granted");
				response.setContentType("text/json");

				PrintWriter out = response.getWriter();
				Gson gson = new Gson();
				long timestamp = Long.parseLong(request.getParameter
						("timestamp"));
				ArrayList<Event> events = null;

				int userId =
						(int) request.getSession().getAttribute("uid");

				if (request.getParameter("gameId") != null) {
					int gameId =
							Integer.parseInt(request.getParameter("gameId"));
					events = DatabaseAccess.getNewEventsForGame(
							gameId, timestamp, DatabaseAccess.getRole(userId,
									gameId)
					);
				} else {
					events = DatabaseAccess.getNewEventsForUser(userId,
							timestamp
					);
				}

				for (Event e : events){
					e.setCurrentUserName("@" + DatabaseAccess.getUser(userId).getUsername());
					if (e.getEventStatus().equals(EventStatus.GAME)) {
						e.parse(true);
					} else {
						e.parse(false);
					}
				}

				out.print(gson.toJson(events));
				out.flush();
			}
		} catch (Exception e){
			logger.error("Exception caught", e);
			response.sendRedirect("/games/user");
		}
	}

	public boolean canAccess(HttpServletRequest request){
		//TODO: Implement heavy load/DDOS handling
		if ((request.getParameter("gameId") != null ||
				request.getParameter("userId") != null)
				&& request.getParameter("timestamp") != null) {
			return true;
		}
		return false;
	}

}