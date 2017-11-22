package org.codedefenders;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.codedefenders.util.DB;
import org.codedefenders.util.DatabaseAccess;
import org.codedefenders.util.DatabaseValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
		DatabaseValue[] valueList;
		String query;
		Connection conn = DB.getConnection();
		logger.debug("Calling BCryptPasswordEncoder.encode");
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		String safePassword = passwordEncoder.encode(password);

		if (id <= 0) {
			query = "INSERT INTO users (Username, Password, Email) VALUES (?, ?, ?);";
			valueList = new DatabaseValue[]{DB.getDBV(username),
					DB.getDBV(safePassword),
					DB.getDBV(email)};
		} else {
			query = "INSERT INTO users (User_ID, Username, Password, Email) VALUES (?, ?, ?, ?);";
			valueList = new DatabaseValue[]{DB.getDBV(id),
					DB.getDBV(username),
					DB.getDBV(safePassword),
					DB.getDBV(email)};
		}
		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
		int key = DB.executeUpdateGetKeys(stmt, conn);
		if( key != -1 ){
			this.id = key;
			return true;
		} else {
			return false;
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