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
import java.util.*;
import java.util.stream.Collectors;

import javassist.ClassPool;
import javassist.CtClass;

public class Test {

	private static final Logger logger = LoggerFactory.getLogger(Test.class);

	private int id;
	private int gameId;
	private String javaFile;
	private String classFile;

	private int roundCreated;
	private int mutantsKilled = 0;

	private int playerId;

	public int getMutantsKilled() {
		return mutantsKilled;
	}

	private LineCoverage lineCoverage = new LineCoverage();

	private int score;

	public void setLineCoverage(LineCoverage lc) {
		lineCoverage = lc;
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

	// TODO Check that increment score does not consider mutants that were killed already
	public void incrementScore(int score) {
		if (score == 0) {
			// Why this is appenining?
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

	private int aiMutantsKilled = 0; //How many generated mutants this test kills.

	public Test(int gameId, String jFile, String cFile, int playerId) {
		this.gameId = gameId;
		try {
			DuelGame g = DatabaseAccess.getGameForKey("ID", gameId);
			if (g != null)
				this.roundCreated = g.getCurrentRound();
		} catch (NullPointerException e) {
			//multiplayer game
			logger.error("Could not fetch game", e);
		}
		this.javaFile = jFile;
		this.classFile = cFile;
		this.playerId = playerId;
		score = 0;
	}

	public Test(int tid, int gid, String jFile, String cFile, int roundCreated, int mutantsKilled, int playerId) {
		this(tid, gid, jFile, cFile, roundCreated, mutantsKilled, playerId, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
	}

	public Test(int tid, int gid, String jFile, String cFile, int roundCreated, int mutantsKilled, int playerId, List<Integer> linesCovered, List<Integer> linesUncovered) {
		this(gid, jFile, cFile, playerId);

		this.id = tid;
		this.roundCreated = roundCreated;
		this.mutantsKilled = mutantsKilled;
		lineCoverage.setLinesCovered(linesCovered);
		lineCoverage.setLinesUncovered(linesUncovered);
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
		if (playerId == DatabaseAccess.getGameForKey("ID", gameId).getDefenderId())
			return mutantsKilled;
		else
			return 0;
	}

	public String getDirectory() {
		File file = new File(javaFile);
		return file.getAbsoluteFile().getParent();
	}

	// Increment the number of mutant killed directly on the DB
	// And update the local object. But it requires several queries/connections
	//
	// TODO Check that this method is neverl called for tests that kill a mutant that was already dead...
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
			logger.error(String.format("Failed to read test class: %s", e.getLocalizedMessage()), e);
		}

		return testLines;
	}


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

	public boolean update() {
		logger.debug("Updating Test");
		Connection conn = DB.getConnection();
		String linesCoveredString = lineCoverage.getLinesCovered().stream().map(Object::toString).collect(Collectors.joining(","));
		String linesUncoveredString = lineCoverage.getLinesUncovered().stream().map(Object::toString).collect(Collectors.joining(","));

		String query = "UPDATE tests SET mutantsKilled=?, NumberAiMutantsKilled=?, Lines_Covered=?, Lines_Uncovered=?, Points = ? WHERE Test_ID=?;";
		DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(mutantsKilled),
				DB.getDBV(aiMutantsKilled),
				DB.getDBV(linesCoveredString),
				DB.getDBV(linesUncoveredString),
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

	public void setAiMutantsKilled(int count) {
		aiMutantsKilled = count;
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

	public void setClassFile(String classFile) {
		this.classFile = classFile;
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
