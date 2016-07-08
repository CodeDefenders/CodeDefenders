package org.codedefenders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class GameClass {

	private static final Logger logger = LoggerFactory.getLogger(GameClass.class);

	public int id;
	public String name; // fully qualified name
	public String javaFile;
	public String classFile;

	public GameClass(String name, String jFile, String cFile) {
		this.name = name;
		this.javaFile = jFile;
		this.classFile = cFile;
	}

	public GameClass(int id, String name, String jFile, String cFile) {
		this(name, jFile, cFile);
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBaseName() {
		String[] tokens = name.split("\\.");
		return tokens[tokens.length-1];
	}

	public String getPackage() {
		return (name.contains(".")) ? name.substring(0, name.lastIndexOf('.')) : "";
	}

	public String getAsString() {
		InputStream resourceContent = null;
		String result = "";
		try {
			resourceContent = new FileInputStream(javaFile);
			BufferedReader is = new BufferedReader(new InputStreamReader(resourceContent));
			String line;
			while ((line = is.readLine()) != null) {
				result += line + "\n";
			}

		} catch (FileNotFoundException e) {
			result = "[File Not Found]";
			e.printStackTrace();
		} catch (IOException e) {
			result = "[File Not Readable]";
			e.printStackTrace();
		}
		return result;

	}

	public boolean insert() {

		logger.debug("Inserting class (Name={}, JavaFile={}, ClassFile={})", name, javaFile, classFile);
		Connection conn = null;
		Statement stmt = null;
		String sql = String.format("INSERT INTO classes (Name, JavaFile, ClassFile) VALUES ('%s', '%s', '%s');", name, javaFile, classFile);

		// Attempt to insert game info into database
		try {
			conn = DatabaseAccess.getConnection();

			stmt = conn.createStatement();

			stmt.execute(sql, Statement.RETURN_GENERATED_KEYS);

			ResultSet rs = stmt.getGeneratedKeys();

			if (rs.next()) {
				this.id = rs.getInt(1);
				System.out.println("Inserted CUT with ID: " + this.id);
				stmt.close();
				conn.close();
				return true;
			}

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

		return false;
	}

	public static void clear() {
		Connection conn = null;
		Statement stmt = null;
		String sql = "DELETE FROM classes;";

		try {
			System.out.println("Clear classes table");
			conn = DatabaseAccess.getConnection();
			stmt = conn.createStatement();
			stmt.execute(sql);
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

	public String getTestTemplate() {
		StringBuilder sb = new StringBuilder();
		if (! getPackage().isEmpty())
			sb.append(String.format("package %s;%n", getPackage()));
		else
			sb.append(String.format("/* no package name */%n"));
		sb.append(String.format("%n"));
		sb.append(String.format("import org.junit.*;%n"));
		sb.append(String.format("import static org.junit.Assert.*;%n%n"));
		sb.append(String.format("public class Test%s {%n", getBaseName()));
		sb.append(String.format("%c@Test(timeout = 4000)%n",'\t'));
		sb.append(String.format("%cpublic void test() throws Throwable {%n",'\t'));
		sb.append(String.format("%c%c// test here!%n",'\t','\t'));
		sb.append(String.format("%c}%n",'\t'));
		sb.append(String.format("}"));
		return sb.toString();
	}

	public static boolean existUniqueClassID(String classID) {
		Connection conn = null;
		Statement stmt = null;
		String sql = String.format("SELECT * FROM classes WHERE Name = '%s';", classID);
		try {
			conn = DatabaseAccess.getConnection();
			stmt = conn.createStatement();
			stmt.execute(sql);
			boolean exist = stmt.getResultSet().next();
			stmt.close();
			conn.close();
			return exist;
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
		return false;
	}
}