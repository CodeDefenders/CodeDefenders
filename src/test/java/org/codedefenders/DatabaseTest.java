package org.codedefenders;

import static org.junit.Assert.assertEquals;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.junit.Rule;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

/**
 * @author Jose Rojas
 */
public class DatabaseTest {

	@Rule
	public DatabaseRule db = new DatabaseRule();

	@Test
	public void testCleanDB() throws Exception {
		Connection conn = DriverManager.getConnection(db.config.getURL(db.DBNAME), "root", "");;
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
