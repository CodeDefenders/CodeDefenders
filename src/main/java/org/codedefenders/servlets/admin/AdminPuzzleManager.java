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

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.codedefenders.execution.BackendExecutorService;
import org.codedefenders.installer.Installer;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.util.Constants;
import org.codedefenders.util.ZipFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * This {@link HttpServlet} handles admin management of puzzles.
 * <p>
 * {@code GET} requests redirect to the admin puzzle page
 * and {@code POST} requests handle batch uploading puzzle related information.
 * <p>
 * Serves under {@code /admin/puzzles}.
 *
 * @author <a href=https://github.com/werli>Phil Werli</a>
 */
public class AdminPuzzleManager extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminPuzzleManager.class);

    @Inject
    private BackendExecutorService backend;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher(Constants.ADMIN_PUZZLE_JSP).forward(request, response);
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        List<FileItem> items;
        try {
            items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
        } catch (FileUploadException e) {
            logger.error("Failed to upload puzzles. Failed to get file upload parameters.", e);
            Redirect.redirectBack(request, response);
            return;
        }

        final Map<Boolean, List<FileItem>> parameters = items.stream().collect(Collectors.partitioningBy(FileItem::isFormField));
        final List<FileItem> uploadParameters = parameters.get(true);
        final List<FileItem> fileParameters = parameters.get(false);

        String action = null;

        for (FileItem uploadParameter : uploadParameters) {
            final String fieldName = uploadParameter.getFieldName();
            final String fieldValue = uploadParameter.getString();
            logger.debug("Upload parameter {" + fieldName + ":" + fieldValue + "}");
            switch (fieldName) {
                case "formType": {
                    action = fieldValue;
                    break;
                }
                default: {
                    logger.warn("Unrecognized parameter: " + fieldName);
                    Redirect.redirectBack(request, response);
                    return;
                }
            }
        }

        if (action == null) {
            logger.warn("No formType provided. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }

        switch (action) {
            case "uploadPuzzles": {
                createPuzzles(request, fileParameters);
                Redirect.redirectBack(request, response);
                break;
            }
            default: {
                logger.info("Action not recognised: {}", action);
                Redirect.redirectBack(request, response);
                break;
            }
        }
    }

    @SuppressWarnings("Duplicates")
    private void createPuzzles(HttpServletRequest request, List<FileItem> fileParameters) throws IOException {
        HttpSession session = request.getSession();
        ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        for (FileItem fileParameter : fileParameters) {
            final String fieldName = fileParameter.getFieldName();
            final String fileName = FilenameUtils.getName(fileParameter.getName());
            logger.debug("Upload file parameter {" + fieldName + ":" + fileName + "}");
            if (fileName == null || fileName.isEmpty()) {
                // even if no file is uploaded, the fieldname is given, but no filename -> skip
                continue;
            }
            byte[] fileContentBytes = fileParameter.get();
            if (fileContentBytes.length == 0) {
                logger.error("Puzzle upload. Given zip file {} was empty.", fileName);
                return;
            }

            switch (fieldName) {
                case "fileUploadPuzzles": {
                    final ZipFile zip = ZipFileUtils.createZip(fileContentBytes);
                    final Path rootDirectory = ZipFileUtils.extractZipGetRootDir(zip, true);

                    Installer.installPuzzles(rootDirectory, backend);

                    FileUtils.forceDelete(rootDirectory.toFile());

                    messages.add("Successfully uploaded puzzles. As of now, please check the logs for errors.");
                    logger.info("Successfully uploaded puzzles.");

                    break;
                }
                default: {
                    logger.warn("Unrecognized parameter: " + fieldName);
                    break;
                }
            }
        }
    }
}
