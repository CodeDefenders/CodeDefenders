package org.codedefenders;

import org.codedefenders.util.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;

public class LoginManager extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(LoginManager.class);

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.LOGIN_VIEW_JSP);
		dispatcher.forward(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		ArrayList<String> messages = new ArrayList<String>();
		request.getSession().setAttribute("messages", messages);


		String username = (String) request.getParameter("username");
		String password = (String) request.getParameter("password");
		String email = (String) request.getParameter("email");
		String formType = (String) request.getParameter("formType");
		int uid;

		if (formType.equals("create")) {
			String confirm = (String) request.getParameter("confirm");
			if (! (validUsername(username)
					&& validEmailAddress(email)
					&& validPassword(password))) {
				messages.add("User not created. Make sure username, password and email are valid.");
				RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.LOGIN_VIEW_JSP);
				dispatcher.forward(request, response);
			} else if (password.equals(confirm)) {
				if (DatabaseAccess.getUserForNameOrEmail(username) == null) {
					User newUser = new User(username, password, email);
					if (newUser.insert()) {
						HttpSession session = request.getSession();
						session.setAttribute("uid", newUser.getId());
						session.setAttribute("username", newUser.getUsername());
						session.setAttribute("messages", messages);

						response.sendRedirect("games");
					} else {
						messages.add("Could not create a user for you, sorry!");
						RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.LOGIN_VIEW_JSP);
						dispatcher.forward(request, response);
					}
				} else {
					messages.add("Username is already taken or Email has already been used");
					RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.LOGIN_VIEW_JSP);
					dispatcher.forward(request, response);
				}
			} else {
				messages.add("Your Two Password Entries Did Not Match");
				RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.LOGIN_VIEW_JSP);
				dispatcher.forward(request, response);
			}
		} else if (formType.equals("login")) {
			User activeUser = DatabaseAccess.getUserForNameOrEmail(username);
			if (activeUser == null) {
				messages.add("User could not be retrieved from DB");
				RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.LOGIN_VIEW_JSP);
				dispatcher.forward(request, response);
			} else {
				String dbPassword = activeUser.getPassword();
				BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
				if (passwordEncoder.matches(password, dbPassword)) {
					HttpSession session = request.getSession();
					DatabaseAccess.logSession(activeUser.getId(), getClientIpAddress(request));
					session.setAttribute("uid", activeUser.getId());
					session.setAttribute("username", activeUser.getUsername());
					Object from = session.getAttribute("loginFrom");
					if (from != null && ! ((String) from).endsWith(".ico")
							&& ! ((String) from).endsWith(".css")
							&& ! ((String) from).endsWith(".js")) {
						response.sendRedirect((String) from);
					} else
						response.sendRedirect("games");
				} else {
					messages.add("Username does not exist or your password ws incorrect.");
					RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.LOGIN_VIEW_JSP);
					dispatcher.forward(request, response);
				}
			}
		}
	}

	public String getClientIpAddress(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		if (invalidIP(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (invalidIP(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (invalidIP(ip)) {
			ip = request.getHeader("HTTP_CLIENT_IP");
		}
		if (invalidIP(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
		}
		if (invalidIP(ip)) {
			ip = request.getRemoteAddr();
		}
		logger.debug("Client IP: " + ip);
		return ip;
	}

	private boolean invalidIP(String ip) {
		return (ip == null) || (ip.length() == 0) ||
				("unknown".equalsIgnoreCase(ip)) || ("0:0:0:0:0:0:0:1".equals(ip));
	}

	/**
	 * Username must contain 3 to 20 alphanumeric characters,
	 * start with an alphabetic character, and have no whitespace or special character.
	 * @param username
	 * @return true iff valid username
	 */

	public static boolean validUsername(String username) {
		String pattern = "^[a-zA-Z][a-zA-Z0-9]{2,19}$";
		return username != null && username.matches(pattern);
	}

	public static boolean validEmailAddress(String email) {
		if (email == null) return false;
		boolean result = true;
		try {
			InternetAddress emailAddr = new InternetAddress(email);
			emailAddr.validate();
		} catch (AddressException ex) {
			result = false;
		}
		return result;
	}

	/**
	 * Password must contain 3 to 20 alphanumeric characters,
	 * with no whitespace or special character.
	*/
	public static boolean validPassword(String password) {
		// 3-10 alphanumeric characters (a-z, A-Z, 0-9) (no whitespaces)
		String pattern = "^[a-zA-Z0-9]{3,10}$";
		return password != null && password.matches(pattern);
	}
}