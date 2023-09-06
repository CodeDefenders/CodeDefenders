/*
 * Copyright (C) 2016-2019,2021 Code Defenders contributors
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
import java.util.List;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.codedefenders.util.DatabaseExtension;
import org.codedefenders.util.tags.DatabaseTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Jose Rojas
 */
@DatabaseTest
@ExtendWith(DatabaseExtension.class)
public class EmptyDatabaseIT {

    /**
     * Checks whether the {@code empty.db} script creates an empty database.
     */
    @Test
    public void testCleanDB(Connection conn) throws Exception {
        try (conn) {
            QueryRunner qr = new QueryRunner();

            List<String> results = qr.query(conn, "SELECT * FROM classes;", new ColumnListHandler<>());
            assertEquals(0, results.size());

            @Language("SQL") String query2 = """
                    SELECT *
                    FROM games
                    WHERE ID > 0
            """;
            results = qr.query(conn, query2, new ColumnListHandler<>());
            assertEquals(0, results.size());

            results = qr.query(conn, "SELECT * FROM mutants;", new ColumnListHandler<>());
            assertEquals(0, results.size());

            results = qr.query(conn, "SELECT * FROM tests;", new ColumnListHandler<>());
            assertEquals(0, results.size());
        }
    }
}
