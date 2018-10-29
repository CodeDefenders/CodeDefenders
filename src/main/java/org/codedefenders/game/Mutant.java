/**
 * Copyright (C) 2016-2018 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.game;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codedefenders.database.DB;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.DatabaseValue;
import org.codedefenders.game.duel.DuelGame;
import org.codedefenders.validation.code.CodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

import difflib.Chunk;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

/**
 * This class represents a mutation in a game class. These mutations are created
 * by attackers in order to survive test cases.
 *
 * @see GameClass
 * @see Test
 */
public class Mutant implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(Mutant.class);

	private int id;
	private int gameId;

	private transient String javaFile;
	private transient String md5;
	private transient String classFile;

    private String creatorName;
	private int creatorId;

	private boolean alive = true;

	private int killingTestId = 0;

	private Equivalence equivalent;

	/* Mutant Equivalence */
	public enum Equivalence {
		ASSUMED_NO, PENDING_TEST, DECLARED_YES, ASSUMED_YES, PROVEN_NO
	}

	private int roundCreated;
	private int roundKilled;

	private int playerId;

	private transient int killedByAITests = 0; //How many times this mutant is killed
	// by an AI test.

	private int score; // multiplayer

	/**
	 * Identifier of the class this mutant is created from.
	 * Of type {@link Integer}, because the classId can be {@code null}.
	 */
	private Integer classId;

	private List<Integer> lines = null;
	private transient List<String> description = null;
	private transient Patch difference = null;

    /**
     * Creates a new Mutant with following attributes:
     * <ul>
     * <li><code>gameId -1</code></li>
     * <li><code>playerId -1</code></li>
     * <li><code>roundCreated -1</code></li>
     * <li><code>score 0</code></li>
     * </ul>
     */
	public Mutant(String javaFilePath, String classFilePath, String md5, Integer classId) {
		this.javaFile = javaFilePath;
		this.classFile = classFilePath;
		this.alive = false;
		this.gameId = -1;
		this.playerId = -1;
		this.roundCreated = -1;
		this.score = 0;
		this.md5 = md5;
		this.classId = classId;
	}

	/**
	 * Creates a mutant
	 *
	 * @param gameId
	 * @param jFile
	 * @param cFile
	 * @param alive
	 * @param playerId
	 */
	public Mutant(int gameId, String jFile, String cFile, boolean alive, int playerId) {
		this.gameId = gameId;
		// FIXME: Why is this limited to a duel game?
		final DuelGame game = DatabaseAccess.getGameForKey("ID", gameId);
		if (game != null) {
			this.roundCreated = game.getCurrentRound();
		}
		this.javaFile = jFile;
		this.classFile = cFile;
		this.alive = alive;
		this.equivalent = Equivalence.ASSUMED_NO;
		this.playerId = playerId;
		this.md5 = CodeValidator.getMD5FromFile(jFile); // TODO: This may be null
	}

	public Mutant(int mid, int gid, String jFile, String cFile, boolean alive, Equivalence equiv, int rCreated, int rKilled, int playerId) {
		this(gid, jFile, cFile, alive, playerId);
		this.id = mid;
		this.equivalent = equiv;
		this.roundCreated = rCreated;
		this.roundKilled = rKilled;
		if(roundKilled > 0)
			this.killingTestId = DatabaseAccess.getKillingTestIdForMutant(id);

		score = 0;
	}

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(int creatorId) {
        this.creatorId = creatorId;
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
		if (e.equals(Equivalence.DECLARED_YES) || e.equals(Equivalence.ASSUMED_YES)) {
			score = 0;
		}
	}

	public String getSourceFile() {
		return javaFile;
	}

	public String getClassFile() {
		return classFile;
	}

	public String getJavaFile() {
		return javaFile;
	}

	public int getRoundCreated() {
		return roundCreated;
	}

	public String getMd5() {
		return md5;
	}

	public String getDirectory() {
		File file = new File(javaFile);
		return file.getAbsoluteFile().getParent();
	}

	public boolean isAlive() {
		return alive;
	}

	public int sqlAlive() {
		return alive ? 1 : 0;
	}

	public int getPlayerId() {
		return playerId;
	}

	public int getScore() {
		return score;
	}

	public Integer getClassId() {
		return classId;
	}

	public void incrementScore(int score){
		if( score == 0 ){
			logger.debug("Do not update mutant {} score by 0", getId());
			return;
		}

		String query = "UPDATE mutants SET Points = Points + ? WHERE Mutant_ID=? AND Alive=1;";
		Connection conn = DB.getConnection();

		DatabaseValue[] valueList = new DatabaseValue[]{
				DB.getDBV(score), DB.getDBV(id)
		};

		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
		DB.executeUpdate(stmt, conn);
	}

	@Deprecated
	public void setScore(int score) {
		this.score += score;
	}

	public boolean kill() {
		return kill(equivalent);
	}

	public boolean kill(Equivalence equivalent) {
		alive = false;
		roundKilled = DatabaseAccess.getGameForKey("ID", gameId).getCurrentRound();
		setEquivalent(equivalent);

		// This should be blocking
		Connection conn = DB.getConnection();

		String query;
		if (equivalent.equals(Equivalence.DECLARED_YES) || equivalent.equals(Equivalence.ASSUMED_YES)) {
			// if mutant is equivalent, we need to set score to 0
			query = "UPDATE mutants SET Equivalent=?, Alive=?, RoundKilled=?, Points=0 WHERE Mutant_ID=? AND Alive=1;";
		} else {
			// We cannot update killed mutants
			query = "UPDATE mutants SET Equivalent=?, Alive=?, RoundKilled=? WHERE Mutant_ID=? AND Alive=1;";
		}

		DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(equivalent.name()),
				DB.getDBV(sqlAlive()),
				DB.getDBV(roundKilled),
				DB.getDBV(id)};
		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
		//
		return DB.executeUpdate(stmt, conn);
	}

	public boolean isCovered() {
		List<Test> tests = DatabaseAccess.getExecutableTests(gameId, true);
		for (Test t : tests) {
			if (CollectionUtils.containsAny(t.getLineCoverage().getLinesCovered(), getLines()))
				return true;
		}
		return false;
	}

	public Set<Test> getCoveringTests() {
		Set<Test> coveringTests = new LinkedHashSet<>();

		for(Test t : DatabaseAccess.getTestsForGame(gameId)) {
			if(t.isMutantCovered(this)) {
				coveringTests.add(t);
			}
		}

		return coveringTests;
	}

	public boolean doesRequireRecompilation() {
		GameClass cut = DatabaseAccess.getClassForGame(gameId);
		return CollectionUtils.containsAny(cut.getLinesOfCompileTimeConstants(), getLines());
	}

	/**
	 * @return attacker points for this mutant in DUEL MODE
	 */
	public int getAttackerPoints() {
		if (alive) {
			// if mutant is alive, as many points as rounds it has survived
			// TODO: as many points as tests it has survived?
			DuelGame g = DatabaseAccess.getGameForKey("ID", gameId);
			int points = g.getCurrentRound() - roundCreated; // rounds survived
			if (g.getState().equals(GameState.FINISHED))
				points++; // add a point for the last round if the game has finished
			logger.info("Alive mutant " + getId() + " contributes " + points + " attacker points");
			return points;
		} else {
			if (classFile == null || classFile.equals("null")) // non-compilable
				return 0;
			if (equivalent.equals(Equivalence.DECLARED_YES)) // accepted equivalent
				return 0;
			if (equivalent.equals(Equivalence.ASSUMED_YES)) // claimed, rejected, test did not kill it
				return 0;
			if (equivalent.equals(Equivalence.PROVEN_NO)) { // claimed, rejected, test killed it
				logger.info("Claimed/rejected/killed mutant " + getId() + " contributes 2 attacker points");
				return 2;
			}
			int points = roundKilled - roundCreated; // rounds survived
			logger.info("Killed mutant " + getId() + " contributes " + points + " attacker points");
			return points;
		}
	}

	/**
	 * @return defender points for this mutant in DUEL MODE
	 */
	public int getDefenderPoints() {
		if (!alive && classFile != null) {
			if (equivalent.equals(Equivalence.ASSUMED_NO) || equivalent.equals(Equivalence.DECLARED_YES)) {
				return 1; // accepted equivalent
			} else if (equivalent.equals(Equivalence.ASSUMED_YES)) {
				return 2; // claimed, rejected, test did not kill it
			} else {
				// claimed, rejected, test killed it
				return 0;
			}

		}
		logger.info("Mutant " + getId() + " contributes 0 defender points (alive or non-compilable)");
		return 0;
	}

	// https://stackoverflow.com/questions/9577930/regular-expression-to-select-all-whitespace-that-isnt-in-quotes
	public static String regex = "\\s+(?=((\\\\[\\\\\"]|[^\\\\\"])*\"(\\\\[\\\\\"]|[^\\\\\"])*\")*(\\\\[\\\\\"]|[^\\\\\"])*$)";

	public synchronized Patch getDifferences() {
		if (difference == null) {
			int classId =
					DatabaseAccess.getGameForKey("ID", gameId).getClassId();
			GameClass sut = DatabaseAccess.getClassForKey("Class_ID", classId);

			File sourceFile = new File(sut.getJavaFile());
			File mutantFile = new File(javaFile);

			List<String> sutLines = readLinesIfFileExist(sourceFile.toPath());
			List<String> mutantLines =
					readLinesIfFileExist(mutantFile.toPath());

			for (int l = 0; l < sutLines.size(); l++){
				sutLines.set(l, sutLines.get(l).replaceAll( regex , ""));
			}

			for (int l = 0; l < mutantLines.size(); l++){
				mutantLines.set(l, mutantLines.get(l).replaceAll( regex , ""));
			}

			difference = DiffUtils.diff(sutLines, mutantLines);
		}
		return difference;
	}

	public String getPatchString() {
		int classId;
		if (this.classId == null) {
			classId = DatabaseAccess.getGameForKey("ID", gameId).getClassId();
		} else {
			classId = this.classId;
		}
		GameClass sut = DatabaseAccess.getClassForKey("Class_ID", classId);

		Path sourceFile = Paths.get(sut.getJavaFile());
		Path mutantFile = Paths.get(javaFile);

		List<String> sutLines = readLinesIfFileExist(sourceFile);
		List<String> mutantLines = readLinesIfFileExist(mutantFile);

		Patch patch = DiffUtils.diff(sutLines, mutantLines);
		List<String> unifiedPatches = DiffUtils.generateUnifiedDiff(null, null, sutLines, patch, 3);
		StringBuilder unifiedPatch = new StringBuilder();
		for (String s : unifiedPatches) {
			if ("--- null".equals(s) || "+++ null".equals(s))
				continue;
			unifiedPatch.append(s).append(System.getProperty("line.separator"));
		}
		return unifiedPatch.toString();
	}

	public String getHTMLEscapedPatchString() {
		return StringEscapeUtils.escapeHtml(getPatchString());
	}

	private List<String> readLinesIfFileExist(Path path) {
		List<String> lines = new ArrayList<>();
		try {
			if (Files.exists(path))
				lines = Files.readAllLines(path, StandardCharsets.UTF_8);
			else {
				logger.error("File not found {}", path);
			}
		} catch (IOException e) {
			e.printStackTrace();  // TODO handle properly
			return null;
		}
		return lines;
	}

	// insert will run once after mutant creation.
	// Stores values of JavaFile, ClassFile, GameID, RoundCreated in DB. These will not change once input.
	// Default values for Equivalent (ASSUMED_NO), Alive(1), RoundKilled(NULL) are assigned.
	// Currently Mutant ID isnt set yet after insertion, if Mutant needs to be used straight away it needs a similar insert method to MultiplayerGame.
	@Deprecated
	public boolean insert() {
		logger.info("Inserting mutant");
		Connection conn = DB.getConnection();
		String jFileDB = DatabaseAccess.addSlashes(javaFile);
		String cFileDB = classFile == null ? null : DatabaseAccess.addSlashes(classFile);
		String query = "INSERT INTO mutants (JavaFile, ClassFile, Game_ID, RoundCreated, Alive, Player_ID, Points, MD5)" +
				" VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
		DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(jFileDB),
				DB.getDBV(cFileDB),
				DB.getDBV(gameId),
				DB.getDBV(roundCreated),
				DB.getDBV(sqlAlive()),
				DB.getDBV(playerId),
				DB.getDBV(score),
				DB.getDBV(md5)};
		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
		int res = DB.executeUpdateGetKeys(stmt, conn);
		if (res > -1) {
			this.id = res;
			return true;
		}
		return false;
	}

	// update will run when changes to a mutant are made.
	// Updates values of Equivalent, Alive, RoundKilled.
	// These values update when Mutants are suspected of being equivalent, go through an equivalence test, or are killed.
	/*
	 * Update a mutant ONLY if in the DB it is still alive. This should prevent zombie mutants. but does not prevent messing up the score.
	 *
	 */
	@Deprecated
	public boolean update() {

		// This should be blocking
		Connection conn = DB.getConnection();

		// We cannot update killed mutants
		String query = "UPDATE mutants SET Equivalent=?, Alive=?, RoundKilled=?, NumberAiKillingTests=?, Points=? WHERE Mutant_ID=? AND Alive=1;";
		DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(equivalent.name()),
				DB.getDBV(sqlAlive()),
				DB.getDBV(roundKilled),
				DB.getDBV(killedByAITests),
				DB.getDBV(score),
				DB.getDBV(id)};
		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

		return DB.executeUpdate(stmt, conn);
	}

	@Override
	public String toString() {
		return "Mutant " + getId() + "; Alive:"+ isAlive() + "; Equivalent: " + getEquivalent() + " - " + getScore();
	}

	public void setTimesKilledAi(int count) {
		killedByAITests = count;
	}

	public int getTimesKilledAi() {
		if (killedByAITests == 0) {
			//Retrieve from DB.
			killedByAITests = DatabaseAccess.getNumTestsKillMutant(getId());
		}
		return killedByAITests;
	}

	public void incrementTimesKilledAi() {
		killedByAITests++;
	}

	/**
	 * Identify lines in the original source code that have been modified
	 * by a mutation.
	 *
	 * An insertion only modifies the line it was inserted in
	 *
	 * @return lines modified in the original class
	 */
	public synchronized  List<Integer> getLines() {
		if (lines != null) {
			return lines;
		}

		List<Integer> lines = new ArrayList<>();
		List<String> description = new ArrayList<>();

		Patch p = getDifferences();
		for (Delta d : p.getDeltas()) {
			Chunk chunk = d.getOriginal();
			// position starts at 0 but code readout starts at 1
			int firstLine = chunk.getPosition() + 1;
			String desc = "line " + firstLine;
			// was it one single line or several?
			lines.add(firstLine);
			int endLine = firstLine + chunk.getLines().size() - 1;
			if (endLine > firstLine) {
				// if more than one line, report range of lines;
				// may not be 100% accurate, but is all we have in the delta chunk
				for (int l = firstLine + 1; l <= endLine; l++) {
					lines.add(l);
				}
				desc = String.format("lines %d-%d", firstLine, endLine);
			}
			// update mutant description
			String text;
			switch (d.getType()) {
				case CHANGE:
					text = "Modified ";
					break;
				case DELETE:
					text = "Removed ";
					break;
				case INSERT:
					text = "Added ";
					break;
				default:
					throw new IllegalStateException("Found unknown delta type " + d.getType());
			}
			description.add(StringEscapeUtils.escapeHtml(text + desc + "\n"));
		}

		this.lines = lines;
		this.description = description;

		return lines;
	}

	public synchronized List<String> getHTMLReadout() {
		if (description != null) {
			return description;
		}
		// for efficiency, getLines actually create the list of messages
		getLines();

		return description;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;

		if (o == null || getClass() != o.getClass()) return false;

		Mutant mutant = (Mutant) o;

		return new EqualsBuilder()
				.append(id, mutant.id)
				.append(gameId, mutant.gameId)
				.append(playerId, mutant.playerId)
				.append(javaFile, mutant.javaFile)
				.append(md5, mutant.md5)
				.append(classFile, mutant.classFile)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(id)
				.append(gameId)
				.append(playerId)
				.append(javaFile)
				.append(md5)
				.append(classFile)
				.toHashCode();
	}

	public void prepareForSerialise(boolean showDifferences) {
        getHTMLReadout();
		getLines();
		if (showDifferences)
			getDifferences();
		else
			difference = new Patch();
	}

	/*
	 * This comparators place first mutants that modify lines at the top of the file.
	 */
	public static Comparator<Mutant> sortByLineNumberAscending() {
		return new Comparator<Mutant>() {
			@Override
			public int compare(Mutant o1, Mutant o2) {
				List<Integer> lines1 = o1.getLines();
				List<Integer> lines2 = o2.getLines();

				if (lines1.isEmpty()) {
					if (lines2.isEmpty()) {
						return 0;
					} else {
						return -1;
					}
				} else if (lines2.isEmpty()) {
					return 1;
				}

				return Collections.min(lines1) - Collections.min(lines2);
			}
		};
	}

	// TODO Ideally this should have a timestamp ... we use the ID instead
	// First created appears first
	public static Comparator<Mutant> orderByIdAscending() {
		return new Comparator<Mutant>() {
			@Override
			public int compare(Mutant o1, Mutant o2) {
				return o1.id - o2.id;
			}
		};
	}

	// Last created appears first
	public static Comparator<Mutant> orderByIdDescending() {
		return new Comparator<Mutant>() {
			@Override
			public int compare(Mutant o1, Mutant o2) {
				return o2.id - o1.id;
			}
		};
	}
}
