package org.codedefenders.model.creategames;

import java.io.Serializable;
import java.util.Optional;

import org.codedefenders.database.AdminDAO;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameType;
import org.codedefenders.game.Role;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.validation.code.CodeValidatorLevel;

import com.google.gson.annotations.Expose;

import static org.codedefenders.game.GameLevel.HARD;
import static org.codedefenders.game.GameType.MULTIPLAYER;
import static org.codedefenders.game.Role.OBSERVER;
import static org.codedefenders.validation.code.CodeValidatorLevel.MODERATE;

public class GameSettings implements Serializable {
    @Expose private GameType gameType;

    @Expose private GameClass cut;
    @Expose private Boolean withMutants;
    @Expose private Boolean withTests;

    @Expose private Integer maxAssertionsPerTest;
    @Expose private CodeValidatorLevel mutantValidatorLevel;
    @Expose private Boolean chatEnabled;
    @Expose private Boolean captureIntentions;
    @Expose private Integer equivalenceThreshold;
    @Expose private GameLevel level;
    @Expose private Role creatorRole;
    @Expose private Integer gameDurationMinutes;

    @Expose private Boolean startGame;

    @Expose private Integer classroomId;

    /**
     * Creates a new GameSettings object with empty settings.
     */
    public GameSettings() {

    }

    /**
     * Creates a copy of the given GameSettings object.
     *
     * @param other The settings to copy.
     */
    public GameSettings(GameSettings other) {
        this.gameType = other.gameType;
        this.cut = other.cut;
        this.withMutants = other.withMutants;
        this.withTests = other.withTests;
        this.maxAssertionsPerTest = other.maxAssertionsPerTest;
        this.mutantValidatorLevel = other.mutantValidatorLevel;
        this.chatEnabled = other.chatEnabled;
        this.captureIntentions = other.captureIntentions;
        this.equivalenceThreshold = other.equivalenceThreshold;
        this.level = other.level;
        this.creatorRole = other.creatorRole;
        this.startGame = other.startGame;
        this.gameDurationMinutes = other.gameDurationMinutes;
        this.classroomId = other.classroomId;
    }

    public GameType getGameType() {
        return gameType;
    }

    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }

    public GameClass getCut() {
        return cut;
    }

    public void setCut(GameClass cut) {
        this.cut = cut;
    }

    public boolean isWithMutants() {
        return withMutants;
    }

    public void setWithMutants(boolean withMutants) {
        this.withMutants = withMutants;
    }

    public boolean isWithTests() {
        return withTests;
    }

    public void setWithTests(boolean withTests) {
        this.withTests = withTests;
    }

    public int getMaxAssertionsPerTest() {
        return maxAssertionsPerTest;
    }

    public void setMaxAssertionsPerTest(int maxAssertionsPerTest) {
        this.maxAssertionsPerTest = maxAssertionsPerTest;
    }

    public CodeValidatorLevel getMutantValidatorLevel() {
        return mutantValidatorLevel;
    }

    public void setMutantValidatorLevel(CodeValidatorLevel mutantValidatorLevel) {
        this.mutantValidatorLevel = mutantValidatorLevel;
    }

    public boolean isChatEnabled() {
        return chatEnabled;
    }

    public void setChatEnabled(boolean chatEnabled) {
        this.chatEnabled = chatEnabled;
    }

    public boolean isCaptureIntentions() {
        return captureIntentions;
    }

    public void setCaptureIntentions(boolean captureIntentions) {
        this.captureIntentions = captureIntentions;
    }

    public int getEquivalenceThreshold() {
        return equivalenceThreshold;
    }

    public void setEquivalenceThreshold(int equivalenceThreshold) {
        this.equivalenceThreshold = equivalenceThreshold;
    }

    public GameLevel getLevel() {
        return level;
    }

    public void setLevel(GameLevel level) {
        this.level = level;
    }

    public Role getCreatorRole() {
        return creatorRole;
    }

    public void setCreatorRole(Role creatorRole) {
        this.creatorRole = creatorRole;
    }

    public int getGameDurationMinutes() {
        return gameDurationMinutes;
    }

    public void setGameDurationMinutes(final int duration) {
        this.gameDurationMinutes = duration;
    }

    public boolean isStartGame() {
        return startGame;
    }

    public void setStartGame(boolean startGame) {
        this.startGame = startGame;
    }

    public Optional<Integer> getClassroomId() {
        return Optional.ofNullable(classroomId);
    }

    public void setClassroomId(Integer classroomId) {
        this.classroomId = classroomId;
    }

    public static GameSettings getDefault() {
        GameSettings gameSettings = new GameSettings();
        gameSettings.setGameType(MULTIPLAYER);
        gameSettings.setWithMutants(false);
        gameSettings.setWithTests(false);
        gameSettings.setMaxAssertionsPerTest(3);
        gameSettings.setMutantValidatorLevel(MODERATE);
        gameSettings.setChatEnabled(true);
        gameSettings.setCaptureIntentions(false);
        gameSettings.setEquivalenceThreshold(0);
        gameSettings.setLevel(HARD);
        gameSettings.setCreatorRole(OBSERVER);
        gameSettings.setStartGame(false);
        gameSettings.setClassroomId(null);

        final int currentDefaultGameDurationMinutes = AdminDAO.getSystemSetting(
                AdminSystemSettings.SETTING_NAME.GAME_DURATION_MINUTES_DEFAULT).getIntValue();
        gameSettings.setGameDurationMinutes(currentDefaultGameDurationMinutes);

        return gameSettings;
    }
}
