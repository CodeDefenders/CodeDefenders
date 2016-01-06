package org.gammut;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;

import static org.gammut.Constants.LOGIN_VIEW_JSP;

public class LoginManager extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		RequestDispatcher dispatcher = request.getRequestDispatcher(LOGIN_VIEW_JSP);
		dispatcher.forward(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		ArrayList<String> messages = new ArrayList<String>();
		request.setAttribute("messages", messages);


		String username = (String) request.getParameter("username");
		String password = (String) request.getParameter("password");
		String formType = (String) request.getParameter("formType");
		int uid;

		if (formType.equals("create")) {
			String confirm = (String) request.getParameter("confirm");
			if (password.equals(confirm)) {
				if (DatabaseAccess.getUserForName(username) == null) {
					User newUser = new User(username, password);
					newUser.insert();

					HttpSession session = request.getSession();
					session.setAttribute("uid", newUser.id);
					session.setAttribute("username", newUser.username);
					session.setAttribute("messages", messages);

					session.setMaxInactiveInterval(1200);

					response.sendRedirect("games");
				} else {
					messages.add("Username Is Already Taken");
					RequestDispatcher dispatcher = request.getRequestDispatcher(LOGIN_VIEW_JSP);
					dispatcher.forward(request, response);
				}
			} else {
				messages.add("Your Two Password Entries Did Not Match");
				RequestDispatcher dispatcher = request.getRequestDispatcher(LOGIN_VIEW_JSP);
				dispatcher.forward(request, response);
			}
		} else if (formType.equals("login")) {
			User activeUser = DatabaseAccess.getUserForName(username);

			if ((activeUser != null) && (activeUser.password.equals(password))) {
				HttpSession session = request.getSession();
				session.setAttribute("uid", activeUser.id);
				session.setAttribute("username", username);
				session.setMaxInactiveInterval(1200);

				response.sendRedirect("games");
			} else {
				messages.add("Username Does Not Exist Or Your Password Was Incorrect");
				RequestDispatcher dispatcher = request.getRequestDispatcher(LOGIN_VIEW_JSP);
				dispatcher.forward(request, response);
			}
		} else if (formType.equals("logOut")) {

			HttpSession session = request.getSession();
			session.invalidate();

			messages.add("Successfully Logged Out");
			RequestDispatcher dispatcher = request.getRequestDispatcher(LOGIN_VIEW_JSP);
			dispatcher.forward(request, response);
		} else {
			messages.add("POST To LoginManager Didn't Supply A FormType Somehow");
			RequestDispatcher dispatcher = request.getRequestDispatcher(LOGIN_VIEW_JSP);
			dispatcher.forward(request, response);
		}
	}
}