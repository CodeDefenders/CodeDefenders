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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import javax.inject.Inject;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.codedefenders.analysis.coverage.CoverageGenerator;
import org.codedefenders.analysis.coverage.CoverageGenerator.CoverageGeneratorException;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.DependencyDAO;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.KillmapDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.execution.BackendExecutorService;
import org.codedefenders.execution.CompileException;
import org.codedefenders.execution.Compiler;
import org.codedefenders.execution.KillMap;
import org.codedefenders.execution.KillMapService;
import org.codedefenders.game.AssertionLibrary;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.LineCoverage;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.TestingFramework;
import org.codedefenders.model.Dependency;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.util.Constants;
import org.codedefenders.util.FileUtils;
import org.codedefenders.util.JavaFileObject;
import org.codedefenders.util.URLUtils;
import org.codedefenders.util.ZipFileUtils;
import org.codedefenders.validation.code.CodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME.CLASS_UPLOAD;
import static org.codedefenders.util.Constants.CUTS_DEPENDENCY_DIR;
import static org.codedefenders.util.Constants.CUTS_MUTANTS_DIR;
import static org.codedefenders.util.Constants.CUTS_TESTS_DIR;

/**
 * This {@link HttpServlet} handles the upload of Java class files, which includes file validation and storing.
 *
 * <p>Serves on path: {@code /class-upload}.
 *
 * @see org.codedefenders.util.Paths#CLASS_UPLOAD
 */
@WebServlet(org.codedefenders.util.Paths.CLASS_UPLOAD)
public class ClassUploadManager extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ClassUploadManager.class);

    @Inject
    private Configuration config;

    @Inject
    private MessagesBean messages;

    @Inject
    private CodeDefendersAuth login;

    @Inject
    private BackendExecutorService backend;

    @Inject
    private CoverageGenerator coverageGenerator;

    @Inject
    private KillMapService killMapService;

    @Inject
    private URLUtils url;


    private static List<String> reservedClassNames = Arrays.asList(
            "Test.java"
    );

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final boolean classUploadEnabled = AdminDAO.getSystemSetting(CLASS_UPLOAD).getBoolValue();
        if (classUploadEnabled || login.isAdmin()) {
            RequestDispatcher dispatcher = request.getRequestDispatcher(Constants.CLASS_UPLOAD_VIEW_JSP);
            dispatcher.forward(request, response);
        } else {
            messages.add("Class upload is disabled.");
            response.sendRedirect(url.forPath(org.codedefenders.util.Paths.GAMES_OVERVIEW));
        }
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final boolean classUploadEnabled = AdminDAO.getSystemSetting(CLASS_UPLOAD).getBoolValue();
        if (!classUploadEnabled && !login.isAdmin()) {
            logger.warn("User {} tried to upload a class, but class upload is disabled.", login.getUserId());
            return;
        }

        logger.debug("Uploading CUT");

        final List<CompiledClass> compiledClasses = new LinkedList<>();

        boolean isMockingEnabled = false;
        TestingFramework testingFramework = null;
        AssertionLibrary assertionLibrary = null;
        boolean shouldPrepareAI = false;
        // Control whether after a successful upload we return to the same back or to the original page
        boolean disableAutomaticRedirect = false;
        // Data about page to automatically redirect to after a successful upload
        String origin = null;

        // Alias of the CUT
        String classAlias = null;
        // Used to check whether multiple CUTs are uploaded.
        final int cutId;
        // Used to check whether mutants have the same name as the class under test.
        final String cutFileName;
        // The directory in which the CUT is saved in.
        final Path cutDir;
        // Used to run calculate line coverage for tests
        final GameClass cut;
        // flag whether upload is with dependencies or not
        boolean withDependencies = false;

        // Get actual parameters, because of the upload component, I can't do
        // request.getParameter before fetching the file
        List<FileItem> items;
        try {
            items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
        } catch (FileUploadException e) {
            logger.error("Failed to upload class. Failed to get file upload parameters.", e);
            Redirect.redirectBack(request, response);
            return;
        }

        // Splits request parameters by FileItem#isFormField into
        // upload and file parameters to ensure that all upload parameters
        // set before storing files.
        final Map<Boolean, List<FileItem>> parameters = items
                .stream()
                .collect(Collectors.partitioningBy(FileItem::isFormField));
        final List<FileItem> uploadParameters = parameters.get(true);
        final List<FileItem> fileParameters = parameters.get(false);

        for (FileItem uploadParameter : uploadParameters) {
            final String fieldName = uploadParameter.getFieldName();
            final String fieldValue = uploadParameter.getString();
            logger.debug("Upload parameter {" + fieldName + ":" + fieldValue + "}");
            switch (fieldName) {
                case "classAlias":
                    classAlias = fieldValue;
                    if (!validateAlias(classAlias)) {
                        logger.error("Class upload failed. Provided alias '{}' contained whitespaces or special "
                                + "characters. Aborting.", classAlias);
                        messages.add("Class upload failed. Alias must not contain whitespaces or special characters.");
                        abortRequestAndCleanUp(request, response);
                        return;
                    }
                    break;
                case "prepareForSingle":
                    // TODO Phil: legacy, will this be used in the future? (look TODO below)
                    shouldPrepareAI = true;
                    break;
                case "disableAutomaticRedirect":
                    disableAutomaticRedirect = true;
                    break;
                case "enableMocking":
                    isMockingEnabled = true;
                    break;
                case "testingFramework":
                    testingFramework = TestingFramework.valueOf(fieldValue);
                    break;
                case "assertionLibrary":
                    assertionLibrary = AssertionLibrary.valueOf(fieldValue);
                    break;
                case "origin":
                    origin = fieldValue;
                    break;
                default:
                    logger.warn("Unrecognized parameter {" + fieldName + ":" + fieldValue + "}");
                    break;
            }
        }

        SimpleFile cutFile = null;
        SimpleFile dependenciesZipFile = null;
        SimpleFile mutantsZipFile = null;
        SimpleFile testsZipFile = null;

        for (FileItem fileParameter : fileParameters) {
            final String fieldName = fileParameter.getFieldName();
            final String fileName = FilenameUtils.getName(fileParameter.getName());
            logger.info("Upload file parameter {" + fieldName + ":" + fileName + "}");
            if (fileName == null || fileName.isEmpty()) {
                // even if no file is uploaded, the fieldname is given, but no filename -> skip
                continue;
            }
            byte[] fileContentBytes = fileParameter.get();
            if (fileContentBytes.length == 0) {
                logger.error("Class upload failed. Given file {} was empty", fileName);
                messages.add("Class upload failed. File content for " + fileName
                        + " could not be read. Please try again.");
                abortRequestAndCleanUp(request, response);
                return;
            }

            switch (fieldName) {
                case "fileUploadCUT": {
                    if (cutFile != null) {
                        // Upload of second CUT? Abort
                        logger.error("Class upload failed. Multiple classes under test uploaded.");
                        messages.add("Class upload failed. Multiple classes under test uploaded.");
                        abortRequestAndCleanUp(request, response);
                        return;
                    }
                    cutFile = new SimpleFile(fileName, fileContentBytes);
                    break;
                }
                case "fileUploadDependency": {
                    if (dependenciesZipFile != null) {
                        // Upload of second dependency ZIP file? Abort
                        logger.error("Class upload failed. Multiple dependency ZIP files uploaded.");
                        messages.add("Class upload failed. Multiple dependency ZIP files uploaded.");
                        abortRequestAndCleanUp(request, response);
                        return;
                    }
                    dependenciesZipFile = new SimpleFile(fileName, fileContentBytes);
                    withDependencies = true;
                    break;
                }
                case "fileUploadMutant": {
                    if (mutantsZipFile != null) {
                        // Upload of second mutant ZIP file? Abort
                        logger.error("Class upload failed. Multiple mutant ZIP files uploaded.");
                        messages.add("Class upload failed. Multiple mutant ZIP files uploaded.");
                        abortRequestAndCleanUp(request, response);
                        return;
                    }
                    mutantsZipFile = new SimpleFile(fileName, fileContentBytes);
                    break;
                }
                case "fileUploadTest": {
                    if (testsZipFile != null) {
                        // Upload of second test ZIP file? Abort
                        logger.error("Class upload failed. Multiple test ZIP files uploaded.");
                        messages.add("Class upload failed. Multiple test ZIP files uploaded.");
                        abortRequestAndCleanUp(request, response);
                        return;
                    }
                    testsZipFile = new SimpleFile(fileName, fileContentBytes);
                    break;
                }
                default:
                    logger.warn("Unrecognized parameter: " + fieldName);
                    break;
            }
        }

        if (cutFile == null) {
            logger.error("Class upload failed. No class under test uploaded.");
            messages.add("Class upload failed. No class under test uploaded.");
            abortRequestAndCleanUp(request, response);
            return;
        }

        final List<JavaFileObject> dependencies = new ArrayList<>();
        final String fileName = cutFile.fileName;
        final String fileContent = new String(cutFile.fileContent, StandardCharsets.UTF_8).trim();
        if (!fileName.endsWith(".java")) {
            logger.error("Class upload failed. Given file {} was not a .java file.", fileName);
            messages.add("Class upload failed. The class under test must be a .java file.");
            abortRequestAndCleanUp(request, response);
            return;
        }
        if (reservedClassNames.contains(fileName)) {
            logger.error("Class with reserved name uploaded. Aborting.");
            messages.add("Class upload failed. " + fileName
                    + " is a reserved class name, please rename your Java class.");
            abortRequestAndCleanUp(request, response);
            return;
        }
        cutFileName = fileName;
        if (fileContent == null) {
            logger.error("Class upload failed. Provided fileContent is null. That shouldn't happen.");
            messages.add("Class upload failed. Internal error. Sorry about that!");
            abortRequestAndCleanUp(request, response);
            return;
        }

        if (classAlias == null || classAlias.equals("")) {
            classAlias = fileName.replace(".java", "");
        }
        if (GameClassDAO.classExistsForAlias(classAlias)) {
            logger.error("Class upload failed. Given alias {} was already used.", classAlias);
            messages.add("Class upload failed. Given alias is already used.");
            abortRequestAndCleanUp(request, response);
            return;
        }

        cutDir = Paths.get(config.getSourcesDir().getAbsolutePath(), classAlias);
        if (Files.exists(cutDir)) {
            logger.warn("Attempting to store new class directory under '" + cutDir + "', but file/directory with the same path already exists. Deleting it.");
            try {
                org.apache.commons.io.FileUtils.forceDelete(cutDir.toFile());
            } catch (IOException e) {
                logger.error("Could not delete '" + cutDir + "'. Please remove the file/directory manually and try again.");
                messages.add("Class upload failed due to a file error. Please contact an admin and show them this message. Or try a different class alias.")
                        .fadeOut(false);
                abortRequestAndCleanUp(request, response);
                return;
            }
        }

        final String cutJavaFilePath;
        try {
            cutJavaFilePath = FileUtils.storeFile(cutDir, fileName, fileContent).toString();
        } catch (IOException e) {
            logger.error("Class upload failed. Could not store java file " + fileName, e);
            messages.add("Class upload failed. Internal error. Sorry about that!");
            abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
            return;
        }

        final String cutClassFilePath;
        final List<JavaFileReferences> dependencyReferences = new LinkedList<>();
        if (!withDependencies) {
            try {
                cutClassFilePath = Compiler.compileJavaFileForContent(cutJavaFilePath, fileContent);
            } catch (CompileException e) {
                logger.error("Class upload failed. Could not compile {}!\n\n{}", fileName, e.getMessage());
                messages.add("Class upload failed. Could not compile " + fileName + "!\n" + e.getMessage());

                abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
                return;
            } catch (IllegalStateException e) {
                logger.error("SEVERE ERROR. Could not find Java compiler. Please reconfigure your "
                        + "installed version.", e);
                messages.add("Class upload failed. Internal error. Sorry about that!");

                abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
                return;
            }
        } else {
            final String zipFileName = dependenciesZipFile.fileName;
            final byte[] zipFileContent = dependenciesZipFile.fileContent;

            if (!zipFileName.endsWith(".zip")) {
                logger.error("Class upload failed. Given file {} was not a .zip file.", zipFileName);
                messages.add("Class upload failed. Dependencies must be provided in a .zip file.");
                abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
                return;
            }

            try {
                final ZipFile zip = ZipFileUtils.createZip(zipFileContent);

                // The returned list contains the files, but without an actual file path, just names.
                dependencies.addAll(ZipFileUtils.getFilesFromZip(zip, true));
            } catch (IOException e) {
                logger.error("Class upload failed. Failed to extract dependencies ZIP file.");
                messages.add("Class upload failed. Failed to extract dependencies ZIP file.");
                abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
                return;
            }
            for (int index = 0; index < dependencies.size(); index++) {
                final JavaFileObject dependencyFile = dependencies.get(index);
                final Path path = Paths.get(dependencyFile.getName());

                final String dependencyFileName = path.getFileName().toString();
                final String dependencyFileContent = dependencyFile.getContent();

                if (!dependencyFileName.endsWith(".java")) {
                    logger.error("Class upload failed. Given file {} was not a .java file.", dependencyFileName);
                    messages.add("Class upload failed. Dependency must be a .java file.");
                    abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
                    return;
                }
                if (dependencyFileContent == null) {
                    logger.error("Class upload failed. Provided fileContent is null. That shouldn't happen.");
                    messages.add("Class upload failed. Internal error. Sorry about that!");
                    abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
                    return;
                }

                final String depJavaFilePath;
                try {
                    final Path folderPath = cutDir.resolve(CUTS_DEPENDENCY_DIR);
                    depJavaFilePath = FileUtils.storeFile(folderPath, dependencyFileName, dependencyFileContent)
                            .toString();
                    final String depClassFilePath = depJavaFilePath.replace(".java", ".class");
                    dependencyReferences.add(new JavaFileReferences(depJavaFilePath, depClassFilePath));
                } catch (IOException e) {
                    logger.error("Class upload failed. Could not store java file " + dependencyFileName, e);
                    messages.add("Class upload failed. Internal error. Sorry about that!");
                    abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
                    return;
                }

                // Update the existing dependency file with the actually stored file path.
                // This is required for the compilation of mutants and tests.
                dependencies.set(index, new JavaFileObject(depJavaFilePath, dependencyFileContent));
            }

            try {
                cutClassFilePath = Compiler.compileJavaFileWithDependencies(cutJavaFilePath, dependencies);
            } catch (CompileException e) {
                logger.error("Class upload failed. Could not compile {}!\n\n{}", fileName, e.getMessage());
                messages.add("Class upload failed. Could not compile " + fileName + "!\n" + e.getMessage());

                abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
                return;
            } catch (IllegalStateException e) {
                logger.error("SEVERE ERROR. Could not find Java compiler. Please reconfigure your instance.", e);
                messages.add("Class upload failed. Internal error. Sorry about that!");

                abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
                return;
            }
        }

        String classQualifiedName;
        try {
            classQualifiedName = FileUtils.getFullyQualifiedName(cutClassFilePath);
        } catch (IOException e) {
            logger.error("Class upload failed. Could not get fully qualified name for " + fileName, e);
            messages.add("Class upload failed. Internal error. Sorry about that!");

            abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
            return;
        }

        cut = GameClass.build()
                .name(classQualifiedName)
                .alias(classAlias)
                .javaFile(cutJavaFilePath)
                .classFile(cutClassFilePath)
                .mockingEnabled(isMockingEnabled)
                .testingFramework(testingFramework)
                .assertionLibrary(assertionLibrary)
                .create();

        try {
            cutId = GameClassDAO.storeClass(cut);
        } catch (Exception e) {
            logger.error("Class upload failed. Could not store class to database.");
            messages.add("Class upload failed. Internal error. Sorry about that!");
            abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
            return;
        }

        compiledClasses.add(new CompiledClass(CompileClassType.CUT, cutId));

        if (withDependencies) {
            for (JavaFileReferences dep : dependencyReferences) {
                final int depId;
                try {
                    depId = DependencyDAO.storeDependency(new Dependency(cutId, dep.javaFile, dep.classFile));
                } catch (Exception e) {
                    logger.error("Class upload failed. Could not store dependency class to database.");
                    messages.add("Class upload failed. Internal error. Sorry about that!");
                    abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
                    return;
                }

                compiledClasses.add(new CompiledClass(CompileClassType.DEPENDENCY, depId));
            }
        }

        if (mutantsZipFile != null) {
            final boolean failed = addMutants(request, response, compiledClasses,
                    cutId, cutFileName, cutDir, mutantsZipFile, dependencies);
            if (failed) {
                // tests zip failed and abort method has been called.
                return;
            }
        }

        if (testsZipFile != null) {
            final boolean failed = addTests(request, response, compiledClasses,
                    cutId, cutDir, cut, testsZipFile, dependencies);
            if (failed) {
                // tests zip failed and abort method has been called.
                return;
            }
        }

        messages.add("Class upload successful.");
        logger.info("Class upload of {} was successful", cutFileName);

        // At this point if there's test and mutants we shall run them against each other.
        // Since this is not happening in the context of a game we shall do it manually.
        List<Mutant> mutants = GameClassDAO.getMappedMutantsForClassId(cutId);
        List<Test> tests = GameClassDAO.getMappedTestsForClassId(cutId);
        try {
            // Custom Killmaps are not store in the DB for whatever reason,
            // while we need that !
            // Since gameID = -1, DAOs cannot find the class linked to this
            // game, hence its if, which is needed instead inside mutants and
            // tests
            KillMap killMap = killMapService.forCustom(tests, mutants, cutId, new ArrayList<>());
            KillmapDAO.insertManyKillMapEntries(killMap.getEntries(), cutId);
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Could error while calculating killmap for successfully uploaded class.", e);
        }

        // Handle the automatic redirection logic.
        if (disableAutomaticRedirect) {
            logger.info("Redirecting to class upload page disableAutomaticRedirect checked");
            Redirect.redirectBack(request, response);
        } else if (origin == null || origin.equalsIgnoreCase("null")) {
            logger.info("Redirecting to class upload page. Null origin");
            Redirect.redirectBack(request, response);
        } else {
            logger.info("Automatically redirecting to origin " + origin);
            response.sendRedirect(url.forPath(origin));
        }
    }

    /**
     * Checks whether a given alias is valid or not. A valid alias is at least one
     * character long and has no special characters.
     *
     * @param alias the checked alias as a {@link String}
     * @return {@code true} if the alias is valid, {@code false} otherwise.
     */
    private boolean validateAlias(String alias) {
        return alias.matches("^[a-zA-Z0-9]*$");
    }

    /**
     * Adds the contents of a given zip file as mutants uploaded together with
     * a class under test.
     *
     * @param request         the request the mutants are added for.
     * @param response        the response to the request.
     * @param compiledClasses a list of previously added CUT, tests and mutants,
     *                        which need to get cleaned up once something fails.
     * @param cutId           the identifier of the class under test.
     * @param cutFileName     the file name of the class under test.
     * @param cutDir          the directory in which the class under test lies.
     * @param mutantsZipFile  the given zip file from which the mutants are added.
     * @param dependencies    dependencies required to compile the mutants.
     * @return {@code true} if addition fails, {@code fail} otherwise.
     * @throws IOException when aborting the request fails.
     */
    private boolean addMutants(HttpServletRequest request, HttpServletResponse response,
                               List<CompiledClass> compiledClasses, int cutId, String cutFileName, Path cutDir,
                               SimpleFile mutantsZipFile, List<JavaFileObject> dependencies) throws IOException {
        boolean withDependencies = !dependencies.isEmpty();

        final String zipFileName = mutantsZipFile.fileName;
        final byte[] zipFileContent = mutantsZipFile.fileContent;

        if (!zipFileName.endsWith(".zip")) {
            logger.error("Class upload failed. Given file {} was not a .zip file.", zipFileName);
            messages.add("Class upload failed. Mutants must be provided in a .zip file.");
            abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
            return true;
        }

        final List<JavaFileObject> mutants;
        try {
            final ZipFile zip = ZipFileUtils.createZip(zipFileContent);
            mutants = ZipFileUtils.getFilesFromZip(zip, true);
        } catch (IOException e) {
            logger.error("Class upload failed. Failed to extract mutants ZIP file.");
            messages.add("Class upload failed. Failed to extract mutants ZIP file.");
            abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
            return true;
        }

        for (int index = 0; index < mutants.size(); index++) {
            final JavaFileObject mutantFile = mutants.get(index);

            final String fileName = mutantFile.getName();
            final String fileContent = mutantFile.getContent();

            if (!fileName.endsWith(".java")) {
                logger.error("Class upload failed. Given file {} was not a .java file.", fileName);
                messages.add("Class upload failed. Mutant must be a .java file.");
                abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
                return true;
            }
            if (!fileName.equals(cutFileName)) {
                logger.error("Class uploaded failed. Mutant {} has not the same class name as CUT, {}",
                        fileName, cutFileName);
                messages.add("Class upload failed. Mutants must have same class name as class under test!");
                abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
                return true;
            }
            if (fileContent == null) {
                logger.error("Class upload failed. Provided fileContent is null. That shouldn't happen.");
                messages.add("Class upload failed. Internal error. Sorry about that!");
                abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
                return true;
            }

            String javaFilePath;
            try {

                final Path folderPath = cutDir.resolve(CUTS_MUTANTS_DIR).resolve(String.valueOf(index));
                javaFilePath = FileUtils.storeFile(folderPath, fileName, fileContent).toString();
            } catch (IOException e) {
                logger.error("Class upload failed. Could not store mutant java file " + fileName, e);
                messages.add("Class upload failed. Internal error. Sorry about that!");
                abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
                return true;
            }
            String classFilePath;

            if (!withDependencies) {
                try {
                    classFilePath = Compiler.compileJavaFileForContent(javaFilePath, fileContent);
                } catch (CompileException e) {
                    logger.error("Class upload failed. Could not compile mutant {}!\n\n{}", fileName, e.getMessage());
                    messages.add("Class upload failed. Could not compile mutant " + fileName + "!\n" + e.getMessage());

                    abortRequestAndCleanUp(request, response, cutDir, compiledClasses, javaFilePath);
                    return true;
                } catch (IllegalStateException e) {
                    logger.error("SEVERE ERROR. Could not find Java compiler. Please reconfigure your"
                            + "installed version.", e);
                    messages.add("Class upload failed. Internal error. Sorry about that!");

                    abortRequestAndCleanUp(request, response, cutDir, compiledClasses, javaFilePath);
                    return true;
                }
            } else {
                try {
                    classFilePath = Compiler.compileJavaFileForContentWithDependencies(javaFilePath, fileContent,
                            dependencies, true);
                } catch (CompileException e) {
                    logger.error("Class upload failed. Could not compile mutant {} with dependencies{}!\n\n{}",
                            fileName, dependencies, e.getMessage());
                    messages.add("Class upload failed. Could not compile mutant " + fileName + "!\n" + e.getMessage());

                    abortRequestAndCleanUp(request, response, cutDir, compiledClasses, javaFilePath);
                    return true;
                } catch (IllegalStateException e) {
                    logger.error("SEVERE ERROR. Could not find Java compiler. Please reconfigure your"
                            + "installed version.", e);
                    messages.add("Class upload failed. Internal error. Sorry about that!");

                    abortRequestAndCleanUp(request, response, cutDir, compiledClasses, javaFilePath);
                    return true;
                }
            }

            int mutantId;
            final String md5 = CodeValidator.getMD5FromText(fileContent);
            final Mutant mutant = new Mutant(javaFilePath, classFilePath, md5, cutId);
            try {
                mutantId = MutantDAO.storeMutant(mutant);
                MutantDAO.mapMutantToClass(mutantId, cutId);
            } catch (Exception e) {
                logger.error("Class upload with mutant failed. Could not store mutant to database.");
                messages.add("Class upload failed. Seems like you uploaded two identical mutants.");

                abortRequestAndCleanUp(request, response, cutDir, compiledClasses, javaFilePath, classFilePath);
                return true;
            }

            compiledClasses.add(new CompiledClass(CompileClassType.MUTANT, mutantId));
        }
        return false;
    }

    /**
     * Adds the contents of a given zip file as tests uploaded together with
     * a class under test.
     *
     * @param request         the request the tests are added for.
     * @param response        the response to the request.
     * @param compiledClasses a list of previously added CUT, tests and mutants,
     *                        which need to get cleaned up once something fails.
     * @param cutId           the identifier of the class under test.
     * @param cutDir          the directory in which the class under test lies.
     * @param cut             the class under test {@link GameClass} object.
     * @param testsZipFile    the given zip file from which the tests are added.
     * @param dependencies    dependencies required to compile the tests.
     * @return {@code true} if addition fails, {@code fail} otherwise.
     * @throws IOException when aborting the request fails.
     */
    private boolean addTests(HttpServletRequest request, HttpServletResponse response,
                             List<CompiledClass> compiledClasses, int cutId, Path cutDir, GameClass cut,
                             SimpleFile testsZipFile, List<JavaFileObject> dependencies) throws IOException {

        // Class under test is a dependency for all tests
        dependencies.add(new JavaFileObject(cut.getJavaFile()));

        final String zipFileName = testsZipFile.fileName;
        final byte[] zipFileContent = testsZipFile.fileContent;

        if (!zipFileName.endsWith(".zip")) {
            logger.error("Class upload failed. Given file {} was not a .zip file.", zipFileName);
            messages.add("Class upload failed. The tests must be provided in a .zip file.");
            abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
            return true;
        }

        final List<JavaFileObject> tests;
        try {
            final ZipFile zip = ZipFileUtils.createZip(zipFileContent);
            tests = ZipFileUtils.getFilesFromZip(zip, true);
        } catch (IOException e) {
            logger.error("Class upload failed. Failed to extract tests ZIP file.");
            messages.add("Class upload failed. Failed to extract tests ZIP file.");
            abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
            return true;
        }

        for (int index = 0; index < tests.size(); index++) {
            final JavaFileObject testFile = tests.get(index);

            final String fileName = testFile.getName();
            final String fileContent = testFile.getContent();

            if (!fileName.endsWith(".java")) {
                logger.error("Class upload failed. Given file {} was not a .java file.", fileName);
                messages.add("Class upload failed. The tests files must be .java file.");
                abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
                return true;
            }
            if (fileContent == null) {
                logger.error("Class upload failed. Provided fileContent is null. That shouldn't happen.");
                messages.add("Class upload failed. Internal error. Sorry about that!");
                abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
                return true;
            }

            String javaFilePath;
            try {
                final Path folderPath = cutDir.resolve(CUTS_TESTS_DIR).resolve(String.valueOf(index));
                javaFilePath = FileUtils.storeFile(folderPath, fileName, fileContent).toString();
            } catch (IOException e) {
                logger.error("Class upload failed. Could not store java file of test class " + fileName, e);
                messages.add("Class upload failed. Could not store java file of test class " + fileName);

                abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
                return true;
            }
            String classFilePath;
            try {
                classFilePath = Compiler.compileJavaTestFileForContent(javaFilePath, fileContent, dependencies, true);
            } catch (CompileException e) {
                logger.error("Class upload failed. Could not compile test {}!\n\n{}", fileName, e.getMessage());
                messages.add("Class upload failed. Could not compile test " + fileName + "!\n" + e.getMessage());

                abortRequestAndCleanUp(request, response, cutDir, compiledClasses, javaFilePath);
                return true;
            } catch (IllegalStateException e) {
                logger.error("SEVERE ERROR. Could not find Java compiler. Please reconfigure your instance.", e);
                messages.add("Class upload failed. Internal error. Sorry about that!");

                abortRequestAndCleanUp(request, response, cutDir, compiledClasses, javaFilePath);
                return true;
            }

            try {
                final String testDir = Paths.get(javaFilePath).getParent().toString();
                final String qualifiedName = FileUtils.getFullyQualifiedName(classFilePath);

                // This adds a jacoco.exec file to the testDir
                backend.testOriginal(cut, testDir, qualifiedName);
            } catch (Exception e) {
                logger.error("Class upload failed. Test " + fileName + " failed", e);
                messages.add("Class upload failed. Test " + fileName + " failed");

                abortRequestAndCleanUp(request, response, cutDir, compiledClasses, javaFilePath, classFilePath);
                return true;
            }

            LineCoverage lineCoverage;
            try {
                lineCoverage = coverageGenerator.generate(cut, Paths.get(javaFilePath));
            } catch (CoverageGeneratorException e) {
                logger.error("Class upload with test failed. Failed to compute coverage.", e);
                messages.add("Class upload failed. Internal error. Sorry about that!");

                abortRequestAndCleanUp(request, response, cutDir, compiledClasses, javaFilePath, classFilePath);
                return true;
            }

            final Test test = new Test(javaFilePath, classFilePath, cutId, lineCoverage);

            int testId;
            try {
                testId = TestDAO.storeTest(test);
                TestDAO.mapTestToClass(testId, cutId);
            } catch (UncheckedSQLException e) {
                logger.error("Class upload with test failed. Could not store test to database.", e);
                messages.add("Class upload failed. Internal error. Sorry about that!");

                abortRequestAndCleanUp(request, response, cutDir, compiledClasses, javaFilePath, classFilePath);
                return true;
            }

            compiledClasses.add(new CompiledClass(CompileClassType.TEST, testId));
        }
        return false;
    }

    /**
     * Aborts a given request by removing all uploaded compile classes from for
     * the database and {@code .java} and {@code .class} files from the system.
     *
     * <p>Also redirects the user.
     *
     * <p>This method should be the last thing called when aborting a request.
     *
     * @param request         The handled request.
     * @param response        The response of the handled requests.
     * @param cutDir          The directory in which all files are located, can be {@code null}.
     * @param compiledClasses A list of {@link CompiledClass}, which will get removed.
     * @param files           Optional additional files, which need to be removed.
     * @throws IOException When an error during redirecting occurs.
     */
    private static void abortRequestAndCleanUp(HttpServletRequest request,
                                               HttpServletResponse response,
                                               Path cutDir,
                                               List<CompiledClass> compiledClasses,
                                               String... files) throws IOException {
        logger.debug("Aborting request...");
        if (cutDir != null) {
            final List<Integer> cuts = new LinkedList<>();
            final List<Integer> dependencies = new LinkedList<>();
            final List<Integer> mutants = new LinkedList<>();
            final List<Integer> tests = new LinkedList<>();
            for (CompiledClass compiledClass : compiledClasses) {
                switch (compiledClass.type) {
                    case CUT:
                        cuts.add(compiledClass.id);
                        break;
                    case DEPENDENCY:
                        dependencies.add(compiledClass.id);
                        break;
                    case MUTANT:
                        mutants.add(compiledClass.id);
                        break;
                    case TEST:
                        tests.add(compiledClass.id);
                        break;
                    default:
                        // ignore
                }
            }

            try {
                logger.info("Removing directory {} again", cutDir);
                org.apache.commons.io.FileUtils.forceDelete(cutDir.toFile());
            } catch (IOException e) {
                // logged, but otherwise ignored. No need to abort while aborting.
                logger.error("Error removing directory of compiled classes.", e);
            }
            for (String file : files) {
                logger.info("Removing {} again.", file);
                try {
                    Files.delete(Paths.get(file));
                } catch (IOException ignored) {
                    // file may have been removed already.
                }

                try {
                    final Path parentFolder = Paths.get(file).getParent();
                    Files.delete(parentFolder);
                } catch (IOException ignored) {
                    // folder may have been removed already.
                }
            }

            MutantDAO.removeMutantsForIds(mutants);
            TestDAO.removeTestsForIds(tests);
            DependencyDAO.removeDependenciesForIds(dependencies);
            GameClassDAO.removeClassesForIds(cuts);
        }

        Redirect.redirectBack(request, response);
        logger.debug("Aborting request...done");
    }

    /**
     * Aborts a given request by redirecting the user.
     *
     * <p>This method should be the last thing called when aborting a request.
     *
     * @param request  The handled request.
     * @param response The response of the handled requests.
     * @throws IOException When an error during redirecting occurs.
     */
    private static void abortRequestAndCleanUp(HttpServletRequest request,
                                               HttpServletResponse response) throws IOException {
        logger.debug("Aborting request without removing files...");
        Redirect.redirectBack(request, response);
        logger.debug("Aborting request without removing files...done");
    }

    /**
     * Container for a file with its name and content.
     *
     * <p>Name is stored as a {@link String}, content as a {@code byte[]}.
     */
    private static class SimpleFile {
        private String fileName;
        private byte[] fileContent;

        SimpleFile(String fileName, byte[] fileContent) {
            this.fileName = fileName;
            this.fileContent = fileContent;
        }
    }

    /**
     * Container for paths to {@code .java} and {@code .class}
     * files of a java class.
     */
    private static class JavaFileReferences {
        private String javaFile;
        private String classFile;

        JavaFileReferences(String javaFile, String classFile) {
            this.javaFile = javaFile;
            this.classFile = classFile;
        }
    }

    /**
     * Wrapper class for classes, which have been compiled already.
     * They have a type {@link CompileClassType}, an {@code id} and
     * paths to {@code .java} and {@code .class} files.
     */
    private static class CompiledClass {
        private CompileClassType type;
        private Integer id;

        CompiledClass(CompileClassType type, Integer id) {
            this.type = type;
            this.id = id;
        }
    }

    private enum CompileClassType {
        CUT,
        DEPENDENCY,
        MUTANT,
        TEST
    }
}
