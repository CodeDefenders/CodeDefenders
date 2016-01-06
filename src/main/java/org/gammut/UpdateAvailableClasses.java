package org.gammut;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class UpdateAvailableClasses extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		System.out.println("Running UpdateAvailableClasses");

		String sources = getServletContext().getRealPath("/WEB-INF/data/sources");
		System.out.println(sources);
		File sourcesFile = new File(sources);
		ArrayList<String> classes = new ArrayList<String>();

		for (String s : sourcesFile.list()) {
			if (s.contains(".java")) {
				classes.add(s.substring(0, s.length() - 5));
			}
		}

		Connection conn = null;
		Statement stmt = null;
		String sql = null;
		String javaFile;
		String classFile;

		try {
			conn = DatabaseAccess.getConnection();


			System.out.println("Gonna delete");
			// First clear the table.
			stmt = conn.createStatement();
			sql = "DELETE FROM classes;";
			stmt.execute(sql);

			System.out.println("Starting to loop");
			// For each differently named class in the resources folder.
			for (String s : classes) {

				System.out.println("Getting file paths");
				// Get the path to each file to be stored.
				javaFile = DatabaseAccess.addSlashes(sources + "/" + s + ".java");
				classFile = DatabaseAccess.addSlashes(sources + "/" + s + ".class");

				System.out.println(javaFile);
				System.out.println(classFile);
				stmt = conn.createStatement();
				sql = String.format("INSERT INTO classes (Name, JavaFile, ClassFile) VALUES ('%s', '%s', '%s');", s, javaFile, classFile);
				stmt.execute(sql);
			}

			stmt.close();
			conn.close();

		} catch (SQLException se) {
			System.out.println(se);
			//Handle errors for JDBC
		} catch (Exception e) {
			System.out.println(e);
			//Handle errors for Class.forName
		} finally {
			//finally block used to close resources
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
			}// nothing we can do

			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				System.out.println(se);
			}//end finally try
		} //end try
	}

}