package org.codedefenders;

import org.codedefenders.util.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.codedefenders.Mutant.Equivalence.ASSUMED_NO;
import static org.codedefenders.Mutant.Equivalence.PROVEN_NO;

/**
 * Created by jmr on 13/07/2016.
 */
public abstract class AbstractGame {
	protected static final Logger logger = LoggerFactory.getLogger(AbstractGame.class);
	protected int id;
	protected int classId;
	protected int creatorId;
	protected GameState state;
	protected GameLevel level;
	protected GameMode mode;

	public int getId() {
		return id;
	}

	public int getClassId() {
		return classId;
	}

	public String getClassName() {
		return DatabaseAccess.getClassForKey("Class_ID", classId).getName();
	}

	public GameClass getCUT() {
		return DatabaseAccess.getClassForKey("Class_ID", classId);
	}

	public int getCreatorId() {
		return creatorId;
	}

	public GameState getState() {
		return state;
	}

	public void setState(GameState s) {
		state = s;
	}

	public GameLevel getLevel() {
		return this.level;
	}

	public void setLevel(GameLevel level) {
		this.level = level;
	}

	public GameMode getMode() {
		return this.mode;
	}

	protected void setMode(GameMode newMode) { this.mode = newMode; }

	// TODO:
	// public abstract ArrayList<Mutant> getMutants();

	public ArrayList<Test> getTests() {
		return getTests(false);
	}

	public ArrayList<Test> getTests(boolean defendersOnly) {
		return DatabaseAccess.getExecutableTests(this.id, defendersOnly);
	}

	public List<Mutant> getMutants() {
		return DatabaseAccess.getMutantsForGame(id);
	}

	public List<Mutant> getAliveMutants() {
		return getMutants().stream().filter(mutant -> mutant.isAlive() &&
				mutant.getEquivalent().equals(Mutant.Equivalence.ASSUMED_NO) &&
				mutant.getClassFile() != null).collect(Collectors.toList());
	}

	public List<Mutant> getKilledMutants() {
		return getMutants().stream().filter(mutant -> !mutant.isAlive() &&
				(mutant.getEquivalent().equals(ASSUMED_NO) || mutant.getEquivalent().equals(PROVEN_NO)) &&
				(mutant.getClassFile() != null)).collect(Collectors.toList());
	}

	public Mutant getMutantByID(int mutantID) {
		for (Mutant m : getMutants()) {
			if (m.getId() == mutantID)
				return m;
		}
		return null;
	}


	public abstract boolean addPlayer(int userId, Role role);

	public Role getRole(int userId){
		return DatabaseAccess.getRole(userId, getId());
	}

	public abstract boolean insert();

	public abstract boolean update();

	public boolean runStatement(String sql) {

		Connection conn = null;
		Statement stmt = null;

		logger.info(sql);

		// Attempt to insert game info into database
		try {
			conn = DatabaseAccess.getConnection();

			stmt = conn.createStatement();
			return stmt.executeUpdate(sql) > 0;
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

}
