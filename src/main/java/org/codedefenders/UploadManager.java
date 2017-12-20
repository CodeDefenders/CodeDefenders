package org.codedefenders;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import javassist.ClassPool;
import javassist.CtClass;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.codedefenders.singleplayer.PrepareAI;
import org.codedefenders.util.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class UploadManager extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(AntRunner.class);
	private boolean fromAdmin;

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		response.sendRedirect(request.getContextPath()+"/games/upload");
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		HttpSession session = request.getSession();
		ArrayList<String> messages = new ArrayList<>();
		session.setAttribute("messages", messages);

		System.out.println("Uploading CUT");

		String classAlias = null;
		String fileName = null;
		String fileContent = null;
		GameClass newSUT = null;

		boolean shouldPrepareAI = false;

		// Get actual parameters, because of the upload component, I can't do request.getParameter before fetching the file
		try {
			List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
			for (FileItem item : items) {

				if (item.isFormField()) {
					// Process class alias
					String fieldName = item.getFieldName();
					String fieldValue = item.getString();
					System.out.println("Upload parameter {" + fieldName + ":" + fieldValue + "}");
					if (fieldName.equals("classAlias"))
						classAlias = fieldValue;
					else if (fieldName.equals("prepareForSingle"))
						shouldPrepareAI = true;
					else if (fieldName.equals("fromAdmin"))
						fromAdmin = fieldValue.equals("true");
					else
						System.out.println("Unrecognized parameter");
				} else {
					// Process class file
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

		// Not a java file
		if (!fileName.endsWith(".java")) {
			messages.add("The class under test must be a .java file.");
			String redirect = (String) request.getHeader("referer");
			if( ! redirect.startsWith(request.getContextPath())){
				redirect = request.getContextPath()+"/" + redirect;
			}
			response.sendRedirect(redirect);
			return;
		}

		// no file content?
		if (fileContent == null || fileContent.isEmpty()) {
			messages.add("File content could not be read. Please try again.");
			String redirect = (String) request.getHeader("referer");
			if( ! redirect.startsWith(request.getContextPath())){
				redirect = request.getContextPath()+"/" + redirect;
			}
			response.sendRedirect(redirect);
			return;
		}

		// alias provided
		if (classAlias != null && !classAlias.isEmpty()) {
			// check if basename exists as CUT dir
			logger.info("Checking if alias {} is a good directory to store the class", classAlias);
			GameClass cut = new GameClass("", classAlias, "", "");
			if (cut.insert()) {
				storeClass(request, response, messages, fileName, fileContent, cut, shouldPrepareAI);
				return;
			} else
				logger.info("Alias has already been used. Trying with class name as alias instead.");
		}

		// try with basename as alias
		String baseName = FilenameUtils.getBaseName(fileName);
		logger.info("Checking if base name {} is a good directory to store the class", baseName);
		GameClass cut = new GameClass("", baseName, "", "");
		if (cut.insert()) {
			storeClass(request, response, messages, fileName, fileContent, cut, shouldPrepareAI);
			return;
		} else
			logger.info("Class name has already been used as alias. Trying with fully qualified class name now.");

		// now try fully qualified name
		String fullName = getFullyQualifiedName(fileName, fileContent);
		cut = new GameClass("", fullName, "", "");
		logger.info("Checking if full name {} is a good directory to store the class", fullName);
		if (cut.insert()) {
			storeClass(request, response, messages, fileName, fileContent, cut, shouldPrepareAI);
			return;
		} else {
			// Neither alias nor basename or fullname are good, make up a name using a suffix
			int index = 2;
			cut = new GameClass("", baseName + index, "", "");
			while (!cut.insert()) {
				index++;
				cut.setAlias(baseName + index);
			}
			storeClass(request, response, messages, fileName, fileContent, cut, shouldPrepareAI);
			return;
		}
	}


	public void storeClass(HttpServletRequest request, HttpServletResponse response, ArrayList<String> messages, String fileName, String fileContent, GameClass cut, boolean shouldPrepareAI) throws IOException {

		String contextPath = request.getContextPath();

		String cutDir = Constants.CUTS_DIR + Constants.F_SEP + cut.getAlias();
		File targetFile = new File(cutDir + Constants.F_SEP + fileName);
		assert (!targetFile.exists());

		FileUtils.writeStringToFile(targetFile, fileContent);
		String javaFileNameDB = DatabaseAccess.addSlashes(targetFile.getAbsolutePath());
		// Create CUT, temporarily using file name as class name for compilation
		cut.setJavaFile(javaFileNameDB);
		// Compile original class, using alias as directory name
		String classFileName = AntRunner.compileCUT(cut);

		if (classFileName != null) {
			String classFileNameDB = DatabaseAccess.addSlashes(classFileName);

			// get fully qualified name
			ClassPool classPool = ClassPool.getDefault();
			CtClass cc = classPool.makeClass(new FileInputStream(new File(classFileName)));
			String classQualifiedName = cc.getName();

			// db insert
			cut.setName(classQualifiedName);
			cut.setClassFile(classFileNameDB);
			cut.update();

			if (shouldPrepareAI) {
				//Prepare AI classes, by generating tests and mutants.
				if (!PrepareAI.createTestsAndMutants(cut.getId())) {
					messages.add("Preparation of AI for the class failed, please prepare the class again, or try a different class.");
				}
			}
			messages.add("Class uploaded successfully. It will be referred to as: " + cut.getAlias());
			// Redirect to admin interface if corresponding url param is set
			String redirect = fromAdmin ? contextPath + "/admin" : contextPath + "/games/user";
			response.sendRedirect(redirect);

		} else {
			cut.delete();
			messages.add("We were unable to compile your class, please try with a simpler one (no dependencies)");
			String redirect = (String) request.getHeader("referer");
			if( ! redirect.startsWith(request.getContextPath())){
				redirect = request.getContextPath()+"/" + redirect;
			}
			response.sendRedirect(redirect);
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
