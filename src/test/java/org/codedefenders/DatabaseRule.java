package org.codedefenders;

import java.awt.dnd.DnDConstants;
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

	private DB db;
	protected DBConfigurationBuilder config;

	private String dbName;
	private String username;
	private String password;
	private String initFile;
	
	public String getDbName() {
		return dbName;
	
	}
	public DatabaseRule(String dbName, String initFile) {
		this(dbName, "root", "", initFile);
	}
	
	public DatabaseRule(String dbName, String username, String password, String initFile) {
		this.dbName= dbName;
		this.initFile = initFile;
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
	}

	@Override
	public void after() {
		try {
			db.stop();
		} catch (ManagedProcessException e) {
			// quiet
		}
	}
	public Connection getConnection() throws SQLException {
		return DriverManager.getConnection(config.getURL(dbName), username, password);
	}
}
