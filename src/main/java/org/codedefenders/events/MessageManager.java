package org.codedefenders.events;

import com.google.gson.Gson;
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

				EventType eventType = EventType.valueOf(request.getParameter
						("target"));

				if (eventType == EventType.ATTACKER_MESSAGE ||
						eventType == EventType.DEFENDER_MESSAGE) {

					EventStatus es = EventStatus.GAME;

					User u = DatabaseAccess.getUser(userId);

					String message = request.getParameter("message");

					Event e = new Event(0, gameId, DatabaseAccess
							.getPlayerIdForMultiplayerGame(userId, gameId),
							u.getUsername() + ": " + message,
							eventType, es, new Timestamp(System.currentTimeMillis()));

					if (e.insert()){
						result = "{'status':'Success'}";
					}
				}

			}
		} catch (Exception e){
			e.printStackTrace();
		}

		out.print(result);
		out.flush();
	}

	public boolean canAccess(HttpServletRequest request){
		//TODO: Implement heavy load/DDOS handling
		if (request.getParameter("gameId") != null
				&& request.getParameter("message") != null
				&& request.getParameter("target") != null) {
			return true;
		}
		return false;
	}

}