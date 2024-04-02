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

import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.database.GameRepository;
import org.codedefenders.database.MutantRepository;
import org.codedefenders.database.TestRepository;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.model.Event;
import org.codedefenders.model.Player;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.validation.code.CodeValidatorLevel;
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

    protected boolean capturePlayersIntention = false;

    protected List<Player> observers;

    protected List<Event> events;
    protected List<Mutant> mutants;
    protected List<Test> tests;
    protected List<Test> testsDefendersOnly;
    /**
     * Validation level used to check submitted mutants.
     */
    protected CodeValidatorLevel mutantValidatorLevel;
    /**
     * Maximum number of allowed assertions per submitted test.
     */
    protected int maxAssertionsPerTest;

    public abstract boolean addPlayer(int userId, Role role);

    public abstract boolean insert();

    public abstract boolean update();

    public int getId() {
        return id;
    }

    public int getClassId() {
        return classId;
    }

    public List<Event> getEvents() {
        if (events == null) {
            EventDAO eventDAO = CDIUtil.getBeanFromCDI(EventDAO.class);
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

    public CodeValidatorLevel getMutantValidatorLevel() {
        return mutantValidatorLevel;
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

    public boolean isCapturePlayersIntention() {
        return capturePlayersIntention;
    }

    public abstract boolean isChatEnabled();

    protected void setMode(GameMode newMode) {
        this.mode = newMode;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Test> getTests() {
        return getTests(false);
    }

    @Deprecated// Get tests through service instead and cache them
    public List<Test> getTests(boolean defendersOnly) {
        TestRepository testRepo = CDIUtil.getBeanFromCDI(TestRepository.class);
        if (defendersOnly) {
            if (testsDefendersOnly == null) {
                testsDefendersOnly = testRepo.getValidDefenderTestsForGame(this.id);
            }
            return testsDefendersOnly;
        } else {
            if (tests == null) {
                tests = testRepo.getValidTestsForGame(this.id);
            }
            return tests;
        }
    }

    public List<Mutant> getMutants() {
        if (mutants == null) {
            MutantRepository mutantRepo = CDIUtil.getBeanFromCDI(MutantRepository.class);
            mutants = mutantRepo.getValidMutantsForGame(id);
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

    public int getMaxAssertionsPerTest() {
        return maxAssertionsPerTest;
    }

    public List<Player> getObserverPlayers() {
        if (observers == null) {
            GameRepository gameRepo = CDIUtil.getBeanFromCDI(GameRepository.class);
            observers = gameRepo.getPlayersForGame(getId(), Role.OBSERVER);
        }
        return observers;
    }
}
