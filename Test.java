package gammut;

import java.io.*;
import javax.servlet.*;
import java.sql.*;

public class Test {

	private int id;
	private int gameId;
	private byte[] javaFile;
	private byte[] classFile;

	boolean validTest = true;

	private int roundCreated;
	private int mutantsKilled = 0;

	public Test(int gid, InputStream jStream, InputStream cStream) {

		this.gameId = gid;
		this.roundCreated = GameManager.getCurrentRoundForGame(gid);

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

	public Test(int tid, int gid, InputStream jStream, InputStream cStream, int roundCreated, int mutantsKilled) {
		this(gid, jStream, cStream);

		this.id = tid;
		this.roundCreated = roundCreated;
		this.mutantsKilled = mutantsKilled;
	}

	public int getPoints() {return mutantsKilled;}

	public void killMutant() {mutantsKilled++;}

	public void setValidTest(boolean b) {validTest = b;}
	public boolean isValidTest() {return validTest;}

	public String getHTMLReadout() throws IOException {

        return "<code>" + "</code>";
	}

	public boolean insert() {

		Connection conn = null;
        PreparedStatement pstmt = null;
        String sql = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            pstmt = conn.prepareStatement("INSERT INTO tests (JavaFile, ClassFile, Game_ID, RoundCreated) VALUES (?, ?, ?, ?);");
        	pstmt.setBinaryStream(1, new ByteArrayInputStream(javaFile));
        	pstmt.setBinaryStream(2, new ByteArrayInputStream(classFile));
        	pstmt.setInt(3, gameId);
        	pstmt.setInt(4, roundCreated);

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