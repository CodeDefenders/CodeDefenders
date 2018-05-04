package org.codedefenders;

import org.apache.commons.lang.math.IntRange;
import org.codedefenders.util.AdminDAO;
import org.codedefenders.util.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * This {@link HttpServlet} handles admin requests for managing {@link User}s.
 * <p>
 * Serves on path: `/admin/users`.
 */
public class AdminUserManagement extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(AdminUserManagement.class);

	static final char[] LOWER = "abcdefghijklmnopqrstuvwxyz".toCharArray();
	static final char[] DIGITS = "0123456789".toCharArray();

	private static final char[] PUNCTUATION = "!@#$%&*()_+-=[]|,./?><".toCharArray();
	private static final String NEW_ACCOUNT_MSG = "Welcome to Code Defenders! \n\n " +
			"An account has been created for you with Username %s and Password %s.\n" +
			"You can log int at %s. \n\n Happy coding!";
	private static final String EMAIL_NOT_SPECIFIED_DOMAIN = "@NOT.SPECIFIED";
	private static final String PASSWORD_RESET_MSG = "%s, \n\n" +
			"your password has been reset to %s\n" +
			"Please change it at your next convenience.";

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		response.sendRedirect(request.getContextPath() + "/" + Constants.ADMIN_USER_JSP);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		final HttpSession session = request.getSession();
		final ArrayList<String> messages = new ArrayList<>();
		session.setAttribute("messages", messages);
		String responsePath = request.getContextPath() + "/admin/users";

		final String formType = request.getParameter("formType");
		switch (formType) {
			case "manageUsers":
				String userToResetIdString = request.getParameter("resetPasswordButton");
				String userToDeleteIdString = request.getParameter("deleteUserButton");
				String userToEditIdString = request.getParameter("editUserInfo");
				if (userToResetIdString != null) {
					messages.add(resetUserPW(Integer.parseInt(userToResetIdString)));
				} else if (userToDeleteIdString != null) {
					messages.add(deleteUser(Integer.parseInt(userToDeleteIdString)));
				} else if (userToEditIdString != null) {
					responsePath = request.getContextPath() + "/" + Constants.ADMIN_USER_JSP + "?editUser=" + userToEditIdString;
				}
				break;
			case "createUsers":
				final String userList = request.getParameter("user_name_list");
				if (userList == null) {
					logger.error("Creating users failed. Missing parameter 'user_name_list'");
				} else {
					logger.info("Creating users....");
					createUserAccounts(request, userList, messages);
					logger.info("Creating users succeeded.");
				}
				break;
			case "editUser":
				String uidString = request.getParameter("uid");
				String successMsg = "Successfully updated info for User " + uidString;
				String msg = editUser(uidString, request, successMsg);
				messages.add(msg);
				if (!msg.equals(successMsg)) {
					responsePath = request.getContextPath() + "/" + Constants.ADMIN_USER_JSP + "?editUser=" + uidString;
				}
				break;
			default:
				logger.error("Action {" + formType + "} not recognised.");
				break;
		}

		response.sendRedirect(responsePath);
	}

	private String editUser(String uid, HttpServletRequest request, String successMsg) {
		User u = DatabaseAccess.getUser(Integer.parseInt(uid));
		if (u == null)
			return "Error. User " + uid + " cannot be retrieved from database.";

		String name = request.getParameter("name");
		String email = request.getParameter("email");
		String password = request.getParameter("password");
		String confirm_password = request.getParameter("confirm_password");

		if (!password.equals(confirm_password))
			return "Error! Passwords don't match!";

		if (!name.equals(u.getUsername()) && DatabaseAccess.getUserForName(name) != null)
			return "Username " + name + " is already taken";

		if (!email.equals(u.getEmail()) && DatabaseAccess.getUserForEmail(email) != null)
			return "Email " + email + " is already in use!";

		if (!LoginManager.validEmailAddress(email))
			return "Email Address is not valid";

		if (!password.equals("")) {
			// we don't want to encode the already encoded password from the DB
			if (!LoginManager.validPassword(password))
				return "Password is not valid";
			BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
			password = passwordEncoder.encode(password);
		}
		u.setUsername(name);
		u.setEmail(email);

		if (!u.update(password))
			return "Error trying to update info for user " + uid + "!";
		return successMsg;
	}

	private void createUserAccounts(HttpServletRequest request, String userNameListString, List<String> messages) {
		final String[] lines = userNameListString.split(AdminCreateGames.USER_NAME_LIST_DELIMITER);

		final boolean sendMail = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.EMAILS_ENABLED).getBoolValue();
		final String hostAddress = request.getRequestURL().toString();

		for (String credentials : lines) {
			createUserAccount(credentials.trim(), messages, sendMail, hostAddress);
		}
	}

	/**
	 * Creates a user for a given string, which has to be formatted like:
	 * <p>
	 * {@code username,password}
	 * <p>
     * {@code username,password,email}
	 *
	 * Values can be separated by either ',' or ';'.
	 */
	private void createUserAccount(String userCredentials, List<String> messages, boolean sendMail, String hostAddress) {
		// credentials have following form: username, password, email (optional)
		final String[] credentials = userCredentials.split("[,;]+");
		if (credentials.length < 2) {
			logger.info("Failed to create user due to not enough arguments:" + credentials.length);
			messages.add("Please provide at least username and password");
			return;
		} else if (credentials.length > 3) {
			logger.info("Failed to create user due to too many arguments:" + credentials.length);
			messages.add("Please provide at maximum username,password and email");
			return;
		}

		final String username = credentials[0].trim();
		if (DatabaseAccess.getUserForName(username) != null) {
		    logger.info("Failed to create user. Username already in use:" + username);
			messages.add("Username '" + username + "' already in use.");
			return;
		}
		if(!LoginManager.validUsername(username)) {
			logger.info("Failed to create user. Username invalid:" + username);
			messages.add("Username '" + username + "' invalid, user not created");
			return;
		}

		final String password = credentials[1].trim();
		if(!LoginManager.validPassword(password)) {
			logger.info("Failed to create user. Password invalid:" + password);
			messages.add("Password for user "+ username +" invalid, user not created");
			return;
		}

		final String email;

		final boolean hasMail = credentials.length == 3;
		if (hasMail) {
			email = credentials[2].trim();
			if (DatabaseAccess.getUserForEmail(email) != null) {
				logger.info("Failed to create user. Email address already in use:" + email);
				messages.add("Email '" + email + "' already in use.");
				return;
			}
		} else {
			email = username + EMAIL_NOT_SPECIFIED_DOMAIN;
		}

		final User user = new User(username, password, email);
		final boolean createSuccess = user.insert();

		if (!createSuccess) {
			final String errorMsg = "Failed to create account for user '" + username + "'";
			logger.error(errorMsg);
			messages.add(errorMsg);
		} else {
			messages.add("Created user "+ username + (hasMail ? " ("+email+")" : ""));
			logger.info("Successfully created account for user '" + username + "'");
			if (hasMail && sendMail) {
				final boolean mailSuccess = sendNewAccountMsg(email, username, password, hostAddress);
				if (!mailSuccess) {
					messages.add("Could not send email to user " + username + " with email " + email);
					logger.error("Failed to send account creation mail to user " + username + "<" + email + ">");
				} else {
					logger.info("Successfully sent account creation mail to user " + username + "<" + email + ">");
				}
			}
		}
	}

	private boolean sendNewAccountMsg(String email, String name, String password, String hostAddr) {
		String message = String.format(NEW_ACCOUNT_MSG, name, password, hostAddr);
		return EmailUtils.sendEmail(email, "Your Code Defenders Account", message);
	}

	private String deleteUser(int uid) {
		return "Currently disabled!";
		//return (AdminDAO.deleteUser(uid) ? "Successfully deleted user " : "Error trying to delete user ") + uid + "!";
	}

	private String resetUserPW(int uid) {

		String newPassword = generatePW();
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

		if (AdminDAO.setUserPassword(uid, passwordEncoder.encode(newPassword))) {
			if (AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.EMAILS_ENABLED).getBoolValue()) {
				User u = DatabaseAccess.getUser(uid);
				String msg = String.format(PASSWORD_RESET_MSG, u.getUsername(), newPassword);
				EmailUtils.sendEmail(u.getEmail(), "Code Defenders Password reset", msg);
			}
			return "User " + uid + "'s password set to: " + newPassword;
		}
		return "Could not reset password for user " + uid;
	}

	private static String generatePW() {
		int length = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.MIN_PASSWORD_LENGTH).getIntValue();

		StringBuilder sb = new StringBuilder();
		char[] initialSet = LOWER;

		Random random = new Random();
		for (int i = 0; i < length; i++) {
			sb.append(initialSet[random.nextInt(initialSet.length)]);
		}
		char[] resultChars = sb.toString().toCharArray();

		List<Integer> randomInts = Arrays.stream(new IntRange(0, length - 1).toArray()).boxed().collect(Collectors.toList());
		Collections.shuffle(randomInts);

		int c = 0;
		resultChars[randomInts.get(c)] = Character.toUpperCase(resultChars[randomInts.get(c)]);
		resultChars[randomInts.get(++c)] = PUNCTUATION[random.nextInt(PUNCTUATION.length)];
		resultChars[randomInts.get(++c)] = DIGITS[random.nextInt(DIGITS.length)];

		return new String(resultChars);
	}
}