package org.codedefenders;

import org.codedefenders.util.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.*;

public class User {

	private static final Logger logger = LoggerFactory.getLogger(User.class);

	private int id;
	private String username;
	private String password;
	private String email;
	private boolean validated;

	public User(String username, String password) {
		this(username, password, "");
	}

	public User(String username, String password, String email) {
		this(0, username, password, email);
	}

	public User(int id, String username, String password, String email) {
		this(id, username, password, email, false);
	}

	public User(int id, String username, String password, String email, boolean validated) {
		this.id = id;
		this.username = username;
		this.password = password;
		this.email = email.toLowerCase();
		this.validated = validated;
	}

	public boolean insert() {

		Connection conn = null;
		PreparedStatement stmt = null;
		int res = -1;

		try {
			conn = DatabaseAccess.getConnection();

			logger.debug("Calling BCryptPasswordEncoder.encode");
			BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
			String safePassword = passwordEncoder.encode(password);

			if (id <= 0) {
				stmt = conn.prepareStatement("INSERT INTO users (Username, Password, Email) VALUES (?, ?, ?);");
				stmt.setString(1, username);
				stmt.setString(2, safePassword);
				stmt.setString(3, email);
			} else {
				stmt = conn.prepareStatement("INSERT INTO users (User_ID, Username, Password, Email) VALUES (?, ?, ?, ?);");
				stmt.setInt(1, id);
				stmt.setString(2, username);
				stmt.setString(3, safePassword);
				stmt.setString(4, email);
			}

			return stmt.executeUpdate() > 0;
		} catch (SQLException se) {
			logger.error("SQL exception caught", se);
			return false;
		} catch (Exception e) {
			logger.error("Exception caught", e);
			return false;
		} finally {
			DatabaseAccess.cleanup(conn, stmt);
		}
	}

	public boolean isValidated() {
		return validated;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void logSession(String ipAddress) {
		DatabaseAccess.logSession(id, ipAddress);
	}

	public String printFriendly(String color){
		String username = getUsername();

		return "<span style='color: " + color + "'>@" + getUsername() +
		"</span>";
	}

}