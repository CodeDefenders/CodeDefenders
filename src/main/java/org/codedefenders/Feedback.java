package org.codedefenders;

import org.codedefenders.util.FeedbackDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Feedback extends HttpServlet {

	public final static int MAX_RATING = 5;
	public final static int MIN_RATING = -1;

	public enum FeedbackType {
		CUT_MUTATION_DIFFICULTY {
			public String toString() {
				return "The CUT is difficult to mutate";
			}
		}, CUT_TEST_DIFFICULTY {
			public String toString() {
				return "The CUT is difficult to test";
			}
		}, ATTACKER_COMPETENCE {
			public String toString() {
				return "The attacking Team is competent";
			}
		}, DEFENDER_COMPETENCE {
			public String toString() {
				return "The defending Team is competent";
			}
		}, ATTACKER_FAIRNESS {
			public String toString() {
				return "The attacking Team is playing fair";
			}
		}, DEFENDER_FAIRNESS {
			public String toString() {
				return "The defending Team is playing fair";
			}
		}, GAME_ENGAGING {
			public String toString() {
				return "The Game is engaging";
			}
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(Feedback.class);

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
