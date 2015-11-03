package gammut;

import java.io.*;
import javax.servlet.*;
import java.sql.*;

public class Test {

	private int id;
	private int gameId;
	private String javaFile;
	private String classFile;

	boolean validTest = true;

	private int roundCreated;
	private int mutantsKilled = 0;

	public Test(int gid, String jFile, String cFile) {

		this.gameId = gid;
		this.roundCreated = GameManager.getCurrentRoundForGame(gid);
		this.javaFile = jFile;
		this.classFile = cFile;
	}

	public Test(int tid, int gid, String jFile, String cFile, int roundCreated, int mutantsKilled) {
		this(gid, jFile, cFile);

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
        Statement stmt = null;
        String sql = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            stmt = conn.createStatement();
            sql = String.format("INSERT INTO tests (JavaFile, ClassFile, Game_ID, RoundCreated) VALUES ('%s', '%s', %d, %d);", DatabaseAccess.addSlashes(javaFile), DatabaseAccess.addSlashes(classFile), gameId, roundCreated);
            stmt.execute(sql);

        	conn.close();
        	stmt.close();
        	return true;
        }
        catch(SQLException se) {System.out.println(se); } // Handle errors for JDBC
        catch(Exception e) {System.out.println(e); } // Handle errors for Class.forName
        finally {
            try { if (stmt!=null) {stmt.close();} } catch(SQLException se2) {} // Nothing we can do
            try { if(conn!=null) {conn.close();} } catch(SQLException se) { System.out.println(se); }
        }
        return false;
	}
}