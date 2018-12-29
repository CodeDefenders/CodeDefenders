package org.codedefenders.database;

import org.codedefenders.database.DB.RSMapper;
import org.codedefenders.execution.TargetExecution;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * This class handles the database logic for target executions.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
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
     * <p>
     * This method does not update the given target execution object.
     * Use {@link TargetExecution#insert()} instead.
     *
     * @param targetExecution the given target execution as a {@link TargetExecution}.
     * @return the generated identifier of the target execution as an {@code int}.
     * @throws UncheckedSQLException If storing the target execution was not successful.
     */
    public static int storeTargetExecution(TargetExecution targetExecution) {
        final String query;
        final DatabaseValue[] values;

        final String insertedMessage = targetExecution.message == null ? ""
                : targetExecution.message.length() <= MESSAGE_LIMIT ? targetExecution.message
                        : targetExecution.message.substring(0, MESSAGE_LIMIT);

        if (targetExecution.hasTest() && targetExecution.hasMutant()) {
            query = "INSERT INTO targetexecutions (Test_ID, Mutant_ID, Target, Status, Message) VALUES (?, ?, ?, ?, ?);";
            values = new DatabaseValue[] {
                    DB.getDBV(targetExecution.testId),
                    DB.getDBV(targetExecution.mutantId),
                    DB.getDBV(targetExecution.target.name()),
                    DB.getDBV(targetExecution.status.name()),
                    DB.getDBV(insertedMessage)
            };
        } else if (targetExecution.hasTest()) {
            query = "INSERT INTO targetexecutions (Test_ID, Target, Status, Message) VALUES (?, ?, ?, ?);";
            values = new DatabaseValue[] {
                   DB.getDBV(targetExecution.testId),
                   DB.getDBV(targetExecution.target.name()),
                   DB.getDBV(targetExecution.status.name()),
                   DB.getDBV(insertedMessage)
            };
        } else if (targetExecution.hasMutant()) {
            query = "INSERT INTO targetexecutions (Mutant_ID, Target, Status, Message) VALUES (?, ?, ?, ?);";
            values = new DatabaseValue[] {
                   DB.getDBV(targetExecution.mutantId),
                   DB.getDBV(targetExecution.target.name()),
                   DB.getDBV(targetExecution.status.name()),
                   DB.getDBV(insertedMessage)
            };
        } else {
            // has no test or mutant data
            query = "INSERT INTO targetexecutions (Target, Status, Message) VALUES (?, ?, ?);";
            values = new DatabaseValue[]{
                   DB.getDBV(targetExecution.target.name()),
                   DB.getDBV(targetExecution.status.name()),
                   DB.getDBV(insertedMessage)
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
        String query = String.join("\n",
                "SELECT *",
                        "FROM targetexecutions",
                        "WHERE Test_ID = ?",
                        "  AND Mutant_ID = ?;"
        );

        DatabaseValue[] values = new DatabaseValue[]{
               DB.getDBV(testId),
               DB.getDBV(mutantId)
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
        final String query = String.join("\n",
                "SELECT *",
                "FROM targetexecutions",
                "WHERE Test_ID = ?",
                "  AND Target = ?;"
        );

        DatabaseValue[] values = new DatabaseValue[]{
               DB.getDBV(test.getId()),
               DB.getDBV(target.name())
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
        final String query = String.join("\n",
                "SELECT *",
                "FROM targetexecutions",
                "WHERE Mutant_ID = ?",
                "  AND Target = ?;"
        );

        DatabaseValue[] values = new DatabaseValue[]{
               DB.getDBV(mutant.getId()),
               DB.getDBV(target.name())
        };

        return DB.executeQueryReturnValue(query, TargetExecutionDAO::targetExecutionFromRS, values);
    }
}
