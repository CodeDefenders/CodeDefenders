package org.codedefenders.game;

import org.apache.commons.collections.CollectionUtils;
import org.codedefenders.database.DB;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.DatabaseValue;
import org.codedefenders.game.duel.DuelGame;
import org.codedefenders.game.multiplayer.LineCoverage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
	private LineCoverage lineCoverage = new LineCoverage();

	/**
	 * Creates a new Test with following attributes:
	 * <ul>
	 * <li><code>gameId -1</code></li>
	 * <li><code>playerId -1</code></li>
	 * <li><code>roundCreated -1</code></li>
	 * <li><code>score 0</code></li>
	 * </ul>
	 */
	public Test(String javaFilePath, String classFilePath, int classId) {
		this.javaFile = javaFilePath;
		this.classFile = classFilePath;
		this.gameId = -1;
		this.playerId = -1;
		this.roundCreated = -1;
		this.score = 0;
		this.classId = classId;
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
		lineCoverage.setLinesCovered(linesCovered);
		lineCoverage.setLinesUncovered(linesUncovered);
	}
	// TODO Check that increment score does not consider mutants that were killed already
	public void incrementScore(int score) {
		if (score == 0) {
			// Why this is happening?
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

	@Deprecated
	public void setScore(int s) {
		score += s;
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

	public Set<Mutant> getCoveredMutants() {
		List<Integer> coverage = lineCoverage.getLinesCovered();
		Set<Mutant> coveredMutants = new TreeSet<>(Mutant.orderByIdAscending());

		for(Mutant m : DatabaseAccess.getMutantsForGame(gameId)) {
			if(CollectionUtils.containsAny(coverage, m.getLines())) {
				coveredMutants.add(m);
			}
		}

		return coveredMutants;
	}

	public Set<Mutant> getKilledMutants() {
		return DatabaseAccess.getKilledMutantsForTestId(id);
	}

	public List<String> getHTMLReadout() {

		File testFile = new File(javaFile);
		List<String> testLines = new LinkedList<String>();

		String line;

		try {
			BufferedReader in = new BufferedReader(new FileReader(testFile));
			while ((line = in.readLine()) != null) {
				testLines.add(line);
			}
		} catch (IOException e) {
			logger.error("Failed to read test class: ", e);
		}

		return testLines;
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
		StringBuilder linesCoveredString = new StringBuilder();
		StringBuilder linesUncoveredString = new StringBuilder();
		if (lineCoverage != null) {
			for (int i : lineCoverage.getLinesCovered()) {
				linesCoveredString.append(i).append(",");
			}
			for (int i : lineCoverage.getLinesUncovered()) {
				linesUncoveredString.append(i).append(",");
			}
			if (linesCoveredString.length() > 0) {
				linesCoveredString.deleteCharAt(linesCoveredString.length() - 1);
//				linesCoveredString = new StringBuilder(linesCoveredString.substring(0, linesCoveredString.length() - 1));
			}
			if (linesUncoveredString.length() > 0) {
				linesUncoveredString.deleteCharAt(linesUncoveredString.length() - 1);
//				linesUncoveredString = new StringBuilder(linesUncoveredString.substring(0, linesUncoveredString.length() - 1));
			}
		}
		//-1 for the left over comma
		String query = "UPDATE tests SET mutantsKilled=?, NumberAiMutantsKilled=?, Lines_Covered=?, Lines_Uncovered=?, Points = ? WHERE Test_ID=?;";
		DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(mutantsKilled),
				DB.getDBV(aiMutantsKilled),
				DB.getDBV(linesCoveredString.toString()),
				DB.getDBV(linesUncoveredString.toString()),
				DB.getDBV(score),
				DB.getDBV(id)};
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

}
