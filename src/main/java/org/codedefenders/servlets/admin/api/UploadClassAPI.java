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
package org.codedefenders.servlets.admin.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.dto.api.ClassUpload;
import org.codedefenders.execution.CompileException;
import org.codedefenders.execution.Compiler;
import org.codedefenders.game.AssertionLibrary;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Test;
import org.codedefenders.game.TestingFramework;
import org.codedefenders.persistence.database.SettingsRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.util.APIUtils;
import org.codedefenders.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import static org.codedefenders.util.Constants.CUTS_DIR;

/**
 * This {@link HttpServlet} offers an API for {@link Test tests}.
 *
 * <p>A {@code GET} request with the {@code testId} parameter results in a JSON string containing
 * test information, including the source code.
 *
 * <p>Serves on path: {@code /api/test}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
@WebServlet("/admin/api/class/upload")
public class UploadClassAPI extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(UploadClassAPI.class);
    final Map<String, Class<?>> parameterTypes = new HashMap<String, Class<?>>() {
        {
            put("name", String.class);
            put("source", String.class);
        }
    };
    @Inject
    CodeDefendersAuth login;
    @Inject
    GameService gameService;
    @Inject
    SettingsRepository settingsRepository;
    @Inject
    UserRepository userRepository;
    @Inject
    UserService userService;

    private static void abortRequestAndCleanUp(Path cutDir) throws IOException {
        if (cutDir != null) {
            try {
                logger.info("Removing directory {} again", cutDir);
                org.apache.commons.io.FileUtils.forceDelete(cutDir.toFile());
            } catch (IOException e) {
                // logged, but otherwise ignored. No need to abort while aborting.
                logger.error("Error removing directory of compiled classes.", e);
            }
        }
    }

    private boolean validateAlias(String alias) {
        return alias.matches("^[a-zA-Z0-9]+\\.java$");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final ClassUpload uploadedClass;
        try {
            uploadedClass = (ClassUpload) APIUtils.parsePostOrRespondJsonError(request, response, ClassUpload.class);
        } catch (JsonParseException e) {
            return;
        }
        String name = uploadedClass.getName();
        String source = uploadedClass.getSource();

        if (!name.endsWith(".java")) {
            APIUtils.respondJsonError(response, "Class upload failed. The class under test must be a .java file.");
            return;
        }
        if (!validateAlias(name)) {
            APIUtils.respondJsonError(response, "Class upload failed. Name must not contain whitespaces or special characters.");
            return;
        }
        if (source == null || source.trim().isEmpty()) {
            APIUtils.respondJsonError(response, "Class source cannot be empty.");
            return;
        }

        String prefix = login.getUser().getName();
        String classAlias = prefix + "_" + name.replace(".java", "");
        Path cutDir = Paths.get(CUTS_DIR, classAlias);
        if (GameClassDAO.classExistsForAlias(classAlias) || Files.exists(cutDir)) {
            APIUtils.respondJsonError(response, "The class name or file already exist", HttpServletResponse.SC_CONFLICT);
            return;
        }

        TestingFramework testingFramework = TestingFramework.JUNIT4;
        AssertionLibrary assertionLibrary = AssertionLibrary.JUNIT4_HAMCREST;

        final int cutId;
        final GameClass cut;
        boolean isMockingEnabled = false;

        final String fileContent = source.trim();

        final String cutJavaFilePath;
        try {
            cutJavaFilePath = FileUtils.storeFile(cutDir, name, fileContent).toString();
        } catch (IOException e) {
            APIUtils.respondJsonError(response, "Class upload failed. Could not store java file\n" + e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            abortRequestAndCleanUp(cutDir);
            return;
        }

        final String cutClassFilePath;
        try {
            cutClassFilePath = Compiler.compileJavaFileForContent(cutJavaFilePath, fileContent);
        } catch (CompileException e) {
            APIUtils.respondJsonError(response, cutJavaFilePath + " " + fileContent + "Class upload failed. Could not compile " + name + "!\n" + e.getMessage());
            abortRequestAndCleanUp(cutDir);
            return;
        } catch (IllegalStateException e) {
            APIUtils.respondJsonError(response, "SEVERE ERROR. Could not find Java compiler. Please reconfigure your " + "installed version.\n" + e,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            abortRequestAndCleanUp(cutDir);
            return;
        }

        String classQualifiedName;
        try {
            classQualifiedName = FileUtils.getFullyQualifiedName(cutClassFilePath);
        } catch (IOException e) {
            APIUtils.respondJsonError(response, "Class upload failed. Could not get fully qualified name for " + name + "\n" + e, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            abortRequestAndCleanUp(cutDir);
            return;
        }

        cut = GameClass.build().name(classQualifiedName).alias(classAlias).javaFile(cutJavaFilePath).classFile(cutClassFilePath).mockingEnabled(isMockingEnabled)
                .testingFramework(testingFramework).assertionLibrary(assertionLibrary).create();
        try {
            cutId = GameClassDAO.storeClass(cut);
        } catch (Exception e) {
            APIUtils.respondJsonError(response, "Class upload failed. Could not store class to database.", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            abortRequestAndCleanUp(cutDir);
            return;
        }

        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        Gson gson = new Gson();
        JsonObject root = new JsonObject();
        root.add("classId", gson.toJsonTree(cutId, Integer.class));
        out.print(new Gson().toJson(root));
        out.flush();
    }

    private enum CompileClassType {
        CUT, DEPENDENCY, MUTANT, TEST
    }

    private static class SimpleFile {
        private final String fileName;
        private final byte[] fileContent;

        SimpleFile(String fileName, byte[] fileContent) {
            this.fileName = fileName;
            this.fileContent = fileContent;
        }
    }

    private static class CompiledClass {
        private final CompileClassType type;
        private final Integer id;

        CompiledClass(CompileClassType type, Integer id) {
            this.type = type;
            this.id = id;
        }
    }

}
