/**
 * Copyright (C) 2016-2018 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.model;

import org.codedefenders.database.DB;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.DatabaseValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class User {

	private static final Logger logger = LoggerFactory.getLogger(User.class);

	private int id;
	private String username;
	private String encodedPassword;
	private String email;
	private boolean validated;
	private boolean active;

	public User(String username) {
		this(username, User.encodePassword(""));
	}

	public User(String username, String encodedPassword) {
		this(username, encodedPassword, "");
	}

	public User(String username, String encodedPassword, String email) {
		this(0, username, encodedPassword, email);
	}

	public User(int id, String username, String encodedPassword, String email) {
		this(id, username, encodedPassword, email, false, true);
	}

	public User(int id, String username, String encodedPassword, String email, boolean validated, boolean active) {
		this.id = id;
		this.username = username;
		this.encodedPassword = encodedPassword;
		this.email = email.toLowerCase();
		this.validated = validated;
		this.active = active;
	}

	public boolean insert() {
		// TODO Phil 12/12/18: Update this like Test#insert() to use DAO insert method but update identifier
		DatabaseValue[] valueList;
		String query;
		Connection conn = DB.getConnection();

		if (id <= 0) {
			query = "INSERT INTO users (Username, Password, Email) VALUES (?, ?, ?);";
			valueList = new DatabaseValue[]{DB.getDBV(username),
					DB.getDBV(encodedPassword),
					DB.getDBV(email)};
		} else {
			query = "INSERT INTO users (User_ID, Username, Password, Email) VALUES (?, ?, ?, ?);";
			valueList = new DatabaseValue[]{DB.getDBV(id),
					DB.getDBV(username),
					DB.getDBV(encodedPassword),
					DB.getDBV(email)};
		}
		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
		int key = DB.executeUpdateGetKeys(stmt, conn);
		if (key != -1) {
			this.id = key;
			return true;
		} else {
			return false;
		}
	}

	public boolean update() {
		DatabaseValue[] valueList;
		Connection conn = DB.getConnection();

		String query = "UPDATE users SET Username = ?, Email = ?, Password = ?, Validated = ?, Active = ? WHERE User_ID = ?;";
		valueList = new DatabaseValue[]{DB.getDBV(username),
				DB.getDBV(email),
				DB.getDBV(encodedPassword),
				DB.getDBV(validated),
				DB.getDBV(active),
				DB.getDBV(id)};
		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
		return DB.executeUpdate(stmt, conn);
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

	public String getEncodedPassword() {
		return encodedPassword;
	}

	public void setEncodedPassword(String encodedPassword) {
		this.encodedPassword = encodedPassword;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public void logSession(String ipAddress) {
		DatabaseAccess.logSession(id, ipAddress);
	}

	public String printFriendly(String color) {
		String username = getUsername();

		return "<span style='color: " + color + "'>@" + getUsername() +
				"</span>";
	}

	public static String encodePassword(String password) {
		return new BCryptPasswordEncoder().encode(password);
	}

	public static boolean passwordMatches(String rawPassword, String encodedPassword) {
		return new BCryptPasswordEncoder().matches(rawPassword, encodedPassword);
	}

}