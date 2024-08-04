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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.codedefenders.analysis.coverage.CoverageGenerator;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.execution.BackendExecutorService;
import org.codedefenders.execution.CompileException;
import org.codedefenders.installer.Installer;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.codedefenders.util.SimpleFile;
import org.codedefenders.util.ZipFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link HttpServlet} handles admin upload of puzzles.
 *
 * <p>{@code GET} requests redirect to the admin puzzle upload page.
 * and {@code POST} requests handle batch uploading puzzle related information.
 *
 * <p>Serves under {@code /admin/puzzles/upload}.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
@WebServlet(Paths.ADMIN_PUZZLE_UPLOAD)
public class AdminPuzzleUpload extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminPuzzleUpload.class);

    @Inject
    private Installer installer;

    @Inject
    private MessagesBean messages;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher(Constants.ADMIN_PUZZLE_UPLOAD_JSP).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
        // Parse request parameters.
        List<DiskFileItem> parameters;
        try {
            var fileUpload = new JakartaServletFileUpload<>(DiskFileItemFactory.builder().get());
            parameters = fileUpload.parseRequest(request);
        } catch (FileUploadException e) {
            logger.error("Failed to get file upload parameters.", e);
            Redirect.redirectBack(request, response);
            return;
        }

        // Find formType parameter.
        Optional<String> formType = parameters.stream()
                .filter(DiskFileItem::isFormField)
                .filter(item -> item.getFieldName().equals("formType"))
                .map(DiskFileItem::getString)
                .findAny();
        if (formType.isEmpty()) {
            logger.warn("No formType provided. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }

        List<byte[]> fileParametersContents = parameters.stream()
                .filter(item -> {
                    // Filter out regular parameters.
                    return !item.isFormField();
                })
                .filter(item -> {
                    // Filter out empty file upload fields.
                    // When no file is uploaded, the field name is given, but the filename isn't.
                    return item.getName() != null && !item.getName().isEmpty();
                })
                .map(DiskFileItem::get)
                .toList();

        if (fileParametersContents.isEmpty()) {
            messages.add("No file parameters found.");
            Redirect.redirectBack(request, response);
        }

        switch (formType.get()) {
            case "uploadPuzzleChapters":
                installPuzzleChapters(fileParametersContents);
                Redirect.redirectBack(request, response);
                break;
            case "uploadPuzzles":
                installPuzzles(fileParametersContents);
                Redirect.redirectBack(request, response);
                break;
            default: {
                logger.info("Action not recognised: {}", formType);
                Redirect.redirectBack(request, response);
                break;
            }
        }
    }

    private void installPuzzles(List<byte[]> files) {
        for (byte[] fileContent : files) {
            try {
                Collection<SimpleFile> puzzleFiles = ZipFileUtils.readZipRecursive(fileContent);
                installer.installPuzzle(puzzleFiles);
                messages.add("Successfully uploaded puzzle. As of now, please check the logs for errors.");
                logger.info("Successfully uploaded puzzle.");
            } catch (CoverageGenerator.CoverageGeneratorException e) {
                messages.add("Error while computing coverage.");
                logger.info("Error while computing coverage", e);
            } catch (CompileException e) {
                messages.add("Error while compiling.");
                logger.info("Error while compiling", e);
            } catch (IOException e) {
                messages.add("IO Error.");
                logger.info("IO Error", e);
            } catch (BackendExecutorService.ExecutionException e) {
                messages.add("Execution Error");
                logger.info("Execution Error", e);
            }
        }
    }

    private void installPuzzleChapters(List<byte[]> files) {
        for (byte[] fileContent : files) {
            try {
                Collection<SimpleFile> chapterFiles = ZipFileUtils.readZipRecursive(fileContent);
                installer.installPuzzleChapter(chapterFiles);
                messages.add("Successfully uploaded chapter. As of now, please check the logs for errors.");
                logger.info("Successfully uploaded chapter.");
            } catch (CoverageGenerator.CoverageGeneratorException e) {
                messages.add("Error while computing coverage.");
                logger.info("Error while computing coverage", e);
            } catch (CompileException e) {
                messages.add("Error while compiling.");
                logger.info("Error while compiling", e);
            } catch (IOException e) {
                messages.add("IO Error.");
                logger.info("IO Error", e);
            } catch (BackendExecutorService.ExecutionException e) {
                messages.add("Execution Error:" + e.getMessage());
                logger.info("Execution Error", e);
            }
        }
    }
}
