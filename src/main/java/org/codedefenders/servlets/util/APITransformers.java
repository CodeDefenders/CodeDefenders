package org.codedefenders.servlets.util;

import org.codedefenders.beans.admin.StagedGameList;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.dto.api.APIGameSettings;
import org.codedefenders.dto.api.NewGameRequest;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.Role;
import org.codedefenders.validation.code.CodeValidatorLevel;

import com.google.gson.annotations.Expose;

public class APITransformers {
    public static StagedGameList.GameSettings NewGameRequestToGameSettings(NewGameRequest game) {
        StagedGameList.GameSettings gameSettings=new StagedGameList.GameSettings();
        APIGameSettings apiGameSettings=game.getSettings();
        gameSettings.setGameType(apiGameSettings.getGameType());
        gameSettings.setCut(GameClassDAO.getClassForId(game.getClassId()));
        gameSettings.setWithMutants(false);
        gameSettings.setWithTests(false);
        gameSettings.setMaxAssertionsPerTest(apiGameSettings.getMaxAssertionsPerTest());
        gameSettings.setMutantValidatorLevel(apiGameSettings.getMutantValidatorLevel());
        gameSettings.setChatEnabled(true);
        gameSettings.setCaptureIntentions(false);
        gameSettings.setEquivalenceThreshold(apiGameSettings.getAutoEquivalenceThreshold());
        gameSettings.setLevel(apiGameSettings.getGameLevel());
        gameSettings.setCreatorRole(Role.OBSERVER);
        gameSettings.setStartGame(false);
        return gameSettings;
    }
}
