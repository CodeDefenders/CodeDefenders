package org.gammut;

import difflib.Chunk;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import static org.gammut.Constants.Equivalence.*;

public class Mutant {

	private int id;
	private int gameId;

	private String javaFile;
	private String classFile;

	private boolean alive = true;

	private String equivalent;

	private int roundCreated;
	private int roundKilled;

	// MUTANT CREATION 01: FROM USER
	// Constructor to create a Mutant from a user created Java File and the compiled Class File.
	// This is creating a new mutant.
	public Mutant(int gid, String jFile, String cFile) {
		this.gameId = gid;
		this.roundCreated = DatabaseAccess.getGameForKey("Game_ID", gid).getCurrentRound();
		this.javaFile = jFile;
		this.classFile = cFile;
	}

	// MUTANT CREATION 02: FROM DATABASE
	// Constructor to create a Mutant from a MySQL Record in the mutants table.
	// This is getting information for an existing mutant.
	public Mutant(int mid, int gid, String jFile, String cFile, boolean alive, String equiv, int rCreated, int rKilled) {
		this(gid, jFile, cFile);

		this.id = mid;
		this.alive = alive;
		this.equivalent = equiv;
		this.roundCreated = rCreated;
		this.roundKilled = rKilled;
	}

	public int getId() {
		return id;
	}

	public int getGameId() {
		return gameId;
	}

	public String getEquivalent() {
		return equivalent;
	}

	public void setEquivalent(String e) {
		equivalent = e;
	}

	public String getFolder() {
		int lio = javaFile.lastIndexOf("/");
		if (lio == -1) {
			lio = javaFile.lastIndexOf("\\");
		}
		return javaFile.substring(0, lio);
	}

	public boolean isAlive() {
		return alive;
	}

	public int sqlAlive() {
		if (alive) {
			return 1;
		} else {
			return 0;
		}
	}

	public void kill() {
		alive = false;
		roundKilled = DatabaseAccess.getGameForKey("Game_ID", gameId).getCurrentRound();
	}

	public int getPoints() {
		int points = 0;
		if (equivalent.equals(DECLARED_YES.name())) {
			points = 0;
			return points;
		}
		if (equivalent.equals(ASSUMED_YES.name())) {
			points = -1;
			return points;
		}
		if (equivalent.equals(PROVEN_NO.name())) {
			points += 2;
		}

		if (alive) {
			points = DatabaseAccess.getGameForKey("Game_ID", gameId).getCurrentRound() - roundCreated;
			return points;
		} else {
			points = roundKilled - roundCreated;
			return points;
		}
	}

	public Patch getDifferences() throws IOException {

		int classId = DatabaseAccess.getGameForKey("Game_ID", gameId).getClassId();
		File sourceFile = new File(DatabaseAccess.getClassForKey("Class_ID", classId).javaFile);
		File mutantFile = new File(javaFile);

		List<String> sourceLines = new LinkedList<String>();
		List<String> mutantLines = new LinkedList<String>();

		String line = "";

		try {
			BufferedReader in = new BufferedReader(new FileReader(sourceFile));
			while ((line = in.readLine()) != null) {
				sourceLines.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			BufferedReader in = new BufferedReader(new FileReader(mutantFile));
			while ((line = in.readLine()) != null) {
				mutantLines.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		Patch patch = DiffUtils.diff(sourceLines, mutantLines);

		for (Delta delta : patch.getDeltas()) {
			System.out.println(delta);
		}


		return patch;
	}

	public List<String> getHTMLReadout() throws IOException {

		Patch p = getDifferences();
		Chunk c;
		Delta.TYPE t;
		int pos;

		List<String> htmlMessages = new LinkedList<String>();

		for (Delta d : p.getDeltas()) {
			c = d.getOriginal();
			t = d.getType();
			// position starts at 0 but code readout starts at 1
			pos = c.getPosition() + 1;
			if (t == Delta.TYPE.CHANGE) {
				htmlMessages.add("Made a change to Line: " + pos + "\n");
			} else if (t == Delta.TYPE.DELETE) {
				htmlMessages.add("Removed Line: " + pos + "\n");
			} else {
				htmlMessages.add("Added Line: " + pos + "\n");
			}

		}

		return htmlMessages;
	}

	// insert will run once after mutant creation.
	// Stores values of JavaFile, ClassFile, GameID, RoundCreated in DB. These will not change once input.
	// Default values for Equivalent (ASSUMED_NO), Alive(1), RoundKilled(NULL) are assigned.
	// Currently Mutant ID isnt set yet after insertion, if Mutant needs to be used straight away it needs a similar insert method to Game.
	public boolean insert() {

		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {
			System.out.println("Inserting mutant");

			conn = DatabaseAccess.getConnection();

			stmt = conn.createStatement();
			sql = String.format("INSERT INTO mutants (JavaFile, ClassFile, Game_ID, RoundCreated) VALUES ('%s', '%s', %d, %d);", DatabaseAccess.addSlashes(javaFile), DatabaseAccess.addSlashes(classFile), gameId, roundCreated);

			stmt.execute(sql, Statement.RETURN_GENERATED_KEYS);

			ResultSet rs = stmt.getGeneratedKeys();

			if (rs.next()) {
				this.id = rs.getInt(1);
				System.out.println("setting mutant ID to: " + this.id);
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

	// update will run when changes to a mutant are made.
	// Updates values of Equivalent, Alive, RoundKilled.
	// These values update when Mutants are suspected of being equivalent, go through an equivalence test, or are killed.
	public boolean update() {

		System.out.println("Updating Mutant");

		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {
			conn = DatabaseAccess.getConnection();

			stmt = conn.createStatement();
			sql = String.format("UPDATE mutants SET Equivalent='%s', Alive='%d', RoundKilled='%d' WHERE Mutant_ID='%d';", equivalent, sqlAlive(), roundKilled, id);
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
}
