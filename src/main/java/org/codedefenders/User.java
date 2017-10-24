package org.codedefenders;

import org.codedefenders.util.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
		Statement stmt = null;
		String sql = null;

		try {
			conn = DatabaseAccess.getConnection();

			stmt = conn.createStatement();
			logger.debug("Calling BCryptPasswordEncoder.encode");
			BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
			String safePassword = passwordEncoder.encode(password);

			if (id <= 0)
				sql = String.format("INSERT INTO users (Username, Password, Email) VALUES ('%s', '%s', '%s');", username, safePassword, email);
			else
				sql = String.format("INSERT INTO users (User_ID, Username, Password, Email) VALUES (%d, '%s', '%s', '%s');", id, username, safePassword, email);

			stmt.execute(sql, Statement.RETURN_GENERATED_KEYS);

			ResultSet rs = stmt.getGeneratedKeys();

			if (rs.next()) {
				id = rs.getInt(1);
				stmt.close();
				conn.close();
				return true;
			}
		} catch (SQLException se) {
			logger.error("SQL exception caught", se);
		} catch (Exception e) {
			logger.error("Exception caught", e);
		} finally {
			DatabaseAccess.cleanup(conn, stmt);
		}
		return false;
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
		return "<span style='color: " + color + "'>@" + getUsername() +
		"</span>";
	}

}