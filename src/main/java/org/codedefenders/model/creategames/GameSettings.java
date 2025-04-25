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
package org.codedefenders.model.creategames;

import java.io.Serializable;
import java.util.Optional;

import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameType;
import org.codedefenders.game.Role;
import org.codedefenders.validation.code.CodeValidatorLevel;

import com.google.gson.annotations.Expose;

public class GameSettings implements Serializable {
    @Expose private final GameType gameType;

    @Expose private final int classId;
    @Expose private final boolean withMutants;
    @Expose private final boolean withTests;

    @Expose private final int maxAssertionsPerTest;
    @Expose private final CodeValidatorLevel mutantValidatorLevel;
    @Expose private final boolean chatEnabled;
    @Expose private final boolean captureIntentions;
    @Expose private final int equivalenceThreshold;
    @Expose private final GameLevel level;
    @Expose private final Role creatorRole;
    @Expose private final int gameDurationMinutes;

    @Expose private final boolean startGame;

    @Expose private final Integer classroomId;

    public GameSettings(
            GameType gameType,
            int classId,
            boolean withMutants,
            boolean withTests,
            int maxAssertionsPerTest,
            CodeValidatorLevel mutantValidatorLevel,
            boolean chatEnabled,
            boolean captureIntentions,
            int equivalenceThreshold,
            GameLevel level,
            Role creatorRole,
            int gameDurationMinutes,
            boolean startGame,
            Integer classroomId) {
        this.gameType = gameType;
        this.classId = classId;
        this.withMutants = withMutants;
        this.withTests = withTests;
        this.maxAssertionsPerTest = maxAssertionsPerTest;
        this.mutantValidatorLevel = mutantValidatorLevel;
        this.chatEnabled = chatEnabled;
        this.captureIntentions = captureIntentions;
        this.equivalenceThreshold = equivalenceThreshold;
        this.level = level;
        this.creatorRole = creatorRole;
        this.gameDurationMinutes = gameDurationMinutes;
        this.startGame = startGame;
        this.classroomId = classroomId;
    }

    /**
     * Creates a builder with the values from {@code other}.
     * Useful for editing game settings.
     */
    public static Builder from(GameSettings other) {
        return new Builder(
            other.gameType,
            other.classId,
            other.withMutants,
            other.withTests,
            other.maxAssertionsPerTest,
            other.mutantValidatorLevel,
            other.chatEnabled,
            other.captureIntentions,
            other.equivalenceThreshold,
            other.level,
            other.creatorRole,
            other.gameDurationMinutes,
            other.startGame,
            other.classroomId
        );
    }

    public GameType getGameType() {
        return gameType;
    }

    public int getClassId() {
        return classId;
    }

    public boolean isWithMutants() {
        return withMutants;
    }

    public boolean isWithTests() {
        return withTests;
    }

    public int getMaxAssertionsPerTest() {
        return maxAssertionsPerTest;
    }

    public CodeValidatorLevel getMutantValidatorLevel() {
        return mutantValidatorLevel;
    }

    public boolean isChatEnabled() {
        return chatEnabled;
    }

    public boolean isCaptureIntentions() {
        return captureIntentions;
    }

    public int getEquivalenceThreshold() {
        return equivalenceThreshold;
    }

    public GameLevel getLevel() {
        return level;
    }

    public Role getCreatorRole() {
        return creatorRole;
    }

    public int getGameDurationMinutes() {
        return gameDurationMinutes;
    }

    public boolean isStartGame() {
        return startGame;
    }

    public Optional<Integer> getClassroomId() {
        return Optional.ofNullable(classroomId);
    }

    public static class Builder {
        private GameType gameType;
        private int classId;
        private boolean withMutants;
        private boolean withTests;
        private int maxAssertionsPerTest;
        private CodeValidatorLevel mutantValidatorLevel;
        private boolean chatEnabled;
        private boolean captureIntentions;
        private int equivalenceThreshold;
        private GameLevel level;
        private Role creatorRole;
        private int gameDurationMinutes;
        private boolean startGame;
        private Integer classroomId;

        private Builder(
                GameType gameType,
                int classId,
                boolean withMutants,
                boolean withTests,
                int maxAssertionsPerTest,
                CodeValidatorLevel mutantValidatorLevel,
                boolean chatEnabled,
                boolean captureIntentions,
                int equivalenceThreshold,
                GameLevel level,
                Role creatorRole,
                int gameDurationMinutes,
                boolean startGame,
                Integer classroomId) {
            this.gameType = gameType;
            this.classId = classId;
            this.withMutants = withMutants;
            this.withTests = withTests;
            this.maxAssertionsPerTest = maxAssertionsPerTest;
            this.mutantValidatorLevel = mutantValidatorLevel;
            this.chatEnabled = chatEnabled;
            this.captureIntentions = captureIntentions;
            this.equivalenceThreshold = equivalenceThreshold;
            this.level = level;
            this.creatorRole = creatorRole;
            this.gameDurationMinutes = gameDurationMinutes;
            this.startGame = startGame;
            this.classroomId = classroomId;
        }

        public Builder setGameType(GameType gameType) {
            this.gameType = gameType;
            return this;
        }

        public Builder setClassId(int classId) {
            this.classId = classId;
            return this;
        }

        public Builder setWithMutants(boolean withMutants) {
            this.withMutants = withMutants;
            return this;
        }

        public Builder setWithTests(boolean withTests) {
            this.withTests = withTests;
            return this;
        }

        public Builder setMaxAssertionsPerTest(int maxAssertionsPerTest) {
            this.maxAssertionsPerTest = maxAssertionsPerTest;
            return this;
        }

        public Builder setMutantValidatorLevel(CodeValidatorLevel mutantValidatorLevel) {
            this.mutantValidatorLevel = mutantValidatorLevel;
            return this;
        }

        public Builder setChatEnabled(boolean chatEnabled) {
            this.chatEnabled = chatEnabled;
            return this;
        }

        public Builder setCaptureIntentions(boolean captureIntentions) {
            this.captureIntentions = captureIntentions;
            return this;
        }

        public Builder setEquivalenceThreshold(int equivalenceThreshold) {
            this.equivalenceThreshold = equivalenceThreshold;
            return this;
        }

        public Builder setLevel(GameLevel level) {
            this.level = level;
            return this;
        }

        public Builder setCreatorRole(Role creatorRole) {
            this.creatorRole = creatorRole;
            return this;
        }

        public Builder setGameDurationMinutes(final int duration) {
            this.gameDurationMinutes = duration;
            return this;
        }

        public Builder setStartGame(boolean startGame) {
            this.startGame = startGame;
            return this;
        }

        public Builder setClassroomId(Integer classroomId) {
            this.classroomId = classroomId;
            return this;
        }

        public GameSettings build() {
            return new GameSettings(
                    gameType,
                    classId,
                    withMutants,
                    withTests,
                    maxAssertionsPerTest,
                    mutantValidatorLevel,
                    chatEnabled,
                    captureIntentions,
                    equivalenceThreshold,
                    level,
                    creatorRole,
                    gameDurationMinutes,
                    startGame,
                    classroomId
            );
        }
    }
}
