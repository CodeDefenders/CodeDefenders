package org.codedefenders;

import javassist.ClassPool;
import javassist.CtClass;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.codedefenders.singleplayer.PrepareAI;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class UploadManager extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		response.sendRedirect("games/upload");
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		HttpSession session = request.getSession();
		ArrayList<String> messages = new ArrayList<>();
		session.setAttribute("messages", messages);

		System.out.println("Uploading CUT");

		String classAlias = null;
		String fileName = null;
		InputStream fileContent = null;
		GameClass newSUT = null;

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
					else
						System.out.println("Unrecognized parameter");
				} else {
					// Process class file
					String fieldName = item.getFieldName();
					fileName = FilenameUtils.getName(item.getName());
					System.out.println("Upload file parameter {" + fieldName + ":" + fileName + "}");

					if (fieldName.equals("fileUpload") && !fileName.isEmpty())
						fileContent = item.getInputStream();
				}
			}
		} catch (FileUploadException e) {
			throw new ServletException("Cannot parse multipart request.", e);
		}

		//Not a java file
		if (!fileName.contains(".java")) {
			messages.add("The class under test must be a .java file.");
			response.sendRedirect(request.getHeader("referer"));
			return;
		}

		// two arguments processed?
		if (classAlias == null || fileContent == null) {
			messages.add("Please provide unique identifier and a .java file.");
			response.sendRedirect(request.getHeader("referer"));
			return;
		}
		// check that class alias is unique
		if (GameClass.existUniqueClassID(classAlias)) {
			messages.add("A class with identifier " + classAlias + " already exists, please use a different name.");
			response.sendRedirect(request.getHeader("referer"));
			return;
		}
		File targetFile = new File(Constants.CUTS_DIR + Constants.F_SEP + classAlias
				+ Constants.F_SEP + fileName);
		if (targetFile.exists()) {
			messages.add("A class with the same name already exists, please try with a different one.");
			response.sendRedirect(request.getHeader("referer"));
			return;
		}
		FileUtils.copyInputStreamToFile(fileContent, targetFile);
		String javaFileNameDB = DatabaseAccess.addSlashes(targetFile.getAbsolutePath());
		// Create CUT, temporarily using file name as class name for compilation
		newSUT = new GameClass("", classAlias, javaFileNameDB, "");
		//Compile original class, using alias as directory name
		String classFileName = AntRunner.compileCUT(newSUT);

		if (classFileName != null) {
			String classFileNameDB = DatabaseAccess.addSlashes(classFileName);

			// get fully qualified name
			ClassPool classPool = ClassPool.getDefault();
			CtClass cc = classPool.makeClass(new FileInputStream(new File(classFileName)));
			String classQualifiedName = cc.getName();

			// db insert
			newSUT.setName(classQualifiedName);
			newSUT.setClassFile(classFileNameDB);
			newSUT.insert();

			//TODO: CHECK SINGLEPLAYER PREPARATION CHECKBOX
			//Prepare AI classes, by generating tests and mutants.
			//PrepareAI.createTestsAndMutants(newSUT.getId());

			response.sendRedirect("games/user");

		} else {
			//TODO: Alternate method of catching errors?
			messages.add("We were unable to compile your class, please try with a simpler one (no dependencies)");
			response.sendRedirect(request.getHeader("referer"));
			return;
		}
	}
}
