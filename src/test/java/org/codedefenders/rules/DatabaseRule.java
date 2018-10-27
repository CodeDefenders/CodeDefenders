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
package org.codedefenders.rules;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.rules.ExternalResource;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;

/**
 * @author Jose Rojas, Alessio Gambi
 */
public class DatabaseRule extends ExternalResource {

	// These ensures we do not mess up with the timeZone problem during integration testing
	private static final String[] DEFAULT_OPTIONS = new String[] { "useUnicode=true",
			"useJDBCCompliantTimezoneShift=true", "useLegacyDatetimeCode=false", "serverTimezone=UTC" };

	private DB db;
	protected DBConfigurationBuilder config;

	private String dbName;
	private String username;
	private String password;
	private String initFile;
	private String connectionOptions;

	public String getDbName() {
		return dbName;

	}

	public DatabaseRule(String dbName, String initFile, String[] options) {
		this(dbName, "root", "", initFile, options);
	}

	public DatabaseRule(String dbName, String initFile) {
		this(dbName, "root", "", initFile, DEFAULT_OPTIONS);
	}

	public DatabaseRule(String dbName, String username, String password, String initFile, String[] connectionOptions) {
		this.dbName = dbName;
		this.initFile = initFile;
		StringBuffer sb = new StringBuffer();
		if (connectionOptions != null && connectionOptions.length > 0) {
			sb.append("?");
			for (String option : connectionOptions) {
				sb.append(option).append("&");
			}
			// Remove trailing "&"
			sb.deleteCharAt(sb.lastIndexOf("&"));
		}
		this.connectionOptions = sb.toString();
	}

	@Override
	public void before() throws Exception {
		config = DBConfigurationBuilder.newBuilder();
		config.setPort(0); // 0 => autom. detect free port
		db = DB.newEmbeddedDB(config.build());
		db.start();
		//
		db.createDB(dbName);
		//
		db.source(initFile, username, password, dbName);
		//
	}

	@Override
	public void after() {
		try {
			db.stop();
		} catch (ManagedProcessException e) {
			// quiet
		}
	}

	// we can add additional connectionOptions to the URL as
	// parameters:+"[?][parameter=value[&parameter=value]]"
	// for example, to return the updated query - not the matched ones
	// +"?""useAffectedRows=true"
	public Connection getConnection() throws SQLException {
		String connectionURL = config.getURL(dbName) + connectionOptions;
		return DriverManager.getConnection(connectionURL, username, password);
	}
}
