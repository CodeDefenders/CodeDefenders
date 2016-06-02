package org.codedefenders;

import javassist.ClassPool;
import javassist.CtClass;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

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

		String classId = null;
		GameClass newSUT = null;

		// Get actual parameters, because of the upload component, I can't do request.getParameter before fetching the file
		try {
			List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
			for (FileItem item : items) {
				if (item.isFormField()) {
					// Process regular form field
					String fieldName = item.getFieldName();
					String fieldValue = item.getString();
					System.out.println("Upload parameter {" + fieldName + ":" + fieldValue + "}");
					if (fieldName.equals("classAlias"))
						classId = fieldValue;
					else
						System.out.println("Unrecognized parameter");
				} else {
					// Process form file field (input type="file").
					String fieldName = item.getFieldName();
					String fileName = FilenameUtils.getName(item.getName());
					System.out.println("Upload file parameter {" + fieldName + ":" + fileName + "}");
					if (fieldName.equals("fileUpload") && !fileName.isEmpty()) {
						System.out.println("Uploading new CUT: " + fileName);

						InputStream fileContent = item.getInputStream();
						File targetFile = new File(getServletContext().getRealPath(Constants.CUTS_DIR + Constants.FILE_SEPARATOR + fileName));
						if (targetFile.exists()) {
							messages.add("A class with the same name already exists, please try with a different one.");
							response.sendRedirect(request.getHeader("referer"));
							break;
						}
						FileUtils.copyInputStreamToFile(fileContent, targetFile);
						String javaFileNameDB = DatabaseAccess.addSlashes(targetFile.getAbsolutePath());
						String classFileName = AntRunner.compileCUT(getServletContext(), fileName);
						if (classFileName != null) {
							String classFileNameDB = DatabaseAccess.addSlashes(classFileName);

							// get fully qualified name
							ClassPool classPool = ClassPool.getDefault();
							CtClass cc = classPool.makeClass(new FileInputStream(new File(classFileName)));
							String fullyQualifiedName = cc.getName();

							// db insert
							newSUT = new GameClass(fullyQualifiedName, javaFileNameDB, classFileNameDB);
							newSUT.insert();

							response.sendRedirect("games/create");

						} else {
							messages.add("We were unable to compile your class, please try with a simpler one (no dependencies)");
							response.sendRedirect(request.getHeader("referer"));
							break;
						}
					} else {
						messages.add("No class was selected.");
						response.sendRedirect(request.getHeader("referer"));
						break;
					}
				}
			}
		} catch (FileUploadException e) {
			throw new ServletException("Cannot parse multipart request.", e);
		}
	}
}