/*
 * Copyright (C) 2025 Code Defenders contributors
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

import java.io.File;
import java.io.IOException;
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
    private final File sourcesDir;
    private final QueryRunner queryRunner = new QueryRunner();

    @Inject
    public V34__UpdateFileStructure(@SuppressWarnings("CdiInjectionPointsInspection") Configuration config) {
        String dataDirPath = config.getDataDir().toString();
        sourcesDir = config.getSourcesDir();
        if (!dataDirPath.endsWith("/")) {
            dataDir = dataDirPath + "/";
        } else {
            dataDir = dataDirPath;
        }
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection conn = context.getConnection();
        Path dataPath = Path.of(dataDir);

        String query = "SELECT * FROM classes WHERE JavaFile NOT LIKE '%classes/%';";
        List<GameClass> wrongClasses = queryRunner.query(conn, query,
                ResultSetUtils.listFromRS(GameClassRepository::gameClassFromRS));

        for (GameClass gameClass : wrongClasses) {
            //Move .java-file from CuT into correct package structure
            Path originalCutPath = dataPath.resolve(gameClass.getJavaFile());
            Path classRoot = originalCutPath.getParent();
            String javaFileNameWithoutExtension = originalCutPath.getFileName().toString().replace(".java", "");
            Path packageStructure = FileUtils.getPackagePathFromJavaFile(Files.readString(originalCutPath));
            Path classesDir = classRoot.resolve(Constants.CUTS_CLASSES_DIR);
            Files.createDirectories(classesDir.resolve(packageStructure));
            Path fullJavaFilePath = Files.move(originalCutPath,
                    classesDir.resolve(packageStructure).resolve(originalCutPath.getFileName()));

            //Move generated .class-files from the CuT, including inner classes, into correct package structure
            moveClassFiles(classRoot.resolve(packageStructure),
                    classesDir.resolve(packageStructure), javaFileNameWithoutExtension);

            //Adjust DB entries for CuT
            String newJavaFileEntry = dataPath.relativize(fullJavaFilePath).toString();

            String newClassFileEntry = newJavaFileEntry.replace(".java", ".class");
            String updateSQL = "UPDATE classes SET JavaFile = ?, ClassFile = ? WHERE Class_ID = ?;";
            queryRunner.update(conn, updateSQL, newJavaFileEntry, newClassFileEntry, gameClass.getId());

            Path dependenciesDir = classRoot.resolve("dependencies");
            if (Files.exists(dependenciesDir)) {
                int classId = gameClass.getId();
                List<Path> dependencyJavaFiles =
                        FileUtils.getAllFilesOfTypeInDirectory(dependenciesDir, ".java");
                for (Path dependencyJavaFile : dependencyJavaFiles) {
                    //Move .java-file
                    String content = Files.readString(dependencyJavaFile);
                    Path dependencyPackagePath = FileUtils.getPackagePathFromJavaFile(content);
                    Path targetPath = classesDir
                            .resolve(dependencyPackagePath)
                            .resolve(dependencyJavaFile.getFileName());
                    Files.createDirectories(targetPath.getParent());
                    Files.move(dependencyJavaFile, targetPath);

                    //Update DB entry
                    String newDepJavaFile = dataPath.relativize(targetPath).toString();
                    String newDepClassFile = newDepJavaFile.replaceAll(".java$", ".class");
                    String updateDepSQL = "UPDATE dependencies SET JavaFile = ?, ClassFile = ? " +
                            "WHERE Class_ID = ? AND JavaFile = ?;";
                    queryRunner.update(conn, updateDepSQL, newDepJavaFile, newDepClassFile, classId,
                            dataPath.relativize(dependencyJavaFile).toString());
                }
                List<Path> dependencyClassFiles =
                        FileUtils.getAllFilesOfTypeInDirectory(dependenciesDir, ".class");
                //Class files are already in the correct relative location, just have to be moved out of the
                //dependencies directory
                for (Path dependencyClassFile : dependencyClassFiles) {
                    Path classFileRelativePath = dependenciesDir.relativize(dependencyClassFile);
                    Path targetPath = classesDir.resolve(classFileRelativePath);
                    Files.move(dependencyClassFile, targetPath);
                }


            }
        }
        FileUtils.deleteEmptySubdirectories(sourcesDir);
    }

    static void moveClassFiles(Path source, Path target, String classname) throws IOException {
        String sep = FileUtils.getFileSeparator().equals("\\") ? "\\\\" : FileUtils.getFileSeparator();
        try (Stream<Path> findStream = Files.find(source, 1,
                (path, attr) -> path.toString().matches(
                        "(.*" + sep + ")*" + classname + "(\\$[^\n\r" + sep + "]*)?\\.class"))) {
            findStream.forEach((path) -> {
                try {
                    Files.move(path, target.resolve(path.getFileName()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
