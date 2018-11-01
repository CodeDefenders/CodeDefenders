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
import java.util.LinkedList;
import java.util.List;

/**
 * This class handles the database logic for Java classes.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
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
        try {
            if (resultSet == null) {
                return false;
            }
            return resultSet.first();
        } catch (SQLException e) {
            logger.error("Error while retrieving classes for alias.", e);
            return false;
        } finally {
            DB.cleanup(conn, stmt);
        }
    }

    /**
     * Return a {@link List} of all identifiers of {@link Mutant}s, which were uploaded
     * together with a given class.
     *
     * @param classId the identifier of the given class
     * @return a list of identifiers of mutants
     */
    public static List<Integer> getMappedMutantIdsForClassId(Integer classId) {
        List<Integer> mutantIds = new LinkedList<>();

        String query = "SELECT Mutant_ID FROM mutants WHERE Class_ID = ?;";

        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(classId)};

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        final ResultSet resultSet = DB.executeQueryReturnRS(conn, stmt);
        try {
            if (resultSet == null) {
                return mutantIds;
            }
            while (resultSet.next()) {
                final int mutantId = resultSet.getInt("Mutant_ID");
                mutantIds.add(mutantId);
            }
        } catch (SQLException e) {
            logger.error("Error during retrieval of mapped mutants for classId:{}", classId);
        } finally {
            DB.cleanup(conn, stmt);
        }

        return mutantIds;
    }

    /**
     * Return a {@link List} of all {@link Mutant}s, which were uploaded
     * together with a given class.
     *
     * @param classId the identifier of the given class
     * @return a list of mutants
     */
    public static List<Mutant> getMappedMutantsForClassId(Integer classId) {
        List<Mutant> mutants = new LinkedList<>();

        String query = "SELECT * FROM mutants WHERE Class_ID = ?;";

        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(classId)};

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        final ResultSet resultSet = DB.executeQueryReturnRS(conn, stmt);
        try {
            if (resultSet == null) {
                return mutants;
            }
            while (resultSet.next()) {
                final int mutantId = resultSet.getInt("Mutant_ID");
                final int gameId = resultSet.getInt("Game_ID");
                final String javaFile = resultSet.getString("JavaFile");
                final String classFile = resultSet.getString("ClassFile");
                final boolean alive = resultSet.getInt("Alive") == 1;
                final Mutant.Equivalence equivalence = Mutant.Equivalence.valueOf(resultSet.getString("Equivalent"));
                final int roundCreated = resultSet.getInt("RoundCreated");
                final int roundKilled = resultSet.getInt("RoundKilled");
                final int playerId = resultSet.getInt("Player_ID");

                mutants.add(new Mutant(mutantId, gameId, javaFile, classFile, alive, equivalence, roundCreated, roundKilled, playerId));
            }
        } catch (SQLException e) {
            logger.error("Error during retrieval of mapped mutants for classId:{}", classId);
        } finally {
            DB.cleanup(conn, stmt);
        }

        return mutants;
    }

    /**
     * Return a {@link List} of all identifiers of {@link Test}s, which were uploaded
     * together with a given class.
     *
     * @param classId the identifier of the given class
     * @return a list of identifiers of tests
     */
    public static List<Integer> getMappedTestIdsForClassId(Integer classId) {
        List<Integer> testIds = new LinkedList<>();

        String query = "SELECT Test_ID FROM tests WHERE Class_ID = ?;";

        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(classId)};

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        final ResultSet resultSet = DB.executeQueryReturnRS(conn, stmt);
        try {
            if (resultSet == null) {
                return testIds;
            }
            while (resultSet.next()) {
                final int testId = resultSet.getInt("Test_ID");
                testIds.add(testId);
            }
        } catch (SQLException e) {
            logger.error("Error during retrieval of mapped tests for classId:{}", classId);
        } finally {
            DB.cleanup(conn, stmt);
        }

        return testIds;
    }


    /**
     * Return a {@link List} of all {@link Test}s, which were uploaded
     * together with a given class.
     *
     * @param classId the identifier of the given class
     * @return a list of tests
     */
    public static List<Test> getMappedTestsForClassId(Integer classId) {
        String query = "SELECT * FROM tests WHERE Class_ID = ?;";

        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(classId)};

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        final List<Test> retrievedTests = DatabaseAccess.getTests(stmt, conn);

        return new LinkedList<>(retrievedTests);
    }

    /**
     * Return a {@link List} of all identifiers of {@link Dependency}s,
     * which were uploaded together with a given class.
     *
     * @param classId the identifier of the given class
     * @return a list of identifiers of dependencies
     */
    public static List<Integer> getMappedDependencyIdsForClassId(Integer classId) {
        List<Integer> testIds = new LinkedList<>();

        String query = "SELECT Dependency_ID FROM dependencies WHERE Class_ID = ?;";

        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(classId)};

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        final ResultSet resultSet = DB.executeQueryReturnRS(conn, stmt);
        try {
            if (resultSet == null) {
                return testIds;
            }
            while (resultSet.next()) {
                final int id = resultSet.getInt("Dependency_ID");
                testIds.add(id);
            }
        } catch (SQLException e) {
            logger.error("Error during retrieval of mapped dependency for classId:{}", classId);
        } finally {
            DB.cleanup(conn, stmt);
        }

        return testIds;
    }

    public static List<Dependency> getMappedDependenciesForClassId(Integer classId) {
        List<Dependency> dependencies = new LinkedList<>();

        String query = String.join(" ",
                "SELECT",
                "   Dependency_ID,",
                "   JavaFile,",
                "   ClassFile",
                "FROM dependencies WHERE Class_ID = ?;"
        );

        DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(classId)};

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

        final ResultSet resultSet = DB.executeQueryReturnRS(conn, stmt);
        try {
            if (resultSet == null) {
                return dependencies;
            }
            while (resultSet.next()) {
                final int id = resultSet.getInt("Dependency_ID");
                final String javaFile = resultSet.getString("JavaFile");
                final String classFile = resultSet.getString("ClassFile");
                final Dependency dependency = new Dependency(id, classId, javaFile, classFile);
                dependencies.add(dependency);
            }
        } catch (SQLException e) {
            logger.error("Error during retrieval of mapped dependency for classId:{}", classId);
        } finally {
            DB.cleanup(conn, stmt);
        }

        return dependencies;
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
