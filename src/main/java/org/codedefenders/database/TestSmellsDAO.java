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
package org.codedefenders.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.codedefenders.game.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import testsmell.AbstractSmell;
import testsmell.TestFile;
import testsmell.smell.ExceptionCatchingThrowing;

/**
 * This class handles the database logic for test smells.
 *
 * @author gambi
 */
public class TestSmellsDAO {
	private static final Logger logger = LoggerFactory.getLogger(TestSmellsDAO.class);

	final static String insertSmellsBatchQuery = "INSERT INTO test_smell (test_ID, smell_name) VALUES (?, ?);";

	final static String getSmellsQuery = "SELECT smell_name FROM test_smell WHERE test_ID = ?;";

	final static String filterSmell = new ExceptionCatchingThrowing().getSmellName();

	// This is not optimized. We shall use transactions, and batch insert
	// instead.
	public static void storeSmell(final Test newTest, final TestFile testFile) throws Exception {
		try {

			Connection conn = DB.getConnection();
			// We cannot use the available machinery to store a batch of
			// insert...
			PreparedStatement stmt = conn.prepareStatement(insertSmellsBatchQuery);

			for (AbstractSmell smell : testFile.getTestSmells()) {
				if (smell.getHasSmell() && !filterSmell.equals(smell.getSmellName())) {
					// Add smell to batch
					stmt.setInt(1, newTest.getId());
					stmt.setString(2, smell.getSmellName());
					stmt.addBatch();
				}
			}

			stmt.executeBatch(); // Execute every 1000 items.
		} catch (SQLException e) {
			logger.warn("Cannot store smell to databsed ", e);
			throw new Exception("Could not store test smell to database.");
		}
	}

	public static List<String> getDetectedTestSmellsFor(Test newTest) {
		Connection conn = DB.getConnection();
		DatabaseValue[] valueList = new DatabaseValue[] { DB.getDBV(newTest.getId()) };
		PreparedStatement stmt = DB.createPreparedStatement(conn, getSmellsQuery, valueList);
		ResultSet rs = DB.executeQueryReturnRS(conn, stmt);

		List<String> detectedTestSmells = new ArrayList<String>();
		try {
			while (rs.next()) {
				detectedTestSmells.add(rs.getString("smell_name"));
			}
		} catch (SQLException se) {
			logger.error("SQL exception caught", se);
		} catch (Exception e) {
			logger.error("Exception caught", e);
		} finally {
			DB.cleanup(conn, stmt);
		}
		return detectedTestSmells;
	}
}
