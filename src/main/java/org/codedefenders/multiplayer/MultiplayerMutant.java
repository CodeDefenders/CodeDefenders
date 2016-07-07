package org.codedefenders.multiplayer;

import difflib.Chunk;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import org.codedefenders.DatabaseAccess;
import org.codedefenders.GameClass;
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

import static org.codedefenders.Mutant.Equivalence;

public class MultiplayerMutant {

	private static final Logger logger = LoggerFactory.getLogger(MultiplayerMutant.class);

	private int id;
	private int gameId;

	private String javaFile;
	private String classFile;

	private boolean alive = true;

	private Equivalence equivalent;

	private int playerId;

	private int score;

	/**
	 * Creates a mutant
	 * @param gameId
	 * @param jFile
	 * @param cFile
	 * @param alive
	 * @param playerId
	 */
	public MultiplayerMutant(int mutantId, int gameId, String jFile, String cFile, String equivalent, boolean alive, int playerId) {
		id = mutantId;
		this.gameId = gameId;
		this.javaFile = jFile;
		this.classFile = cFile;
		this.alive = alive;
		this.equivalent = Equivalence.valueOf(equivalent);
		this.playerId = playerId;

		score = 0;
	}


	public int getPlayerId(){
		return playerId;
	}

	public int getScore(){
		return score;
	}

	public void setScore(int score){
		this.score += score;
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

	public void kill() {
		alive = false;
		equivalent = Equivalence.PROVEN_NO;
		update();
	}

	public Patch getDifferences() {

		int classId = DatabaseAccess.getMultiplayerGame(gameId).getClassId();
		GameClass sut = DatabaseAccess.getClassForKey("Class_ID", classId);

		File sourceFile = new File(sut.javaFile);
		File mutantFile = new File(javaFile);

		List<String> sutLines = readLinesIfFileExist(sourceFile.toPath());
		List<String> mutantLines = readLinesIfFileExist(mutantFile.toPath());

		return DiffUtils.diff(sutLines, mutantLines);
	}

	public String getPatchString() {
		int classId = DatabaseAccess.getMultiplayerGame(gameId).getClassId();
		GameClass sut = DatabaseAccess.getClassForKey("Class_ID", classId);

		File sourceFile = new File(sut.javaFile);
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
			String sql = String.format("INSERT INTO mutants (JavaFile, ClassFile, Game_ID, RoundCreated, Alive, Player_ID, Points)" +
					" VALUES (%s, %s, %d, %d, %d, %d, %d);", jFileDB, cFileDB, gameId, -1, sqlAlive(), playerId, score);

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
			String sql = String.format("UPDATE mutants SET Equivalent='%s', Alive='%d', Points=%d WHERE Mutant_ID='%d';",
					equivalent.name(), sqlAlive(), score, id);
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
