package gammut;

import java.io.*;
import javax.servlet.*;
import java.sql.*;

public class Test {

	private int id;
	private int gameId;
	private byte[] javaFile;
	private byte[] classFile;

	File folder;
	String className;
	String text;

	boolean validTest = true;

	private int points = 0;

	public Test(File folder, String className) {
		this.folder = folder;
		this.className = className;
	}

	public Test(int gid, InputStream jStream, InputStream cStream) {
		this.gameId = gid;

		try {

			int nRead;

			ByteArrayOutputStream jBuffer = new ByteArrayOutputStream();

			while ((nRead = jStream.read()) != -1) {
				jBuffer.write(nRead);
			}

			jBuffer.flush();
			javaFile = jBuffer.toByteArray();

			ByteArrayOutputStream cBuffer = new ByteArrayOutputStream();

			while ((nRead = cStream.read()) != -1) {
				cBuffer.write(nRead);
			}

			cBuffer.flush();
			classFile = cBuffer.toByteArray();

		} 
		catch (IOException e) {System.out.println(e);}
	}

	public Test(int tid, int gid, InputStream jStream, InputStream cStream, int points) {
		this(gid, jStream, cStream);

		this.id = tid;
		this.points = points;
	}


	public String getFolder() {
		return folder.getAbsolutePath();
	}

	public String getJava() {
		return folder.getAbsolutePath() + "Test" + className + ".java";
	}

	public String getClassFile() {
		return folder.getAbsolutePath() + "Test" + className + ".class";
	}

	public void setText(String t) {text = t;}
	public String getText() {return text;}

	public void scorePoints(int p) {points += p;}
	public int getPoints() {return points;}

	public void setValidTest(boolean b) {validTest = b;}
	public boolean isValidTest() {return validTest;}

	public String getHTMLReadout() throws IOException {

        return "<code>" + getText().replace("\n", "<br>") + "</code>";
	}

	public boolean insert() {

		Connection conn = null;
        PreparedStatement pstmt = null;
        String sql = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            pstmt = conn.prepareStatement("INSERT INTO tests (JavaFile, ClassFile, Game_ID) VALUES (?, ?, ?);");
        	pstmt.setBinaryStream(1, new ByteArrayInputStream(javaFile));
        	pstmt.setBinaryStream(2, new ByteArrayInputStream(classFile));
        	pstmt.setInt(3, gameId);

        	System.out.println("Executing statement: " + pstmt);
        	pstmt.execute();

        	conn.close();
        	pstmt.close();
        	return true;
        }
        catch(SQLException se) {System.out.println(se); } // Handle errors for JDBC
        catch(Exception e) {System.out.println(e); } // Handle errors for Class.forName
        finally {
            try { if (pstmt!=null) {pstmt.close();} } catch(SQLException se2) {} // Nothing we can do
            try { if(conn!=null) {conn.close();} } catch(SQLException se) { System.out.println(se); }
        }
        return false;
	}
}