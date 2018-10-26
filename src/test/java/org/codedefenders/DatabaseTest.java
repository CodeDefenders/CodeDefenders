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
 * along with Code Defenders.  If not, see <http://www.gnu.org/licenses/>.
 */
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

}
