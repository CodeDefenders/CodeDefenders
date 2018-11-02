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
import org.codedefenders.database.DB;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.DatabaseValue;
import org.codedefenders.game.duel.DuelGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javassist.ClassPool;
import javassist.CtClass;

/**
 * This class represents a test case. These test cases are created by defenders
 * to find mutations in a game class.
 *
 * @see GameClass
 * @see Mutant
 */
public class Test {
	private static final Logger logger = LoggerFactory.getLogger(Test.class);

	private int id;
	private int playerId;
	private int gameId;
	private String javaFile;
	private String classFile;

	/**
	 * Identifier of the class this test is created for.
	 * Of type {@link Integer}, because the classId can be {@code null}.
	 */
	private Integer classId;

	private int roundCreated;
	private int mutantsKilled;
	private int score;
	private int aiMutantsKilled; // how many generated mutants this test killed.
	private LineCoverage lineCoverage;

	/**
	 * Creates a new Test with following attributes:
	 * <ul>
	 * <li><code>gameId -1</code></li>
	 * <li><code>playerId -1</code></li>
	 * <li><code>roundCreated -1</code></li>
	 * <li><code>score 0</code></li>
	 * </ul>
	 */
	public Test(String javaFilePath, String classFilePath, int classId, LineCoverage lineCoverage) {
		this.javaFile = javaFilePath;
		this.classFile = classFilePath;
		this.gameId = -1;
		this.playerId = -1;
		this.roundCreated = -1;
		this.score = 0;
		this.classId = classId;
		this.lineCoverage = lineCoverage;
	}

	public Test(int gameId, String javaFile, String classFile, int playerId) {
		this.gameId = gameId;
        DuelGame g = DatabaseAccess.getGameForKey("ID", gameId);
        if (g != null) {
            this.roundCreated = g.getCurrentRound();
        } else {
            logger.error("Could not fetch game for gameId: " + gameId);
        }
		this.javaFile = javaFile;
		this.classFile = classFile;
		this.playerId = playerId;
		this.score = 0;
		this.lineCoverage = new LineCoverage();
	}

	@Deprecated
	public Test(int testId, int gameId, String javaFile, String classFile, int roundCreated, int mutantsKilled, int playerId) {
		this(testId, gameId, javaFile, classFile, roundCreated, mutantsKilled, playerId, Collections.emptyList(), Collections.emptyList(), 0);
	}

	public Test(int testId, int gameId, String javaFile, String classFile, int roundCreated, int mutantsKilled,
				int playerId, List<Integer> linesCovered, List<Integer> linesUncovered, int score) {
		this(gameId, javaFile, classFile, playerId);

		this.id = testId;
		this.roundCreated = roundCreated;
		this.mutantsKilled = mutantsKilled;
		this.score = score;
		lineCoverage = new LineCoverage(linesCovered, linesUncovered);
	}
	// TODO Check that increment score does not consider mutants that were killed already
	public void incrementScore(int score) {
		if (score == 0) {
			// Why this is happening?
			// Phil: ^ because the calculated score for this test so far is zero (e.g. no mutants in a game yet)
			logger.warn("Do not increment score for test {} when score is zero", getId());
			return;
		}

		String query = "UPDATE tests SET Points = Points + ? WHERE Test_ID=?;";
		Connection conn = DB.getConnection();

		DatabaseValue[] valueList = new DatabaseValue[] { DB.getDBV(score), DB.getDBV(id) };

		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

		boolean incremented = DB.executeUpdate(stmt, conn);

		logger.info("Increment score for {} by {}. Update? {} ", toString(), score, incremented);
	}

	@Deprecated()
	public void setScore(int store) {
		score = store;
	}

	public void updateScore(int score) {
		this.score += score;
	}

	public int getId() {
		return id;
	}

	public int getGameId() {
		return gameId;
	}

	public int getMutantsKilled() {
		return mutantsKilled;
	}

	public int getRoundCreated() {
		return roundCreated;
	}

	public int getDefenderPoints() {
		final DuelGame game = DatabaseAccess.getGameForKey("ID", this.gameId);
		if (game != null && playerId == game.getDefenderId()) {
			return mutantsKilled;
		} else {
			return 0;
		}
	}


	public String getDirectory() {
		File file = new File(javaFile);
		return file.getAbsoluteFile().getParent();
	}

	// Increment the number of mutant killed directly on the DB
	// And update the local object. But it requires several queries/connections
	//
	// TODO Check that this method is never called for tests that kill a mutant that was already dead...
	public void killMutant() {
		// mutantsKilled++;
		// update();
		logger.info("Test {} killed a new mutant", getId());

		String query = "UPDATE tests SET MutantsKilled = MutantsKilled + ? WHERE Test_ID=?;";
		Connection conn = DB.getConnection();

		DatabaseValue[] valueList = new DatabaseValue[] { DB.getDBV(1), DB.getDBV(id) };

		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);

		boolean updated = DB.executeUpdate(stmt, conn);

		// Eventually update the kill count from the DB
		mutantsKilled = DatabaseAccess.getTestForId(getId()).getMutantsKilled();
		
		logger.info("Test {} new killcount is {}. Was updated ? {} ", toString(), mutantsKilled, updated);
	}

	public boolean isMutantCovered(Mutant mutant) {
		return CollectionUtils.containsAny(lineCoverage.getLinesCovered(), mutant.getLines());
	}

	public Set<Mutant> getCoveredMutants(List<Mutant> mutants) {
		List<Integer> coverage = lineCoverage.getLinesCovered();
		Set<Mutant> coveredMutants = new TreeSet<>(Mutant.orderByIdAscending());

		for(Mutant m : mutants) {
			if(CollectionUtils.containsAny(coverage, m.getLines())) {
				coveredMutants.add(m);
			}
		}

		return coveredMutants;
	}

	public Set<Mutant> getKilledMutants() {
		return DatabaseAccess.getKilledMutantsForTestId(id);
	}

	@SuppressWarnings("Duplicates")
	public String getAsString() {
		try {
			return new String(Files.readAllBytes(Paths.get(javaFile)));
		} catch (FileNotFoundException e) {
			logger.error("Could not find file " + javaFile);
			return "[File Not Found]";
		} catch (IOException e) {
			logger.error("Could not read file " + javaFile);
			return "[File Not Readable]";
		}
	}

	@SuppressWarnings("Duplicates")
	public String getAsHTMLEscapedString() {
		return StringEscapeUtils.escapeHtml(getAsString());
	}


	@Deprecated
	public boolean insert() {
		String jFileDB = DatabaseAccess.addSlashes(javaFile);
		String cFileDB = classFile == null ? null : DatabaseAccess.addSlashes(classFile);

		Connection conn = DB.getConnection();
		String query = "INSERT INTO tests (JavaFile, ClassFile, Game_ID, RoundCreated, Player_ID, Points) VALUES (?, ?, ?, ?, ?, ?);";
		DatabaseValue[] valueList = new DatabaseValue[]{
				DB.getDBV(jFileDB),
				DB.getDBV(cFileDB),
				DB.getDBV(gameId),
				DB.getDBV(roundCreated),
				DB.getDBV(playerId),
				DB.getDBV(score)
		};

		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
		this.id = DB.executeUpdateGetKeys(stmt, conn);
		return this.id > 0;
	}

	@Deprecated
	public boolean update() {
		logger.debug("Updating Test");
		Connection conn = DB.getConnection();

		String linesCoveredString = "";
		String linesUncoveredString= "";

		if (lineCoverage != null) {
			linesCoveredString = lineCoverage.getLinesCovered().stream().map(Object::toString).collect(Collectors.joining(","));
			linesUncoveredString = lineCoverage.getLinesUncovered().stream().map(Object::toString).collect(Collectors.joining(","));
		}


		String query = "UPDATE tests SET mutantsKilled=?,NumberAiMutantsKilled=?,Lines_Covered=?,Lines_Uncovered=?,Points=? WHERE Test_ID=?;";
		DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(mutantsKilled),
				DB.getDBV(aiMutantsKilled),
				DB.getDBV(linesCoveredString),
				DB.getDBV(linesUncoveredString),
				DB.getDBV(score),
				DB.getDBV(id)
		};

		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
		return DB.executeUpdate(stmt, conn);
	}

	public String getFullyQualifiedClassName() {
		if (classFile == null)
			return null;

		ClassPool classPool = ClassPool.getDefault();
		CtClass cc = null;
		try {
			cc = classPool.makeClass(new FileInputStream(new File(classFile)));
		} catch (IOException e) {
			logger.error("IO exception caught", e);
		}
		return cc == null ? null : cc.getName();
	}

	public boolean isValid() {
		return classFile != null;
	}

	public int getAiMutantsKilled() {
		if (aiMutantsKilled == 0) {
			//Retrieve from DB.
			aiMutantsKilled = DatabaseAccess.getNumAiMutantsKilledByTest(getId());
		}
		return aiMutantsKilled;
	}

	public void incrementAiMutantsKilled() {
		aiMutantsKilled++;
	}

	public String getJavaFile() {
		return javaFile;
	}

	public void setJavaFile(String javaFile) {
		this.javaFile = javaFile;
	}

	public String getClassFile() {
		return classFile;
	}

	public LineCoverage getLineCoverage() {
		return lineCoverage;
	}

	public void setPlayerId(int id) {
		playerId = id;
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

	@Deprecated
	public void setAiMutantsKilled(int count) {
		aiMutantsKilled = count;
	}

	@Deprecated
	public void setLineCoverage(LineCoverage lineCoverage) {
		this.lineCoverage = lineCoverage;
	}

	// First created appears first
	public static Comparator<Test> orderByIdAscending() {
		return new Comparator<Test>() {
			@Override
			public int compare(Test o1, Test o2) {
				return o1.id - o2.id;
			}
		};
	}

	// Last created appears first
	public static Comparator<Test> orderByIdDescending() {
		return new Comparator<Test>() {
			@Override
			public int compare(Test o1, Test o2) {
				return o2.id - o1.id;
			}
		};
	}
}
