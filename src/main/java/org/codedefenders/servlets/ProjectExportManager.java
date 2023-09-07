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
package org.codedefenders.servlets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Role;
import org.codedefenders.model.Dependency;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.ZipFileUtils;

/**
 * This {@link HttpServlet} handles requests for exporting a {@link GameClass}
 * as a Gradle project.
 *
 * <p>Serves on path: {@code /project-export}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
@WebServlet(org.codedefenders.util.Paths.PROJECT_EXPORT)
public class ProjectExportManager extends HttpServlet {

    private static final Path mainDir = Paths.get("src/main/java");
    private static final Path testDir = Paths.get("src/test/java");
    private static final Path exporterDir = Paths.get("project-exporter");

    private static final String[] gradleFiles = {
        "build.gradle",
        "gradlew",
        "gradlew.bat",
        "gradle/wrapper/gradle-wrapper.jar",
        "gradle/wrapper/gradle-wrapper.properties"
    };

    @Inject
    private CodeDefendersAuth login;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        final Optional<Integer> gameId = ServletUtils.gameId(request);
        if (gameId.isEmpty()) {
            Redirect.redirectBack(request, response);
            return;
        }

        if (GameDAO.getRole(login.getUserId(), gameId.get()) == Role.NONE) {
            Redirect.redirectBack(request, response);
            return;
        }

        GameClass cut = GameClassDAO.getClassForGameId(gameId.get());
        Path packagePath = Paths.get(cut.getPackage().replace(".", "/"));
        List<Dependency> dependencies = GameClassDAO.getMappedDependenciesForClassId(cut.getId());

        final Set<Path> paths = dependencies
                .stream()
                .map(Dependency::getJavaFile)
                .map(Paths::get)
                .collect(Collectors.toSet());
        paths.add(Paths.get(cut.getJavaFile()));

        final Map<String, byte[]> files = new HashMap<>();
        {
            final String templateFileName = testDir
                    .resolve(packagePath.resolve("Test" + Paths.get(cut.getJavaFile()).getFileName().toString()))
                    .toString();
            final byte[] templateFileContent = cut.getTestTemplate().getBytes();
            files.put(templateFileName, templateFileContent);
        }
        files.put("settings.gradle", ("rootProject.name = 'Code Defenders - " + cut.getBaseName() + "'").getBytes());

        for (Path path : paths) {
            String filePath = mainDir.resolve(packagePath.resolve(path.getFileName())).toString();
            byte[] fileContent = Files.readAllBytes(path);
            files.put(filePath, fileContent);
        }

        Path gradleDir = Paths.get(getServletContext().getRealPath("/")).resolve(exporterDir);
        for (String gradleFilePath : gradleFiles) {
            byte[] fileContent = Files.readAllBytes(gradleDir.resolve(gradleFilePath));
            files.put(gradleFilePath, fileContent);
        }

        byte[] zipFileBytes = ZipFileUtils.zipFiles(files);

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=CodeDefenders_" + cut.getBaseName() + ".zip");

        ServletOutputStream out = response.getOutputStream();
        out.write(zipFileBytes);
        out.flush();
        out.close();
    }

}
