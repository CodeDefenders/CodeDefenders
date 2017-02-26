package org.codedefenders.multiplayer;

import org.apache.commons.lang.ArrayUtils;
import org.codedefenders.*;
import org.codedefenders.util.DatabaseAccess;

import java.sql.*;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.codedefenders.Mutant.Equivalence.*;

public class MultiplayerGame extends AbstractGame {

	private int defenderValue;
	private int attackerValue;
	private float lineCoverage;
	private float mutantCoverage;
	private float prize;
	private int attackerLimit;
	private int defenderLimit;
	private int minAttackers;
	private int minDefenders;
	private long startDateTime;
	private long finishDateTime;
	private boolean requiresValidation;

	public MultiplayerGame(int classId, int creatorId, GameLevel level,
	                       float lineCoverage, float mutantCoverage, float prize,
	                       int defenderValue, int attackerValue, int defenderLimit,
	                       int attackerLimit, int minDefenders, int minAttackers,
	                       long startDateTime, long finishDateTime, String status) {
		this(classId, creatorId, level, lineCoverage, mutantCoverage, prize, defenderValue, attackerValue, defenderLimit, attackerLimit,
				minDefenders, minAttackers, startDateTime, finishDateTime, status, false);
	}

	public MultiplayerGame(int classId, int creatorId, GameLevel level,
	                       float lineCoverage, float mutantCoverage, float prize,
	                       int defenderValue, int attackerValue, int defenderLimit,
	                       int attackerLimit, int minDefenders, int minAttackers,
	                       long startDateTime, long finishDateTime, String status, boolean requiresValidation) {
		this.classId = classId;
		this.creatorId = creatorId;
		this.level = level;
		this.mode = GameMode.PARTY;
		this.lineCoverage = lineCoverage;
		this.mutantCoverage = mutantCoverage;
		this.prize = prize;
		this.defenderValue = defenderValue;
		this.attackerValue = attackerValue;
		this.defenderLimit = defenderLimit;
		this.attackerLimit = attackerLimit;
		this.minDefenders = minDefenders;
		this.minAttackers = minAttackers;
		this.state = GameState.valueOf(status);
		this.startDateTime = startDateTime;
		this.finishDateTime = finishDateTime;
		this.requiresValidation = requiresValidation;
	}

	public int getAttackerLimit() {
		return attackerLimit;
	}

	public int getDefenderLimit() {
		return defenderLimit;
	}

	public int getMinAttackers() {
		return minAttackers;
	}

	public int getMinDefenders() {
		return minDefenders;
	}

	public void setId(int id) {
		this.id = id;
		if (this.state != GameState.FINISHED && finishDateTime < System.currentTimeMillis()){
			this.state = GameState.FINISHED;
			update();
		}
	}

	public int getDefenderValue() {
		return defenderValue;
	}

	public void setDefenderValue(int defenderValue) {
		this.defenderValue = defenderValue;
	}

	public int getAttackerValue() {
		return attackerValue;
	}

	public void setAttackerValue(int attackerValue) {
		this.attackerValue = attackerValue;
	}

	public float getLineCoverage() {
		return lineCoverage;
	}

	public void setLineCoverage(float lineCoverage) {
		this.lineCoverage = lineCoverage;
	}

	public float getMutantCoverage() {
		return mutantCoverage;
	}

	public void setMutantCoverage(float mutantCoverage) {
		this.mutantCoverage = mutantCoverage;
	}

	public float getPrize() {
		return prize;
	}

	public void setPrize(float prize) {
		this.prize = prize;
	}

	/**
	 * Get winning team. Return NONE if a draw.
	 * @return Winning team.
     */
	public Role getWinningTeam() {
		Role victor = Role.NONE;
		int scoreAtt = getAttackerTeamScore();
		int scoreDef = getDefenderTeamScore();
		if(scoreAtt > scoreDef) {
			victor = Role.ATTACKER;
		} else if (scoreAtt < scoreDef) {
			victor = Role.DEFENDER;
		}
		return victor;
	}

	public String getStartDateTime() {
		Date date = new Date(startDateTime);
		Format format = new SimpleDateFormat("dd/MM/yy HH:mm");
		return format.format(date);
	}

	public String getFinishDateTime() {
		Date date = new Date(finishDateTime);
		Format format = new SimpleDateFormat("dd/MM/yy HH:mm");
		return format.format(date);
	}

	public List<Mutant> getMutantsMarkedEquivalent() {
		return getMutants().stream().filter(mutant -> mutant.getEquivalent().equals(ASSUMED_YES) ||
				                            mutant.getEquivalent().equals(DECLARED_YES)).collect(Collectors.toList());
	}

	public List<Mutant> getMutantsMarkedEquivalentPending() {
		return getMutants().stream().filter(mutant -> mutant.getEquivalent().equals(PENDING_TEST)).collect(Collectors.toList());
	}

	public int[] getDefenderIds(){
		return DatabaseAccess.getPlayersForMultiplayerGame(getId(), Role.DEFENDER);
	}

	public int[] getAttackerIds(){
		return DatabaseAccess.getPlayersForMultiplayerGame(getId(), Role.ATTACKER);
	}

	public int[] getPlayerIds() { return ArrayUtils.addAll(getDefenderIds(), getAttackerIds());}

	public boolean addPlayer(int userId, Role role) {
		if (state != GameState.FINISHED && canJoinGame(userId, role)) {
			String sql = String.format("INSERT INTO players " +
							"(Game_ID, User_ID, Points, Role) " +
							"VALUES (%d, %d, 0, '%s') " +
							"ON DUPLICATE KEY UPDATE Role='%3$s', Active=TRUE;",
					id, userId, role);
			return DatabaseAccess.executeUpdate(sql);
		}
		return false;
	}

	public boolean removePlayer(int userId) {
		if (state == GameState.CREATED) {
			String sql = String.format("UPDATE players " +
							"SET Active=FALSE WHERE Game_ID=%d AND User_ID=%d;",
					id, userId);
			return DatabaseAccess.executeUpdate(sql);
		}
		return false;
	}

	private boolean canJoinGame(int userId, Role role) {
		if (!requiresValidation || DatabaseAccess.getUserForKey("User_ID", userId).isValidated()) {
			if (role.equals(Role.ATTACKER))
				return (attackerLimit == 0 || getAttackerIds().length < attackerLimit);
			else
				return (defenderLimit == 0 || getDefenderIds().length < defenderLimit);
		} else {
			return false;
		}
	}



	public boolean insert() {

		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		// Attempt to insert game info into database
		try {
			conn = DatabaseAccess.getConnection();

			stmt = conn.createStatement();
			sql = String.format("INSERT INTO games " +
					"(Class_ID, Level, Prize, Defender_Value, Attacker_Value, Coverage_Goal, Mutant_Goal, Creator_ID, " +
					"Attackers_Needed, Defenders_Needed, Attackers_Limit, Defenders_Limit, Start_Time, Finish_Time, State, Mode) VALUES " +
					"('%s', 	'%s', '%f', 	'%d',			'%d',			'%f',			'%f',		'%d'," +
					"'%d',				'%d',				'%d',			'%d',			'%s', '%s',		'%s', 'PARTY');",
					classId, level.name(), prize, defenderValue, attackerValue, lineCoverage, mutantCoverage, creatorId,
					minAttackers, minDefenders, attackerLimit, defenderLimit, new Timestamp(startDateTime), new Timestamp(finishDateTime), state.name());

			stmt.execute(sql, Statement.RETURN_GENERATED_KEYS);

			ResultSet rs = stmt.getGeneratedKeys();

			if (rs.next()) {
				id = rs.getInt(1);
				stmt.close();
				conn.close();
				return true;
			}

		} catch (SQLException se) {
			System.out.println(se);
			//Handle errors for JDBC
		} catch (Exception e) {
			System.out.println(e);
			//Handle errors for Class.forName
		} finally {
			DatabaseAccess.cleanup(conn, stmt);
		} //end try

		return false;
	}

	public boolean update() {

		Connection conn = null;
		Statement stmt = null;
		String sql = null;

		try {
			conn = DatabaseAccess.getConnection();

			// Get all rows from the database which have the chosen username
			stmt = conn.createStatement();
				sql = String.format("UPDATE games SET " +
						"Class_ID = '%s', Level = '%s', Prize = %f, Defender_Value=%d, Attacker_Value=%d, Coverage_Goal=%f" +
						", Mutant_Goal=%f, State='%s' WHERE ID='%d'",
						classId, level.name(), prize, defenderValue, attackerValue, lineCoverage, mutantCoverage, state.name(), id);
			stmt.execute(sql);
			return true;

		} catch (SQLException se) {
			System.out.println(se);
			//Handle errors for JDBC
			se.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			//Handle errors for Class.forName
			e.printStackTrace();
		} finally {
			DatabaseAccess.cleanup(conn, stmt);
		} //end try

		return false;
	}

	public HashMap<Integer, PlayerScore> getMutantScores(){
		HashMap<Integer, PlayerScore> mutantScores = new HashMap<Integer, PlayerScore>();

		HashMap<Integer, Integer> mutantsAlive = new HashMap<Integer, Integer>();
		HashMap<Integer, Integer> mutantsKilled = new HashMap<Integer, Integer>();
		HashMap<Integer, Integer> mutantsEquiv = new HashMap<Integer, Integer>();

		List<Mutant> allMutants = getAliveMutants();
		allMutants.addAll(getKilledMutants());
		allMutants.addAll(getMutantsMarkedEquivalent());
		allMutants.addAll(getMutantsMarkedEquivalentPending());


		if (!mutantScores.containsKey(-1)){
			mutantScores.put(-1, new PlayerScore(-1));
			mutantsAlive.put(-1, 0);
			mutantsEquiv.put(-1, 0);
			mutantsKilled.put(-1, 0);
		}

		for (Mutant mm : allMutants){

			if (!mutantScores.containsKey(mm.getPlayerId())){
				mutantScores.put(mm.getPlayerId(), new PlayerScore(mm.getPlayerId()));
				mutantsAlive.put(mm.getPlayerId(), 0);
				mutantsEquiv.put(mm.getPlayerId(), 0);
				mutantsKilled.put(mm.getPlayerId(), 0);
			}

			PlayerScore ps = mutantScores.get(mm.getPlayerId());
			ps.increaseQuantity();
			ps.increaseTotalScore(mm.getScore());

			PlayerScore ts = mutantScores.get(-1);
			ts.increaseQuantity();
			ts.increaseTotalScore(mm.getScore());


			if (mm.getEquivalent().equals(ASSUMED_YES) || mm.getEquivalent().equals(DECLARED_YES)){
				mutantsEquiv.put(mm.getPlayerId(), mutantsEquiv.get(mm.getPlayerId())+1);
				mutantsEquiv.put(-1, mutantsEquiv.get(-1)+1);
			} else if (mm.isAlive()){
				//This includes mutants marked equivalent
				mutantsAlive.put(mm.getPlayerId(), mutantsAlive.get(mm.getPlayerId())+1);
				mutantsAlive.put(-1, mutantsAlive.get(-1)+1);
			} else {
				mutantsKilled.put(mm.getPlayerId(), mutantsKilled.get(mm.getPlayerId())+1);
				mutantsKilled.put(-1, mutantsKilled.get(-1)+1);
			}

		}

		for (int i : mutantsKilled.keySet()){
			PlayerScore ps = mutantScores.get(i);
			ps.setAdditionalInformation(mutantsAlive.get(i) + " / " + mutantsKilled.get(i) + " / " + mutantsEquiv.get((i)));
		}

		return mutantScores;
	}

	public HashMap<Integer, PlayerScore> getTestScores(){
		HashMap<Integer, PlayerScore> testScores = new HashMap<Integer, PlayerScore>();
		HashMap<Integer, Integer> mutantsKilled = new HashMap<Integer, Integer>();
		int defendersTeamId = -1;
		testScores.put(defendersTeamId, new PlayerScore(defendersTeamId));
		mutantsKilled.put(defendersTeamId, 0);

		for (int defenderId : getDefenderIds()){
			testScores.put(defenderId, new PlayerScore(defenderId));
			mutantsKilled.put(defenderId, 0);
		}

		int[] attackers = getAttackerIds();
		for (Test test : getTests()){
			if (ArrayUtils.contains(attackers, test.getPlayerId()))
				continue;
			if (!testScores.containsKey(test.getPlayerId())){
				testScores.put(test.getPlayerId(), new PlayerScore(test.getPlayerId()));
				mutantsKilled.put(test.getPlayerId(), 0);
			}
			PlayerScore ps = testScores.get(test.getPlayerId());
			ps.increaseQuantity();
			ps.increaseTotalScore(test.getScore());

			int teamKey = defendersTeamId;

			PlayerScore ts = testScores.get(teamKey);
			ts.increaseQuantity();
			ts.increaseTotalScore(test.getScore());

			mutantsKilled.put(test.getPlayerId(), mutantsKilled.get(test.getPlayerId()) + test.getMutantsKilled());

			mutantsKilled.put(teamKey, mutantsKilled.get(teamKey)+test.getMutantsKilled());

		}

		for (int playerId : mutantsKilled.keySet()){
			if (playerId < 0 || ArrayUtils.contains(attackers, playerId))
				continue;

			int teamKey = defendersTeamId;

			PlayerScore ps = testScores.get(playerId);
			int playerScore = DatabaseAccess.getPlayerPoints(playerId);
			ps.increaseTotalScore(playerScore);

			PlayerScore ts = testScores.get(teamKey);
			ts.increaseTotalScore(playerScore);
		}

		for (int i : mutantsKilled.keySet()){
			PlayerScore ps = testScores.get(i);
			ps.setAdditionalInformation("" + mutantsKilled.get(i));
		}

		return testScores;
	}

	public int getAttackerTeamScore() {
		int totalScore = 0;

		for (Mutant m : getMutants())
			totalScore += m.getAttackerPoints();
		logger.debug("Attacker Score: " + totalScore);
		return totalScore;
	}

	public int getDefenderTeamScore() {
		int totalScore = 0;

		for (Test t : getTests(true))
			totalScore += t.getDefenderPoints();

		for (Mutant m : getMutants())
			totalScore += m.getDefenderPoints();
		logger.debug("Defender Score: " + totalScore);
		return totalScore;
	}
}
