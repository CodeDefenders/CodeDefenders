/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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
import java.util.List;

import org.codedefenders.database.DB.RSMapper;
import org.codedefenders.model.Dependency;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.util.FileUtils;
import org.intellij.lang.annotations.Language;

import static org.codedefenders.persistence.database.util.QueryUtils.batchParamsFromList;

/**
 * This class handles the database logic for dependencies.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 * @see Dependency
 */
public class DependencyDAO {

    /**
     * Constructs a dependency from a {@link ResultSet} entry.
     * @param rs The {@link ResultSet}.
     * @return The constructed dependency.
     * @see RSMapper
     */
    public static Dependency dependencyFromRS(ResultSet rs) throws SQLException {
        final int id = rs.getInt("Dependency_ID");
        final int classId = rs.getInt("Class_ID");
        final String javaFile = rs.getString("JavaFile");
        String absoluteJavaFile = FileUtils.getAbsoluteDataPath(javaFile).toString();
        return new Dependency(id, classId, absoluteJavaFile);
    }

    /**
     * Stores a given {@link Dependency} in the database.
     *
     * @param dependency the given dependency as a {@link Dependency}.
     * @return the generated identifier of the dependency as an {@code int}.
     * @throws Exception If storing the dependency was not successful.
     */
    public static int storeDependency(Dependency dependency) {
        int classId = dependency.getClassId();
        String relativeJavaFile = FileUtils.getRelativeDataPath(dependency.getJavaFile()).toString();

        @Language("SQL") String query = "INSERT INTO dependencies (Class_ID, JavaFile) VALUES (?, ?);";
        DatabaseValue<?>[] values = new DatabaseValue[]{
                DatabaseValue.of(classId),
                DatabaseValue.of(relativeJavaFile)
        };

        final int result = DB.executeUpdateQueryGetKeys(query, values);
        if (result != -1) {
            return result;
        } else {
            throw new UncheckedSQLException("Could not store dependency to database.");
        }
    }

    /**
     * Removes a dependency for a given identifier.
     *
     * @param id the identifier of the dependency to be removed.
     * @return {@code true} for successful removal, {@code false} otherwise.
     */
    public static boolean removeDependencyForId(Integer id) {
        @Language("SQL") String query = "DELETE FROM dependencies WHERE Dependency_ID = ?;";
        DatabaseValue<?>[] values = new DatabaseValue[]{
                DatabaseValue.of(id),
        };

        return DB.executeUpdateQuery(query, values);
    }

    /**
     * Removes multiple dependencies for a given list of identifiers.
     *
     * @param dependencies the identifiers of the dependencies to be removed.
     */
    public static void removeDependenciesForIds(List<Integer> dependencies) {
        if (dependencies.isEmpty()) {
            return;
        }

        @Language("SQL") String query = "DELETE FROM dependencies WHERE Dependency_ID = ?;";
        QueryRunner queryRunner = CDIUtil.getBeanFromCDI(QueryRunner.class);

        queryRunner.batch(query, batchParamsFromList(dependencies));
    }
}
