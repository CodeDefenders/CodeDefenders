package org.codedefenders.servlets;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.codedefenders.execution.AntRunner;
import org.codedefenders.execution.CutCompileException;
import org.codedefenders.util.Constants;
import org.codedefenders.game.GameClass;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.game.singleplayer.PrepareAI;
import org.codedefenders.servlets.util.Redirect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javassist.ClassPool;
import javassist.CtClass;

public class UploadManager extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(AntRunner.class);
	private boolean fromAdmin;

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		response.sendRedirect(request.getContextPath() + "/games/upload");
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		HttpSession session = request.getSession();
		ArrayList<String> messages = new ArrayList<>();
		session.setAttribute("messages", messages);

		logger.debug("Uploading CUT");

		String classAlias = null;
		String fileName = null;
		String fileContent = null;

		GameClass newSUT = null;

		boolean isMockingEnabled = false;
		boolean shouldPrepareAI = false;

		// Get actual parameters, because of the upload component, I can't do
		// request.getParameter before fetching the file
		try {
			List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
			for (FileItem item : items) {

				if (item.isFormField()) {
					// Process class alias
					String fieldName = item.getFieldName();
					String fieldValue = item.getString();
					logger.debug("Upload parameter {" + fieldName + ":" + fieldValue + "}");
					if (fieldName.equals("classAlias"))
						classAlias = fieldValue;
					else if (fieldName.equals("prepareForSingle"))
						shouldPrepareAI = true;
					else if (fieldName.equals("fromAdmin"))
						fromAdmin = fieldValue.equals("true");
					else if (fieldName.equals("enableMocking")) {
						isMockingEnabled = true;
					} else
						logger.warn("Unrecognized parameter");
				} else {
					// Process class file. Store the file content inside the
					// fileContent String
					String fieldName = item.getFieldName();
					fileName = FilenameUtils.getName(item.getName());
					logger.info("Upload file parameter {" + fieldName + ":" + fileName + "}");

					if (fieldName.equals("fileUpload") && !fileName.isEmpty()) {
						StringWriter writer = new StringWriter();
						IOUtils.copy(item.getInputStream(), writer, "UTF-8");
						fileContent = writer.toString().trim();
					}
				}
			}
		} catch (FileUploadException e) {
			throw new ServletException("Cannot parse multipart request.", e);
		}

		// Not a java file - file is not yet stored, so no need to cleanup
		if (!fileName.endsWith(".java")) {
			messages.add("The class under test must be a .java file.");
			Redirect.redirectBack(request, response);
			return;
		}

		// No file content? = no need to clean up
		if (fileContent == null || fileContent.isEmpty()) {
			messages.add("File content could not be read. Please try again.");
			Redirect.redirectBack(request, response);
			return;
		}

		// TODO Is this aliasing thing even working ?
		// alias provided
		if (classAlias != null && !classAlias.isEmpty()) {
			// check if basename exists as CUT dir
			logger.info("Checking if alias {} is a good directory to store the class", classAlias);

			GameClass cut = new GameClass("", classAlias, "", "");

			// This inserts
			if (cut.insert()) {
				storeClass(request, response, messages, fileName, fileContent, cut, shouldPrepareAI, isMockingEnabled);
				return;
			} else {
				logger.info("Alias has already been used. Trying with class name as alias instead.");
			}
		}

		// try with basename as alias
		String baseName = FilenameUtils.getBaseName(fileName);
		logger.info("Checking if base name {} is a good directory to store the class", baseName);
		GameClass cut = new GameClass("", baseName, "", "");
		if (cut.insert()) {
			storeClass(request, response, messages, fileName, fileContent, cut, shouldPrepareAI, isMockingEnabled);
			return;
		} else{
			logger.info("Class name has already been used as alias. Trying with fully qualified class name now.");
		}

		// now try fully qualified name
		String fullName = getFullyQualifiedName(fileName, fileContent);
		cut = new GameClass("", fullName, "", "");
		logger.info("Checking if full name {} is a good directory to store the class", fullName);
		if (cut.insert()) {
			storeClass(request, response, messages, fileName, fileContent, cut, shouldPrepareAI, isMockingEnabled);
			return;
		} else {
			// Neither alias nor basename or fullname are good, make up a name
			// using a suffix
			int index = 2;
			cut = new GameClass("", baseName + index, "", "");
			while (!cut.insert()) {
				index++;
				cut.setAlias(baseName + index);
			}
			storeClass(request, response, messages, fileName, fileContent, cut, shouldPrepareAI, isMockingEnabled);
			return;
		}
	}

	/**
	 * Store the fileContent of the class in the file fileName under the source
	 * folder if and only if there's not problems with the file. Other
	 * 
	 * @param request
	 * @param response
	 * @param messages
	 * @param fileName
	 * @param fileContent
	 * @param cut
	 * @param shouldPrepareAI
	 * @param isMockingEnabled
	 * @throws IOException
	 */
	public void storeClass(HttpServletRequest request, HttpServletResponse response, ArrayList<String> messages,
			String fileName, String fileContent, GameClass cut, boolean shouldPrepareAI, boolean isMockingEnabled)
			throws IOException {

		String contextPath = request.getContextPath();

		String cutDir = Constants.CUTS_DIR + Constants.F_SEP + cut.getAlias();
		File targetFile = new File(cutDir + Constants.F_SEP + fileName);

		// TODO: What we do, if the same file exits ?!
		assert (!targetFile.exists());

		/*
		 * Try to store the file on FS. This should prevent cases in which
		 * folder is not writable but CUT is inside the DB
		 */
		try {
			FileUtils.writeStringToFile(targetFile, fileContent);
		} catch (IOException e) {
			/*
			 * Before calling store class, we call cut.insert() which creates
			 * the entry into the DB, so we need to delete it if we cannot store
			 * the file on the file system
			 */
			cut.delete();
			logger.warn("Cannot create file " + targetFile +". Reason: " + e.getMessage() );
			messages.add("Sorry, we are unable to process your class at this moment. Contact the administrator.");
			Redirect.redirectBack(request, response);
			return;
		}

		String javaFileNameDB = DatabaseAccess.addSlashes(targetFile.getAbsolutePath());
		// Create CUT, temporarily using file name as class name for compilation
		cut.setJavaFile(javaFileNameDB);

		// Try to compile original class, using alias as directory name
		String classFileName = null;
		try{
			classFileName = AntRunner.compileCUT(cut);

			// If compilation succeeds
			String classFileNameDB = DatabaseAccess.addSlashes(classFileName);

			// get fully qualified name
			ClassPool classPool = ClassPool.getDefault();
			CtClass cc = classPool.makeClass(new FileInputStream(new File(classFileName)));
			String classQualifiedName = cc.getName();

			// db insert
			cut.setMockingEnabled(isMockingEnabled);
			cut.setName(classQualifiedName);
			cut.setClassFile(classFileNameDB);
			cut.update();

			if (shouldPrepareAI) {
				// Prepare AI classes, by generating tests and mutants.
				if (!PrepareAI.createTestsAndMutants(cut.getId())) {
					messages.add(
							"Preparation of AI for the class failed, please prepare the class again, or try a different class.");
				}
			}
			messages.add("Class uploaded successfully. It will be referred to as: " + cut.getAlias());
			// Redirect to admin interface if corresponding url param is set
			String redirect = fromAdmin ? contextPath + "/admin" : contextPath + "/games/user";
			response.sendRedirect(redirect);
		} catch (CutCompileException e) {
			/*
			 * If the class was not compilable, we delete the entry from the db
			 * and the files from the file system.
			 */
			cut.delete();

			File parentFolder = targetFile.getParentFile();
			boolean classRemoved = targetFile.delete();
			if (!classRemoved) {
				logger.error("Cannot remove uncompilable class " + targetFile);
			}
			boolean folderRemoved = parentFolder.delete();
			if (!folderRemoved) {
				logger.error("Cannot remove source folder (" + folderRemoved + ")for uncompilable class " + targetFile);
			}

			messages.add("We were unable to compile your class");
			messages.add( e.getMessage() );

			Redirect.redirectBack(request, response);
			return;
		}
	}

	private String getFullyQualifiedName(String fileName, String fileContent) {
		try {
			Path tmp = Files.createTempDirectory("code-defenders-upload-");
			File tmpDir = tmp.toFile();
			tmpDir.deleteOnExit();
			File tmpFile = new File(tmpDir.getAbsolutePath() + Constants.F_SEP + fileName);
			FileUtils.writeStringToFile(tmpFile, fileContent);
			FileInputStream in = new FileInputStream(tmpFile);
			CompilationUnit cu = JavaParser.parse(in);
			if (null != cu && null != cu.getPackage() && !cu.getPackage().getName().getName().isEmpty())
				return cu.getPackage().getName() + "." + FilenameUtils.getBaseName(fileName);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return FilenameUtils.getBaseName(fileName);
	}
}
