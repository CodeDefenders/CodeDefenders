package gammut;

import java.io.*;
import javax.servlet.*;
import java.sql.*;

public class Test {

	private int id;
	private int gameId;
	private String javaFile;
	private String classFile;

	private int roundCreated;
	private int mutantsKilled = 0;

	public Test(int gid, String jFile, String cFile) {

		this.gameId = gid;
		System.out.println("Test Constructor");
		this.roundCreated = DatabaseAccess.getGameForKey("Game_ID", gid).getCurrentRound();
		this.javaFile = jFile;
		this.classFile = cFile;
	}

	public Test(int tid, int gid, String jFile, String cFile, int roundCreated, int mutantsKilled) {
		this(gid, jFile, cFile);

		this.id = tid;
		this.roundCreated = roundCreated;
		this.mutantsKilled = mutantsKilled;
	}

    public int getId() {return id;}
    public int getGameId() {return gameId;}

	public int getPoints() {return mutantsKilled;}

	public String getFolder() {
		int lio = javaFile.lastIndexOf("/");
        if (lio == -1) {lio = javaFile.lastIndexOf("\\");}
		return javaFile.substring(0, lio);
	}

	public void killMutant() {mutantsKilled++;}

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
            
            stmt.execute(sql, Statement.RETURN_GENERATED_KEYS);

            ResultSet rs = stmt.getGeneratedKeys();

            if (rs.next()) {
                this.id = rs.getInt(1);
                stmt.close();
                conn.close();
                return true;
            }
        }
        catch(SQLException se) {System.out.println(se); } // Handle errors for JDBC
        catch(Exception e) {System.out.println(e); } // Handle errors for Class.forName
        finally {
            try { if (stmt!=null) {stmt.close();} } catch(SQLException se2) {} // Nothing we can do
            try { if(conn!=null) {conn.close();} } catch(SQLException se) { System.out.println(se); }
        }
        return false;
	}

	public boolean update() {

        System.out.println("Updating Test");
		Connection conn = null;
        Statement stmt = null;
        String sql = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            stmt = conn.createStatement();
            sql = String.format("UPDATE tests SET mutantsKilled='%d' WHERE Test_ID='%d';", mutantsKilled, id);
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