package org.codedefenders;

import org.apache.commons.lang.math.IntRange;
import org.codedefenders.util.AdminDAO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class AdminUserMgmt extends HttpServlet {

	private static final char[] LOWER = "abcdefghijklmnopqrstuvwxyz".toCharArray();
	private static final char[] DIGITS = "0123456789".toCharArray();
	private static final char[] PUNCTUATION = "!@#$%&*()_+-=[]|,./?><".toCharArray();
	private static final int PASSWORD_LENGTH = 8;

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		response.sendRedirect(request.getContextPath() + "/" + Constants.ADMIN_USER_JSP);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		HttpSession session = request.getSession();
		// Get their user id from the session.
		int currentUserID = (Integer) session.getAttribute("uid");
		ArrayList<String> messages = new ArrayList<String>();
		session.setAttribute("messages", messages);

		switch (request.getParameter("formType")) {
			case "manageUsers":
				String userToResetIdString = request.getParameter("resetPasswordButton");
				String userToDeleteIdString = request.getParameter("deleteUserButton");
				if(userToResetIdString != null) {
					messages.add(resetUserPW(Integer.parseInt(userToResetIdString)));
				} else if(userToDeleteIdString != null) {
					messages.add(deleteUser(Integer.parseInt(userToDeleteIdString)));
				}
				break;

			default:
				System.err.println("Action not recognised");
				break;
		}

		response.sendRedirect(request.getContextPath() + "/admin/users");
	}

	private String deleteUser(int uid) {
		return "not implemented yet";
	}

	private String resetUserPW(int uid) {

		String newPassword = generatePW();
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		boolean success = AdminDAO.setUserPassword(uid, passwordEncoder.encode(newPassword));

		return success ? "User " + uid + "'s password set to: " + newPassword : "Could not reset password for user " + uid;
	}

	private String generatePW() {

		StringBuilder sb = new StringBuilder();
		char[] initialSet = LOWER;

		Random random = new Random();
		for (int i= 0; i < PASSWORD_LENGTH; i++) {
			sb.append(initialSet[random.nextInt(initialSet.length)]);
		}
		char[] resultChars = sb.toString().toCharArray();

		List<Integer> randomInts = Arrays.stream(new IntRange(0, PASSWORD_LENGTH-1).toArray()).boxed().collect(Collectors.toList());
		Collections.shuffle(randomInts);

		int c = 0;
		resultChars[randomInts.get(c)] = Character.toUpperCase(resultChars[randomInts.get(c)]);
		resultChars[randomInts.get(++c)] = PUNCTUATION[random.nextInt(PUNCTUATION.length)];
		resultChars[randomInts.get(++c)] = DIGITS[random.nextInt(DIGITS.length)];

		return new String(resultChars);
	}

}