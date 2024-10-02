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
package org.codedefenders.persistence.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.database.DependencyDAO;
import org.codedefenders.database.SQLMappingException;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.AssertionLibrary;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.TestingFramework;
import org.codedefenders.game.puzzle.Puzzle;
import org.codedefenders.model.Dependency;
import org.codedefenders.model.GameClassInfo;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.util.FileUtils;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.QueryUtils.batchParamsFromList;
import static org.codedefenders.persistence.database.util.ResultSetUtils.generatedKeyFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.listFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.oneFromRS;
import static org.codedefenders.util.NamingUtils.nextFreeName;

/**
 * This class handles the database logic for Java classes.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 * @see GameClass
 * @see GameClassInfo
 */
@ApplicationScoped
public class GameClassRepository {
    private static final Logger logger = LoggerFactory.getLogger(GameClassRepository.class);

    private final QueryRunner queryRunner;

    @Inject
    public GameClassRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    /**
     * Constructs a game class from a {@link ResultSet} entry.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed game class.
     */
    public static GameClass gameClassFromRS(ResultSet rs) throws SQLException {
        int classId = rs.getInt("Class_ID");
        String name = rs.getString("Name");
        String alias = rs.getString("Alias");
        String javaFile = rs.getString("JavaFile");
        String classFile = rs.getString("ClassFile");
        String absoluteJavaFile = FileUtils.getAbsoluteDataPath(javaFile).toString();
        String absoluteClassFile = FileUtils.getAbsoluteDataPath(classFile).toString();
        boolean requireMocking = rs.getBoolean("RequireMocking");
        TestingFramework testingFramework = TestingFramework.valueOf(rs.getString("TestingFramework"));
        AssertionLibrary assertionLibrary = AssertionLibrary.valueOf(rs.getString("AssertionLibrary"));
        boolean isActive = rs.getBoolean("Active");
        boolean isPuzzleClass = rs.getBoolean("Puzzle");
        Integer parentClassId = rs.getInt("Parent_Class");
        if (rs.wasNull()) {
            parentClassId = null;
        }

        return GameClass.build()
                .id(classId)
                .name(name)
                .alias(alias)
                .javaFile(absoluteJavaFile)
                .classFile(absoluteClassFile)
                .mockingEnabled(requireMocking)
                .testingFramework(testingFramework)
                .assertionLibrary(assertionLibrary)
                .active(isActive)
                .puzzleClass(isPuzzleClass)
                .parentClassId(parentClassId)
                .create();
    }

    /**
     * Constructs class information from a {@link ResultSet} entry.
     *
     * @param rs The {@link ResultSet}.
     * @return The constructed game class information instance.
     */
    private static GameClassInfo gameClassInfoFromRS(ResultSet rs) throws SQLException {
        GameClass gc = gameClassFromRS(rs);
        int gamesWithClass = rs.getInt("games_count");

        return new GameClassInfo(gc, gamesWithClass);
    }

    /**
     * Retrieves a game class for a given identifier.
     *
     * @param classId the given identifier.
     * @return a {@link GameClass} instance, or {@code null}. // TODO change
     */
    public Optional<GameClass> getClassForId(int classId) {
        @Language("SQL") String query = "SELECT * FROM classes WHERE Class_ID = ?;";

        return queryRunner.query(query, oneFromRS(GameClassRepository::gameClassFromRS), classId);
    }

    /**
     * Retrieves a game class for a given identifier of a {@link AbstractGame Game}.
     *
     * @param gameId the given game identifier.
     * @return a {@link GameClass} instance, or {@code null}. // TODO change
     */
    public Optional<GameClass> getClassForGameId(int gameId) {
        @Language("SQL") String query = """
                SELECT classes.*
                FROM classes
                INNER JOIN games
                  ON classes.Class_ID = games.Class_ID
                WHERE games.ID=?;
        """;
        return queryRunner.query(query, oneFromRS(GameClassRepository::gameClassFromRS), gameId);
    }

    /**
     * Retrieves all (excluding puzzle) game classes in a {@link List}. Even non-playable or
     * inactive classes. These classes should never be shown to non-admin users, use
     * {@link #getAllPlayableClasses()} instead.
     *
     * <p>If no game classes are found, the list is empty, but not {@link null}.
     *
     * @return all game classes.
     */
    public List<GameClassInfo> getAllClassInfos() {
        @Language("SQL") String query = """
                SELECT classes.*,
                    (SELECT COUNT(games.ID) from games WHERE games.Class_ID = classes.Class_ID) as games_count
                FROM classes
                WHERE Puzzle != 1
                GROUP BY classes.Class_ID;
        """;

        return queryRunner.query(query, listFromRS(GameClassRepository::gameClassInfoFromRS));
    }

    /**
     * Retrieves all playable game classes in a {@link List}. You can use a playable
     * class for the creation of games.
     *
     * <p>If no game classes are found, the list is empty, but not {@link null}.
     *
     * @return all game classes.
     */
    public List<GameClass> getAllPlayableClasses() {
        @Language("SQL") String query = "SELECT * FROM view_playable_classes;";

        return queryRunner.query(query, listFromRS(GameClassRepository::gameClassFromRS));
    }

    /**
     * Checks for a given class identifier whether at least one game
     * with this class exists.
     *
     * @param classId the class identifier of the checked class.
     * @return {@code true} if at least one game does exist, {@code false} otherwise.
     */
    public boolean gamesExistsForClass(Integer classId) {
        @Language("SQL") String query = """
                SELECT (COUNT(games.ID) > 0) AS games_exist
                FROM games
                WHERE games.Class_ID = ?
        """;
        return queryRunner.query(query, rs -> rs.getBoolean("games_exist"), classId);
    }

    /**
     * Checks for a given alias whether a class with this alias already exists.
     *
     * @param alias the alias that is checked.
     * @return {@code true} if alias does exist, {@code false} otherwise.
     */
    public boolean classExistsForAlias(String alias) throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = "SELECT * FROM classes WHERE Alias = ?";
        return queryRunner.query(query, oneFromRS(rs -> true), alias).orElse(false);
    }

    /**
     * Return a {@link List} of all identifiers of {@link Mutant Mutants}, which were uploaded
     * together with a given class.
     *
     * @param classId the identifier of the given class
     * @return a list of identifiers of mutants
     */
    public List<Integer> getMappedMutantIdsForClassId(Integer classId)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT Mutant_ID
                FROM mutant_uploaded_with_class
                WHERE Class_ID = ?
        """;
        return queryRunner.query(query, listFromRS(rs -> rs.getInt("Mutant_ID")), classId);
    }

    /**
     * Return a {@link List} of all {@link Mutant Mutants}, which were uploaded
     * together with a given class.
     *
     * @param classId the identifier of the given class
     * @return a list of mutants
     */
    public List<Mutant> getMappedMutantsForClassId(Integer classId)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT mutants.*
                FROM mutants, mutant_uploaded_with_class up
                WHERE up.Class_ID = ?
                   AND up.Mutant_ID = mutants.Mutant_ID
        """;
        return queryRunner.query(query, listFromRS(MutantRepository::mutantFromRS), classId);
    }

    /**
     * Return a {@link List} of all identifiers of {@link Test Tests}, which were uploaded
     * together with a given class.
     *
     * @param classId the identifier of the given class
     * @return a list of identifiers of tests
     */
    public List<Integer> getMappedTestIdsForClassId(Integer classId)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT Test_ID
                FROM test_uploaded_with_class
                WHERE Class_ID = ?;
        """;
        return queryRunner.query(query, listFromRS(rs -> rs.getInt("Test_ID")), classId);
    }

    /**
     * Return a {@link List} of all {@link Test Tests}, which were uploaded
     * together with a given class.
     *
     * @param classId the identifier of the given class
     * @return a list of tests
     */
    public List<Test> getMappedTestsForClassId(Integer classId)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT tests.*
                FROM tests, test_uploaded_with_class up
                WHERE up.Class_ID = ?
                   AND up.Test_ID = tests.Test_ID
        """;
        return queryRunner.query(query, listFromRS(TestRepository::testFromRS), classId);
    }

    /**
     * Return a {@link List} of all identifiers of {@link Dependency Dependencies},
     * which were uploaded together with a given class.
     *
     * @param classId the identifier of the given class
     * @return a list of identifiers of dependencies
     */
    public List<Integer> getMappedDependencyIdsForClassId(Integer classId)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = "SELECT Dependency_ID FROM dependencies WHERE Class_ID = ?;";
        return queryRunner.query(query, listFromRS(rs -> rs.getInt("Dependency_ID")), classId);
    }

    /**
     * Return a {@link List} of all {@link Dependency Dependencies},
     * which were uploaded together with a given class.
     *
     * @param classId the identifier of the given class
     * @return a list of identifiers of dependencies
     */
    public List<Dependency> getMappedDependenciesForClassId(Integer classId)
            throws UncheckedSQLException, SQLMappingException {
        @Language("SQL") String query = """
                SELECT
                   Dependency_ID,
                   JavaFile,
                   ClassFile
                FROM dependencies WHERE Class_ID = ?;
        """;
        return queryRunner.query(query,
                listFromRS(rs -> DependencyDAO.dependencyFromRS(rs, classId)),
                classId);
    }

    /**
     * Stores a given {@link GameClass} in the database and
     * returns an ID.
     *
     * @param cut the given class as a {@link GameClass}.
     * @return An identifier as an integer value.
     * @throws UncheckedSQLException If storing the class was not successful.
     */
    public int storeClass(GameClass cut) throws UncheckedSQLException {
        String name = cut.getName();
        String alias = cut.getAlias();
        String relativeJavaFile = FileUtils.getRelativeDataPath(cut.getJavaFile()).toString();
        String relativeClassFile = FileUtils.getRelativeDataPath(cut.getClassFile()).toString();
        boolean isMockingEnabled = cut.isMockingEnabled();
        TestingFramework testingFramework = cut.getTestingFramework();
        AssertionLibrary assertionLibrary = cut.getAssertionLibrary();
        boolean isPuzzleClass = cut.isPuzzleClass();
        Integer parentClassId = cut.getParentClassId();
        boolean isActive = cut.isActive();

        @Language("SQL") String query = """
                INSERT INTO classes (
                    Name,
                    Alias,
                    JavaFile,
                    ClassFile,
                    RequireMocking,
                    TestingFramework,
                    AssertionLibrary,
                    Puzzle,
                    Parent_Class,
                    Active
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        var key = queryRunner.insert(query,
                generatedKeyFromRS(),
                name,
                alias,
                relativeJavaFile,
                relativeClassFile,
                isMockingEnabled,
                testingFramework.name(),
                assertionLibrary.name(),
                isPuzzleClass,
                parentClassId,
                isActive);

        if (key.isPresent()) {
            logger.debug("Successfully stored class in database[Name={}, Alias={}].", cut.getName(), cut.getAlias());
            cut.setId(key.get());
            return key.get();
        } else {
            throw new UncheckedSQLException("Could not store class to database.");
        }
    }

    /**
     * Updates a given {@link GameClass} in the database and returns whether
     * updating was successful or not.
     *
     * @param cut the given class as a {@link GameClass}.
     * @return whether updating was successful or not
     * @throws UncheckedSQLException If storing the class was not successful.
     */
    public boolean updateClass(GameClass cut) throws UncheckedSQLException {
        int classId = cut.getId();
        String alias = cut.getAlias();
        boolean isMockingEnabled = cut.isMockingEnabled();
        boolean isActive = cut.isActive();

        @Language("SQL") String query = """
                UPDATE classes
                  SET
                  Alias = ? ,
                  RequireMocking = ? ,
                  Active = ?
                WHERE class_ID = ?
        """;

        return queryRunner.update(query,
                alias,
                isMockingEnabled,
                isActive,
                classId) > 0;
    }

    /**
     * Removes a class for a given identifier.
     *
     * @param id the identifier of the class to be removed.
     * @return {@code true} for successful removal, {@code false} otherwise.
     */
    public boolean removeClassForId(int id) {
        @Language("SQL") String query = "DELETE FROM classes WHERE Class_ID = ?;";

        return queryRunner.update(query, id) > 0;
    }

    /**
     * <b>This method should be treated with caution. Read the first two paragraphs first.</b>
     *
     * <p>Call {@link #gamesExistsForClass(Integer)} beforehand to make sure the class is
     * not actually used.
     *
     * <p>If a {@link Puzzle} is referencing the to be deleted class, the puzzle will be removed too.
     *
     * <p>Removes a class and all of its corresponding dependencies, mutants and tests
     * from the database for a given identifier. Does not remove the files associated to the class,
     * its dependencies, mutants and tests.
     *
     * @param id the identifier of the class to be removed.
     * @return {@code true} for successful removal, {@code false} otherwise.
     */
    public boolean forceRemoveClassForId(int id) {
        @Language("SQL") String query1 = "DELETE FROM dependencies WHERE Class_ID = ?;";
        @Language("SQL") String query2 = "DELETE FROM mutant_uploaded_with_class WHERE Class_ID = ?;";
        @Language("SQL") String query3 = "DELETE FROM test_uploaded_with_class WHERE Class_ID = ?;";
        @Language("SQL") String query4 = """
                DELETE FROM targetexecutions
                WHERE Mutant_ID IN
                (SELECT Mutant_ID FROM mutants WHERE Class_ID = ?);
        """;
        @Language("SQL") String query5 = "DELETE FROM mutants WHERE Class_ID = ?;";
        @Language("SQL") String query6 = "DELETE FROM tests WHERE Class_ID = ?;";

        queryRunner.update(query1, id);
        queryRunner.update(query2, id);
        queryRunner.update(query3, id);
        queryRunner.update(query4, id);
        queryRunner.update(query5, id);
        queryRunner.update(query6, id);

        return removeClassForId(id);
    }

    /**
     * Removes multiple classes for a given list of identifiers.
     *
     * @param classes the identifiers of the classes to be removed.
     */
    public void removeClassesForIds(List<Integer> classes) {
        if (classes.isEmpty()) {
            return;
        }

        @Language("SQL") String query = "DELETE FROM classes WHERE Class_ID = ?;";

        queryRunner.batch(query, batchParamsFromList(classes));
    }

    /**
     * Checks for which of the given IDs classes exist in the database.
     *
     * @param ids The class IDs to check.
     * @return The given class IDs for which classes exist.
     */
    public List<Integer> filterExistingClassIDs(Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        String idsString = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        @Language("SQL") String query =
                "SELECT Class_ID FROM classes WHERE Class_ID in (%s);".formatted(idsString);
        return queryRunner.query(query, listFromRS(rs -> rs.getInt("Class_ID")));
    }

    public String nextFreeAlias(String bareAlias) {
        @Language("SQL") String query = """
                SELECT *
                FROM classes
                WHERE Alias LIKE CONCAT(?, '%');
                """;
        List<String> similarAliases = queryRunner.query(query, listFromRS(rs -> rs.getString("Alias")), bareAlias);
        return nextFreeName(similarAliases, bareAlias);
    }
}
