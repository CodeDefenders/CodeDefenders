/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
import java.sql.SQLException;
import java.util.List;

import javax.annotation.ManagedBean;

import org.codedefenders.game.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import testsmell.AbstractSmell;
import testsmell.TestFile;

/**
 * This class handles the database logic for test smells.
 *
 * @author gambi
 */
@ManagedBean
public class TestSmellsDAO {
    private static final Logger logger = LoggerFactory.getLogger(TestSmellsDAO.class);

    private final String INSERT_SMELL_QUERY = String.join("\n",
            "INSERT INTO test_smell (Test_ID, smell_name)",
            "VALUES (?, ?);"
    );

    private final String GET_SMELL_QUERY = String.join("\n",
            "SELECT smell_name",
            "FROM test_smell",
            "WHERE Test_ID = ?;"
    );

    /**
     * Stores all test smells of a test to the database.
     *
     * @param test the given test as a {@link Test}.
     * @param testFile a container for all test smells.
     * @throws UncheckedSQLException If storing test smells was not successful.
     */
    public void storeSmell(final Test test, final TestFile testFile) throws UncheckedSQLException {
        Connection conn = DB.getConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(INSERT_SMELL_QUERY);
            for (AbstractSmell smell : testFile.getTestSmells()) {
                if (smell.getHasSmell() ){ // && !filterSmell.equals(smell.getSmellName())) {
                    stmt.setInt(1, test.getId());
                    stmt.setString(2, smell.getSmellName());
                    stmt.addBatch();
                }
            }

            stmt.executeBatch(); // Execute every 1000 items.
        } catch (SQLException e) {
            logger.warn("Cannot store smell to database ", e);
            throw new UncheckedSQLException("Could not store test smell to database.");
        } finally {
            DB.cleanup(conn, stmt);
        }
    }

    /**
     * Retrieves and returns a {@link List} of test smells for a given test.
     *
     * @param test the given test the smells are retrieved for.
     * @return A list of all tests smells for a given test.
     */
    public List<String> getDetectedTestSmellsForTest(Test test) {
        return DB.executeQueryReturnList(GET_SMELL_QUERY, rs -> rs.getString("smell_name"), DatabaseValue.of(test.getId()));
    }
}
