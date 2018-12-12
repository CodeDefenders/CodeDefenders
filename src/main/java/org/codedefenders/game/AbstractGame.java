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

import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.game.duel.DuelGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.singleplayer.SinglePlayerGame;
import org.codedefenders.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.codedefenders.game.Mutant.Equivalence.ASSUMED_NO;
import static org.codedefenders.game.Mutant.Equivalence.ASSUMED_YES;
import static org.codedefenders.game.Mutant.Equivalence.DECLARED_YES;
import static org.codedefenders.game.Mutant.Equivalence.PENDING_TEST;
import static org.codedefenders.game.Mutant.Equivalence.PROVEN_NO;

/**
 * Abstract class for games of different modes.
 *
 * @see DuelGame
 * @see MultiplayerGame
 * @see SinglePlayerGame
 */
public abstract class AbstractGame {
	protected static final Logger logger = LoggerFactory.getLogger(AbstractGame.class);

	protected GameClass cut;

	protected int id;
	protected int classId;
	protected int creatorId;
	protected GameState state;
	protected GameLevel level;
	protected GameMode mode;

	protected ArrayList<Event> events;
	protected List<Mutant> mutants;
	protected List<Test> tests;

	public abstract boolean addPlayer(int userId, Role role);

	public abstract boolean insert();

	public abstract boolean update();


	public int getId() {
		return id;
	}

	public int getClassId() {
		return classId;
	}

	public ArrayList<Event> getEvents(){
		if (events == null){
			events = DatabaseAccess.getEventsForGame(getId());
		}
		return events;
	}

	public GameClass getCUT() {
		if (cut == null) {
			cut = GameClassDAO.getClassForId(classId);
		}
		return cut;
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
		if (tests == null) {
			tests = TestDAO.getValidTestsForGame(this.id, defendersOnly);
		}
		return tests;
	}

	public List<Mutant> getMutants() {
		if (mutants == null){
			mutants = MutantDAO.getValidMutantsForGame(id);
		}
		return mutants;
	}

	public List<Mutant> getAliveMutants() {
		return getMutants().stream().filter(mutant -> mutant.isAlive() &&
				mutant.getEquivalent().equals(Mutant.Equivalence.ASSUMED_NO) &&
				mutant.getClassFile() != null).collect(Collectors.toList());
	}

	public List<Mutant> getKilledMutants() {
		return getMutants()
				.stream()
				.filter(mutant -> !mutant.isAlive()
						&& (mutant.getEquivalent().equals(ASSUMED_NO)
						|| mutant.getEquivalent().equals(PROVEN_NO)) && (mutant.getClassFile() != null))
				.collect(Collectors.toList());
	}

	public List<Mutant> getMutantsMarkedEquivalent() {
		return getMutants()
				.stream()
				.filter(mutant -> mutant.getEquivalent().equals(ASSUMED_YES) || mutant.getEquivalent().equals(DECLARED_YES))
				.collect(Collectors.toList());
	}

	public List<Mutant> getMutantsMarkedEquivalentPending() {
		return getMutants()
				.stream()
				.filter(mutant -> mutant.getEquivalent().equals(PENDING_TEST))
				.collect(Collectors.toList());
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
