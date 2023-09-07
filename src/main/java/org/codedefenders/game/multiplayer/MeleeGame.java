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

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.codedefenders.database.GameDAO;
import org.codedefenders.database.MeleeGameDAO;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.scoring.ScoreCalculator;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.model.Player;
import org.codedefenders.model.UserEntity;
import org.codedefenders.validation.code.CodeValidatorLevel;

public class MeleeGame extends AbstractGame {

    /*
     * Inherited from AbstractGame
     *
     * protected GameClass cut; protected int id; protected int classId; protected
     * int creatorId; protected GameState state; protected GameLevel level;
     * protected GameMode mode; protected ArrayList<Event> events; protected
     * List<Mutant> mutants; protected List<Test> tests;
     */
    private List<Player> players;

    // TODO Does it make sense to have injection inside data objects ?
    //@Inject
    private ScoreCalculator scoringBean;

    public void setScoringBean(ScoreCalculator scoringBean) {
        this.scoringBean = scoringBean;
    }

    // Injection done at AbstractGame level?
    //@Inject
    //private EventDAO eventDAO;
    //
    //public void setEventDAO(EventDAO eventDAO) {
    //    this.eventDAO = eventDAO;
    //}

    //@Inject
    private MeleeGameDAO meleeGameDAO;

    public void setMeleeGameDAO(MeleeGameDAO meleeGameDAO) {
        this.meleeGameDAO = meleeGameDAO;
    }

    @Deprecated
    private int defenderValue;
    @Deprecated
    private int attackerValue;

    private float lineCoverage;
    private float mutantCoverage;
    private float prize;

    private boolean requiresValidation;

    private boolean chatEnabled;

    private int gameDurationMinutes;

    private long startTimeUnixSeconds;

    // We need a temporary location where to store information about system tests
    // and mutants
    private boolean withTests;
    private boolean withMutants;

    // 0 means disabled
    private int automaticMutantEquivalenceThreshold = 0;

    private Integer classroomId;

    public static class Builder {
        // mandatory values
        private final int classId;
        private final int creatorId;
        private final int maxAssertionsPerTest;

        // optional values with default values
        private GameClass cut = null;

        // Melee games do not have attackers and defenders, just players
        private List<Player> players = null;

        private int id = -1;
        private boolean requiresValidation = false;
        private boolean capturePlayersIntention = false;
        private boolean chatEnabled = false;
        private float lineCoverage = 1f;
        private float mutantCoverage = 1f;
        private float prize = 1f;
        // private int defenderValue = 100;
        // private int attackerValue = 100;
        private GameState state = GameState.CREATED;
        private GameLevel level = GameLevel.HARD;
        private CodeValidatorLevel mutantValidatorLevel = CodeValidatorLevel.STRICT;

        private int gameDurationMinutes;
        private long startTimeUnixSeconds;

        private boolean withTests = false;
        private boolean withMutants = false;

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

        public Builder players(List<Player> players) {
            this.players = players;
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

        public Builder withTests(boolean withTests) {
            this.withTests = withTests;
            return this;
        }

        public Builder withMutants(boolean withMutants) {
            this.withMutants = withMutants;
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

        public MeleeGame build() {
            return new MeleeGame(this);
        }
    }

    protected MeleeGame(Builder builder) {
        this.mode = GameMode.MELEE;

        this.cut = builder.cut;
        this.players = builder.players;
        this.id = builder.id;
        this.classId = builder.classId;
        this.creatorId = builder.creatorId;
        this.state = builder.state;
        this.level = builder.level;
        this.lineCoverage = builder.lineCoverage;
        this.mutantCoverage = builder.mutantCoverage;
        this.prize = builder.prize;
        this.requiresValidation = builder.requiresValidation;
        this.maxAssertionsPerTest = builder.maxAssertionsPerTest;
        this.chatEnabled = builder.chatEnabled;
        this.mutantValidatorLevel = builder.mutantValidatorLevel;
        this.capturePlayersIntention = builder.capturePlayersIntention;
        this.gameDurationMinutes = builder.gameDurationMinutes;
        this.startTimeUnixSeconds = builder.startTimeUnixSeconds;

        // This is mostly a temporary patch
        this.withMutants = builder.withMutants;
        this.withTests = builder.withTests;

        this.automaticMutantEquivalenceThreshold = builder.automaticMutantEquivalenceThreshold;
        this.classroomId = builder.classroomId;
    }

    public boolean hasSystemTests() {
        return this.withTests;
    }

    public boolean hasSystemMutants() {
        return this.withMutants;
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

    public Role getRole(int userId) {
        if (getPlayers().stream().anyMatch(player -> player.getUser().getId() == userId)) {
            return Role.PLAYER;
        } else if (userId == getCreatorId()) {
            return Role.OBSERVER;
        } else {
            return Role.NONE;
        }
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

    public Optional<Integer> getClassroomId() {
        return Optional.ofNullable(classroomId);
    }

    // TODO Those methods should be removed? The scoring bean should take the game
    // as input and then compute the score
    public Map<Integer, PlayerScore> getMutantScores() {
        return scoringBean.getMutantScores(id);
    }

    public Map<Integer, PlayerScore> getTestScores() {
        return scoringBean.getTestScores(id);
    }

    /*
     * Every user has two players, one as defender and one as attacker
     */
    public List<Player> getPlayers() {
        List<Player> players = GameDAO.getPlayersForGame(getId(), Role.PLAYER);
        return players;
    }

    protected boolean canJoinGame(int userId) {
        return !requiresValidation || userRepository.getUserById(userId).map(UserEntity::isValidated).orElse(false);
    }

    @Override
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
        Optional<UserEntity> u = userRepository.getUserById(userId);
        final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Event e = new Event(-1, id, userId, u.map(UserEntity::getUsername).orElse("") + " joined melee game", EventType.PLAYER_JOINED,
                EventStatus.GAME, timestamp);
        eventDAO.insert(e);
        Event notif = new Event(-1, id, userId, "You joined melee game", EventType.PLAYER_JOINED, EventStatus.NEW,
                timestamp);
        eventDAO.insert(notif);

        return true;
    }

    public boolean removePlayer(int userId) {
        if (state == GameState.CREATED) {
            return GameDAO.removeUserFromGame(id, userId);
        }
        return false;
    }

    // We do not check that the user is in both roles !!
    public boolean hasUserJoined(int userId) {
        for (Player p : GameDAO.getValidPlayersForGame(this.getId())) {
            if (p.getUser().getId() == userId) {
                return true;
            }
        }
        return false;
    }

    // TODO Change this to reflect the PLAYER roles
    public void notifyPlayers() {
        List<Event> events = getEvents();

        switch (state) {
            case ACTIVE:
                if (events.stream().map(Event::getEventType).noneMatch(e -> e == EventType.GAME_STARTED)) {
                    EventType et = EventType.GAME_STARTED;
                    notifyPlayers("Game has started. Attack and Defend now!", et);
                    notifyCreator("Your game as started!", et);
                    notifyGame("The game has started!", et);
                }
                break;
            case GRACE_ONE:
                if (events.stream().map(Event::getEventType).noneMatch(e -> e == EventType.GAME_GRACE_ONE)) {
                    EventType et = EventType.GAME_GRACE_ONE;
                    notifyPlayers("A game has entered Grace One.", et);
                    notifyCreator("Your game has entered Grace One", et);
                    notifyGame("The game as entered Grace Period One", et);
                }
                break;
            case GRACE_TWO:
                if (events.stream().map(Event::getEventType).noneMatch(e -> e == EventType.GAME_GRACE_TWO)) {
                    EventType et = EventType.GAME_GRACE_TWO;
                    notifyPlayers("A game has entered Grace Two.", et);
                    notifyCreator("Your game has entered Grace Two", et);
                    notifyGame("The game as entered Grace Period Two", et);
                }
                break;
            case FINISHED:
                if (events.stream().map(Event::getEventType).noneMatch(e -> e == EventType.GAME_FINISHED)) {
                    EventType et = EventType.GAME_FINISHED;
                    notifyPlayers("A game has finished.", et);
                    notifyCreator("Your game has finished.", et);
                    notifyGame("The game has ended.", et);
                }
                break;
            default:
                // ignored
        }
    }

    private void notifyPlayers(String message, EventType et) {
        for (Player player : getPlayers()) {
            Event notif = new Event(-1, id, player.getUser().getId(), message, et, EventStatus.NEW,
                    new Timestamp(System.currentTimeMillis()));
            eventDAO.insert(notif);
        }
    }

    private void notifyCreator(String message, EventType et) {
        // Event for game log: started
        Event notif = new Event(-1, id, creatorId, message, et, EventStatus.NEW,
                new Timestamp(System.currentTimeMillis()));
        eventDAO.insert(notif);
    }

    private void notifyGame(String message, EventType et) {
        // Event for game log: started
        Event notif = new Event(-1, id, creatorId, message, et, EventStatus.GAME,
                new Timestamp(System.currentTimeMillis()));
        eventDAO.insert(notif);
    }

    @Override
    public boolean insert() {
        try {
            this.id = meleeGameDAO.storeMeleeGame(this);
            return true;
        } catch (UncheckedSQLException e) {
            logger.error("Failed to store multiplayer game to database.", e);
            return false;
        }
    }

    @Override
    public boolean update() {
        return meleeGameDAO.updateMeleeGame(this);
    }

    public boolean isLineCovered(int lineNumber) {
        for (Test test : getTests(true)) {
            if (test.getLineCoverage().getLinesCovered().contains(lineNumber)) {
                return true;
            }
        }
        return false;
    }

}
