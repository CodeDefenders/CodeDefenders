package org.codedefenders;

import javassist.ClassPool;
import javassist.CtClass;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

public class UpdateAvailableClasses extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		System.out.println("Running UpdateAvailableClasses");

		String cutsDirPath = Constants.CUTS_DIR;
		System.out.println(cutsDirPath);
		File cutsDirFile = new File(cutsDirPath);

		Collection<File> sutFiles = FileUtils.listFiles(cutsDirFile, new String[]{"java"}, true);

		/* Clear the list of CUTs in the DB */
		GameClass.clear();

		ClassPool classPool = ClassPool.getDefault();
		for (File sutFile : sutFiles) {
			String name = FilenameUtils.getBaseName(sutFile.getName());
			System.out.println("Getting file paths");
			// Get the path to each file to be stored.
			String sourceFileName = cutsDirPath + Constants.F_SEP + name + Constants.JAVA_SOURCE_EXT;

			final String compiledClassName = name + Constants.JAVA_CLASS_EXT;
			LinkedList<File> matchingFiles = (LinkedList)FileUtils.listFiles(cutsDirFile, FileFilterUtils.nameFileFilter(compiledClassName), FileFilterUtils.trueFileFilter());
			if (! matchingFiles.isEmpty()) {
				String compiledFileName = matchingFiles.get(0).getAbsolutePath();
				String javaFile = DatabaseAccess.addSlashes(sourceFileName);
				String classFile = DatabaseAccess.addSlashes(compiledFileName);
				CtClass cc = classPool.makeClass(new FileInputStream(new File(compiledFileName)));
				String fullyQualifiedName = cc.getName();
				GameClass sut = new GameClass(fullyQualifiedName, javaFile, classFile);
				sut.insert();
				System.out.println(javaFile);
				System.out.println(classFile);
			}
		}
	}

}