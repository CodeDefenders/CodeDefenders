package org.codedefenders;

import static org.codedefenders.Mutant.Equivalence.ASSUMED_YES;
import static org.codedefenders.Mutant.Equivalence.DECLARED_YES;
import static org.codedefenders.Mutant.Equivalence.PROVEN_NO;

import difflib.Chunk;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Mutant {

	private static final Logger logger = LoggerFactory.getLogger(Mutant.class);

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

	private int ownerId;

	/**
	 * Creates a mutant
	 * @param gameId
	 * @param jFile
	 * @param cFile
	 * @param alive
	 * @param ownerId
	 */
	public Mutant(int gameId, String jFile, String cFile, boolean alive, int ownerId) {
		this.gameId = gameId;
		this.roundCreated = DatabaseAccess.getGameForKey("Game_ID", gameId).getCurrentRound();
		this.javaFile = jFile;
		this.classFile = cFile;
		this.alive = alive;
		this.equivalent = Equivalence.ASSUMED_NO;
		this.ownerId = ownerId;
	}

	/**
	 * Creates a mutant
	 * @param mid
	 * @param gid
	 * @param jFile
	 * @param cFile
	 * @param alive
	 * @param equiv
	 * @param rCreated
	 * @param rKilled
	 * @param ownerId
	 */
	public Mutant(int mid, int gid, String jFile, String cFile, boolean alive, Equivalence equiv, int rCreated, int rKilled, int ownerId) {
		this(gid, jFile, cFile, alive, ownerId);
		this.id = mid;
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

	public String getSourceFile() {
		return javaFile;
	}

	public String getClassFile() {
		return classFile;
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

	public int getOwnerId() {
		return ownerId;
	}

	public void kill() {
		alive = false;
		roundKilled = DatabaseAccess.getGameForKey("Game_ID", gameId).getCurrentRound();
		update();
	}

	public int getAttackerPoints() {
		if (alive) {
			// if mutant is alive, as many points as rounds it has survived
			// TODO: as many points as tests it has survived?
			int points = DatabaseAccess.getGameForKey("Game_ID", gameId).getCurrentRound() - roundCreated; // rounds survived
			logger.info("Alive mutant " + getId() + " contributes " + points + " attacker points");
			return points;
		} else {
			if (classFile == null || classFile.equals("null")) // non-compilable
				return 0;
			if (equivalent.equals(DECLARED_YES)) // accepted equivalent
				return 0;
			if (equivalent.equals(ASSUMED_YES)) // claimed, rejected, test did not kill it
				return 0;
			if (equivalent.equals(PROVEN_NO)) { // claimed, rejected, test killed it
				logger.info("Claimed/rejected/killed mutant " + getId() + " contributes 2 attacker points");
				return 2;
			}
			int points = roundKilled - roundCreated; // rounds survived
			logger.info("Killed mutant " + getId() + " contributes " + points + " attacker points");
			return points;
		}
	}

	public int getDefenderPoints() {
		if (! alive) {
			if (classFile == null) // non-compilable
				return 0;
			if (equivalent.equals(DECLARED_YES)) // accepted equivalent
				return 1;
			if (equivalent.equals(ASSUMED_YES)) // claimed, rejected, test did not kill it
				return 2;
			if (equivalent.equals(PROVEN_NO)) // claimed, rejected, test killed it
				return 0;
			return 0;
		}
		return 0;
	}

	public Patch getDifferences() {

		int classId = DatabaseAccess.getGameForKey("Game_ID", gameId).getClassId();
		GameClass sut = DatabaseAccess.getClassForKey("Class_ID", classId);

		File sourceFile = new File(sut.getJavaFile());
		File mutantFile = new File(javaFile);

		List<String> sutLines = readLinesIfFileExist(sourceFile.toPath());
		List<String> mutantLines = readLinesIfFileExist(mutantFile.toPath());

		return DiffUtils.diff(sutLines, mutantLines);
	}

	public String getPatchString() {
		int classId = DatabaseAccess.getGameForKey("Game_ID", gameId).getClassId();
		GameClass sut = DatabaseAccess.getClassForKey("Class_ID", classId);

		File sourceFile = new File(sut.getJavaFile());
		File mutantFile = new File(javaFile);

		List<String> sutLines = readLinesIfFileExist(sourceFile.toPath());
		List<String> mutantLines = readLinesIfFileExist(mutantFile.toPath());

		Patch patch = DiffUtils.diff(sutLines, mutantLines);
		List<String> unifiedPatches = DiffUtils.generateUnifiedDiff(null, null, sutLines, patch, 3);
		StringBuilder unifiedPatch = new StringBuilder();
		for (String s : unifiedPatches) {
			unifiedPatch.append(s + System.getProperty("line.separator"));
		}
		return unifiedPatch.toString();
	}

	private List<String> readLinesIfFileExist(Path path) {
		List<String> lines = new ArrayList<>();
		try {
			if (Files.exists(path))
				lines = Files.readAllLines(path, StandardCharsets.UTF_8);
			else
				logger.error("File not found {}", path);
		} catch (IOException e) {
			e.printStackTrace();  // TODO handle properly
		} finally {
			return lines;
		}
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
	// Currently Mutant ID isnt set yet after insertion, if Mutant needs to be used straight away it needs a similar insert method to MultiplayerGame.
	public boolean insert() {

		Connection conn = null;
		Statement stmt = null;

		try {
			logger.info("Inserting mutant");

			conn = DatabaseAccess.getConnection();

			stmt = conn.createStatement();
			String jFileDB = "'" + DatabaseAccess.addSlashes(javaFile) + "'";
			String cFileDB = classFile == null ? null : "'" + DatabaseAccess.addSlashes(classFile) + "'";
			String sql = String.format("INSERT INTO mutants (JavaFile, ClassFile, Game_ID, RoundCreated, Alive, Player_ID)" +
					" VALUES (%s, %s, %d, %d, %d, %d);", jFileDB, cFileDB, gameId, roundCreated, sqlAlive(), ownerId);

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
			logger.error(se.getMessage());
			System.out.println(se);
		} // Handle errors for JDBC
		catch (Exception e) {
			logger.error(e.getMessage());
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
				logger.error(se.getMessage());
				System.out.println(se);
			}
		}
		return false;
	}

	// update will run when changes to a mutant are made.
	// Updates values of Equivalent, Alive, RoundKilled.
	// These values update when Mutants are suspected of being equivalent, go through an equivalence test, or are killed.
	public boolean update() {

		logger.info("Updating Mutant");

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
			logger.error(se.getMessage());
			System.out.println(se);
		} // Handle errors for JDBC
		catch (Exception e) {
			logger.error(e.getMessage());
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
