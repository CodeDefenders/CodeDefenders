package org.codedefenders.database;

import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * This class handles the database logic for Java classes.
 *
 * @author <a href=https://github.com/werli>Phil Werli<a/>
 * @see GameClass
 */
public class GameClassDAO {
    private static final Logger logger = LoggerFactory.getLogger(GameClassDAO.class);

    /**
     * Checks for a given alias whether a class with this alias do not yet exist.
     *
     * @param alias the alias that is checked.
     * @return {@code true} if alias does not exist, {@code false} otherwise.
     */
    public static boolean classNotExistsForAlias(String alias) {
        String query = "SELECT * FROM classes WHERE Alias = ?";

        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(alias)};

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        final ResultSet resultSet = DB.executeQueryReturnRS(conn, stmt);
        if (resultSet == null) {
            return false;
        }
        try {
            return resultSet.first();
        } catch (SQLException e) {
            logger.error("Error while retrieving classes for alias.", e);
            return false;
        }
    }

    /**
     * Return a {@link List} of all identifiers of {@link Mutant}s, which were uploaded
     * together with a given class.
     *
     * @param classId the identifier of the given class
     * @return a list of identifiers of mutants
     */
    public static List<Integer> getMappedMutantsForId(Integer classId) {
        List<Integer> mutantIds = new LinkedList<>();

        String query = "SELECT Mutant_ID FROM mutant_belongs_to_class WHERE Class_ID = ?;";

        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(classId)};

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        final ResultSet resultSet = DB.executeQueryReturnRS(conn, stmt);
        if (resultSet == null) {
            return mutantIds;
        }
        try {
            while (resultSet.next()) {
                final int mutantId = resultSet.getInt("Mutant_ID");
                mutantIds.add(mutantId);
            }
        } catch (SQLException e) {
            logger.error("Error during retrieval of mapped mutants for classId:{}", classId);
        }

        return mutantIds;
    }

    /**
     * Return a {@link List} of all identifiers of {@link Test}s, which were uploaded
     * together with a given class.
     *
     * @param classId the identifier of the given class
     * @return a list of identifiers of tests
     */
    public static List<Integer> getMappedTestsForId(Integer classId) {
        List<Integer> testIds = new LinkedList<>();

        String query = "SELECT Test_ID FROM test_belongs_to_class WHERE Class_ID = ?;";

        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(classId)};

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        final ResultSet resultSet = DB.executeQueryReturnRS(conn, stmt);
        if (resultSet == null) {
            return testIds;
        }
        try {
            while (resultSet.next()) {
                final int testId = resultSet.getInt("Test_ID");
                testIds.add(testId);
            }
        } catch (SQLException e) {
            logger.error("Error during retrieval of mapped tests for classId:{}", classId);
        }

        return testIds;
    }

    /**
     * Stores a given {@link GameClass} in the database and
     * returns an ID.
     *
     * @param cut the given class as a {@link GameClass}.
     * @return An identifier as an integer value.
     * @throws Exception If storing the class was not successful.
     */
    public static int storeClass(GameClass cut) throws Exception {
        String name = cut.getName();
        String alias = cut.getAlias();
        String javaFile = DatabaseAccess.addSlashes(cut.getJavaFile());
        String classFile = DatabaseAccess.addSlashes(cut.getClassFile());
        boolean isMockingEnabled = cut.isMockingEnabled();

        String query = "INSERT INTO classes (Name, Alias, JavaFile, ClassFile, RequireMocking) VALUES (?, ?, ?, ?, ?);";
        DatabaseValue[] valueList = new DatabaseValue[]{

                DB.getDBV(name),
                DB.getDBV(alias),
                DB.getDBV(javaFile),
                DB.getDBV(classFile),
                DB.getDBV(isMockingEnabled)};

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        final int result = DB.executeUpdateGetKeys(stmt, conn);
        if (result != -1) {
            logger.debug("Successfully stored class in database[Name={}, Alias={}].", cut.getName(), cut.getAlias());
            return result;
        } else {
            throw new Exception("Could not store class to database.");
        }
    }

    /**
     * Removes a class for a given identifier.
     *
     * @param id the identifier of the class to be removed.
     * @return {@code true} for successful removal, {@code false} otherwise.
     */
    public static boolean removeClassForId(Integer id) {
        String query = "DELETE FROM classes WHERE Class_ID = ?;";
        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(id)};

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        return DB.executeUpdate(stmt, conn);

    }

    /**
     * Removes multiple classes for a given list of identifiers.
     *
     * @param classes the identifiers of the classes to be removed.
     * @return {@code true} for successful removal, {@code false} otherwise.
     */
    public static boolean removeClassesForIds(List<Integer> classes) {
        if (classes.isEmpty()) {
            return false;
        }

        final StringBuilder bob = new StringBuilder("DELETE FROM classes WHERE Class_ID in (");
        for (int i = 0; i < classes.size() - 1; i++) {
            bob.append("?,");
        }
        bob.append("?);");

        String query = bob.toString();
        DatabaseValue[] valueList = classes.stream().map(DB::getDBV).toArray(DatabaseValue[]::new);

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        return DB.executeUpdate(stmt, conn);
    }
}
