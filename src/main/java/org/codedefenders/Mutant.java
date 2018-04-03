package org.codedefenders;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codedefenders.duel.DuelGame;
import org.codedefenders.util.DB;
import org.codedefenders.util.DatabaseAccess;
import org.codedefenders.util.DatabaseValue;
import org.codedefenders.validation.CodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import difflib.Chunk;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

public class Mutant implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(Mutant.class);

	private int id;
	private int gameId;

	private transient String javaFile;
	private transient String md5;
	private transient String classFile;

	private boolean alive = true;

	private Equivalence equivalent;

	/* Mutant Equivalence */
	public enum Equivalence {
		ASSUMED_NO, PENDING_TEST, DECLARED_YES, ASSUMED_YES, PROVEN_NO
	}

	private int roundCreated;
	private int roundKilled;

	private int playerId;
	private String playerName;

	private transient int killedByAITests = 0; //How many times this mutant is killed
	// by an AI test.

	private int score; // multiplayer

	private ArrayList<Integer> lines = null;
	private transient ArrayList<String> description = null;
	private transient Patch difference = null;

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
		this.roundCreated = DatabaseAccess.getGameForKey("ID", gameId).getCurrentRound();
		this.javaFile = jFile;
		this.classFile = cFile;
		this.alive = alive;
		this.equivalent = Equivalence.ASSUMED_NO;
		this.playerId = playerId;
		this.md5 = CodeValidator.getMD5FromFile(jFile);
	}

	/**
	 * Creates a mutant
	 *
	 * @param mid
	 * @param gid
	 * @param jFile
	 * @param cFile
	 * @param alive
	 * @param equiv
	 * @param rCreated
	 * @param rKilled
	 * @param playerId
	 */
	public Mutant(int mid, int gid, String jFile, String cFile, boolean alive, Equivalence equiv, int rCreated, int rKilled, int playerId) {
		this(gid, jFile, cFile, alive, playerId);
		this.id = mid;
		this.equivalent = equiv;
		this.roundCreated = rCreated;
		this.roundKilled = rKilled;

		score = 0;
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

	//
	public void incrementScore(int score){
		
		if( score == 0 ){
			logger.debug("Do not update mutant {} score by 0", getId());
			return;
		}
		
		String query = "UPDATE mutants SET Points = Points + ? WHERE Mutant_ID=? AND Alive=1;";
		Connection conn = DB.getConnection();

		DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(score),
						DB.getDBV(id)};
				
		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
				
		DB.executeUpdate(stmt, conn);
	}
	
	@Deprecated
	public void setScore(int score) {
		this.score += score;
	}

	public void setPlayerName(String username){
		playerName = username;
	}

	public String getPlayerName(){
		return playerName;
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

	public Patch getDifferences() {
		if (difference == null) {
			int classId =
					DatabaseAccess.getGameForKey("ID", gameId).getClassId();
			GameClass sut = DatabaseAccess.getClassForKey("Class_ID", classId);

			File sourceFile = new File(sut.getJavaFile());
			File mutantFile = new File(javaFile);

			List<String> sutLines = readLinesIfFileExist(sourceFile.toPath());
			List<String> mutantLines =
					readLinesIfFileExist(mutantFile.toPath());

			difference = DiffUtils.diff(sutLines, mutantLines);
		}
		return difference;
	}

	public String getPatchString() {
		int classId = DatabaseAccess.getGameForKey("ID", gameId).getClassId();
		GameClass sut = DatabaseAccess.getClassForKey("Class_ID", classId);

		File sourceFile = new File(sut.getJavaFile());
		File mutantFile = new File(javaFile);

		List<String> sutLines = readLinesIfFileExist(sourceFile.toPath());
		List<String> mutantLines = readLinesIfFileExist(mutantFile.toPath());

		Patch patch = DiffUtils.diff(sutLines, mutantLines);
		List<String> unifiedPatches = DiffUtils.generateUnifiedDiff(null, null, sutLines, patch, 3);
		StringBuilder unifiedPatch = new StringBuilder();
		for (String s : unifiedPatches) {
			if ("--- null".equals(s) || "+++ null".equals(s))
				continue;
			unifiedPatch.append(s + System.getProperty("line.separator"));
		}
		return unifiedPatch.toString();
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
		} finally {
			return lines;
		}
	}

	// insert will run once after mutant creation.
	// Stores values of JavaFile, ClassFile, GameID, RoundCreated in DB. These will not change once input.
	// Default values for Equivalent (ASSUMED_NO), Alive(1), RoundKilled(NULL) are assigned.
	// Currently Mutant ID isnt set yet after insertion, if Mutant needs to be used straight away it needs a similar insert method to MultiplayerGame.
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

	public ArrayList<Integer> getLines() {
		if (lines != null) {
			return lines;
		}
		lines = new ArrayList<>();
		description = new ArrayList<>();

		Patch p = getDifferences();
		for (Delta d : p.getDeltas()) {
			Chunk c = d.getOriginal();
			Delta.TYPE t = d.getType();
			// position starts at 0 but code readout starts at 1
			int firstLine = c.getPosition() + 1;
			String desc = "line " + firstLine;
			// was it one single line or several?
			lines.add(firstLine);
			int endLine = firstLine + c.getLines().size() - 1;
			if (endLine > firstLine) {
				// if more than one line, report range of lines;
				// may not be 100% accurate, but is all we have in the delta chunk
				for (int l = firstLine + 1; l <= endLine; l++) {
					lines.add(l);
				}
				desc = String.format("lines %d-%d", firstLine, endLine);
			}
			// update mutant description
			if (t == Delta.TYPE.CHANGE) {
				description.add("Modified " + desc + "\n");
			} else if (t == Delta.TYPE.DELETE) {
				description.add("Removed " + desc + "\n");
			} else {
				description.add("Added " + desc + "\n");
			}
		}
		return lines;
	}

	public List<String> getHTMLReadout() throws IOException {
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
		try {
			getHTMLReadout();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
				return ((Integer) Collections.min( o1.getLines() ) ) - ((Integer) Collections.min( o2.getLines() )); 
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
