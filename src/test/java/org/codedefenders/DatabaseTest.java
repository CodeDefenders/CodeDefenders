package org.codedefenders;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.codedefenders.rules.DatabaseRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Jose Rojas
 */
public class DatabaseTest {

	// Eclipse by default filters out resources
	@Rule
	public DatabaseRule db = new DatabaseRule("defender", "db/emptydb.sql", "useAffectedRows=true");

	@Test
	public void testCleanDB() throws Exception {
		// Get a new connection from the rule 
		Connection conn = db.getConnection();

		try {
			QueryRunner qr = new QueryRunner();
			List<String> results = qr.query(conn, "SELECT * FROM classes;", new ColumnListHandler<String>());
			assertEquals(0, results.size());

			results = qr.query(conn, "SELECT * FROM games;", new ColumnListHandler<String>());
			assertEquals(0, results.size());

			results = qr.query(conn, "SELECT * FROM mutants;", new ColumnListHandler<String>());
			assertEquals(0, results.size());

			results = qr.query(conn, "SELECT * FROM tests;", new ColumnListHandler<String>());
			assertEquals(0, results.size());
		} finally {
			DbUtils.closeQuietly(conn);
		}
	}

}
