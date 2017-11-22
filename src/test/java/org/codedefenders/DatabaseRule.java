package org.codedefenders;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import org.junit.rules.ExternalResource;

/**
 * @author Jose Rojas
 */
public class DatabaseRule extends ExternalResource {

	private DB db;
	protected DBConfigurationBuilder config;
	protected static final String DBNAME = "codedefenders";

	@Override
	public void before() throws Exception {
		config = DBConfigurationBuilder.newBuilder();
		config.setPort(0); // 0 => autom. detect free port
		db = DB.newEmbeddedDB(config.build());
		db.start();
		db.source("db/codedefenders.sql");
	}

	@Override
	public void after() {
		try {
			db.stop();
		} catch (ManagedProcessException e) {
			// quiet
		}
	}
}
