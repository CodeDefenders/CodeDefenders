/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.codedefenders.database.EventDAO;
import org.codedefenders.database.UncheckedSQLException;
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
import org.codedefenders.model.UserEntity;
import org.codedefenders.model.WhitelistType;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.MultiplayerGameRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.persistence.database.PlayerRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.persistence.database.WhitelistRepository;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.util.Constants;
import org.codedefenders.validation.code.CodeValidatorLevel;

import static org.codedefenders.game.Mutant.Equivalence.ASSUMED_YES;
import static org.codedefenders.game.Mutant.Equivalence.DECLARED_YES;
import static org.codedefenders.game.Mutant.Equivalence.PENDING_TEST;
import static org.codedefenders.game.Mutant.Equivalence.PROVEN_NO;

public class MultiplayerGame extends AbstractGame {

    /*
     * Inherited from AbstractGame
     *
     * protected GameClass cut; protected int id; protected int classId; protected
     * int creatorId; protected GameState state; protected GameLevel level;
     * protected GameMode mode; protected ArrayList<Event> events; protected
     * List<Mutant> mutants; protected List<Test> tests;
     */
    private List<Player> attackers;
    private List<Player> defenders;
    private int defenderValue;
    private int attackerValue;
    private float lineCoverage;
    private float mutantCoverage;
    private float prize;

    private boolean requiresValidation;

    private boolean chatEnabled;

    private int gameDurationMinutes;

    private long startTimeUnixSeconds;

    // 0 means disabled
    private int automaticMutantEquivalenceThreshold = 0;

    private Integer classroomId;

    private boolean mayChooseRoles = true;

    public static class Builder {
        // mandatory values
        private final int classId;
        private final int creatorId;
        private final int maxAssertionsPerTest;

        // optional values with default values
        private GameClass cut = null;
        private List<Player> attackers = null;
        private List<Player> defenders = null;
        private int id = -1;
        private boolean requiresValidation = false;
        private boolean capturePlayersIntention = false;
        private boolean mayChooseRoles = true;
        private boolean inviteOnly = false;
        private Integer inviteId = null;
        private boolean chatEnabled = false;
        private int gameDurationMinutes;
        private long startTimeUnixSeconds;
        private float lineCoverage = 1f;
        private float mutantCoverage = 1f;
        private float prize = 1f;
        private int defenderValue = 100;
        private int attackerValue = 100;
        private GameState state = GameState.CREATED;
        private GameLevel level = GameLevel.HARD;
        private CodeValidatorLevel mutantValidatorLevel = CodeValidatorLevel.STRICT;

        private int automaticMutantEquivalenceThreshold = 0;

        private Integer classroomId = null;

        public Builder(int classId, int creatorId, int maxAssertionsPerTest) {
            this.classId = classId;
            this.creatorId = creatorId;
            this.maxAssertionsPerTest = maxAssertionsPerTest;
        }

        public Builder cut(GameClass cut) {
            this.cut = cut;
            return this;
        }

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder requiresValidation(boolean requiresValidation) {
            this.requiresValidation = requiresValidation;
            return this;
        }

        public Builder capturePlayersIntention(boolean capturePlayersIntention) {
            this.capturePlayersIntention = capturePlayersIntention;
            return this;
        }

        public Builder chatEnabled(boolean chatEnabled) {
            this.chatEnabled = chatEnabled;
            return this;
        }

        public Builder gameDurationMinutes(int gameDurationMinutes) {
            this.gameDurationMinutes = gameDurationMinutes;
            return this;
        }

        public Builder startTimeUnixSeconds(long startTimeUnixSeconds) {
            this.startTimeUnixSeconds = startTimeUnixSeconds;
            return this;
        }

        public Builder prize(float prize) {
            this.prize = prize;
            return this;
        }

        public Builder lineCoverage(float lineCoverage) {
            this.lineCoverage = lineCoverage;
            return this;
        }

        public Builder mutantCoverage(float mutantCoverage) {
            this.mutantCoverage = mutantCoverage;
            return this;
        }

        public Builder defenderValue(int defenderValue) {
            this.defenderValue = defenderValue;
            return this;
        }

        public Builder attackerValue(int attackerValue) {
            this.attackerValue = attackerValue;
            return this;
        }

        public Builder state(GameState state) {
            this.state = state;
            return this;
        }

        public Builder level(GameLevel level) {
            this.level = level;
            return this;
        }

        public Builder mutantValidatorLevel(CodeValidatorLevel mutantValidatorLevel) {
            this.mutantValidatorLevel = mutantValidatorLevel;
            return this;
        }

        public Builder attackers(List<Player> attackers) {
            this.attackers = attackers;
            return this;
        }

        public Builder defenders(List<Player> defenders) {
            this.defenders = defenders;
            return this;
        }

        public Builder automaticMutantEquivalenceThreshold(int threshold) {
            this.automaticMutantEquivalenceThreshold = threshold;
            return this;
        }

        public Builder classroomId(Integer classroomId) {
            this.classroomId = classroomId;
            return this;
        }

        public Builder mayChooseRoles(boolean mayChooseRoles) {
            this.mayChooseRoles = mayChooseRoles;
            return this;
        }

        public Builder inviteOnly(boolean inviteOnly) {
            this.inviteOnly = inviteOnly;
            return this;
        }

        public Builder inviteId(Integer inviteId) {
            this.inviteId = inviteId;
            return this;
        }

        public MultiplayerGame build() {
            return new MultiplayerGame(this);
        }
    }

    private MultiplayerGame(Builder builder) {
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
        this.chatEnabled = builder.chatEnabled;
        this.mutantValidatorLevel = builder.mutantValidatorLevel;
        this.capturePlayersIntention = builder.capturePlayersIntention;
        this.automaticMutantEquivalenceThreshold = builder.automaticMutantEquivalenceThreshold;
        this.gameDurationMinutes = builder.gameDurationMinutes;
        this.startTimeUnixSeconds = builder.startTimeUnixSeconds;
        this.classroomId = builder.classroomId;
        this.inviteOnly = builder.inviteOnly;
        this.mayChooseRoles = builder.mayChooseRoles;
        this.inviteId = builder.inviteId;
    }

    public int getGameDurationMinutes() {
        return gameDurationMinutes;
    }

    public void setGameDurationMinutes(int gameDurationMinutes) {
        this.gameDurationMinutes = gameDurationMinutes;
    }

    public long getStartTimeUnixSeconds() {
        return startTimeUnixSeconds;
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

    @Override
    public boolean isChatEnabled() {
        return chatEnabled;
    }

    public int getAutomaticMutantEquivalenceThreshold() {
        return automaticMutantEquivalenceThreshold;
    }

    public Optional<Integer> getClassroomId() {
        return Optional.ofNullable(classroomId);
    }

    public Role getRole(int userId) {
        if (getDefenderPlayers().stream().anyMatch(player -> player.getUser().getId() == userId)) {
            return Role.DEFENDER;
        } else if (getAttackerPlayers().stream().anyMatch(player -> player.getUser().getId() == userId)) {
            return Role.ATTACKER;
        } else if (getObserverPlayers().stream().anyMatch(player -> player.getUser().getId() == userId)) {
            return Role.OBSERVER;
        } else {
            return Role.NONE;
        }
    }

    public boolean isMayChooseRoles() {
        return mayChooseRoles;
    }

    public List<Player> getDefenderPlayers() {
        GameRepository gameRepo = CDIUtil.getBeanFromCDI(GameRepository.class);

        if (defenders == null) {
            defenders = gameRepo.getPlayersForGame(getId(), Role.DEFENDER);
        }
        return defenders;
    }

    public List<Player> getAttackerPlayers() {
        GameRepository gameRepo = CDIUtil.getBeanFromCDI(GameRepository.class);

        if (attackers == null) {
            attackers = gameRepo.getPlayersForGame(getId(), Role.ATTACKER);
        }
        return attackers;
    }

    @Override
    public boolean addPlayer(int userId, Role role) {
        if (!mayChooseRoles && inviteOnly) {
            WhitelistRepository whitelistRepo = CDIUtil.getBeanFromCDI(WhitelistRepository.class);
            WhitelistType whitelistType = whitelistRepo.getWhitelistType(id, userId);
            if (whitelistType != null) {
                if (whitelistType == WhitelistType.FLEX || (whitelistType == WhitelistType.CHOICE && role == null)) {
                    role = attackers.size() > defenders.size() ? Role.DEFENDER : Role.ATTACKER;
                } else if (whitelistType == WhitelistType.ATTACKER) {
                    role = Role.ATTACKER;
                } else if (whitelistType == WhitelistType.DEFENDER) {
                    role = Role.DEFENDER;
                }
                //If whitelist type is choice and role is set, keep role untouched.
            }
        }
        return canJoinGame(userId) && addPlayerForce(userId, role);
    }

    public boolean addPlayerForce(int userId, Role role) {
        GameRepository gameRepo = CDIUtil.getBeanFromCDI(GameRepository.class);
        EventDAO eventDAO = CDIUtil.getBeanFromCDI(EventDAO.class);
        UserRepository userRepo = CDIUtil.getBeanFromCDI(UserRepository.class);

        if (state == GameState.FINISHED) {
            return false;
        }
        if (!gameRepo.addPlayerToGame(id, userId, role)) {
            return false;
        }
        Optional<UserEntity> u = userRepo.getUserById(userId);
        EventType et = role == Role.ATTACKER ? EventType.ATTACKER_JOINED : EventType.DEFENDER_JOINED;
        final Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        Event e = new Event(-1, id, userId, u.map(UserEntity::getUsername).orElse("") + " joined the game as " + role, et, EventStatus.GAME,
                timestamp);
        eventDAO.insert(e);

        Event notif = new Event(-1, id, userId, "You joined a game as " + role, et, EventStatus.NEW, timestamp);
        eventDAO.insert(notif);

        return true;
    }

    public boolean removePlayer(int userId) {
        GameRepository gameRepo = CDIUtil.getBeanFromCDI(GameRepository.class);

        if (state == GameState.CREATED) {
            return gameRepo.removeUserFromGame(id, userId);
        }
        return false;
    }

    private boolean canJoinGame(int userId) {
        UserRepository userRepo = CDIUtil.getBeanFromCDI(UserRepository.class);
        WhitelistRepository whitelistRepo = CDIUtil.getBeanFromCDI(WhitelistRepository.class);
        if (userId != Constants.DUMMY_ATTACKER_USER_ID && userId != Constants.DUMMY_DEFENDER_USER_ID
                && inviteOnly && !whitelistRepo.isWhitelisted(id, userId)) {
            return false;
        }
        return !requiresValidation || userRepo.getUserById(userId).map(UserEntity::isValidated).orElse(false);
    }

    @Override
    public boolean insert() {
        var multiplayerGameRepo = CDIUtil.getBeanFromCDI(MultiplayerGameRepository.class);
        try {
            this.id = multiplayerGameRepo.storeMultiplayerGame(this);
            return true;
        } catch (UncheckedSQLException e) {
            logger.error("Failed to store multiplayer game to database.", e);
            return false;
        }
    }

    @Override
    public boolean update() {
        var multiplayerGameRepo = CDIUtil.getBeanFromCDI(MultiplayerGameRepository.class);
        return multiplayerGameRepo.updateMultiplayerGame(this);
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

        final HashMap<Integer, Integer> mutantsAlive = new HashMap<>();
        final HashMap<Integer, Integer> mutantsKilled = new HashMap<>();
        final HashMap<Integer, Integer> mutantsEquiv = new HashMap<>();
        final HashMap<Integer, Integer> mutantsChallenged = new HashMap<>();
        final HashMap<Integer, Integer> duelsWon = new HashMap<>();

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
                // This includes mutants marked equivalent
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
                    mutantsAlive.get(i) + " / " + mutantsKilled.get(i) + " / " + mutantsEquiv.get((i)));
            ps.setDuelInformation(duelsWon.get(i) + " / " + mutantsEquiv.get(i) + " / " + mutantsChallenged.get((i)));

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
        MutantRepository mutantRepo = CDIUtil.getBeanFromCDI(MutantRepository.class);
        PlayerRepository playerRepo = CDIUtil.getBeanFromCDI(PlayerRepository.class);

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
            int playerScore = playerRepo.getPlayerPoints(playerId);
            ps.increaseTotalScore(playerScore);

            PlayerScore ts = testScores.get(teamKey);
            ts.increaseTotalScore(playerScore);
        }

        for (Mutant m : getKilledMutants()) {
            if (!m.getEquivalent().equals(PROVEN_NO)) {
                continue;
            }

            int defenderId = mutantRepo.getEquivalentDefenderId(m);
            challengesLost.put(defenderId, challengesLost.get(defenderId) + 1);
            if (defenderId != defendersTeamId) {
                challengesLost.put(defendersTeamId, challengesLost.get(defendersTeamId) + 1);
            }
        }
        for (Mutant m : getMutantsMarkedEquivalent()) {
            int defenderId = mutantRepo.getEquivalentDefenderId(m);
            challengesWon.put(defenderId, challengesWon.get(defenderId) + 1);
            if (defenderId != defendersTeamId) {
                challengesWon.put(defendersTeamId, challengesWon.get(defendersTeamId) + 1);
            }
        }
        for (Mutant m : getMutantsMarkedEquivalentPending()) {
            int defenderId = mutantRepo.getEquivalentDefenderId(m);
            challengesOpen.put(defenderId, challengesOpen.get(defenderId) + 1);
            if (defenderId != defendersTeamId) {
                challengesOpen.put(defendersTeamId, challengesOpen.get(defendersTeamId) + 1);
            }
        }

        for (int playerId : testScores.keySet()) {
            testScores.get(playerId).setDuelInformation(challengesWon.get(playerId) + " / "
                    + challengesLost.get(playerId) + " / " + challengesOpen.get((playerId)));
        }
        testScores.get(defendersTeamId).setDuelInformation(challengesWon.get(defendersTeamId) + " / "
                + challengesLost.get(defendersTeamId) + " / " + challengesOpen.get((defendersTeamId)));
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

    /**
     * Adds a player that wants to join via an invite link.
     * If {@link MultiplayerGame:allowPlayersToChooseRole} is true, the {@code wantedRole} is used to determine
     * the role of the player. It must be either "attacker" or "defender".
     * Otherwise, the player will be added to the side with fewer players, defaulting to "Attacker" when evenly matched.
     * TODO: Is synchronization correct?
     */
    public synchronized Role joinWithInvite(int userId, String wantedRole) throws IllegalArgumentException {
        Role role; //TODO Enum für Rolle benutzen?
        if (mayChooseRoles && wantedRole != null && !wantedRole.equalsIgnoreCase("flex")) {
            if (wantedRole.equalsIgnoreCase("attacker")) {
                role = Role.ATTACKER;
            } else if (wantedRole.equalsIgnoreCase("defender")) {
                role = Role.DEFENDER;
            } else {
                throw new IllegalArgumentException("Invalid role: " + wantedRole);
            }
        } else {
            role = getAttackerPlayers().size() > getDefenderPlayers().size() ? Role.DEFENDER : Role.ATTACKER;
        }
        addPlayer(userId, role);
        return role;
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
        EventDAO eventDAO = CDIUtil.getBeanFromCDI(EventDAO.class);
        for (Player player : getAttackerPlayers()) {
            Event notif = new Event(-1, id, player.getUser().getId(), message, et, EventStatus.NEW,
                    new Timestamp(System.currentTimeMillis()));
            eventDAO.insert(notif);
        }
    }

    private void notifyDefenders(String message, EventType et) {
        EventDAO eventDAO = CDIUtil.getBeanFromCDI(EventDAO.class);
        for (Player player : getDefenderPlayers()) {
            Event notif = new Event(-1, id, player.getUser().getId(), message, et, EventStatus.NEW,
                    new Timestamp(System.currentTimeMillis()));
            eventDAO.insert(notif);
        }
    }

    private void notifyCreator(String message, EventType et) {
        // Event for game log: started
        EventDAO eventDAO = CDIUtil.getBeanFromCDI(EventDAO.class);
        Event notif = new Event(-1, id, creatorId, message, et, EventStatus.NEW,
                new Timestamp(System.currentTimeMillis()));
        eventDAO.insert(notif);
    }

    private void notifyGame(String message, EventType et) {
        // Event for game log: started
        EventDAO eventDAO = CDIUtil.getBeanFromCDI(EventDAO.class);
        Event notif = new Event(-1, id, creatorId, message, et, EventStatus.GAME,
                new Timestamp(System.currentTimeMillis()));
        eventDAO.insert(notif);
    }

}
