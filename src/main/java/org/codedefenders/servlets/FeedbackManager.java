/**
 * Copyright (C) 2016-2018 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.servlets;

import org.codedefenders.database.FeedbackDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class FeedbackManager extends HttpServlet {

	public final static int MAX_RATING = 5;
	public final static int MIN_RATING = -1;

	public enum FeedbackType {
		CUT_MUTATION_DIFFICULTY {
			public String toString() {
				return "The class under test is difficult to mutate";
			}
		}, CUT_TEST_DIFFICULTY {
			public String toString() {
				return "The class under test is difficult to test";
			}
		}, ATTACKER_COMPETENCE {
			public String toString() {
				return "The attacking team is competent";
			}
		}, DEFENDER_COMPETENCE {
			public String toString() {
				return "The defending team is competent";
			}
		}, ATTACKER_FAIRNESS {
			public String toString() {
				return "The attacking team is playing fair";
			}
		}, DEFENDER_FAIRNESS {
			public String toString() {
				return "The defending team is playing fair";
			}
		}, GAME_ENGAGING {
			public String toString() {
				return "The game is engaging";
			}
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(FeedbackManager.class);

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		ArrayList<String> messages = new ArrayList<String>();
		HttpSession session = request.getSession();
		int uid = (Integer) session.getAttribute("uid");

		int gameId = -1;
		if (session.getAttribute("mpGameId") != null) {
			gameId = (Integer) session.getAttribute("mpGameId");
		} else if (request.getParameter("mpGameID") != null) {
			gameId = Integer.parseInt(request.getParameter("mpGameID"));
			session.setAttribute("mpGameId", gameId);
		} else {
			// TODO Not sure this is 100% right
			logger.error("Problem setting gameID !");
			response.setStatus(500);
			return;
		}
		session.setAttribute("messages", messages);

		String contextPath = request.getContextPath();

		switch (request.getParameter("formType")) {
			case "sendFeedback":
				if (!saveFeedback(request, uid, gameId))
					messages.add("Could not save your feedback. Please try again later!");
		}

		response.sendRedirect(request.getHeader("referer"));
	}

	private boolean saveFeedback(HttpServletRequest request, int uid, int gid) {
		List ratingsList = new ArrayList<Integer>();
		for (FeedbackType f : FeedbackType.values()) {
			String rating = request.getParameter("rating" + f.name());
			ratingsList.add(rating == null ? 0 : Integer.parseInt(rating));
		}
		if   (FeedbackDAO.hasNotRated(gid, uid))
			return FeedbackDAO.insertFeedback(gid, uid, ratingsList);
		return FeedbackDAO.updateFeedback(gid, uid, ratingsList);
	}

}
