package gammut;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.sql.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

public class UpdateAvailableClasses extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

    	System.out.println("Running UpdateAvailableClasses");

    	File resourcesFile = new File(getServletContext().getRealPath("/WEB-INF/resources"));
    	ArrayList<String> classes = new ArrayList<String>();

    	System.out.println("Got Resources path");

    	for (String s : resourcesFile.list()) {
    		if (s.contains(".java")) {
            	classes.add(s.substring(0, s.length()-5));
          	}
    	}

    	InputStream javaStream;
    	InputStream classStream;

        Connection conn = null;
        Statement stmt = null;
        String sql = null;
        PreparedStatement pstmt = null;

        try {
        	System.out.println("Trying to connect to db");
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);


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
            	javaStream = getServletContext().getResourceAsStream("/WEB-INF/resources/"+s+".java");
            	classStream = getServletContext().getResourceAsStream("/WEB-INF/resources/"+s+".class");

            	System.out.println("Adding to statement");
            	// Add the blobs to the prepared statement
            	pstmt = conn.prepareStatement("INSERT INTO classes (Name, JavaFile, ClassFile) VALUES (?, ?, ?);");
            	pstmt.setString(1, s);
            	pstmt.setBinaryStream(2, javaStream);
            	pstmt.setBinaryStream(3, classStream);

            	System.out.println("Executing statement: " + pstmt);
            	pstmt.execute();
            }

            stmt.close();
            pstmt.close();
            conn.close();

        } catch(SQLException se) {
            System.out.println(se);
            //Handle errors for JDBC
        } catch(Exception e) {
            System.out.println(e);
            //Handle errors for Class.forName
            e.printStackTrace();
        } finally{
            //finally block used to close resources
            try {
                if(stmt!=null)
                   stmt.close();
            } catch(SQLException se2) {}// nothing we can do

            try {
                if(conn!=null)
                conn.close();
            } catch(SQLException se) {
                se.printStackTrace();
            }//end finally try
        } //end try

        RequestDispatcher dispatcher = request.getRequestDispatcher("html/login_view.jsp");
        dispatcher.forward(request, response);
    }

}