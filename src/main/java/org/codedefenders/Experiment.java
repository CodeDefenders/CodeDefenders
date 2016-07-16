package org.codedefenders;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by jmr on 04/05/2016.
 */
public class Experiment extends HttpServlet {


	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		RequestDispatcher dispatcher = request.getRequestDispatcher("jsp" + Constants.F_SEP + "experiment_view.jsp");
		dispatcher.forward(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		ArrayList<String> userList = new ArrayList<String>();
		request.setAttribute("userList", userList);
		ArrayList<String> errors = new ArrayList<String>();
		request.setAttribute("errors", errors);

		int minUserID = Integer.parseInt(request.getParameter("minUserID"));
		int maxUserID = Integer.parseInt(request.getParameter("maxUserID"));
		String formType = (String) request.getParameter("formType");

		HttpSession session = request.getSession();
		session.setAttribute("userList", userList);
		session.setAttribute("errors", errors);
		RandomString randomString = new RandomString(8);
		if (formType.equals("createUsers")) {
			for (int id=minUserID; id<=maxUserID; id++) {
				String username = "hs"+id;
				String password = randomString.nextString();
				User newUser = new User(id, username, password);
				if (newUser.insert()) {
					userList.add("<tr><td>" + id + "</td><td>" + username + "</td><td>" + password + "</td></tr>");
				} else {
					errors.add("<tr><td>" + id + "</td><td>" + username + "</td><td>ERROR!</td></tr>");
				}
			}
			response.sendRedirect("experiment");
		}
	}
}

class RandomString {

	private static final char[] symbols;
	private final Random random = new Random();
	private final char[] buf;

	static {
		StringBuilder tmp = new StringBuilder();
		for (char ch = '0'; ch <= '9'; ++ch)
			tmp.append(ch);
		for (char ch = 'a'; ch <= 'z'; ++ch)
			tmp.append(ch);
		symbols = tmp.toString().toCharArray();
	}

	public RandomString(int length) {
		if (length < 1)
			throw new IllegalArgumentException("length < 1: " + length);
		buf = new char[length];
	}

	public String nextString() {
		for (int idx = 0; idx < buf.length; ++idx)
			buf[idx] = symbols[random.nextInt(symbols.length)];
		return new String(buf);
	}
}
