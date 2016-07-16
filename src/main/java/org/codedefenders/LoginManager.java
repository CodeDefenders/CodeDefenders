package org.codedefenders;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;

public class LoginManager extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.LOGIN_VIEW_JSP);
		dispatcher.forward(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		ArrayList<String> messages = new ArrayList<String>();
		request.setAttribute("messages", messages);


		String username = (String) request.getParameter("username");
		String password = (String) request.getParameter("password");
		String email = (String) request.getParameter("email");
		String formType = (String) request.getParameter("formType");
		int uid;

		if (formType.equals("create")) {
			String confirm = (String) request.getParameter("confirm");
			if (password.equals(confirm)) {
				if (DatabaseAccess.getUserForName(username) == null) {
					User newUser = new User(username, password, email);
					if (newUser.insert()) {
						HttpSession session = request.getSession();
						session.setAttribute("uid", newUser.id);
						session.setAttribute("username", newUser.username);
						session.setAttribute("messages", messages);

						response.sendRedirect("games");
					} else {
						messages.add("Could not create a user for you, sorry!");
						RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.LOGIN_VIEW_JSP);
						dispatcher.forward(request, response);
					}
				} else {
					messages.add("Username Is Already Taken");
					RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.LOGIN_VIEW_JSP);
					dispatcher.forward(request, response);
				}
			} else {
				messages.add("Your Two Password Entries Did Not Match");
				RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.LOGIN_VIEW_JSP);
				dispatcher.forward(request, response);
			}
		} else if (formType.equals("login")) {
			User activeUser = DatabaseAccess.getUserForName(username);
			if (activeUser == null) {
				messages.add("User could not be retrieved from DB");
				RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.LOGIN_VIEW_JSP);
				dispatcher.forward(request, response);
			} else {
				String dbPassword = activeUser.password;
				BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
				if (passwordEncoder.matches(password, dbPassword)) {
					HttpSession session = request.getSession();
					DatabaseAccess.logSession(activeUser.id, getClientIpAddress(request));
					session.setAttribute("uid", activeUser.id);
					session.setAttribute("username", username);
					Object from = session.getAttribute("loginFrom");
					if (from != null && ! ((String) from).endsWith(".ico")
							&& ! ((String) from).endsWith(".css")
							&& ! ((String) from).endsWith(".js")) {
						response.sendRedirect((String) from);
					} else
						response.sendRedirect("games");
				} else {
					messages.add("Username Does Not Exist Or Your Password Was Incorrect");
					RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.LOGIN_VIEW_JSP);
					dispatcher.forward(request, response);
				}
			}
		}
	}

	public String getClientIpAddress(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		System.out.println("X-Forwarded-For: " + ip);
		if (invalidIP(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
			System.out.println("Proxy-Client-IP: " + ip);
		}
		if (invalidIP(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
			System.out.println("WL-Proxy-Client-IP: " + ip);
		}
		if (invalidIP(ip)) {
			ip = request.getHeader("HTTP_CLIENT_IP");
			System.out.println("HTTP_CLIENT_IP: " + ip);
		}
		if (invalidIP(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
			System.out.println("HTTP_X_FORWARDED_FOR: " + ip);
		}
		if (invalidIP(ip)) {
			ip = request.getRemoteAddr();
			System.out.println("getRemoteAddr(): " + ip);
		}
		return ip;
	}

	private boolean invalidIP(String ip) {
		return (ip == null) || (ip.length() == 0) ||
				("unknown".equalsIgnoreCase(ip)) || ("0:0:0:0:0:0:0:1".equals(ip));
	}
}