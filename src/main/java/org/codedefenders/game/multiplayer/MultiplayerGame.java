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

import org.apache.commons.lang.ArrayUtils;
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
import org.codedefenders.model.User;
import org.codedefenders.validation.code.CodeValidatorLevel;
import org.codedefenders.validation.input.CheckDateFormat;

import java.sql.Timestamp;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    private int defenderValue;
    private int attackerValue;
    private float lineCoverage;
    private float mutantCoverage;
    private float prize;
    private int attackerLimit;
    private int defenderLimit;
    private int minAttackers;
    private int minDefenders;

    @CheckDateFormat(patterns = {"yyyy/MM/dd HH:mm", "yyyy/MM/dd H:m", "yyyy/MM/dd HH:m", "yyyy/MM/dd H:mm"}, message = "Invalid date format for Start Time")
    private long startDateTime;

    @CheckDateFormat(patterns = {"yyyy/MM/dd HH:mm", "yyyy/MM/dd H:m", "yyyy/MM/dd HH:m", "yyyy/MM/dd H:mm"}, message = "Invalid date format for Finish Time")
    private long finishDateTime;
    private boolean requiresValidation;
    private int maxAssertionsPerTest;
    private boolean chatEnabled;
    private CodeValidatorLevel mutantValidatorLevel;
    private boolean markUncovered;

    private boolean capturePlayersIntention;

    // We need a temporary locatio where to store information about system tests
    // and mutants
    private boolean withTests;
    private boolean withMutants;

    private static final Format format = new SimpleDateFormat("yy/MM/dd HH:mm");

    public static class Builder {
        // mandatory values
        private final int classId;
        private final int creatorId;
        private final long startDateTime;
        private final long finishDateTime;
        private final int maxAssertionsPerTest;
        private final int defenderLimit;
        private final int attackerLimit;
        private final int minimumDefenders;
        private final int minimumAttackers;

        // optional values with default values
        private GameClass cut = null;
        private int id = -1;
        private boolean requiresValidation = false;
        private boolean capturePlayersIntention = false;
        private boolean chatEnabled = false;
        private boolean markUncovered = false;
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

        public Builder(int classId, int creatorId, long startDateTime, long finishDateTime, int maxAssertionsPerTest,
                       int defenderLimit, int attackerLimit, int minimumDefenders, int minimumAttackers) {
            this.classId = classId;
            this.creatorId = creatorId;
            this.startDateTime = startDateTime;
            this.finishDateTime = finishDateTime;
            this.maxAssertionsPerTest = maxAssertionsPerTest;
            this.defenderLimit = defenderLimit;
            this.attackerLimit = attackerLimit;
            this.minimumDefenders = minimumDefenders;
            this.minimumAttackers = minimumAttackers;
        }

        public Builder cut(GameClass cut) { this.cut = cut; return this; }
        public Builder id(int id) { this.id = id; return this; }
        public Builder requiresValidation(boolean requiresValidation) { this.requiresValidation = requiresValidation; return this; }
        public Builder capturePlayersIntention(boolean capturePlayersIntention) { this.capturePlayersIntention = capturePlayersIntention; return this; }
        public Builder chatEnabled(boolean chatEnabled) { this.chatEnabled = chatEnabled; return this; }
        public Builder markUncovered(boolean markUncovered) { this.markUncovered = markUncovered; return this; }
        public Builder prize(float prize) { this.prize = prize; return this; }
        public Builder lineCoverage(float lineCoverage) { this.lineCoverage = lineCoverage; return this; }
        public Builder mutantCoverage(float mutantCoverage) { this.mutantCoverage = mutantCoverage; return this; }
        public Builder defenderValue(int defenderValue) { this.defenderValue = defenderValue; return this; }
        public Builder attackerValue(int attackerValue) { this.attackerValue = attackerValue; return this; }
        public Builder state(GameState state) { this.state = state; return this; }
        public Builder level(GameLevel level) { this.level = level; return this; }
        public Builder mutantValidatorLevel(CodeValidatorLevel mutantValidatorLevel) { this.mutantValidatorLevel = mutantValidatorLevel; return this; }

        public Builder withTests(boolean withTests) { this.withTests = withTests; return this; }
        public Builder withMutants(boolean withMutants) { this.withMutants = withMutants; return this; }

        public MultiplayerGame build() {
            return new MultiplayerGame(this);
        }
    }

    private MultiplayerGame(Builder builder) {
        this.mode = GameMode.PARTY;

        this.cut = builder.cut;
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
        this.attackerLimit = builder.attackerLimit;
        this.defenderLimit = builder.defenderLimit;
        this.minAttackers = builder.minimumAttackers;
        this.minDefenders = builder.minimumDefenders;
        this.startDateTime = builder.startDateTime;
        this.finishDateTime = builder.finishDateTime;
        this.requiresValidation = builder.requiresValidation;
        this.maxAssertionsPerTest = builder.maxAssertionsPerTest;
        this.chatEnabled = builder.chatEnabled;
        this.mutantValidatorLevel = builder.mutantValidatorLevel;
        this.markUncovered = builder.markUncovered;
        this.capturePlayersIntention = builder.capturePlayersIntention;

        // This is mostly a temporary patch
        this.withMutants = builder.withMutants;
        this.withTests = builder.withTests;
    }

    public boolean hasSystemTests() {
        return this.withTests;
    }

    public boolean hasSystemMutants() {
        return this.withMutants;
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
        if (this.state != GameState.FINISHED && finishDateTime < System.currentTimeMillis()) {
            this.state = GameState.FINISHED;
            update();
        }
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

    public boolean isMarkUncovered() {
        return markUncovered;
    }

    public CodeValidatorLevel getMutantValidatorLevel() {
        return mutantValidatorLevel;
    }

    public boolean isCapturePlayersIntention() {
        return capturePlayersIntention;
    }

    public long getStartDateTime() {
        return startDateTime;
    }

    public long getFinishDateTime() {
        return finishDateTime;
    }

    public String getFormattedStartDateTime() {
        Date date = new Date(startDateTime);
        return format.format(date);
    }

    public String getFormattedFinishDateTime() {
        Date date = new Date(finishDateTime);
        return format.format(date);
    }

    /**
     * This returns the ID of the Player not of the User
     *
     * @return
     */
    public int[] getDefenderIds() {
        // TODO Phil 27/12/18: improve this. Make getDefenderIds return List<Integer> and rename the method to 'getDefenderPlayerIds'. No need to have an array here
        return GameDAO.getPlayersForGame(getId(), Role.DEFENDER).stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * This returns the ID of the Player not of the User
     *
     * @return
     */
    public int[] getAttackerIds() {
        return GameDAO.getPlayersForGame(getId(), Role.ATTACKER).stream().mapToInt(Integer::intValue).toArray();
    }

    public boolean addPlayer(int userId, Role role) {
        return canJoinGame(userId, role) && addPlayerForce(userId, role);
    }

    public boolean addPlayerForce(int userId, Role role) {
        if (state == GameState.FINISHED) {
            return false;
        }
        if (!GameDAO.addPlayerToGame(id, userId, role)) {
            return false;
        }
        User u = UserDAO.getUserById(userId);
        EventType et = role.equals(Role.ATTACKER) ? EventType.ATTACKER_JOINED : EventType.DEFENDER_JOINED;
        final Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        Event e = new Event(-1, id, userId, u.getUsername() + " joined the game as " + role,
                et, EventStatus.GAME,
                timestamp);
        e.insert();

        EventType notifType = role.equals(Role.ATTACKER) ?
                EventType.ATTACKER_JOINED : EventType.DEFENDER_JOINED;
        Event notif = new Event(-1, id, userId, "You joined a game as " + role,
                notifType, EventStatus.NEW,
                timestamp);
        notif.insert();

        return true;
    }

    public boolean removePlayer(int userId) {
        if (state == GameState.CREATED) {
            return GameDAO.removeUserFromGame(id, userId);
        }
        return false;
    }

    private boolean canJoinGame(int userId, Role role) {
        if (!requiresValidation || UserDAO.getUserById(userId).isValidated()) {
            if (role.equals(Role.ATTACKER))
                return (attackerLimit == 0 || getAttackerIds().length < attackerLimit);
            else
                return (defenderLimit == 0 || getDefenderIds().length < defenderLimit);
        } else {
            return false;
        }
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

    public HashMap<Integer, PlayerScore> getMutantScores() {
        HashMap<Integer, PlayerScore> mutantScores = new HashMap<Integer, PlayerScore>();

        HashMap<Integer, Integer> mutantsAlive = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> mutantsKilled = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> mutantsEquiv = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> mutantsChallenged = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> duelsWon = new HashMap<Integer, Integer>();

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
                }
            }

        }

        for (int i : mutantsKilled.keySet()) {
            PlayerScore ps = mutantScores.get(i);
            ps.setMutantKillInformation(mutantsAlive.get(i) + " / " + mutantsKilled.get(i) + " / " + mutantsEquiv.get((i)));
            ps.setDuelInformation(duelsWon.get(i) + " / " + mutantsEquiv.get(i) + " / " + mutantsChallenged.get((i)));

        }

        return mutantScores;
    }

    public HashMap<Integer, PlayerScore> getTestScores() {
        HashMap<Integer, PlayerScore> testScores = new HashMap<>();
        HashMap<Integer, Integer> mutantsKilled = new HashMap<>();

        HashMap<Integer, Integer> challengesOpen = new HashMap<>();
        HashMap<Integer, Integer> challengesWon = new HashMap<>();
        HashMap<Integer, Integer> challengesLost = new HashMap<>();

        int defendersTeamId = -1;
        testScores.put(defendersTeamId, new PlayerScore(defendersTeamId));
        mutantsKilled.put(defendersTeamId, 0);
        challengesOpen.put(defendersTeamId, 0);
        challengesWon.put(defendersTeamId, 0);
        challengesLost.put(defendersTeamId, 0);

        for (int defenderId : getDefenderIds()) {
            testScores.put(defenderId, new PlayerScore(defenderId));
            mutantsKilled.put(defenderId, 0);
            challengesOpen.put(defenderId, 0);
            challengesWon.put(defenderId, 0);
            challengesLost.put(defenderId, 0);
        }

        int[] attackers = getAttackerIds();
        for (Test test : getTests()) {
            if (ArrayUtils.contains(attackers, test.getPlayerId()))
                continue;
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
            if (playerId < 0 || ArrayUtils.contains(attackers, playerId))
                continue;

            int teamKey = defendersTeamId;

            PlayerScore ps = testScores.get(playerId);
            int playerScore = DatabaseAccess.getPlayerPoints(playerId);
            ps.increaseTotalScore(playerScore);

            PlayerScore ts = testScores.get(teamKey);
            ts.increaseTotalScore(playerScore);
        }

        for (Mutant m : getKilledMutants()) {
            if (!m.getEquivalent().equals(PROVEN_NO))
                continue;

            int defenderId = DatabaseAccess.getEquivalentDefenderId(m);
            challengesLost.put(defenderId, challengesLost.get(defenderId) + 1);
            challengesLost.put(defendersTeamId, challengesLost.get(defendersTeamId) + 1);
        }
        for (Mutant m : getMutantsMarkedEquivalent()) {
            int defenderId = DatabaseAccess.getEquivalentDefenderId(m);
            challengesWon.put(defenderId, challengesWon.get(defenderId) + 1);
            challengesWon.put(defendersTeamId, challengesWon.get(defendersTeamId) + 1);
        }
        for (Mutant m : getMutantsMarkedEquivalentPending()) {
            int defenderId = DatabaseAccess.getEquivalentDefenderId(m);
            challengesOpen.put(defenderId, challengesOpen.get(defenderId) + 1);
            challengesOpen.put(defendersTeamId, challengesOpen.get(defendersTeamId) + 1);
        }

        for (int playerId : testScores.keySet()) {
            testScores.get(playerId).setDuelInformation(challengesWon.get(playerId) + " / " + challengesLost.get(playerId) + " / " + challengesOpen.get((playerId)));
        }
        testScores.get(defendersTeamId).setDuelInformation(challengesWon.get(defendersTeamId) + " / " + challengesLost.get(defendersTeamId) + " / " + challengesOpen.get((defendersTeamId)));

        for (int i : mutantsKilled.keySet()) {
            PlayerScore ps = testScores.get(i);
            ps.setMutantKillInformation("" + mutantsKilled.get(i));
        }

        return testScores;
    }

    public boolean isLineCovered(int lineNumber) {
        for (Test test : getTests(true)) {
            if (test.getLineCoverage().getLinesCovered().contains(lineNumber))
                return true;
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

        for (int attacker : getAttackerIds()) {
            Event notif = new Event(-1, id,
                    UserDAO.getUserForPlayer(attacker).getId(),
                    message,
                    et, EventStatus.NEW,
                    new Timestamp(System.currentTimeMillis()));
            notif.insert();
        }
    }

    private void notifyDefenders(String message, EventType et) {
        for (int defender : getDefenderIds()) {
            Event notif = new Event(-1, id,
                    UserDAO.getUserForPlayer(defender).getId(),
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
