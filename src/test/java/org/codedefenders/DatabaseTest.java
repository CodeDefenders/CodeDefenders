package org.codedefenders;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Jose Rojas
 */
public class DatabaseTest {

	// Eclipse by default filters out resources
	@Rule
	public DatabaseRule db = new DatabaseRule("defender", "db/emptydb.sql");

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

	// @org.junit.Test
	// public void loadResource() throws IOException {
	// InputStream from =
	// getClass().getClassLoader().getResourceAsStream("db/emptydb.sql");
	// assertNotNull( "Resource is null", from );
	// try (BufferedReader reader = new BufferedReader(new
	// InputStreamReader(from));) {
	// String str;
	// while ((str = reader.readLine()) != null) {
	// System.out.println("DatabaseTest.loadResource() " + str);
	// }
	//
	// }
	// }
}
