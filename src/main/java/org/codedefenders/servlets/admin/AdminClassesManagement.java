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
package org.codedefenders.servlets.admin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.game.GameClass;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link HttpServlet} handles admin requests for managing {@link org.codedefenders.game.GameClass GameClasses}.
 *
 * <p>Serves on path: {@code /admin/classes}.
 */
@WebServlet(Paths.ADMIN_CLASSES)
public class AdminClassesManagement extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminClassesManagement.class);

    @Inject
    private MessagesBean messages;

    @Inject
    private URLUtils url;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.setAttribute("classInfos", GameClassDAO.getAllClassInfos());

        request.getRequestDispatcher(Constants.ADMIN_CLASSES_JSP).forward(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final String formType = ServletUtils.formType(request);
        switch (formType) {
            case "setClassInactive":
            case "setClassActive": {
                boolean active = formType.equals("setClassActive");
                String newState = active ? "active" : "inactive";

                final Optional<Integer> classId = ServletUtils.getIntParameter(request, "classId");
                if (classId.isEmpty()) {
                    logger.warn("Setting class as " + newState + " failed. Missing request parameter 'classId'.");
                    messages.add("Failed to set class as " + newState + ".");
                    break;
                }

                logger.info("Setting class as " + newState + "...");
                if (setClassActive(classId.get(), active)) {
                    logger.info("Successfully set class as " + newState + "!");
                    messages.add("Successfully set class as " + newState + "!");
                } else {
                    logger.info("Failed to set class as " + newState + "!");
                    messages.add("Failed to set class as " + newState + "!");
                }
                break;
            }
            case "classRemoval": {
                final Optional<Integer> classId = ServletUtils.getIntParameter(request, "classId");
                if (classId.isEmpty()) {
                    logger.warn("Removing class failed. Missing request parameter 'classId'.");
                    messages.add("Failed to remove class.");
                    break;
                }
                logger.info("Removing class...");
                if (GameClassDAO.gamesExistsForClass(classId.get())) {
                    logger.info("Failed to remove class {}! At least one game already exists.", classId.get());
                    messages.add("Failed to remove class! There are existing games with this class.");
                    break;
                }
                if (forceRemoveClass(classId.get())) {
                    logger.info("Successfully removed class {}", classId.get());
                    messages.add("Successfully removed class!");
                } else {
                    logger.warn("Failed to remove class {}!", classId.get());
                    messages.add("Failed to remove class!");
                }
                break;
            }
            default:
                logger.error("Action {" + formType + "} not recognised.");
                break;
        }

        response.sendRedirect(url.forPath(Paths.ADMIN_CLASSES));
    }

    private boolean setClassActive(int classId, boolean active) {
        final GameClass gameClass = GameClassDAO.getClassForId(classId);
        if (gameClass == null || gameClass.isPuzzleClass()) {
            return false;
        }
        gameClass.setActive(active);
        return gameClass.update();
    }

    private boolean forceRemoveClass(int classId) {
        final GameClass gameClass = GameClassDAO.getClassForId(classId);
        if (gameClass == null || gameClass.isPuzzleClass()) {
            return false;
        }

        final boolean removalSuccess = GameClassDAO.forceRemoveClassForId(classId);
        if (!removalSuccess) {
            return false;
        }

        final String javaFile = gameClass.getJavaFile();
        final Path parent = java.nio.file.Paths.get(javaFile).getParent();
        try {
            org.apache.commons.io.FileUtils.forceDelete(parent.toFile());
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
