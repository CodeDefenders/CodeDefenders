package org.codedefenders.events;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.TargetExecution;
import org.codedefenders.util.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class EventManager extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(EventManager.class);

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		try {
			if (!canAccess(request)) {
				logger.debug("Access denied");
				response.setStatus(403);
			} else {
				Gson gson = new Gson();

				logger.debug("Access granted");
				response.setContentType("text/json");

				int userId = (int) request.getSession().getAttribute("uid");

				if (request.getParameter("progressBar") != null) {

					TargetExecution.Target status = null;
					int gameId = -1;
					int lastTestId = -1;
					userId = -1;

					if (request.getParameter("gameId") == null) {
						response.setStatus(400);
					} else {
						gameId = Integer.parseInt(request.getParameter("gameId"));

						if (request.getParameter("userId") == null) {
							response.setStatus(400);
						} else {
							userId = Integer.parseInt(request.getParameter("userId"));

							// Check if we have any data on the
							// lastSubmittedTest
							if (request.getSession().getAttribute("lastTest") != null ) {
								lastTestId = (int) request.getSession().getAttribute("lastTest");
							} else {
								// Retrieve from the DB, it might be also -1 -
								// none
								lastTestId = DatabaseAccess.getLastCompletedTestForUserInGame(userId, gameId);
								request.getSession().setAttribute("lastTest", lastTestId);
							}

							PrintWriter out = response.getWriter();

							status = DatabaseAccess.getStatusOfRequestForUserInGame(userId, gameId, lastTestId);

							ArrayList<TargetExecution.Target> progressBarUpdates = new ArrayList<>();
							
							if (status != null) {
								progressBarUpdates.add( status );
							}

							out.print(gson.toJson(progressBarUpdates));
							out.flush();
						}
					}
				} else {
					PrintWriter out = response.getWriter();
					long timestamp = Long.parseLong(request.getParameter("timestamp"));
					ArrayList<Event> events = null;

					if (request.getParameter("gameId") != null) {
						int gameId = Integer.parseInt(request.getParameter("gameId"));
						events = DatabaseAccess.getNewEventsForGame(gameId, timestamp,
								DatabaseAccess.getRole(userId, gameId));
					} else {
						events = DatabaseAccess.getNewEventsForUser(userId, timestamp);
					}

					for (Event e : events) {
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
			}
		} catch (Exception e) {
			logger.error("Exception caught", e);
			response.setStatus(500);
			// Do not redirect this request, it's from game_notification !
			// response.sendRedirect(contextPath + "/games/user");
		}
	}

	public boolean canAccess(HttpServletRequest request) {
		// TODO: Implement heavy load/DDOS handling
		if (request.getSession().getAttribute("uid") == null) {
			return false;
		}
		if ((request.getParameter("gameId") != null && request.getParameter("userId") != null
				&& request.getParameter("progressBar") != null)
				|| ((request.getParameter("gameId") != null || request.getParameter("userId") != null)
						&& request.getParameter("timestamp") != null)) {
			return true;
		}
		return false;
	}

}