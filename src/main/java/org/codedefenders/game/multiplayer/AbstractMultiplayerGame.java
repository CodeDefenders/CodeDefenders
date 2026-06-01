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

import java.util.HashSet;
import java.util.Set;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.model.Player;
import org.codedefenders.model.WhitelistElement;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.validation.code.DefaultRuleSets;
import org.codedefenders.validation.code.MutantValidationRuleSet;

public abstract class AbstractMultiplayerGame extends AbstractGame {

    protected float lineCoverage;
    protected float mutantCoverage;
    protected float prize;

    protected boolean chatEnabled;

    protected int gameDurationMinutes;
    protected long startTimeUnixSeconds;
    protected long finishTimeUnixSeconds;

    // 0 means disabled
    protected int automaticMutantEquivalenceThreshold = 0;

    protected Integer classroomId;

    public abstract static class Builder<T extends Builder<T, G>, G extends AbstractMultiplayerGame> {
        // mandatory values
        protected final int classId;
        protected final int creatorId;
        protected final int maxAssertionsPerTest;

        // optional values with default values
        protected GameClass cut = null;
        protected int id = -1;
        protected boolean requiresValidation = false;
        protected boolean capturePlayersIntention = false;
        protected boolean chatEnabled = false;
        protected boolean inviteOnly = false;
        protected Integer inviteId = null;
        protected int gameDurationMinutes;
        protected long startTimeUnixSeconds;
        protected long finishTimeUnixSeconds;
        protected float lineCoverage = 1f;
        protected float mutantCoverage = 1f;
        protected float prize = 1f;
        protected GameState state = GameState.CREATED;
        protected GameLevel level = GameLevel.HARD;
        protected MutantValidationRuleSet mutantValidatorLevel = DefaultRuleSets.STRICT;
        protected int automaticMutantEquivalenceThreshold = 0;
        protected Integer classroomId = null;
        protected Set<WhitelistElement> whitelist = new HashSet<>();

        public Builder(int classId, int creatorId, int maxAssertionsPerTest) {
            this.classId = classId;
            this.creatorId = creatorId;
            this.maxAssertionsPerTest = maxAssertionsPerTest;
        }

        public T cut(GameClass cut) {
            this.cut = cut;
            return self();
        }

        public T id(int id) {
            this.id = id;
            return self();
        }

        public T requiresValidation(boolean requiresValidation) {
            this.requiresValidation = requiresValidation;
            return self();
        }

        public T capturePlayersIntention(boolean capturePlayersIntention) {
            this.capturePlayersIntention = capturePlayersIntention;
            return self();
        }

        public T chatEnabled(boolean chatEnabled) {
            this.chatEnabled = chatEnabled;
            return self();
        }

        public T gameDurationMinutes(int gameDurationMinutes) {
            this.gameDurationMinutes = gameDurationMinutes;
            return self();
        }

        public T startTimeUnixSeconds(long startTimeUnixSeconds) {
            this.startTimeUnixSeconds = startTimeUnixSeconds;
            return self();
        }

        public T finishTimeUnixSeconds(long finishTimeUnixSeconds) {
            this.finishTimeUnixSeconds = finishTimeUnixSeconds;
            return self();
        }

        public T prize(float prize) {
            this.prize = prize;
            return self();
        }

        public T lineCoverage(float lineCoverage) {
            this.lineCoverage = lineCoverage;
            return self();
        }

        public T mutantCoverage(float mutantCoverage) {
            this.mutantCoverage = mutantCoverage;
            return self();
        }

        public T state(GameState state) {
            this.state = state;
            return self();
        }

        public T level(GameLevel level) {
            this.level = level;
            return self();
        }

        public T mutantValidatorLevel(MutantValidationRuleSet mutantValidatorLevel) {
            this.mutantValidatorLevel = mutantValidatorLevel;
            return self();
        }

        public T automaticMutantEquivalenceThreshold(int threshold) {
            this.automaticMutantEquivalenceThreshold = threshold;
            return self();
        }

        public T classroomId(Integer classroomId) {
            this.classroomId = classroomId;
            return self();
        }

        public T inviteOnly(boolean inviteOnly) {
            this.inviteOnly = inviteOnly;
            return self();
        }

        public T inviteId(Integer inviteId) {
            this.inviteId = inviteId;
            return self();
        }

        public T whitelist(Set<WhitelistElement> whitelist) {
            this.whitelist = whitelist;
            return self();
        }

        protected abstract T self();
        public abstract G build();
    }

    protected <T extends AbstractMultiplayerGame.Builder<T, ?>> AbstractMultiplayerGame(T builder) {
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
        this.automaticMutantEquivalenceThreshold = builder.automaticMutantEquivalenceThreshold;
        this.gameDurationMinutes = builder.gameDurationMinutes;
        this.startTimeUnixSeconds = builder.startTimeUnixSeconds;
        this.finishTimeUnixSeconds = builder.finishTimeUnixSeconds;
        this.classroomId = builder.classroomId;
        this.inviteOnly = builder.inviteOnly;
        this.inviteId = builder.inviteId;
        this.whitelist = builder.whitelist;
    }


    @Override
    public boolean isChatEnabled() {
        return chatEnabled;
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

    public long getFinishTimeUnixSeconds() {
        return finishTimeUnixSeconds;
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


    public boolean isLineCovered(int lineNumber) {
        for (Test test : getTests(true)) {
            if (test.getLineCoverage().getLinesCovered().contains(lineNumber)) {
                return true;
            }
        }
        return false;
    }

    public boolean removePlayer(int userId) {
        GameRepository gameRepo = CDIUtil.getBeanFromCDI(GameRepository.class);

        if (state == GameState.CREATED) {
            return gameRepo.removeUserFromGame(id, userId);
        }
        return false;
    }

    public boolean hasUserJoined(int userId) {
        GameRepository gameRepo = CDIUtil.getBeanFromCDI(GameRepository.class);

        for (Player p : gameRepo.getValidPlayersForGame(this.getId())) {
            if (p.getUser().getId() == userId) {
                return true;
            }
        }
        return false;
    }

    public abstract Role getRole(int userId);
}
