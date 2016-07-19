package org.codedefenders;

import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.crypto.Data;
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

	private int id;
	private String name; // fully qualified name
	private String alias;
	private String javaFile;
	private String classFile;

	public GameClass(String name, String alias, String jFile, String cFile) {
		this.name = name;
		this.alias = alias;
		this.javaFile = jFile;
		this.classFile = cFile;
	}

	public GameClass(int id, String name, String alias, String jFile, String cFile) {
		this(name, alias, jFile, cFile);
		this.id = id;
	}

	public int getId() {
		return id;
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

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
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

		logger.debug("Inserting class (Name={}, Alias={}, JavaFile={}, ClassFile={})", name, alias, javaFile, classFile);
		Connection conn = null;
		Statement stmt = null;
		String sql = String.format("INSERT INTO classes (Name, Alias, JavaFile, ClassFile) VALUES ('%s', '%s', '%s', '%s');", name, alias, javaFile, classFile);

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
			DatabaseAccess.cleanup(conn, stmt);
		} //end try

		return false;
	}

	public boolean update() {

		logger.debug("Updating class (Name={}, Alias={}, JavaFile={}, ClassFile={})", name, alias, javaFile, classFile);
		Connection conn = null;
		Statement stmt = null;

		String sql = String.format("UPDATE classes SET Name='%s', Alias='%s', JavaFile='%s', ClassFile='%s' WHERE Class_ID='%d';", name, alias, javaFile, classFile, id);

		// Attempt to update game info into database
		try {
			conn = DatabaseAccess.getConnection();
			stmt = conn.createStatement();
			stmt.execute(sql);
			stmt.close();
			conn.close();
			return true;
		} catch (SQLException se) {
			System.out.println(se);
			//Handle errors for JDBC
		} catch (Exception e) {
			System.out.println(e);
			//Handle errors for Class.forName
		} finally {
			DatabaseAccess.cleanup(conn, stmt);
		} //end try
		return false;
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

	public String getJavaFile() {
		return javaFile;
	}

	public void setJavaFile(String javaFile) {
		this.javaFile = javaFile;
	}

	public String getClassFile() {
		return classFile;
	}

	public void setClassFile(String classFile) {
		this.classFile = classFile;
	}
}