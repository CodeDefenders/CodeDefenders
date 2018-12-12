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

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.model.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.codedefenders.database.DB.RSMapper;
import static org.codedefenders.database.DB.createPreparedStatement;
import static org.codedefenders.database.DB.executeQueryReturnList;
import static org.codedefenders.database.DB.executeQueryReturnValue;
import static org.codedefenders.database.DB.executeUpdate;
import static org.codedefenders.database.DB.executeUpdateGetKeys;
import static org.codedefenders.database.DB.getConnection;
import static org.codedefenders.database.DB.getDBV;

/**
 * This class handles the database logic for Java classes.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 * @see GameClass
 */
public class GameClassDAO {
    private static final Logger logger = LoggerFactory.getLogger(GameClassDAO.class);

    /**
     * Constructs a game class from a {@link ResultSet} entry.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed game class.
     * @see RSMapper
     */
    public static GameClass gameClassFromRS(ResultSet rs) throws SQLException {
        int classId = rs.getInt("Class_ID");
        String name = rs.getString("Name");
        String alias = rs.getString("Alias");
        String javaFile = rs.getString("JavaFile");
        String classFile = rs.getString("ClassFile");
        boolean requireMocking = rs.getBoolean("RequireMocking");

        return new GameClass(classId , name, alias, javaFile, classFile, requireMocking);
    }


    /**
     * Retrieves a game class for a given identifier.
     *
     * @param classId the given identifier.
     * @return a {@link GameClass} instance, or {@code null}.
     */
    public static GameClass getClassForId(int classId) {
        String query = "SELECT * FROM classes WHERE Class_ID = ?;";

        return executeQueryReturnValue(query, GameClassDAO::gameClassFromRS, getDBV(classId));
    }

    /**
     * Retrieves a game class for a given identifier of a {@link AbstractGame Game}.
     *
     * @param gameId the given game identifier.
     * @return a {@link GameClass} instance, or {@code null}.
     */
    public static GameClass getClassForGameId(int gameId) {
        String query = String.join("\n",
                "SELECT classes.*",
                "FROM classes",
                "INNER JOIN games",
                "  ON classes.Class_ID = games.Class_ID",
                "WHERE games.ID=?;"
        );
        return executeQueryReturnValue(query, GameClassDAO::gameClassFromRS, getDBV(gameId));
    }

    /**
     * Retrieves all game classes in a {@link List}. If no game classes
     * are found, the list is empty, but not {@link null}.
     *
     * @return all game classes.
     */
    public static List<GameClass> getAllClasses() {
        String query = "SELECT * FROM classes;";

        return executeQueryReturnList(query, GameClassDAO::gameClassFromRS);
    }

    /**
     * Checks for a given alias whether a class with this alias do not yet exist.
     *
     * @param alias the alias that is checked.
     * @return {@code true} if alias does not exist, {@code false} otherwise.
     */
    public static boolean classNotExistsForAlias(String alias) throws UncheckedSQLException, SQLMappingException {
        String query = "SELECT * FROM classes WHERE Alias = ?";
        Boolean rv = executeQueryReturnValue(query, rs -> true, getDBV(alias));
        return rv != null;
    }

    /**
     * Return a {@link List} of all identifiers of {@link Mutant Mutants}, which were uploaded
     * together with a given class.
     *
     * @param classId the identifier of the given class
     * @return a list of identifiers of mutants
     */
    public static List<Integer> getMappedMutantIdsForClassId(Integer classId)
            throws UncheckedSQLException, SQLMappingException {
        final String query = String.join("\n",
                "SELECT Mutant_ID",
                "FROM mutant_uploaded_with_class",
                "WHERE Class_ID = ?"
        );
        return executeQueryReturnList(query, rs -> rs.getInt("Mutant_ID"), getDBV(classId));
    }

    /**
     * Return a {@link List} of all {@link Mutant Mutants}, which were uploaded
     * together with a given class.
     *
     * @param classId the identifier of the given class
     * @return a list of mutants
     */
    public static List<Mutant> getMappedMutantsForClassId(Integer classId)
            throws UncheckedSQLException, SQLMappingException {
        final String query = String.join("\n",
                "SELECT mutants.*",
                "FROM mutants, mutant_uploaded_with_class up",
                "WHERE up.Class_ID = ?",
                "   AND up.Mutant_ID = mutants.Mutant_ID"
        );
        return executeQueryReturnList(query, MutantDAO::mutantFromRS, getDBV(classId));
    }

    /**
     * Return a {@link List} of all identifiers of {@link Test Tests}, which were uploaded
     * together with a given class.
     *
     * @param classId the identifier of the given class
     * @return a list of identifiers of tests
     */
    public static List<Integer> getMappedTestIdsForClassId(Integer classId)
            throws UncheckedSQLException, SQLMappingException {
        final String query = String.join("\n",
                "SELECT Test_ID",
                "FROM test_uploaded_with_class",
                "WHERE Class_ID = ?"
        );
        return executeQueryReturnList(query, rs -> rs.getInt("Test_ID"), getDBV(classId));
    }

    /**
     * Return a {@link List} of all {@link Test Tests}, which were uploaded
     * together with a given class.
     *
     * @param classId the identifier of the given class
     * @return a list of tests
     */
    public static List<Test> getMappedTestsForClassId(Integer classId) throws UncheckedSQLException, SQLMappingException {
        final String query = String.join("\n",
                "SELECT tests.*",
                "FROM tests, test_uploaded_with_class up",
                "WHERE up.Class_ID = ?",
                "   AND up.Test_ID = tests.Test_ID"
                );
        return executeQueryReturnList(query, TestDAO::testFromRS, getDBV(classId));
    }

    /**
     * Return a {@link List} of all identifiers of {@link Dependency Dependencies},
     * which were uploaded together with a given class.
     *
     * @param classId the identifier of the given class
     * @return a list of identifiers of dependencies
     */
    public static List<Integer> getMappedDependencyIdsForClassId(Integer classId)
            throws UncheckedSQLException, SQLMappingException {
        String query = "SELECT Dependency_ID FROM dependencies WHERE Class_ID = ?;";
        return executeQueryReturnList(query, rs -> rs.getInt("Dependency_ID"), getDBV(classId));
    }

    /**
     * Return a {@link List} of all {@link Dependency Dependencies},
     * which were uploaded together with a given class.
     *
     * @param classId the identifier of the given class
     * @return a list of identifiers of dependencies
     */
    public static List<Dependency> getMappedDependenciesForClassId(Integer classId)
            throws UncheckedSQLException, SQLMappingException {
        String query = String.join("\n",
                "SELECT",
                "   Dependency_ID,",
                "   JavaFile,",
                "   ClassFile",
                "FROM dependencies WHERE Class_ID = ?;");
        return executeQueryReturnList(query, rs -> DependencyDAO.dependencyFromRS(rs, classId), getDBV(classId));
    }

    /**
     * Stores a given {@link GameClass} in the database and
     * returns an ID.
     *
     * @param cut the given class as a {@link GameClass}.
     * @return An identifier as an integer value.
     * @throws UncheckedSQLException If storing the class was not successful.
     */
    public static int storeClass(GameClass cut) throws UncheckedSQLException {
        String name = cut.getName();
        String alias = cut.getAlias();
        String javaFile = DatabaseAccess.addSlashes(cut.getJavaFile());
        String classFile = DatabaseAccess.addSlashes(cut.getClassFile());
        boolean isMockingEnabled = cut.isMockingEnabled();

        String query = "INSERT INTO classes (Name, Alias, JavaFile, ClassFile, RequireMocking) VALUES (?, ?, ?, ?, ?);";
        DatabaseValue[] valueList = new DatabaseValue[]{
                getDBV(name),
                getDBV(alias),
                getDBV(javaFile),
                getDBV(classFile),
                getDBV(isMockingEnabled)};

        Connection conn = getConnection();
        PreparedStatement stmt = createPreparedStatement(conn, query, valueList);

        final int result = executeUpdateGetKeys(stmt, conn);
        if (result != -1) {
            logger.debug("Successfully stored class in database[Name={}, Alias={}].", cut.getName(), cut.getAlias());
            return result;
        } else {
            throw new UncheckedSQLException("Could not store class to database.");
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
        DatabaseValue[] valueList = new DatabaseValue[]{getDBV(id)};

        Connection conn = getConnection();
        PreparedStatement stmt = createPreparedStatement(conn, query, valueList);

        return executeUpdate(stmt, conn);

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

        Connection conn = getConnection();
        PreparedStatement stmt = createPreparedStatement(conn, query, valueList);

        return executeUpdate(stmt, conn);
    }
}
