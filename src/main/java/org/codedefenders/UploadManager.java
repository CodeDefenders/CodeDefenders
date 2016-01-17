package org.codedefenders;

import javassist.ClassPool;
import javassist.CtClass;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class UploadManager extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		response.sendRedirect("games/upload");
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		HttpSession session = request.getSession();
		// Get their user id from the session.
		int uid = (Integer) session.getAttribute("uid");

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
						FileUtils.copyInputStreamToFile(fileContent, targetFile);
						String javaFileNameDB = DatabaseAccess.addSlashes(targetFile.getAbsolutePath());
						String classFileName = compileNewCUT(getServletContext(), fileName);
						String classFileNameDB = DatabaseAccess.addSlashes(classFileName);

						// get fully qualified name
						ClassPool classPool = ClassPool.getDefault();
						CtClass cc = classPool.makeClass(new FileInputStream(new File(classFileName)));
						String fullyQualifiedName = cc.getName();

						// db insert
						newSUT = new GameClass(fullyQualifiedName, javaFileNameDB, classFileNameDB);
						newSUT.insert();

						response.sendRedirect("games/create");
					} else
						System.err.println("No class was selected.");
				}
			}
		} catch (FileUploadException e) {
			throw new ServletException("Cannot parse multipart request.", e);
		}
	}

	private String compileNewCUT(ServletContext context, final String className) {

		String[] resultArray = AntRunner.compileClass(context,className);
		System.out.println("Compile New CUT, Compilation result:");
		System.out.println(Arrays.toString(resultArray));

		String pathCompiledClassName = null;
		if (resultArray[0].toLowerCase().contains("build successful")) {
			// If the input stream returned a 'successful build' message, the CUT compiled correctly
			System.out.println("Compiled uploaded CUT successfully");
			File f = new File(context.getRealPath(Constants.CUTS_DIR));
			final String compiledClassName = FilenameUtils.getBaseName(className) + Constants.JAVA_CLASS_EXT;
			LinkedList<File> matchingFiles = (LinkedList)FileUtils.listFiles(f, FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter());
			if (! matchingFiles.isEmpty())
				pathCompiledClassName = matchingFiles.get(0).getAbsolutePath();
		} else {
			// Otherwise the CUT failed to compile
			String message = resultArray[0].substring(resultArray[0].indexOf("[javac]"));
			System.err.println("Failed to compile uploaded CUT");
			System.err.println(message);
		}
		return pathCompiledClassName;
	}
}