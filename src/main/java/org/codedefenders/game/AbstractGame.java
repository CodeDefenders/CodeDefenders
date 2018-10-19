package org.codedefenders.game;

import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.game.duel.DuelGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.singleplayer.SinglePlayerGame;
import org.codedefenders.model.Event;
import org.codedefenders.database.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.codedefenders.game.Mutant.Equivalence.*;

/**
 * Abstract class for games of different modes.
 *
 * @see DuelGame
 * @see MultiplayerGame
 * @see SinglePlayerGame
 */
public abstract class AbstractGame {
	protected static final Logger logger = LoggerFactory.getLogger(AbstractGame.class);
	protected int id;
	protected int classId;
	protected int creatorId;
	protected GameState state;
	protected GameLevel level;
	protected GameMode mode;

	protected ArrayList<Event> events;
	protected List<Mutant> mutants;

	public abstract boolean addPlayer(int userId, Role role);

	public abstract boolean insert();

	public abstract boolean update();


	public int getId() {
		return id;
	}

	public int getClassId() {
		return classId;
	}

	public String getClassName() {
		return DatabaseAccess.getClassForKey("Class_ID", classId).getName();
	}

	public ArrayList<Event> getEvents(){
		if (events == null){
			events = DatabaseAccess.getEventsForGame(getId());
		}
		return events;
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

	public List<Test> getTests() {
		return getTests(false);
	}

	public List<Test> getTests(boolean defendersOnly) {
		return DatabaseAccess.getExecutableTests(this.id, defendersOnly);
	}

	public List<Mutant> getMutants() {
		// This ensures we're only querying the db once for mutants
		if (mutants == null){
			mutants = DatabaseAccess.getMutantsForGame(id);
		}
		return mutants;
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

	public List<Mutant> getMutantsMarkedEquivalent() {
		return getMutants().stream().filter(mutant -> mutant.getEquivalent().equals(ASSUMED_YES) ||
				mutant.getEquivalent().equals(DECLARED_YES)).collect(Collectors.toList());
	}

	public List<Mutant> getMutantsMarkedEquivalentPending() {
		return getMutants().stream().filter(mutant -> mutant.getEquivalent().equals(PENDING_TEST)).collect(Collectors.toList());
	}

	public Mutant getMutantByID(int mutantID) {
		for (Mutant m : getMutants()) {
			if (m.getId() == mutantID)
				return m;
		}
		return null;
	}

	public Role getRole(int userId){
		return DatabaseAccess.getRole(userId, getId());
	}
}
