package org.codedefenders.rules;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.rules.ExternalResource;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;

/**
 * @author Jose Rojas
 */
public class DatabaseRule extends ExternalResource {

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
	public DatabaseRule(String dbName, String initFile, String... options) {
		this(dbName, "root", "", initFile, options);
	}
	
	public DatabaseRule(String dbName, String username, String password, String initFile, String... connectionOptions) {
		this.dbName= dbName;
		this.initFile = initFile;
		StringBuffer sb = new StringBuffer();
		if( connectionOptions != null && connectionOptions.length > 0 ){
			sb.append("?");
			for( String option : connectionOptions ){
				sb.append(option).append("&");
			}
			// Remove trailing "&"
			sb.deleteCharAt( sb.lastIndexOf("&"));
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
	// we can add additional connectionOptions to the URL as parameters:+"[?][parameter=value[&parameter=value]]"
	// for example, to return the updated query - not the matched ones +"?""useAffectedRows=true"
	public Connection getConnection() throws SQLException {
		String connectionURL = config.getURL(dbName) + connectionOptions;
		return DriverManager.getConnection( connectionURL, username, password);
	}
}
