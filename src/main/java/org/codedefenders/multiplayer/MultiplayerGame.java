package org.codedefenders.multiplayer;

import org.apache.commons.lang.ArrayUtils;
import org.codedefenders.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.codedefenders.Mutant.Equivalence.ASSUMED_NO;
import static org.codedefenders.Mutant.Equivalence.PENDING_TEST;
import static org.codedefenders.Mutant.Equivalence.PROVEN_NO;

public class MultiplayerGame {

	private static final Logger logger = LoggerFactory.getLogger(MultiplayerGame.class);

	public enum Status {
		WAITING, STARTED, FINISHED;
	}

	private int id;

	private int classId;


	private int creatorId;
	private int defenderValue;
	private int attackerValue;
	private float lineCoverage;
	private float mutantCoverage;
	private float price;
	private int attackerLimit;
	private int defenderLimit;
	private int minAttackers;
	private int minDefenders;
	private long finishTime;
	private Status status;

	private Game.Level level;

	public void setId(int id) {
		this.id = id;
		if (this.status != Status.FINISHED && finishTime < System.currentTimeMillis()){
			this.status = Status.FINISHED;
			update();
		}
	}

	public Status getStatus(){
		return status;
	}

	public void setClassId(int classId) {
		this.classId = classId;
	}

	public int getCreatorId() {
		return creatorId;
	}

	public void setCreatorId(int creatorId) {
		this.creatorId = creatorId;
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

	public float getPrice() {
		return price;
	}

	public void setPrice(float price) {
		this.price = price;
	}

	public MultiplayerGame(int classId, int creatorId, Game.Level level,
						   float lineCoverage, float mutantCoverage, float price,
						   int defenderValue, int attackerValue, int defenderLimit,
						   int attackerLimit, int minDefenders, int minAttackers,
						   long finishTime, String status) {
		this.classId = classId;
		this.creatorId = creatorId;
		this.level = level;
		this.lineCoverage = lineCoverage;
		this.mutantCoverage = mutantCoverage;
		this.price = price;
		this.defenderValue = defenderValue;
		this.attackerValue = attackerValue;
		this.defenderLimit = defenderLimit;
		this.attackerLimit = attackerLimit;
		this.minDefenders = minDefenders;
		this.minAttackers = minAttackers;
		this.status = Status.valueOf(status);
		this.finishTime = finishTime;
	}

	public int getId() {
		return id;
	}

	public int getClassId() {
		return classId;
	}

	public String getClassName() {
		return DatabaseAccess.getClassForKey("Class_ID", classId).name;
	}

	public GameClass getCUT() {
		return DatabaseAccess.getClassForKey("Class_ID", classId);
	}

	public Game.Level getLevel() {
		return this.level;
	}

	public void setLevel(Game.Level level) {
		this.level = level;
	}

	//private List<Mutant> mutants = null;


	public ArrayList<MultiplayerMutant> getMutants() {
		int[] attackers = getPlayerIds();
		return DatabaseAccess.getMutantsForAttackers(attackers);
	}

	public ArrayList<MultiplayerMutant> getAliveMutants() {
		ArrayList<MultiplayerMutant> aliveMutants = new ArrayList<>();
		for (MultiplayerMutant m : getMutants()) {
			if (m.isAlive() && m.getEquivalent().equals(Mutant.Equivalence.ASSUMED_NO) && (m.getClassFile() != null)) {
				aliveMutants.add(m);
			}
		}
		return aliveMutants;
	}

	public ArrayList<MultiplayerMutant> getKilledMutants() {
		ArrayList<MultiplayerMutant> killedMutants = new ArrayList<>();
		for (MultiplayerMutant m : getMutants()) {
			if (!m.isAlive() && (m.getClassFile() != null)) {
				killedMutants.add(m);
			}
		}
		return killedMutants;
	}

	public ArrayList<MultiplayerMutant> getMutantsMarkedEquivalent() {
		ArrayList<MultiplayerMutant> equivMutants = new ArrayList<>();
		for (MultiplayerMutant m : getMutants()) {
			if (!m.getEquivalent().equals(ASSUMED_NO) && !m.getEquivalent().equals(PROVEN_NO)) {
				equivMutants.add(m);
			}
		}
		return equivMutants;
	}

	public MultiplayerMutant getMutantByID(int mutantID) {
		for (MultiplayerMutant m : getMutants()) {
			if (m.getId() == mutantID)
				return m;
		}
		return null;
	}

	public ArrayList<Test> getTests() {
		return getExecutableTests();
	}

	public ArrayList<Test> getExecutableTests() {
		ArrayList<Test> allTests = new ArrayList<>();
		int[] defenders = getPlayerIds();
		for (int i = 0; i < defenders.length; i++){
			ArrayList<Test> tests = DatabaseAccess.getExecutableTestsForMultiplayerGame(defenders[i]);
			allTests.addAll(tests);
		}
		return allTests;
	}

	public int[] getDefenderIds(){
		return DatabaseAccess.getDefendersForMultiplayerGame(getId());
	}

	public int[] getAttackerIds(){
		return DatabaseAccess.getAttackersForMultiplayerGame(getId());
	}

	public int[] getPlayerIds() { return ArrayUtils.addAll(getDefenderIds(), getAttackerIds());}

	public boolean addUserAsAttacker(int userId) {
		if (status != Status.FINISHED && (defenderLimit == 0 || getDefenderIds().length < defenderLimit)) {
			String sql = String.format("INSERT INTO players " +
							"(Game_ID, User_ID, Points, Role) VALUES " +
							"(%d, %d, 0, '%s');",
					id, userId, Participance.ATTACKER);

			return runStatement(sql);
		}
		return false;
	}

	public boolean addUserAsDefender(int userId) {
		if (status != Status.FINISHED && (attackerLimit == 0 || getAttackerIds().length < attackerLimit)) {
			String sql = String.format("INSERT INTO players " +
							"(Game_ID, User_ID, Points, Role) VALUES " +
							"(%d, %d, 0, '%s');",
					id, userId, Participance.DEFENDER);

			return runStatement(sql);
		}
		return false;
	}

	public boolean runStatement(String sql) {

		Connection conn = null;
		Statement stmt = null;

		System.out.println(sql);

		// Attempt to insert game info into database
		try {
			conn = DatabaseAccess.getConnection();

			stmt = conn.createStatement();
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
			//finally block used to close resources
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
			}// nothing we can do

			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				System.out.println(se);
			}//end finally try
		} //end try

		return false;
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
					"(Class_ID, Level, Price, Defender_Value, Attacker_Value, Coverage_Goal, Mutant_Goal, Creator_ID, " +
					"Attackers_Needed, Defenders_Needed, Attackers_Limit, Defenders_Limit, Finish_Time, Status, Mode) VALUES " +
					"('%s', 	'%s', '%f', 	'%d',			'%d',			'%f',			'%f',		'%d'," +
					"'%d',				'%d',				'%d',			'%d',			'%d',		'%s', 'PARTY');",
					classId, level.name(), price, defenderValue, attackerValue, lineCoverage, mutantCoverage, creatorId,
					minAttackers, minDefenders, attackerLimit, defenderLimit, finishTime, status.name());

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
			//finally block used to close resources
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
			}// nothing we can do

			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				System.out.println(se);
			}//end finally try
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
						"Class_ID = '%s', Level = '%s', Price = %f, Defender_Value=%d, Attacker_Value=%d, Coverage_Goal=%f" +
						", Mutant_Goal=%f, Status='%s' WHERE ID='%d'",
						classId, level.name(), price, defenderValue, attackerValue, lineCoverage, mutantCoverage, status.name(), id);
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
			//finally block used to close resources
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
			}// nothing we can do

			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			}//end finally try
		} //end try

		return false;
	}

	public Participance getParticipance(int userId){
		return DatabaseAccess.getParticipance(userId, getId());
	}

	public HashMap<Integer, Integer> getMutantScores(){
		HashMap<Integer, Integer> mutantScores = new HashMap<Integer, Integer>();

		ArrayList<MultiplayerMutant> allMutants = new ArrayList<MultiplayerMutant>();
		allMutants.addAll(getAliveMutants());
		allMutants.addAll(getKilledMutants());
		for (MultiplayerMutant mm : getMutantsMarkedEquivalent()){
			if (!mm.getEquivalent().equals(Mutant.Equivalence.DECLARED_YES) && !mm.getEquivalent().equals(Mutant.Equivalence.ASSUMED_YES)){
				allMutants.add(mm);
			}
		}


		for (MultiplayerMutant mm : allMutants){
			if (!mutantScores.containsKey(mm.getPlayerId())){
				mutantScores.put(mm.getPlayerId(), 0);
			}

			mutantScores.put(mm.getPlayerId(), mutantScores.get(mm.getPlayerId())+mm.getScore());
		}
		return mutantScores;
	}

	public HashMap<Integer, Integer> getTestScores(){
		HashMap<Integer, Integer> testScores = new HashMap<Integer, Integer>();

		for (Test tt : getTests()){
			if (!testScores.containsKey(tt.getPlayerId())){
				testScores.put(tt.getPlayerId(), 0);
			}
			testScores.put(tt.getPlayerId(), testScores.get(tt.getPlayerId()) + tt.getScore());
		}

		return testScores;
	}

}
