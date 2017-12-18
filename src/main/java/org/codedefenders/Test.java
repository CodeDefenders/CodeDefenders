package org.codedefenders;

import edu.emory.mathcs.backport.java.util.Arrays;
import javassist.ClassPool;
import javassist.CtClass;
import org.apache.commons.collections.CollectionUtils;
import org.codedefenders.duel.DuelGame;
import org.codedefenders.multiplayer.LineCoverage;
import org.codedefenders.util.DB;
import org.codedefenders.util.DatabaseAccess;
import org.codedefenders.util.DatabaseValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
		this(gid, jFile, cFile, playerId);

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
		if (playerId == DatabaseAccess.getGameForKey("ID", gameId).getDefenderId())
			return mutantsKilled;
		else
			return 0;
	}

	public String getDirectory() {
		File file = new File(javaFile);
		return file.getAbsoluteFile().getParent();
	}

	public void killMutant() {
		mutantsKilled++;
		update();
	}

	public boolean isMutantCovered(Mutant mutant) {
		return CollectionUtils.containsAny(lineCoverage.getLinesCovered(), mutant.getLines());
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
		String sql = null;
		Connection conn = DB.getConnection();
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
}
