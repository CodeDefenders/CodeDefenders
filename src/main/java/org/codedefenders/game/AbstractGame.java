/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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

import java.util.List;
import java.util.stream.Collectors;

import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for games of different modes.
 *
 * @see MultiplayerGame
 * @see MeleeGame
 * @see PuzzleGame
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

    protected List<Event> events;
    protected List<Mutant> mutants;
    protected List<Test> tests;

    public abstract boolean addPlayer(int userId, Role role);

    public abstract boolean insert();

    public abstract boolean update();

    // TODO Dependency Injection. This suggests that AbstractGame might not be the right place to query for events
    // Consider to move this into a setEvents method instead !
    protected EventDAO eventDAO;
    public void setEventDAO(EventDAO eventDAO) {
        this.eventDAO = eventDAO;
    }
    
    public int getId() {
        return id;
    }

    public int getClassId() {
        return classId;
    }

    public List<Event> getEvents() {
        if (events == null) {
            events = eventDAO.getEventsForGame(getId());
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

    protected void setMode(GameMode newMode) {
        this.mode = newMode;
    }

    public List<Test> getTests() {
        return getTests(false);
    }

    // NOTE: I do not want to break compatibility so I define yet another method...
    public List<Test> getAllTests() {
        return TestDAO.getValidTestsForGame(this.id, false);
    }


    public List<Test> getTests(boolean defendersOnly) {
        // TODO Why we cache this result?
        if (tests == null) {
            tests = TestDAO.getValidTestsForGame(this.id, defendersOnly);
        }
        return tests;
    }

    public List<Mutant> getMutants() {
        if (mutants == null) {
            mutants = MutantDAO.getValidMutantsForGame(id);
        }
        return mutants;
    }

    public List<Mutant> getAliveMutants() {
        return getMutants().stream()
                .filter(mutant -> mutant.getState() == Mutant.State.ALIVE)
                .collect(Collectors.toList());
    }

    public List<Mutant> getKilledMutants() {
        return getMutants()
                .stream()
                .filter(mutant -> mutant.getState() == Mutant.State.KILLED)
                .collect(Collectors.toList());
    }

    public List<Mutant> getMutantsMarkedEquivalent() {
        return getMutants()
                .stream()
                .filter(mutant -> mutant.getState() == Mutant.State.EQUIVALENT)
                .collect(Collectors.toList());
    }

    public List<Mutant> getMutantsMarkedEquivalentPending() {
        return getMutants()
                .stream()
                .filter(mutant -> mutant.getState() == Mutant.State.FLAGGED)
                .collect(Collectors.toList());
    }

    public Mutant getMutantByID(int mutantId) {
        for (Mutant m : getMutants()) {
            if (m.getId() == mutantId) {
                return m;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "[gameId=" + id + ",classId=" + classId + ",state=" + state + ",mode=" + mode + "]";
    }

    public boolean isFinished() {
        return this.state == GameState.FINISHED || this.state == GameState.SOLVED || this.state == GameState.FAILED;
    }
}
