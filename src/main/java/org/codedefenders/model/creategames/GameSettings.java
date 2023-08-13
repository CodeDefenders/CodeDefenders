package org.codedefenders.model.creategames;

import java.io.Serializable;
import java.util.Optional;

import org.codedefenders.database.AdminDAO;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameType;
import org.codedefenders.game.Role;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.validation.code.CodeValidatorLevel;

import com.google.gson.annotations.Expose;

public class GameSettings implements Serializable {
    @Expose private final GameType gameType;

    @Expose private final Integer classId;
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
            Integer classId,
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

    public static Builder builder() {
        return new Builder();
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
        private Integer classId;
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

        public Builder() {

        }

        public Builder withDefaultSettings() {
            final int currentDefaultGameDurationMinutes = AdminDAO.getSystemSetting(
                    AdminSystemSettings.SETTING_NAME.GAME_DURATION_MINUTES_DEFAULT).getIntValue();
            this.gameType = GameType.MULTIPLAYER;
            this.classId = null;
            this.withMutants = false;
            this.withTests = false;
            this.maxAssertionsPerTest = 3;
            this.mutantValidatorLevel = CodeValidatorLevel.MODERATE;
            this.chatEnabled = true;
            this.captureIntentions = false;
            this.equivalenceThreshold = 0;
            this.level = GameLevel.HARD;
            this.creatorRole = Role.OBSERVER;
            this.gameDurationMinutes = currentDefaultGameDurationMinutes;
            this.startGame = false;
            this.classroomId = null;
            return this;
        }

        public Builder withSettings(GameSettings other) {
            this.gameType = other.gameType;
            this.classId = other.classId;
            this.withMutants = other.withMutants;
            this.withTests = other.withTests;
            this.maxAssertionsPerTest = other.maxAssertionsPerTest;
            this.mutantValidatorLevel = other.mutantValidatorLevel;
            this.chatEnabled = other.chatEnabled;
            this.captureIntentions = other.captureIntentions;
            this.equivalenceThreshold = other.equivalenceThreshold;
            this.level = other.level;
            this.creatorRole = other.creatorRole;
            this.gameDurationMinutes = other.gameDurationMinutes;
            this.startGame = other.startGame;
            this.classroomId = other.classroomId;
            return this;
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
