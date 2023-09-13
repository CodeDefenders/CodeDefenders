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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.codedefenders.database.DB.RSMapper;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.intellij.lang.annotations.Language;

/**
 * This class handles the database logic for target executions.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 * @see TargetExecution
 */
public class TargetExecutionDAO {
    private static final int MESSAGE_LIMIT = 2000;

    /**
     * Constructs a test from a {@link ResultSet} entry.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed test.
     * @see RSMapper
     */
    static TargetExecution targetExecutionFromRS(ResultSet rs) throws SQLException {
        int targetExecutionId = rs.getInt("TargetExecution_ID");
        int testId = rs.getInt("Test_ID");
        int mutantId = rs.getInt("Mutant_ID");

        TargetExecution.Target target = TargetExecution.Target.valueOf(rs.getString("Target"));

        String message = rs.getString("Message");
        TargetExecution.Status status = TargetExecution.Status.valueOf(rs.getString("Status"));
        Timestamp timestamp = rs.getTimestamp("Timestamp");

        return new TargetExecution(targetExecutionId, testId, mutantId, target, status, message, timestamp);
    }

    /**
     * Stores a given {@link TargetExecution} in the database.
     *
     * <p>This method does not update the given target execution object.
     * Use {@link TargetExecution#insert()} instead.
     *
     * @param targetExecution the given target execution as a {@link TargetExecution}.
     * @return the generated identifier of the target execution as an {@code int}.
     * @throws UncheckedSQLException If storing the target execution was not successful.
     */
    public static int storeTargetExecution(TargetExecution targetExecution) {
        @Language("SQL") final String query;
        final DatabaseValue<?>[] values;

        final String insertedMessage = targetExecution.message == null ? ""
                : targetExecution.message.length() <= MESSAGE_LIMIT ? targetExecution.message
                        : targetExecution.message.substring(0, MESSAGE_LIMIT);

        if (targetExecution.hasTest() && targetExecution.hasMutant()) {
            query = "INSERT INTO targetexecutions (Test_ID, Mutant_ID, Target, Status, Message)"
                    + " VALUES (?, ?, ?, ?, ?);";
            values = new DatabaseValue[] {
                    DatabaseValue.of(targetExecution.testId),
                    DatabaseValue.of(targetExecution.mutantId),
                    DatabaseValue.of(targetExecution.target.name()),
                    DatabaseValue.of(targetExecution.status.name()),
                    DatabaseValue.of(insertedMessage)
            };
        } else if (targetExecution.hasTest()) {
            query = "INSERT INTO targetexecutions (Test_ID, Target, Status, Message) VALUES (?, ?, ?, ?);";
            values = new DatabaseValue[] {
                   DatabaseValue.of(targetExecution.testId),
                   DatabaseValue.of(targetExecution.target.name()),
                   DatabaseValue.of(targetExecution.status.name()),
                   DatabaseValue.of(insertedMessage)
            };
        } else if (targetExecution.hasMutant()) {
            query = "INSERT INTO targetexecutions (Mutant_ID, Target, Status, Message) VALUES (?, ?, ?, ?);";
            values = new DatabaseValue[] {
                   DatabaseValue.of(targetExecution.mutantId),
                   DatabaseValue.of(targetExecution.target.name()),
                   DatabaseValue.of(targetExecution.status.name()),
                   DatabaseValue.of(insertedMessage)
            };
        } else {
            // has no test or mutant data
            query = "INSERT INTO targetexecutions (Target, Status, Message) VALUES (?, ?, ?);";
            values = new DatabaseValue[]{
                   DatabaseValue.of(targetExecution.target.name()),
                   DatabaseValue.of(targetExecution.status.name()),
                   DatabaseValue.of(insertedMessage)
            };
        }

        final int result = DB.executeUpdateQueryGetKeys(query, values);
        if (result != -1) {
            return result;
        } else {
            throw new UncheckedSQLException("Could not store target execution to database.");
        }
    }

    /**
     * Retrieves the specific {@link TargetExecution} for given test and mutant identifiers.
     *
     * @param testId the given {@link Test} identifier.
     * @param mutantId the given {@link Mutant} identifier
     * @return the target execution for the given test and mutant if found, {@link null} otherwise.
     */
    public static TargetExecution getTargetExecutionForPair(int testId, int mutantId) {
        @Language("SQL") String query = """
                SELECT *
                FROM targetexecutions
                WHERE Test_ID = ?
                  AND Mutant_ID = ?;
        """;

        DatabaseValue<?>[] values = new DatabaseValue[]{
               DatabaseValue.of(testId),
               DatabaseValue.of(mutantId)
        };

        return DB.executeQueryReturnValue(query, TargetExecutionDAO::targetExecutionFromRS, values);
    }

    /**
     * Retrieves the specific {@link TargetExecution} for a given test and execution target.
     *
     * @param test the given {@link Test}.
     * @param target the given {@link TargetExecution.Target Target}.
     * @return the target execution for the given test and target if found, {@link null} otherwise.
     */
    public static TargetExecution getTargetExecutionForTest(Test test, TargetExecution.Target target) {
        @Language("SQL") final String query = """
                SELECT *
                FROM targetexecutions
                WHERE Test_ID = ?
                  AND Target = ?;
        """;

        DatabaseValue<?>[] values = new DatabaseValue[]{
               DatabaseValue.of(test.getId()),
               DatabaseValue.of(target.name())
        };

        return DB.executeQueryReturnValue(query, TargetExecutionDAO::targetExecutionFromRS, values);
    }

    /**
     * Retrieves the specific {@link TargetExecution} for a given mutant and execution target.
     *
     * @param mutant the given {@link Mutant}.
     * @param target the given {@link TargetExecution.Target Target}.
     * @return the target execution for the given mutant and target if found, {@link null} otherwise.
     */
    public static TargetExecution getTargetExecutionForMutant(Mutant mutant, TargetExecution.Target target) {
        @Language("SQL") final String query = """
                SELECT *
                FROM targetexecutions
                WHERE Mutant_ID = ?
                  AND Target = ?;
        """;

        DatabaseValue<?>[] values = new DatabaseValue[]{
               DatabaseValue.of(mutant.getId()),
               DatabaseValue.of(target.name())
        };

        return DB.executeQueryReturnValue(query, TargetExecutionDAO::targetExecutionFromRS, values);
    }

    /**
     * Retrieves all {@link TargetExecution TargetExecutions} which resulted from mutants and tests
     * being executed against each other after being uploaded with that class.
     *
     * @param classId the identifier of the class.
     * @return a list of target executions of mutants and tests which were uploaded together with the same given class.
     */
    public static List<TargetExecution> getTargetExecutionsForUploadedWithClass(int classId) {
        @Language("SQL") final String query = """
                SELECT te.*
                FROM targetexecutions te, classes c
                WHERE c.Class_ID = ?
                  AND te.Mutant_ID IN (SELECT Mutant_ID FROM mutant_uploaded_with_class up WHERE up.Class_ID = c.Class_ID)
                  AND te.Test_ID IN (SELECT Test_ID FROM test_uploaded_with_class up WHERE up.Class_ID = c.Class_ID);
        """;

        return DB.executeQueryReturnList(query, TargetExecutionDAO::targetExecutionFromRS, DatabaseValue.of(classId));
    }
}
