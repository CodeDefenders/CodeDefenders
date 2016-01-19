package org.codedefenders;

import static org.codedefenders.Mutant.Equivalence.ASSUMED_YES;
import static org.codedefenders.Mutant.Equivalence.DECLARED_YES;
import static org.codedefenders.Mutant.Equivalence.PROVEN_NO;

import difflib.Chunk;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Mutant {

	private int id;
	private int gameId;

	private String javaFile;
	private String classFile;

	private boolean alive = true;

	private Equivalence equivalent;

	/* Mutant Equivalence */
	public enum Equivalence { ASSUMED_NO, PENDING_TEST, DECLARED_YES, ASSUMED_YES, PROVEN_NO}

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
		this.equivalent = Equivalence.ASSUMED_NO;
	}

	// MUTANT CREATION 02: FROM DATABASE
	// Constructor to create a Mutant from a MySQL Record in the mutants table.
	// This is getting information for an existing mutant.
	public Mutant(int mid, int gid, String jFile, String cFile, boolean alive, Equivalence equiv, int rCreated, int rKilled) {
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

	public Equivalence getEquivalent() {
		return equivalent;
	}

	public void setEquivalent(Equivalence e) {
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
		return alive ? 1 : 0;
	}

	public void kill() {
		alive = false;
		roundKilled = DatabaseAccess.getGameForKey("Game_ID", gameId).getCurrentRound();
		update();
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

	public Patch getDifferences() {

		int classId = DatabaseAccess.getGameForKey("Game_ID", gameId).getClassId();
		GameClass sut = DatabaseAccess.getClassForKey("Class_ID", classId);

		File sourceFile = new File(sut.javaFile);
		File mutantFile = new File(javaFile);

		List<String> sutLines = new ArrayList<>();
		List<String> mutantLines = new ArrayList<>();
		try {
			sutLines = Files.readAllLines(sourceFile.toPath(), StandardCharsets.UTF_8);
			mutantLines = Files.readAllLines(mutantFile.toPath(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace(); // TODO handle properly
		}
		return DiffUtils.diff(sutLines, mutantLines);
	}

	public String getPatchString() {
		int classId = DatabaseAccess.getGameForKey("Game_ID", gameId).getClassId();
		GameClass sut = DatabaseAccess.getClassForKey("Class_ID", classId);

		File sourceFile = new File(sut.javaFile);
		File mutantFile = new File(javaFile);

		List<String> sutLines = new ArrayList<>();
		List<String> mutantLines = new ArrayList<>();
		try {
			sutLines = Files.readAllLines(sourceFile.toPath(), StandardCharsets.UTF_8);
			mutantLines = Files.readAllLines(mutantFile.toPath(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();  // TODO handle properly
		}
		Patch patch = DiffUtils.diff(sutLines, mutantLines);
		List<String> unifiedPatches = DiffUtils.generateUnifiedDiff(null, null, sutLines, patch, 3);
		StringBuilder unifiedPatch = new StringBuilder();
		for (String s : unifiedPatches) {
			unifiedPatch.append(s + System.getProperty("line.separator"));
		}
		return unifiedPatch.toString();
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

		try {
			System.out.println("Inserting mutant");

			conn = DatabaseAccess.getConnection();

			stmt = conn.createStatement();
			String jFileDB = DatabaseAccess.addSlashes(javaFile);
			String cFileDB = classFile == null ? null : DatabaseAccess.addSlashes(classFile);
			String sql = String.format("INSERT INTO mutants (JavaFile, ClassFile, Game_ID, RoundCreated) VALUES ('%s', '%s', %d, %d);", jFileDB, cFileDB, gameId, roundCreated);

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

		try {
			conn = DatabaseAccess.getConnection();

			stmt = conn.createStatement();
			String sql = String.format("UPDATE mutants SET Equivalent='%s', Alive='%d', RoundKilled='%d' WHERE Mutant_ID='%d';", equivalent.name(), sqlAlive(), roundKilled, id);
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
