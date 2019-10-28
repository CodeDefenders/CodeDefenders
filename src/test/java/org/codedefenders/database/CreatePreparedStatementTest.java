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

import org.codedefenders.rules.DatabaseRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;

/**
 * Tests the {@link DB#createPreparedStatement} implementations.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ConnectionPool.class})
public class CreatePreparedStatementTest {
    @Rule
    public DatabaseRule db = new DatabaseRule("defender", "db/emptydb.sql");

    @Test
    public void testCreatePreparedStatementSingle() throws Exception {
        try (Connection connection = db.getConnection()) {

            PowerMockito.mockStatic(ConnectionPool.class);
            given(ConnectionPool.instance()).willReturn(null); // ConnectionPool is not required for this test.

            final String query = String.join("\n",
                    "SELECT User_ID",
                    "FROM users",
                    "WHERE Username = ?"
            );

            final String name = "Manuel Neuer";
            final PreparedStatement preparedStatement = DB.createPreparedStatement(connection, query, DatabaseValue.of(name));
            assertNotNull(preparedStatement);

            final ParameterMetaData parameterMetaData = preparedStatement.getParameterMetaData();
            assertNotNull(parameterMetaData);

            assertEquals(1, parameterMetaData.getParameterCount());
        }
    }

    @Test
    public void testCreatePreparedStatementMultiple() throws Exception {
        try (Connection connection = db.getConnection()) {

            PowerMockito.mockStatic(ConnectionPool.class);
            given(ConnectionPool.instance()).willReturn(null); // ConnectionPool is not required for this test.

            final String query = String.join("\n",
                    "SELECT Username",
                    "FROM users",
                    "WHERE Username = ?",
                    "AND Password = ?",
                    "AND Validated = ?",
                    "AND Active = ?",
                    "AND User_ID IN (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)" // 19 ?'s in this line
            );

            final DatabaseValue[] databaseValues = new DatabaseValue[4 + 19];
            final String name = null;
            databaseValues[0] = DatabaseValue.of(name);
            databaseValues[1] = DatabaseValue.of("number1");
            databaseValues[2] = DatabaseValue.of(false);
            databaseValues[3] = DatabaseValue.of(true);
            for (int i = 4; i < databaseValues.length; i++) {
                databaseValues[i] = DatabaseValue.of(i + 123);
            }

            final PreparedStatement preparedStatement = DB.createPreparedStatement(connection, query, databaseValues);
            assertNotNull(preparedStatement);

            final ParameterMetaData parameterMetaData = preparedStatement.getParameterMetaData();
            assertNotNull(parameterMetaData);

            assertEquals(databaseValues.length, parameterMetaData.getParameterCount());
        }
    }
}
