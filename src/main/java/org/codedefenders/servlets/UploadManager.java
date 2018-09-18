package org.codedefenders.servlets;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.execution.CompileException;
import org.codedefenders.execution.Compiler;
import org.codedefenders.execution.Runner;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.util.Constants;
import org.codedefenders.validation.CodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javassist.ClassPool;
import javassist.CtClass;

import static org.codedefenders.util.Constants.F_SEP;

/**
 * This {@link HttpServlet} handles the upload of Java class files, which includes file validation and storing.
 *
 * Serves on path: `/upload`, but redirects the view to `/games/upload`.
 */
public class UploadManager extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(UploadManager.class);

	private static List<String> reservedClassNames = Arrays.asList(
			"Test.java"
	);

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		response.sendRedirect(request.getContextPath() + "/games/upload");
	}

	@SuppressWarnings("Duplicates")
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		HttpSession session = request.getSession();
		ArrayList<String> messages = new ArrayList<>();
		session.setAttribute("messages", messages);

		logger.debug("Uploading CUT");

		final List<CompiledClass> compiledClasses = new LinkedList<>();

		String classAlias = null;

		boolean isMockingEnabled = false;
		boolean shouldPrepareAI = false;

		// Used to check whether multiple CUTs are uploaded.
		int cutId = -1;
		// The directory in which the CUT is saved in.
		String cutDir = null;
		// Used to run tests against the CUT.
		String cutJavaFilePath = null;

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
		final Map<Boolean, List<FileItem>> parameters = items.stream().collect(Collectors.partitioningBy(FileItem::isFormField));
		final List<FileItem> uploadParameters = parameters.get(true);
		final List<FileItem> fileParameters = parameters.get(false);

		for (FileItem uploadParameter : uploadParameters) {
			final String fieldName = uploadParameter.getFieldName();
			final String fieldValue = uploadParameter.getString();
			logger.debug("Upload parameter {" + fieldName + ":" + fieldValue + "}");
			switch (fieldName) {
				case "classAlias":
					classAlias = fieldValue;
					break;
				case "prepareForSingle":
				    // TODO Phil: legacy, will this be used in the future? (look TODO below)
					shouldPrepareAI = true;
					break;
				case "enableMocking":
					isMockingEnabled = true;
					break;
				default:
					logger.warn("Unrecognized parameter: " + fieldName);
					break;
			}
		}

		for (FileItem fileParameter : fileParameters) {
			final String fieldName = fileParameter.getFieldName();
			final String fileName = FilenameUtils.getName(fileParameter.getName());
			logger.info("Upload file parameter {" + fieldName + ":" + fileName + "}");

			if (fileName == null) {
				logger.error("Class upload failed. No class uploaded.");
				messages.add("Class upload failed. No class uploaded.");
				abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
				return;
			}

			// Upload parameter is not used.
			if (fileName.equals("")) {
				continue;
			}

			// Not a java file - file is not yet stored, so no need to cleanup
			if (!fileName.endsWith(".java")) {
				logger.error("Class upload failed. Given file {} was not a .java file.", fileName);
				messages.add("The class under test must be a .java file.");
				abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
				return;
			}

			if (reservedClassNames.contains(fileName)) {
				messages.add(fileName + " is a reserved class name, please rename your Java class.");
				abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
				return;
			}

			final String fileContent = new String(fileParameter.get(), Charset.forName("UTF-8")).trim();

			if (fileContent.isEmpty()) {
				logger.info("Class upload failed. Given Java file {} was empty", fileName);
				messages.add("File for " + fileName + "content could not be read. Please try again.");
				abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
				return;
			}

			// Currently, it is just assumed that the class under test is checked first here
			switch (fieldName) {
				case "fileUploadCUT": {
					// For superclasses and dependencies, this case has to be adjusted.
					// Also, it must be assured that all files, which belong to the cut, are handled first
					if (cutId != -1) {
						// Upload of second CUT? Abort
						logger.error("Class upload failed. Multiple classes under test uploaded.");
						messages.add("Class upload failed. Multiple classes under test uploaded.");
						abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
						return;
					}

					if (classAlias == null || classAlias.equals("")) {
						classAlias = fileName.replace(".java", "");
					}
					if (GameClassDAO.classNotExistsForAlias(classAlias)) {
						logger.error("Class upload failed. Given alias {} was already used.", classAlias);
						messages.add("Class upload failed. Given alias is already used.");
						abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
						return;
					}

					String javaFilePath;
					try {
						cutDir = Constants.CUTS_DIR + F_SEP + classAlias;
						javaFilePath = storeJavaFile(cutDir, fileName, fileContent);
						cutJavaFilePath = javaFilePath;
					} catch (IOException e) {
						logger.error("Could not store java file " + fileName, e);
						messages.add("Internal error. Sorry about that!");
						abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
						return;
					}

					String classFilePath;
					try {
						classFilePath = Compiler.compileJavaFileForContent(javaFilePath, fileContent);
					} catch (CompileException e) {
						logger.error("Could not compile {}!\n{}", fileName, e.getMessage());
						messages.add("Could not compile " + fileName + "!\n" + e.getMessage());

						abortRequestAndCleanUp(request, response, cutDir, compiledClasses, javaFilePath);
						return;
					}

					String classQualifiedName;
					try {
						classQualifiedName = getFullyQualifiedName(classFilePath);
					} catch (IOException e) {
						logger.error("Could not get fully qualified name for " + fileName, e);
						messages.add("Internal error. Sorry about that!");

						abortRequestAndCleanUp(request, response, cutDir, compiledClasses, javaFilePath, classFilePath);
						return;
					}

					final GameClass cut = new GameClass(classQualifiedName, classAlias, javaFilePath, classFilePath, isMockingEnabled);
					try {
						cutId = GameClassDAO.storeClass(cut);
					} catch (Exception e) {
						logger.error("Class upload failed. Could not store class to database.");
						messages.add("Internal error. Sorry about that!");
						abortRequestAndCleanUp(request, response, cutDir, compiledClasses, javaFilePath, classFilePath);
						return;
					}

					logger.debug("Successfully uploaded Class Under Test: {}", classAlias);
					compiledClasses.add(new CompiledClass(CompileClassType.CUT, cutId, javaFilePath, classFilePath));
					break;
				}
				case "fileUploadMutant": {
					if (cutId == -1) {
						logger.error("Class upload failed. Mutant uploaded, but no class under test.");
						messages.add("Class upload failed. Mutant uploaded, but no class under test.");
						abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
						return;
					}
					String javaFilePath;
					try {
						javaFilePath = storeJavaFile(cutDir + F_SEP + "mutants", fileName, fileContent);
					} catch (IOException e) {
						logger.error("Could not store java file " + fileName, e);
						messages.add("Internal error. Sorry about that!");
						abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
						return;
					}
					String classFilePath;
					try {
						classFilePath = Compiler.compileJavaFileForContent(javaFilePath, fileContent);
					} catch (CompileException e) {
						logger.error("Could not compile {}!\n{}", fileName, e.getMessage());
						messages.add("Could not compile " + fileName + "!\n" + e.getMessage());

						abortRequestAndCleanUp(request, response, cutDir, compiledClasses, javaFilePath);
						return;
					}

					Integer mutantId;
					final String md5 = CodeValidator.getMD5FromText(fileContent);
					final Mutant mutant = new Mutant(javaFilePath, classFilePath, md5, cutId);
					try {
						mutantId = MutantDAO.storeMutant(mutant);
					} catch (Exception e) {
						logger.error("Class upload with mutant failed. Could not store mutant to database.");
						messages.add("Internal error. Sorry about that!");

						abortRequestAndCleanUp(request, response, cutDir, compiledClasses, javaFilePath, classFilePath);
						return;
					}

					compiledClasses.add(new CompiledClass(CompileClassType.MUTANT, mutantId, javaFilePath, classFilePath));
					break;
				}
				case "fileUploadTest": {
					if (cutId == -1) {
						logger.error("Class upload failed. Test uploaded, but no class under test.");
						messages.add("Class upload failed. Test uploaded, but no class under test.");

						abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
						return;
					}
					String javaFilePath;
					try {
						javaFilePath = storeJavaFile(cutDir + F_SEP + "tests", fileName, fileContent);
					} catch (IOException e) {
						logger.error("Class upload failed. Could not store java file of test class " + fileName, e);
						messages.add("Class upload failed. Could not store java file of test class " + fileName);

						abortRequestAndCleanUp(request, response, cutDir, compiledClasses);
						return;
					}
					String classFilePath;
					try {
						classFilePath = Compiler.compileJavaTestFileForContent(javaFilePath, fileContent, cutJavaFilePath);
					} catch (CompileException e) {
						logger.error("Class upload failed. Could not compile {}!\n{}", fileName, e.getMessage());
						messages.add("Class upload failed. Could not compile " + fileName + "!\n" + e.getMessage());

						abortRequestAndCleanUp(request, response, cutDir, compiledClasses, javaFilePath);
						return;
					}

					try {
						Runner.runTestAgainstClass(javaFilePath, cutJavaFilePath);
					} catch (Exception e) {
						logger.error("Class upload failed. Test " + fileName + " failed", e);
						messages.add("Class upload failed. Test " + fileName + " failed");

						abortRequestAndCleanUp(request, response, cutDir, compiledClasses, javaFilePath, classFilePath);
						return;
					}

					Integer testId;
					final Test test = new Test(javaFilePath, classFilePath, cutId);
					try {
						testId = TestDAO.storeTest(test);
					} catch (Exception e) {
						logger.error("Class upload with mutant failed. Could not store mutant to database.");
						messages.add("Internal error. Sorry about that!");

						abortRequestAndCleanUp(request, response, cutDir, compiledClasses, javaFilePath, classFilePath);
						return;
					}

					compiledClasses.add(new CompiledClass(CompileClassType.TEST, testId, javaFilePath, classFilePath));
					break;
				}
				default:
					logger.warn("Unrecognized parameter: " + fieldName);
					break;
			}
		}

		Redirect.redirectBack(request, response);
		messages.add("Class upload successful.");

		// TODO Phil: Will this be used in the future? Looks like legacy code.
//			if (shouldPrepareAI) {
//				if (!PrepareAI.createTestsAndMutants()) {
//					logger.error("Preparation of AI for class failed, please prepare the class again, or try a different class.");
//					messages.add("Preparation of AI for class failed, please prepare the class again, or try a different class.");
//				}
//			}

	}

	/**
	 * Returns the qualified name of a java class for a given {@code .java} file content.
	 * <p>
	 * E.g. {@code java.util.Collection} for {@link Collection}.
	 *
	 *
	 * @param javaClassFilePath The path to the java class file.
	 * @return A qualified name of the given java class.
	 * @throws IOException when reading the java file fails.
	 */
	private String getFullyQualifiedName(String javaClassFilePath) throws IOException {
		ClassPool classPool = ClassPool.getDefault();
		CtClass cc = classPool.makeClass(new FileInputStream(new File(javaClassFilePath)));
		return cc.getName();
	}

	/**
	 * Stores a Java file for given parameters on the hard drive.
	 *
	 * @param folderPath The path of the folder the Java file will be stored in.
	 * @param fileName The file name (e.g. {@code MyClass.java}).
	 * @param fileContent The actual file content.
	 * @return The path of the newly stored Java file.
	 * @throws IOException when storing the file fails.
	 */
	private String storeJavaFile(String folderPath, String fileName, String fileContent) throws IOException {
		final String filePath = folderPath + F_SEP + fileName;
		logger.debug("storeJavaFile: folderPath={}", folderPath);
		logger.debug("storeJavaFile: filePath={}", filePath);
		try {
			Files.createDirectories(Paths.get(folderPath));
			final Path path = Files.createFile(Paths.get(filePath));
			Files.write(path, fileContent.getBytes());
			return path.toString();
		} catch (IOException e) {
			logger.error("Could not store Java File.", e);
			try {
				// removing folder again, if empty
				Files.delete(Paths.get(folderPath));
			} catch (DirectoryNotEmptyException ignored) {
			}
			throw e;
		}
	}

	/**
	 * Aborts a given request by removing all uploaded compile classes from for
	 * the database and {@code .java} and {@code .class} files from the system.
	 * <p>
     * Also redirects the user.
	 * <p>
     * This method should be the last thing called when aborting a request.
     *
     * @param request The handled request.
     * @param response The response of the handled requests.
	 * @param cutDir The directory in which all files are located.
     * @param compiledClasses A list of {@link CompiledClass}, which will get removed.
	 * @param files Optional additional files, which need to be removed.
     * @throws IOException When an error during redirecting occurs.
     */
    private void abortRequestAndCleanUp(HttpServletRequest request, HttpServletResponse response, String cutDir, List<CompiledClass> compiledClasses, String... files) throws IOException {
		logger.info("Aborting request...");
		if (cutDir != null) {
            final List<Integer> cuts = new LinkedList<>();
            final List<Integer> mutants = new LinkedList<>();
            final List<Integer> tests = new LinkedList<>();
            for (CompiledClass compiledClass : compiledClasses) {
                switch (compiledClass.type) {
                    case CUT:
                        cuts.add(compiledClass.id);
                        break;
                    case MUTANT:
                        mutants.add(compiledClass.id);
                        break;
                    case TEST:
                        tests.add(compiledClass.id);
                        break;
                }
            }

            try {
                logger.info("Removing directory {} again", cutDir);
                FileUtils.forceDelete(new File(cutDir));
            } catch (IOException e) {
                // logged, but otherwise ignored. Not need to abort while aborting...
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
            GameClassDAO.removeClassesForIds(cuts);
		}

		Redirect.redirectBack(request, response);
		logger.info("Aborting request...done");
	}

	/**
	 * Wrapper class for classes, which have been compiled already.
	 * They have a type {@link CompileClassType}, an {@code id} and
	 * paths to {@code .java} and {@code .class} files.
	 */
	private class CompiledClass {
		private CompileClassType type;
		private Integer id;
		private String javaFile;
		private String classFile;

		CompiledClass(CompileClassType type, Integer id, String javaFile, String classFile) {
			this.type = type;
			this.id = id;
			this.javaFile = javaFile;
			this.classFile = classFile;
		}
	}
	private enum CompileClassType {
		CUT,
		MUTANT,
		TEST
	}
}
