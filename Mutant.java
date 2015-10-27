package gammut;

import java.io.File;
import java.util.*;
import java.io.*;
import diff_match_patch.*;
import java.sql.*;

public class Mutant {

	private int id;
	private int gameId;

	private byte[] javaFile;
	private byte[] classFile;

	private boolean alive = true;
	private boolean equivalent = false;
	private boolean suspectedEquivalent = false;
	private boolean declaredEquivalent = false;

	private int roundCreated;
	private int roundKilled;

	// MUTANT CREATION 01: FROM USER
	// Constructor to create a Mutant from a user created Java File and the compiled Class File.
	// This is creating a new mutant.
	public Mutant(int gid, InputStream jStream, InputStream cStream) {
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

	// MUTANT CREATION 02: FROM DATABASE
	// Constructor to create a Mutant from a MySQL Record in the mutants table.
	// This is getting information for an existing mutant.
	public Mutant(int mid, int gid, InputStream jStream, InputStream cStream, boolean alive, boolean sEquiv, boolean dEquiv, int rCreated, int rKilled) {
		this(mid, jStream, cStream);

		this.id = mid;
		this.alive = alive;
		this.suspectedEquivalent = sEquiv;
		this.declaredEquivalent = dEquiv;
		this.roundCreated = rCreated;
		this.roundKilled = rKilled;
	}

	public void setEquivalent(boolean e) {equivalent = e;}
	public boolean isEquivalent() {return equivalent;}

	public boolean isAlive() {return alive;}

	public void kill() {
		alive = false;
		roundKilled = GameManager.getCurrentRoundForGame(gameId) - roundCreated;
	}

	public int getPoints() {
		int points = 0;
		if (declaredEquivalent) {points = 0; return points;}

		if (alive) {
			points = GameManager.getCurrentRoundForGame(gameId) - roundCreated;
			return points;
		}
		else {
			points = roundKilled - roundCreated;
			return points;
		}
	}

	public ArrayList<diff_match_patch.Diff> getDifferences() {

		int classId = GameManager.getClassForGame(gameId);
		byte[] sourceFile = GameManager.getJavaFileForClass(classId);
        String sourceCode = new String(sourceFile);
        String mutantText = new String(javaFile);

        // Runs diff match patch between the two Strings to see if there are any differences.
        diff_match_patch dmp = new diff_match_patch();
        ArrayList<diff_match_patch.Diff> changes = new ArrayList<diff_match_patch.Diff>();

        LinkedList<diff_match_patch.Diff> diffs = dmp.diff_main(sourceCode.trim().replace("\n", "").replace("\r", ""), mutantText.trim().replace("\n", "").replace("\r", ""), true);
        boolean noChange = true;
        for (diff_match_patch.Diff d : diffs) {
            if (d.operation != diff_match_patch.Operation.EQUAL) {
            	changes.add(d);
                noChange = false;
            }
        }
        return changes;
	}

	public String getHTMLReadout() {
		String html = "";

        for (diff_match_patch.Diff d : getDifferences()) {
            if (d.operation == diff_match_patch.Operation.INSERT) {
            		html += "<p> +: " + d.text;
            }
            else {
            	html += "<p> -: " + d.text;
            }
        }
        html += "<br>";
        return html;
	}

	public boolean insert() {

		Connection conn = null;
        PreparedStatement pstmt = null;
        String sql = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            pstmt = conn.prepareStatement("INSERT INTO mutants (JavaFile, ClassFile, Game_ID) VALUES (?, ?, ?);");
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