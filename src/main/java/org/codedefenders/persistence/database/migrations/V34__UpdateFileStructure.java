/*
 * Copyright (C) 2021 Code Defenders contributors
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

package org.codedefenders.persistence.database.migrations;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.apache.commons.dbutils.QueryRunner;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.DependencyDAO;
import org.codedefenders.game.GameClass;
import org.codedefenders.model.Dependency;
import org.codedefenders.persistence.database.GameClassRepository;
import org.codedefenders.persistence.database.util.ResultSetUtils;
import org.codedefenders.util.Constants;
import org.codedefenders.util.FileUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

@Singleton
public class V34__UpdateFileStructure extends BaseJavaMigration {

    private final String dataDir;
    private Connection conn;
    private final QueryRunner queryRunner = new QueryRunner();

    @Inject
    public V34__UpdateFileStructure(@SuppressWarnings("CdiInjectionPointsInspection") Configuration config) {
        String dataDirPath = config.getDataDir().toString();
        if (!dataDirPath.endsWith("/")) {
            dataDir = dataDirPath + "/";
        } else {
            dataDir = dataDirPath;
        }
    }

    @Override
    public void migrate(Context context) throws Exception {
        conn = context.getConnection();
        String query = "SELECT * FROM classes WHERE JavaFile NOT LIKE '%classes/%';";
        List<GameClass> wrongClasses = queryRunner.query(conn, query,
                ResultSetUtils.listFromRS(GameClassRepository::gameClassFromRS));
        for (GameClass gameClass : wrongClasses) {

            //Move .java-file from CuT into correct package structure
            Path originalCutPath = Path.of(dataDir).resolve(gameClass.getJavaFile());
            String javaFileNameWithoutExtension = originalCutPath.getFileName().toString().replace(".java", "");
            Path packageStructure = FileUtils.getPackagePathFromJavaFile(originalCutPath.toString());
            Path classesDir = originalCutPath.getParent().resolve(Constants.CUTS_CLASSES_DIR);
            Files.createDirectories(classesDir);
            Path newJavaFilePath = Files.move(originalCutPath,
                    classesDir.resolve(packageStructure).resolve(originalCutPath.getFileName()));

            //Move generated .class-files from the CuT, including inner classes, into correct package structure
            try (Stream<Path> findStream = Files.find(originalCutPath.resolve(packageStructure), 1,
                    (path, attr) -> path.toString().matches(
                            javaFileNameWithoutExtension + "[.]*\\.class"))) {
                List<Path> cutClassFiles = findStream.toList();
                for (Path cutClassFile : cutClassFiles) {
                    Files.move(cutClassFile, classesDir.resolve(packageStructure)
                            .resolve(cutClassFile.getFileName()));
                }
            } catch (FileNotFoundException ignored) {
                //This should only happen for very specific edge cases that were broken before this refactoring.
                //Since it was broken before, it should not be fixed now.
            }

            //Adjust DB entries for CuT
            Path newClassFile = newJavaFilePath.resolveSibling(newJavaFilePath.getFileName().toString()
                    .replace(".java", ".class"));
            String updateSQL = "UPDATE classes SET JavaFile = ?, ClassFile = ? WHERE Class_ID = ?;";
            queryRunner.update(conn, updateSQL, newJavaFilePath.toString(), newClassFile.toString(), gameClass.getId());

            if (Files.exists(originalCutPath.resolve(Constants.CUTS_DEPENDECY_DIR))) {
                //Move dependencies
                FileUtils.copyFileTree(originalCutPath.resolve(Constants.CUTS_DEPENDECY_DIR), classesDir);

                //Adjust DB entries for dependencies
                int classId = gameClass.getId();
                String dependencyQuery = "SELECT * FROM dependencies WHERE class_id = ?;";
                //TODO Warum wird an dieser Stelle die classId als Argument gebraucht? Warum nicht aus dem ResultSet
                //TODO holen?
                List<Dependency> dependencies = queryRunner.query(conn, dependencyQuery,
                        ResultSetUtils.listFromRS((rs) -> DependencyDAO.dependencyFromRS(rs, classId)));
                for (Dependency dependency : dependencies) {
                    int depId = dependency.getId();
                    String[] oldJavaFile = dependency.getJavaFile().split(System.lineSeparator());
                    String[] oldClassFile = dependency.getClassFile().split(System.lineSeparator());
                    oldJavaFile[3] = oldJavaFile[3].replace("dependencies", "classes");
                    oldClassFile[3] = oldClassFile[3].replace("dependencies", "classes");
                    String newDepJavaFile = String.join(System.lineSeparator(), oldJavaFile);
                    String newDepClassFile = String.join(System.lineSeparator(), oldClassFile);

                    String updateDepSQL = "UPDATE dependencies SET JavaFile = ?, ClassFile = ? " +
                            "WHERE Dependency_ID = ?;";
                    queryRunner.update(conn, updateDepSQL, newDepJavaFile, newDepClassFile, depId);
                    //TODO Muss das noch committet werden?
                }

                //Remove old dependency directory
                org.apache.commons.io.FileUtils.deleteDirectory(
                        originalCutPath.resolve(Constants.CUTS_DEPENDECY_DIR).toFile());
            }
        }
    }
}
