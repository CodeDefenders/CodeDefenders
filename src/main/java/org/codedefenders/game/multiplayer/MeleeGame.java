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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.codedefenders.database.EventDAO;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.scoring.ScoreCalculator;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.model.Player;
import org.codedefenders.model.UserEntity;
import org.codedefenders.model.WhitelistElement;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.MeleeGameRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.validation.code.DefaultRuleSets;
import org.codedefenders.validation.code.MutantValidationRuleSet;

public class MeleeGame extends AbstractMultiplayerGame {

    /*
     * Inherited from AbstractGame
     *
     * protected GameClass cut; protected int id; protected int classId; protected
     * int creatorId; protected GameState state; protected GameLevel level;
     * protected GameMode mode; protected ArrayList<Event> events; protected
     * List<Mutant> mutants; protected List<Test> tests;
     *
     *
     * Inherited from AbstractMultiplayerGame
     *
     * protected float lineCoverage;
     * protected float mutantCoverage;
     * protected float prize;
     * protected boolean chatEnabled;
     * protected int gameDurationMinutes;
     * protected long startTimeUnixSeconds;
     * protected long finishTimeUnixSeconds;
     * protected int automaticMutantEquivalenceThreshold = 0;
     * protected Integer classroomId;
     */

    public static class Builder {
        // mandatory values
        private final int classId;
        private final int creatorId;
        private final int maxAssertionsPerTest;

        // optional values with default values
        private GameClass cut = null;

        private int id = -1;
        private boolean requiresValidation = false;
        private boolean capturePlayersIntention = false;
        private boolean chatEnabled = false;
        private float lineCoverage = 1f;
        private float mutantCoverage = 1f;
        private float prize = 1f;
        private GameState state = GameState.CREATED;
        private GameLevel level = GameLevel.HARD;
        private MutantValidationRuleSet mutantValidatorLevel = DefaultRuleSets.STRICT;

        private int gameDurationMinutes;
        private long startTimeUnixSeconds;
        private long finishTimeUnixSeconds;

        private int automaticMutantEquivalenceThreshold = 0;

        private Integer classroomId = null;

        private boolean inviteOnly = false;
        private Integer inviteId = null;
        private Set<WhitelistElement> whitelist = new HashSet<>();

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

        public Builder mutantValidatorLevel(MutantValidationRuleSet mutantValidatorLevel) {
            this.mutantValidatorLevel = mutantValidatorLevel;
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

        public Builder finishTimeUnixSeconds(long finishTimeUnixSeconds) {
            this.finishTimeUnixSeconds = finishTimeUnixSeconds;
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

        public Builder inviteOnly(boolean inviteOnly) {
            this.inviteOnly = inviteOnly;
            return this;
        }

        public Builder inviteId(Integer inviteId) {
            this.inviteId = inviteId;
            return this;
        }

        public Builder whitelist(Set<WhitelistElement> whitelist) {
            this.whitelist = whitelist;
            return this;
        }

        public MeleeGame build() {
            return new MeleeGame(this);
        }
    }

    protected MeleeGame(Builder builder) {
        this.mode = GameMode.MELEE;

        this.cut = builder.cut;
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
        this.finishTimeUnixSeconds = builder.finishTimeUnixSeconds;

        this.automaticMutantEquivalenceThreshold = builder.automaticMutantEquivalenceThreshold;
        this.classroomId = builder.classroomId;

        this.inviteOnly = builder.inviteOnly;
        this.inviteId = builder.inviteId;
        this.whitelist = builder.whitelist;
    }

    public int getAutomaticMutantEquivalenceThreshold() {
        return automaticMutantEquivalenceThreshold;
    }

    public Role getRole(int userId) {
        if (getPlayers().stream().anyMatch(player -> player.getUser().getId() == userId)) {
            return Role.PLAYER;
        } else if (getObserverPlayers().stream().anyMatch(player -> player.getUser().getId() == userId)) {
            return Role.OBSERVER;
        } else {
            return Role.NONE;
        }
    }

    public Optional<Integer> getClassroomId() {
        return Optional.ofNullable(classroomId);
    }

    // TODO Those methods should be removed? The scoring bean should take the game
    // as input and then compute the score
    public Map<Integer, PlayerScore> getMutantScores() {
        var scoringBean = CDIUtil.getBeanFromCDI(ScoreCalculator.class);
        return scoringBean.getMutantScores(id);
    }

    public Map<Integer, PlayerScore> getTestScores() {
        var scoringBean = CDIUtil.getBeanFromCDI(ScoreCalculator.class);
        return scoringBean.getTestScores(id);
    }

    /*
     * Every user has two players, one as defender and one as attacker
     */
    public List<Player> getPlayers() {
        GameRepository gameRepo = CDIUtil.getBeanFromCDI(GameRepository.class);
        // TODO: use & set cache instead (see MultiplayerGame.java)?
        List<Player> players = gameRepo.getPlayersForGame(getId(), Role.PLAYER);
        return players;
    }

    @Override
    public boolean addPlayer(int userId, Role role) {
        return canJoinGame(userId) && addPlayerForce(userId, role);
    }

    public boolean addPlayerForce(int userId, Role role) {
        GameRepository gameRepo = CDIUtil.getBeanFromCDI(GameRepository.class);
        UserRepository userRepo = CDIUtil.getBeanFromCDI(UserRepository.class);
        EventDAO eventDAO = CDIUtil.getBeanFromCDI(EventDAO.class);

        if (state == GameState.FINISHED && role != Role.OBSERVER) {
            return false;
        }

        if (!gameRepo.addPlayerToGame(id, userId, role)) {
            return false;
        }

        // Do not add events for observers joining
        if (role == Role.OBSERVER) {
            return true;
        }

        // TODO: move notifications outside of data objects.
        Optional<UserEntity> u = userRepo.getUserById(userId);
        final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Event e = new Event(-1, id, userId, u.map(UserEntity::getUsername).orElse("") + " joined melee game", EventType.PLAYER_JOINED,
                EventStatus.GAME, timestamp);
        eventDAO.insert(e);
        Event notif = new Event(-1, id, userId, "You joined melee game", EventType.PLAYER_JOINED, EventStatus.NEW,
                timestamp);
        eventDAO.insert(notif);

        return true;
    }

    @Override
    public boolean insert() {
        try {
            MeleeGameRepository meleeGameRepo = CDIUtil.getBeanFromCDI(MeleeGameRepository.class);
            this.id = meleeGameRepo.storeMeleeGame(this);
            return true;
        } catch (UncheckedSQLException e) {
            logger.error("Failed to store multiplayer game to database.", e);
            return false;
        }
    }

    @Override
    public boolean update() {
        MeleeGameRepository meleeGameRepo = CDIUtil.getBeanFromCDI(MeleeGameRepository.class);
        return meleeGameRepo.updateMeleeGame(this);
    }

}
