package org.codedefenders.dto.api;

import org.codedefenders.beans.admin.StagedGameList;
import org.codedefenders.validation.code.CodeValidatorLevel;

public class APIGameSettings {
    private StagedGameList.GameSettings.GameType gameType;
    private org.codedefenders.game.GameLevel gameLevel;
    private CodeValidatorLevel mutantValidatorLevel;
    private int maxAssertionsPerTest;
    private int autoEquivalenceThreshold;

    public StagedGameList.GameSettings.GameType getGameType() {
        return gameType;
    }

    public org.codedefenders.game.GameLevel getGameLevel() {
        return gameLevel;
    }

    public CodeValidatorLevel getMutantValidatorLevel() {
        return mutantValidatorLevel;
    }

    public int getMaxAssertionsPerTest() {
        return maxAssertionsPerTest;
    }

    public int getAutoEquivalenceThreshold() {
        return autoEquivalenceThreshold;
    }
}
