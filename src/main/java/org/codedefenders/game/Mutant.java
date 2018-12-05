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
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.game.duel.DuelGame;
import org.codedefenders.util.MutantUtils;
import org.codedefenders.validation.code.CodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
	private int classId;

	private transient String javaFile;
	private transient String md5;
	private transient String classFile;

    private String creatorName;
	private int creatorId;

	// Every mutant has its own
	private MutantUtils mutantUtils = new MutantUtils();
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

	// Computed on the fly if not read in the db
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
	public Mutant(String javaFilePath, String classFilePath, String md5, int classId) {
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
	public Mutant(int gameId, int classId,  String jFile, String cFile, boolean alive, int playerId) {
		this.gameId = gameId;
		this.classId = classId;
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

	public Mutant(int mid, int classId, int gid, String jFile, String cFile, boolean alive, Equivalence equiv, int rCreated, int rKilled, int playerId) {
		this(gid, classId, jFile, cFile, alive, playerId);
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

	public int getClassId() {
		return classId;
	}

	// TODO why does incrementScore update the DB entry, shouldn't this be done with update()
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

	public void setScore(int score) {
		this.score = score;
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
		List<Test> tests = TestDAO.getValidTestsForGame(gameId, true);
		for (Test t : tests) {
			if (CollectionUtils.containsAny(t.getLineCoverage().getLinesCovered(), getLines()))
				return true;
		}
		return false;
	}

	public Set<Test> getCoveringTests() {
		Set<Test> coveringTests = new LinkedHashSet<>();

		for(Test t : TestDAO.getValidTestsForGame(gameId, false)) {
			if(t.isMutantCovered(this)) {
				coveringTests.add(t);
			}
		}

		return coveringTests;
	}

	public boolean doesRequireRecompilation() {
	    // dummy game with id = -1 has null class, and this check cannot be implemented...
		GameClass cut = DatabaseAccess.getClassForGame(gameId);
		if(  cut == null ){
		    cut = DatabaseAccess.getClassForKey("Class_ID", classId);
		}
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

    public Patch getDifferences() {
        if (difference == null) {
            computeDifferences();
        }
        return difference;
    }

	// Not sure
	public void computeDifferences() {
		GameClass sut = DatabaseAccess.getClassForGame(gameId);
		if( sut == null ){
            // in this case gameId might have been -1 (upload)
            // so we try to reload the sut
            sut = DatabaseAccess.getClassForKey("Class_ID", getClassId());
        }

		assert sut != null;

		File sourceFile = new File(sut.getJavaFile());
		File mutantFile = new File(javaFile);

		List<String> sutLines = mutantUtils.readLinesIfFileExist(sourceFile.toPath());
		List<String> mutantLines = mutantUtils.readLinesIfFileExist(mutantFile.toPath());

		for (int l = 0; l < sutLines.size(); l++) {
			sutLines.set(l, sutLines.get(l).replaceAll(regex, ""));
		}

		for (int l = 0; l < mutantLines.size(); l++) {
			mutantLines.set(l, mutantLines.get(l).replaceAll(regex, ""));
		}

		difference = DiffUtils.diff(sutLines, mutantLines);
	}

	public String getPatchString() {
	    GameClass sut = DatabaseAccess.getClassForGame(gameId);
	    if( sut == null ){
	        // in this case gameId might have been -1 (upload)
	        // so we try to reload the sut
	        sut = DatabaseAccess.getClassForKey("Class_ID", getClassId());
	    }

		Path sourceFile = Paths.get(sut.getJavaFile());
		Path mutantFile = Paths.get(javaFile);

		List<String> sutLines = mutantUtils.readLinesIfFileExist(sourceFile);
		List<String> mutantLines = mutantUtils.readLinesIfFileExist(mutantFile);

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

	public boolean insert() {
		try {
			this.id = MutantDAO.storeMutant(this);
			return true;
		} catch (Exception e) {
		    return false;
		}
	}

	// update will run when changes to a mutant are made.
	// Updates values of Equivalent, Alive, RoundKilled.
	// These values update when Mutants are suspected of being equivalent, go through an equivalence test, or are killed.
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

	// Does this every get called if mutant is not stored to DB ?
	public List<Integer> getLines() {
		if (lines == null) {
			computeLinesAndDescription();
		}
		return lines;
	}

	/**
	 * Identify lines in the original source code that have been modified
	 * by a mutation.
	 *
	 * An insertion only modifies the line it was inserted in
	 *
	 * @return lines modified in the original class
	 */
	public void computeLinesAndDescription() {
		// This workflow is not really nice...
		List<Integer> mutatedLines = new ArrayList<Integer>();
		description = new ArrayList<String>();

		Patch p = getDifferences();
		for (Delta d : p.getDeltas()) {
			Chunk chunk = d.getOriginal();
			// position starts at 0 but code readout starts at 1
			int firstLine = chunk.getPosition() + 1;
			String desc = "line " + firstLine;
			// was it one single line or several?
			mutatedLines.add(firstLine);
			int endLine = firstLine + chunk.getLines().size() - 1;
			if (endLine > firstLine) {
				// if more than one line, report range of lines;
				// may not be 100% accurate, but is all we have in the delta
				// chunk
				for (int l = firstLine + 1; l <= endLine; l++) {
					mutatedLines.add(l);
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

		setLines( mutatedLines );
	}

	public synchronized List<String> getHTMLReadout() {
		if (description == null) {
			computeLinesAndDescription();
		}
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

	public void setLines(List<Integer> mutatedLines) {
		this.lines = mutatedLines;
	}
}
