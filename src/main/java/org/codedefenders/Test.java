package org.codedefenders;

import javassist.ClassPool;
import javassist.CtClass;
import org.codedefenders.multiplayer.LineCoverage;
import org.codedefenders.multiplayer.LineCovered;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

public class Test {

	private int id;
	private int gameId;
	private String javaFile;
	private String classFile;

	private int roundCreated;
	private int mutantsKilled = 0;

	private int ownerId;
	private int defenderId = -1;

	private LineCoverage lineCoverage = LineCoverage.NONE;

	public void setLineCoverage(LineCoverage lc){
		lineCoverage = lc;
	}

	public LineCoverage getLineCoverage(){
		return lineCoverage;
	}

	public void setDefenderId(int defId){
		defenderId = defId;
	}



	public int getDefenderId(){
		return defenderId;
	}

	public Test(int gameId, String jFile, String cFile, int ownerId) {
		this.gameId = gameId;
		this.roundCreated = DatabaseAccess.getGameForKey("Game_ID", gameId).getCurrentRound();
		this.javaFile = jFile;
		this.classFile = cFile;
		this.ownerId = ownerId;
	}

	public Test(int tid, int gid, String jFile, String cFile, int roundCreated, int mutantsKilled, int ownerId) {
		this(gid, jFile, cFile, ownerId);

		this.id = tid;
		this.roundCreated = roundCreated;
		this.mutantsKilled = mutantsKilled;
	}

	public int getId() {
		return id;
	}

	public int getGameId() {
		return gameId;
	}

	public int getAttackerPoints() {
		return 0;
	}

	public int getDefenderPoints() {
		if (ownerId == DatabaseAccess.getGameForKey("Game_ID", gameId).getDefenderId())
			return mutantsKilled;
		else
			return 0;
	}

	public String getFolder() {
		int lio = javaFile.lastIndexOf("/");
		if (lio == -1) {
			lio = javaFile.lastIndexOf("\\");
		}
		return javaFile.substring(0, lio);
	}

	public void killMutant() {
		mutantsKilled++;
		update();
	}

	public List<String> getHTMLReadout() throws IOException {

		File testFile = new File(javaFile);
		List<String> testLines = new LinkedList<String>();

		String line = "";

		try {
			BufferedReader in = new BufferedReader(new FileReader(testFile));
			while ((line = in.readLine()) != null) {
				testLines.add(line);
			}
		} catch (IOException e) {
			System.err.println(e.getLocalizedMessage());
		}

		return testLines;
	}

	public boolean insert() {

		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {
			conn = DatabaseAccess.getConnection();

			stmt = conn.createStatement();
			String jFileDB = "'" + DatabaseAccess.addSlashes(javaFile) + "'";
			// class file can be null
			String cFileDB = classFile == null ? null : "'" + DatabaseAccess.addSlashes(classFile) + "'";
			if (defenderId >= 0){
				sql = String.format("INSERT INTO tests (JavaFile, ClassFile, Game_ID, RoundCreated, Owner_ID, Defender_ID) " +
						"VALUES (%s, %s, %d, %d, %d);", jFileDB, cFileDB, gameId, roundCreated, ownerId, defenderId);
			} else {
				sql = String.format("INSERT INTO tests (JavaFile, ClassFile, Game_ID, RoundCreated, Owner_ID) " +
						"VALUES (%s, %s, %d, %d, %d);", jFileDB, cFileDB, gameId, roundCreated, ownerId);
			}

			stmt.execute(sql, Statement.RETURN_GENERATED_KEYS);

			ResultSet rs = stmt.getGeneratedKeys();

			if (rs.next()) {
				this.id = rs.getInt(1);
				stmt.close();
				conn.close();
				return true;
			}
		} catch (SQLException se) {
			System.out.println(se);
		} // Handle errors for JDBC
		catch (Exception e) {
			System.out.println(e);
		} // Handle errors for Class.forName
		finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException se2) {
			} // Nothing we can do
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException se) {
				System.out.println(se);
			}
		}
		return false;
	}

	public boolean update() {

		System.out.println("Updating Test");
		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {
			conn = DatabaseAccess.getConnection();

			stmt = conn.createStatement();

			String linesCoveredString = "";
			String linesUncoveredString = "";
			if (lineCoverage != null) {
				for (int i : lineCoverage.getLinesCovered()) {
					linesCoveredString += i + ",";
				}

				for (int i : lineCoverage.getLinesUncovered()) {
					linesUncoveredString += i + ",";
				}
				if (linesCoveredString.length() > 0) {
					linesCoveredString = linesCoveredString.substring(0, linesCoveredString.length() - 1);
				}

				if (linesUncoveredString.length() > 0) {
					linesUncoveredString = linesUncoveredString.substring(0, linesUncoveredString.length() - 1);
				}
			}

			//-1 for the left over comma

			if (defenderId >= 0){
				sql = String.format("UPDATE tests SET mutantsKilled='%d', " +
						"Defender_ID=%d, " +
						"Lines_Covered='%s', " +
						"Lines_Uncovered='%s' " +
						"WHERE Test_ID='%d';",
						mutantsKilled, defenderId, linesCoveredString, linesUncoveredString, id);
			} else {
				sql = String.format("UPDATE tests SET mutantsKilled='%d', " +
						"Lines_Covered='%s', " +
						"Lines_Uncovered='%s' " +
						"WHERE Test_ID='%d';",
						mutantsKilled, linesCoveredString, linesUncoveredString, id);
			}
			stmt.execute(sql);

			conn.close();
			stmt.close();
			return true;
		} catch (SQLException se) {
			System.out.println(se);
		} // Handle errors for JDBC
		catch (Exception e) {
			System.out.println(e);
		} // Handle errors for Class.forName
		finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException se2) {
			} // Nothing we can do
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException se) {
				System.out.println(se);
			}
		}
		return false;
	}

	public String getFullyQualifiedClassName() {
		if (classFile == null)
			return null;

		ClassPool classPool = ClassPool.getDefault();
		CtClass cc = null;
		try {
			cc = classPool.makeClass(new FileInputStream(new File(classFile)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cc == null ? null : cc.getName();
	}

	public boolean isValid() {
		return classFile != null;
	}
}
