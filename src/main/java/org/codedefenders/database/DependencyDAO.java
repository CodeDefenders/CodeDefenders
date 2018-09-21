package org.codedefenders.database;

import org.codedefenders.model.Dependency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * This class handles the database logic for dependencies.
 *
 * @author <a href=https://github.com/werli>Phil Werli<a/>
 * @see Dependency
 */
public class DependencyDAO {
    /**
     * Stores a given {@link Dependency} in the database.
     *
     * @param dependency the given dependency as a {@link Dependency}.
     * @return the generated identifier of the dependency as an {@code int}.
     * @throws Exception If storing the dependency was not successful.
     */
    public static int storeDependency(Dependency dependency) throws Exception {
        Integer classId = dependency.getClassId();
        String javaFile = DatabaseAccess.addSlashes(dependency.getJavaFile());
        String classFile = DatabaseAccess.addSlashes(dependency.getClassFile());

        String query = "INSERT INTO dependencies (Class_ID, JavaFile, ClassFile) VALUES (?, ?, ?);";
        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(classId),
                DB.getDBV(javaFile),
                DB.getDBV(classFile)
        };
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        final int result = DB.executeUpdateGetKeys(stmt, conn);
        if (result != -1) {
            return result;
        } else {
            throw new Exception("Could not store dependency to database.");
        }
    }

    /**
     * Removes a dependency for a given identifier.
     *
     * @param id the identifier of the dependency to be removed.
     * @return {@code true} for successful removal, {@code false} otherwise.
     */
    public static boolean removeDependencyForId(Integer id) {
        String query = "DELETE FROM dependencies WHERE Dependency_ID = ?;";
        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(id),
        };

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        return DB.executeUpdate(stmt, conn);
    }

    /**
     * Removes multiple dependencies for a given list of identifiers.
     *
     * @param dependencies the identifiers of the dependencies to be removed.
     * @return {@code true} for successful removal, {@code false} otherwise.
     */
    public static boolean removeDependenciesForIds(List<Integer> dependencies) {
        if (dependencies.isEmpty()) {
            return false;
        }

        final StringBuilder bob = new StringBuilder("(");
        for (int i = 0; i < dependencies.size() - 1; i++) {
            bob.append("?,");
        }
        bob.append("?);");

        final String range = bob.toString();
        String query = "DELETE FROM dependencies WHERE Dependency_ID in " + range;

        DatabaseValue[] valueList = dependencies.stream().map(DB::getDBV).toArray(DatabaseValue[]::new);

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        return DB.executeUpdate(stmt, conn);
    }
}
