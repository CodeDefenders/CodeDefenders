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
package org.codedefenders.game.multiplayer;

import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.MultiplayerGameDAO;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.database.UserDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.model.Player;
import org.codedefenders.model.User;
import org.codedefenders.validation.code.CodeValidatorLevel;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

import static org.codedefenders.game.Mutant.Equivalence.ASSUMED_YES;
import static org.codedefenders.game.Mutant.Equivalence.DECLARED_YES;
import static org.codedefenders.game.Mutant.Equivalence.PENDING_TEST;
import static org.codedefenders.game.Mutant.Equivalence.PROVEN_NO;

public class MultiplayerGame extends AbstractGame {
    /*
    Inherited from AbstractGame

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
    */
    protected List<Player> attackers;
    protected List<Player> defenders;
    protected int defenderValue;
    protected int attackerValue;
    protected float lineCoverage;
    protected float mutantCoverage;
    protected float prize;

    protected boolean requiresValidation;
    protected int maxAssertionsPerTest;
    protected boolean forceHamcrest;

    protected boolean chatEnabled;
    protected CodeValidatorLevel mutantValidatorLevel;

    protected boolean capturePlayersIntention;

    // We need a temporary location where to store information about system tests
    // and mutants
    protected boolean withTests;
    protected boolean withMutants;

    // 0 means disabled
    protected int automaticMutantEquivalenceThreshold = 0;

    public static class Builder<T extends Builder<T>>  {
        // mandatory values
        private final int classId;
        private final int creatorId;
        private final int maxAssertionsPerTest;
        private final boolean forceHamcrest;

        // optional values with default values
        private GameClass cut = null;
        private List<Player> attackers = null;
        private List<Player> defenders = null;
        private int id = -1;
        private boolean requiresValidation = false;
        private boolean capturePlayersIntention = false;
        private boolean chatEnabled = false;
        private float lineCoverage = 1f;
        private float mutantCoverage = 1f;
        private float prize = 1f;
        private int defenderValue = 100;
        private int attackerValue = 100;
        private GameState state = GameState.CREATED;
        private GameLevel level = GameLevel.HARD;
        private CodeValidatorLevel mutantValidatorLevel = CodeValidatorLevel.STRICT;

        private boolean withTests = false;
        private boolean withMutants = false;

        private int automaticMutantEquivalenceThreshold = 0;

        public Builder(int classId, int creatorId, int maxAssertionsPerTest, boolean forceHamcrest) {
            this.classId = classId;
            this.creatorId = creatorId;
            this.maxAssertionsPerTest = maxAssertionsPerTest;
            this.forceHamcrest = forceHamcrest;
        }

		public T cut(GameClass cut) {
			this.cut = cut;
			return (T) this;
		}

		public T id(int id) {
			this.id = id;
			return (T) this;
		}

		public T requiresValidation(boolean requiresValidation) {
			this.requiresValidation = requiresValidation;
			return (T) this;
		}

		public T capturePlayersIntention(boolean capturePlayersIntention) {
			this.capturePlayersIntention = capturePlayersIntention;
			return (T) this;
		}

		public T chatEnabled(boolean chatEnabled) {
			this.chatEnabled = chatEnabled;
			return (T) this;
		}

		public T prize(float prize) {
			this.prize = prize;
			return (T) this;
		}

		public T lineCoverage(float lineCoverage) {
			this.lineCoverage = lineCoverage;
			return (T) this;
		}

		public T mutantCoverage(float mutantCoverage) {
			this.mutantCoverage = mutantCoverage;
			return (T) this;
		}

		public T defenderValue(int defenderValue) {
			this.defenderValue = defenderValue;
			return (T) this;
		}

		public T attackerValue(int attackerValue) {
			this.attackerValue = attackerValue;
			return (T) this;
		}

		public T state(GameState state) {
			this.state = state;
			return (T) this;
		}

		public T level(GameLevel level) {
			this.level = level;
			return (T) this;
		}

		public T mutantValidatorLevel(CodeValidatorLevel mutantValidatorLevel) {
			this.mutantValidatorLevel = mutantValidatorLevel;
			return (T) this;
		}

		public T attackers(List<Player> attackers) {
			this.attackers = attackers;
			return (T) this;
		}

		public T defenders(List<Player> defenders) {
			this.defenders = defenders;
			return (T) this;
		}

		public T withTests(boolean withTests) {
			this.withTests = withTests;
			return (T) this;
		}

		public T withMutants(boolean withMutants) {
			this.withMutants = withMutants;
			return (T) this;
		}

		public T automaticMutantEquivalenceThreshold(int threshold) {
			this.automaticMutantEquivalenceThreshold = threshold;
			return (T) this;
		}

		public MultiplayerGame build() {
			return new MultiplayerGame(this);
		}
	}

    protected MultiplayerGame(Builder builder) {
        this.mode = GameMode.PARTY;

        this.cut = builder.cut;
        this.attackers = builder.attackers;
        this.defenders = builder.defenders;
        this.id = builder.id;
        this.classId = builder.classId;
        this.creatorId = builder.creatorId;
        this.state = builder.state;
        this.level = builder.level;
        this.defenderValue = builder.defenderValue;
        this.attackerValue = builder.attackerValue;
        this.lineCoverage = builder.lineCoverage;
        this.mutantCoverage = builder.mutantCoverage;
        this.prize = builder.prize;
        this.requiresValidation = builder.requiresValidation;
        this.maxAssertionsPerTest = builder.maxAssertionsPerTest;
        this.forceHamcrest = builder.forceHamcrest;
        this.chatEnabled = builder.chatEnabled;
        this.mutantValidatorLevel = builder.mutantValidatorLevel;
        this.capturePlayersIntention = builder.capturePlayersIntention;

        // This is mostly a temporary patch
        this.withMutants = builder.withMutants;
        this.withTests = builder.withTests;

        this.automaticMutantEquivalenceThreshold = builder.automaticMutantEquivalenceThreshold;
    }

    public boolean hasSystemTests() {
        return this.withTests;
    }

    public boolean hasSystemMutants() {
        return this.withMutants;
    }

    public int getDefenderValue() {
        return defenderValue;
    }

    public int getAttackerValue() {
        return attackerValue;
    }

    public float getLineCoverage() {
        return lineCoverage;
    }

    public float getMutantCoverage() {
        return mutantCoverage;
    }

    public float getPrize() {
        return prize;
    }

    public void setPrize(float prize) {
        this.prize = prize;
    }

    public boolean isChatEnabled() {
        return chatEnabled;
    }

    public int getMaxAssertionsPerTest() {
        return maxAssertionsPerTest;
    }

    public boolean isForceHamcrest() {
        return forceHamcrest;
    }

    public CodeValidatorLevel getMutantValidatorLevel() {
        return mutantValidatorLevel;
    }

    public boolean isCapturePlayersIntention() {
        return capturePlayersIntention;
    }

    public int getAutomaticMutantEquivalenceThreshold() {
        return automaticMutantEquivalenceThreshold;
    }

    public Role getRole(int userId) {
        if (getDefenderPlayers().stream().anyMatch(player -> player.getUser().getId() == userId)) {
            return Role.DEFENDER;
        } else if (getAttackerPlayers().stream().anyMatch(player -> player.getUser().getId() == userId)) {
            return Role.ATTACKER;
        } else if (userId == getCreatorId()) {
            return Role.OBSERVER;
        } else {
            return Role.NONE;
        }
    }

    public List<Player> getDefenderPlayers() {
        if (defenders == null) {
            defenders = GameDAO.getPlayersForGame(getId(), Role.DEFENDER);
        }
        return defenders;
    }

    public List<Player> getAttackerPlayers() {
        if (attackers == null) {
            attackers = GameDAO.getPlayersForGame(getId(), Role.ATTACKER);
        }
        return attackers;
    }

    public boolean addPlayer(int userId, Role role) {
        return canJoinGame(userId) && addPlayerForce(userId, role);
    }

    public boolean addPlayerForce(int userId, Role role) {
        if (state == GameState.FINISHED) {
            return false;
        }
        if (!GameDAO.addPlayerToGame(id, userId, role)) {
            return false;
        }
        User u = UserDAO.getUserById(userId);
        EventType et = role == Role.ATTACKER ? EventType.ATTACKER_JOINED : EventType.DEFENDER_JOINED;
        final Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        Event e = new Event(-1, id, userId, u.getUsername() + " joined the game as " + role,
                et, EventStatus.GAME, timestamp);
        e.insert();

        Event notif = new Event(-1, id, userId, "You joined a game as " + role, et, EventStatus.NEW, timestamp);
        notif.insert();

        return true;
    }

    public boolean removePlayer(int userId) {
        if (state == GameState.CREATED) {
            return GameDAO.removeUserFromGame(id, userId);
        }
        return false;
    }

    protected boolean canJoinGame(int userId) {
        return !requiresValidation || UserDAO.getUserById(userId).isValidated();
    }

    public boolean insert() {
        try {
            this.id = MultiplayerGameDAO.storeMultiplayerGame(this);
            return true;
        } catch (UncheckedSQLException e) {
            logger.error("Failed to store multiplayer game to database.", e);
            return false;
        }
    }

    public boolean update() {
        return MultiplayerGameDAO.updateMultiplayerGame(this);
    }

    /**
     * This method calculates the mutant score for every attacker in the game.
     *
     * <p>The result is a mapping of playerId to player score.
     *
     * @return mapping from playerId to player score.
     */
    public HashMap<Integer, PlayerScore> getMutantScores() {
        final HashMap<Integer, PlayerScore> mutantScores = new HashMap<>();

        final HashMap<Integer, Integer> mutantsAlive = new HashMap<Integer, Integer>();
        final HashMap<Integer, Integer> mutantsKilled = new HashMap<Integer, Integer>();
        final HashMap<Integer, Integer> mutantsEquiv = new HashMap<Integer, Integer>();
        final HashMap<Integer, Integer> mutantsChallenged = new HashMap<Integer, Integer>();
        final HashMap<Integer, Integer> duelsWon = new HashMap<Integer, Integer>();

        // TODO why not getMutants()
        List<Mutant> allMutants = getAliveMutants();
        allMutants.addAll(getKilledMutants());
        allMutants.addAll(getMutantsMarkedEquivalent());
        allMutants.addAll(getMutantsMarkedEquivalentPending());

        if (!mutantScores.containsKey(-1)) {
            mutantScores.put(-1, new PlayerScore(-1));
            mutantsAlive.put(-1, 0);
            mutantsEquiv.put(-1, 0);
            mutantsChallenged.put(-1, 0);
            mutantsKilled.put(-1, 0);
            duelsWon.put(-1, 0);
        }

        for (Mutant mm : allMutants) {

            if (!mutantScores.containsKey(mm.getPlayerId())) {
                mutantScores.put(mm.getPlayerId(), new PlayerScore(mm.getPlayerId()));
                mutantsAlive.put(mm.getPlayerId(), 0);
                mutantsEquiv.put(mm.getPlayerId(), 0);
                mutantsChallenged.put(mm.getPlayerId(), 0);
                mutantsKilled.put(mm.getPlayerId(), 0);
                duelsWon.put(mm.getPlayerId(), 0);
            }

            PlayerScore ps = mutantScores.get(mm.getPlayerId());
            ps.increaseQuantity();
            ps.increaseTotalScore(mm.getScore());

            PlayerScore ts = mutantScores.get(-1);
            ts.increaseQuantity();
            ts.increaseTotalScore(mm.getScore());

            if (mm.getEquivalent().equals(ASSUMED_YES) || mm.getEquivalent().equals(DECLARED_YES)) {
                mutantsEquiv.put(mm.getPlayerId(), mutantsEquiv.get(mm.getPlayerId()) + 1);
                mutantsEquiv.put(-1, mutantsEquiv.get(-1) + 1);
            } else if (mm.isAlive()) {
                //This includes mutants marked equivalent
                mutantsAlive.put(mm.getPlayerId(), mutantsAlive.get(mm.getPlayerId()) + 1);
                mutantsAlive.put(-1, mutantsAlive.get(-1) + 1);
                if (mm.getEquivalent().equals(PENDING_TEST)) {
                    mutantsChallenged.put(mm.getPlayerId(), mutantsChallenged.get(mm.getPlayerId()) + 1);
                    mutantsChallenged.put(-1, mutantsChallenged.get(-1) + 1);
                }
            } else {
                mutantsKilled.put(mm.getPlayerId(), mutantsKilled.get(mm.getPlayerId()) + 1);
                mutantsKilled.put(-1, mutantsKilled.get(-1) + 1);
                if (mm.getEquivalent().equals(PROVEN_NO)) {
                    duelsWon.put(mm.getPlayerId(), duelsWon.get(mm.getPlayerId()) + 1);
                    duelsWon.put(-1, duelsWon.get(-1) + 1);
                    // Actually adds points for the player and the team
                    ps.increaseTotalScore(1);
                    ts.increaseTotalScore(1);
                }
            }

        }

        for (int i : mutantsKilled.keySet()) {
            PlayerScore ps = mutantScores.get(i);
            ps.setMutantKillInformation(
                    mutantsAlive.get(i) + " / " + mutantsKilled.get(i) + " / " + mutantsEquiv.get((i))
            );
            ps.setDuelInformation(
                    duelsWon.get(i) + " / " + mutantsEquiv.get(i) + " / " + mutantsChallenged.get((i))
            );

        }

        return mutantScores;
    }

    /**
     * This method calculates the test score for every defender in the game.
     *
     * <p>The result is a mapping of playerId to player score.
     *
     * @return mapping from playerId to player score.
     */
    public HashMap<Integer, PlayerScore> getTestScores() {
        final HashMap<Integer, PlayerScore> testScores = new HashMap<>();
        final HashMap<Integer, Integer> mutantsKilled = new HashMap<>();

        final HashMap<Integer, Integer> challengesOpen = new HashMap<>();
        final HashMap<Integer, Integer> challengesWon = new HashMap<>();
        final HashMap<Integer, Integer> challengesLost = new HashMap<>();

        int defendersTeamId = -1;
        testScores.put(defendersTeamId, new PlayerScore(defendersTeamId));
        mutantsKilled.put(defendersTeamId, 0);
        challengesOpen.put(defendersTeamId, 0);
        challengesWon.put(defendersTeamId, 0);
        challengesLost.put(defendersTeamId, 0);

        for (Player player : getDefenderPlayers()) {
            int defenderId = player.getId();
            testScores.put(defenderId, new PlayerScore(defenderId));
            mutantsKilled.put(defenderId, 0);
            challengesOpen.put(defenderId, 0);
            challengesWon.put(defenderId, 0);
            challengesLost.put(defenderId, 0);
        }

        for (Test test : getTests()) {
            if (getAttackerPlayers().stream().anyMatch(p -> p.getId() == test.getPlayerId())) {
                continue;
            }
            if (!testScores.containsKey(test.getPlayerId())) {
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
            mutantsKilled.put(teamKey, mutantsKilled.get(teamKey) + test.getMutantsKilled());

        }

        for (int playerId : mutantsKilled.keySet()) {
            if (playerId < 0 || getAttackerPlayers().stream().anyMatch(p -> p.getId() == playerId)) {
                continue;
            }
            int teamKey = defendersTeamId;

            PlayerScore ps = testScores.get(playerId);
            int playerScore = DatabaseAccess.getPlayerPoints(playerId);
            ps.increaseTotalScore(playerScore);

            PlayerScore ts = testScores.get(teamKey);
            ts.increaseTotalScore(playerScore);
        }

        for (Mutant m : getKilledMutants()) {
            if (!m.getEquivalent().equals(PROVEN_NO)) {
                continue;
            }

            int defenderId = DatabaseAccess.getEquivalentDefenderId(m);
            challengesLost.put(defenderId, challengesLost.get(defenderId) + 1);
            if (defenderId != defendersTeamId) {
                challengesLost.put(defendersTeamId, challengesLost.get(defendersTeamId) + 1);
            }
        }
        for (Mutant m : getMutantsMarkedEquivalent()) {
            int defenderId = DatabaseAccess.getEquivalentDefenderId(m);
            challengesWon.put(defenderId, challengesWon.get(defenderId) + 1);
            if (defenderId != defendersTeamId) {
                challengesWon.put(defendersTeamId, challengesWon.get(defendersTeamId) + 1);
            }
        }
        for (Mutant m : getMutantsMarkedEquivalentPending()) {
            int defenderId = DatabaseAccess.getEquivalentDefenderId(m);
            challengesOpen.put(defenderId, challengesOpen.get(defenderId) + 1);
            if (defenderId != defendersTeamId) {
                challengesOpen.put(defendersTeamId, challengesOpen.get(defendersTeamId) + 1);
            }
        }

        for (int playerId : testScores.keySet()) {
            testScores.get(playerId).setDuelInformation(challengesWon.get(playerId)
                    + " / " + challengesLost.get(playerId) + " / " + challengesOpen.get((playerId)));
        }
        testScores.get(defendersTeamId).setDuelInformation(challengesWon.get(defendersTeamId)
                + " / " + challengesLost.get(defendersTeamId) + " / " + challengesOpen.get((defendersTeamId)));
        for (int i : mutantsKilled.keySet()) {
            PlayerScore ps = testScores.get(i);
            ps.setMutantKillInformation("" + mutantsKilled.get(i));
        }

        return testScores;
    }

    public boolean isLineCovered(int lineNumber) {
        for (Test test : getTests(true)) {
            if (test.getLineCoverage().getLinesCovered().contains(lineNumber)) {
                return true;
            }
        }
        return false;
    }

    public void notifyPlayers() {
        List<Event> events = getEvents();

        switch (state) {
            case ACTIVE:
                if (!listContainsEvent(events, EventType.GAME_STARTED)) {
                    EventType et = EventType.GAME_STARTED;
                    notifyAttackers("Game has started. Attack now!", et);
                    notifyDefenders("Game has started. Defend now!", et);
                    notifyCreator("Your game as started!", et);
                    notifyGame("The game has started!", et);
                }
                break;
            case GRACE_ONE:
                if (!listContainsEvent(events, EventType.GAME_GRACE_ONE)) {
                    EventType et = EventType.GAME_GRACE_ONE;
                    notifyAttackers("A game has entered Grace One.", et);
                    notifyDefenders("A game has entered Grace One.", et);
                    notifyCreator("Your game has entered Grace One", et);
                    notifyGame("The game as entered Grace Period One", et);
                }
                break;
            case GRACE_TWO:
                if (!listContainsEvent(events, EventType.GAME_GRACE_TWO)) {
                    EventType et = EventType.GAME_GRACE_TWO;
                    notifyAttackers("A game has entered Grace Two.", et);
                    notifyDefenders("A game has entered Grace Two.", et);
                    notifyCreator("Your game has entered Grace Two", et);
                    notifyGame("The game as entered Grace Period Two", et);
                }
                break;
            case FINISHED:
                if (!listContainsEvent(events, EventType.GAME_FINISHED)) {
                    EventType et = EventType.GAME_FINISHED;
                    notifyAttackers("A game has finished.", et);
                    notifyDefenders("A game has finished.", et);
                    notifyCreator("Your game has finished.", et);
                    notifyGame("The game has ended.", et);
                }
                break;
            default:
                // ignored
        }
    }

    private boolean listContainsEvent(List<Event> events, EventType et) {
        for (Event e : events) {
            if (e.getEventType().equals(et)) {
                return true;
            }
        }
        return false;
    }

    private void notifyAttackers(String message, EventType et) {
        for (Player player : getAttackerPlayers()) {
            Event notif = new Event(-1, id,
                    player.getUser().getId(),
                    message,
                    et, EventStatus.NEW,
                    new Timestamp(System.currentTimeMillis()));
            notif.insert();
        }
    }

    private void notifyDefenders(String message, EventType et) {
        for (Player player : getDefenderPlayers()) {
            Event notif = new Event(-1, id,
                    player.getUser().getId(),
                    message,
                    et, EventStatus.NEW,
                    new Timestamp(System.currentTimeMillis()));
            notif.insert();
        }
    }

    private void notifyCreator(String message, EventType et) {
        //Event for game log: started
        Event notif = new Event(-1, id,
                creatorId,
                message,
                et, EventStatus.NEW,
                new Timestamp(System.currentTimeMillis()));
        notif.insert();
    }

    private void notifyGame(String message, EventType et) {
        //Event for game log: started
        Event notif = new Event(-1, id,
                creatorId,
                message,
                et, EventStatus.GAME,
                new Timestamp(System.currentTimeMillis()));
        notif.insert();
    }

}
