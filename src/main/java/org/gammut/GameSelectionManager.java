package org.gammut;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class GameSelectionManager extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		response.sendRedirect("games/user");
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		HttpSession session = request.getSession();
		// Get their user id from the session.
		int uid = (Integer) session.getAttribute("uid");
		int gameId;

		switch (request.getParameter("formType")) {

			case "createGame":

				// Get the identifying information required to create a game from the submitted form.
				int classId = Integer.parseInt((String) request.getParameter("class"));
				int rounds = Integer.parseInt((String) request.getParameter("rounds"));
				String role = request.getParameter("role") == null ? "DEFENDER" : "ATTACKER";
				Game.Level level = request.getParameter("level") == null ? Game.Level.HARD : Game.Level.EASY;

				// Create the game with supplied parameters and insert it in the database.
				Game nGame = new Game(classId, uid, rounds, role, level);
				nGame.insert();

				// Redirect to the game selection menu.
				response.sendRedirect("games");

				break;

			case "joinGame":

				// Get the identifying information required to create a game from the submitted form.
				gameId = Integer.parseInt((String) request.getParameter("game"));

				Game jGame = DatabaseAccess.getGameForKey("Game_ID", gameId);

				if (jGame.getAttackerId() == 0) {
					jGame.setAttackerId(uid);
				} else {
					jGame.setDefenderId(uid);
				}

				jGame.setState("IN PROGRESS");
				jGame.setActivePlayer("ATTACKER");

				jGame.update();

				session.setAttribute("gid", gameId);

				response.sendRedirect("play");

				break;

			case "enterGame":

				gameId = Integer.parseInt((String) request.getParameter("game"));
				Game eGame = DatabaseAccess.getGameForKey("Game_ID", gameId);

				if (eGame.isUserInGame(uid)) {

					session.setAttribute("gid", gameId);

					response.sendRedirect("play");
				} else {
					response.sendRedirect(request.getHeader("referer"));
				}

				break;
		}
	}
}