package org.codedefenders.beans.game;

import java.util.List;

import jakarta.enterprise.context.RequestScoped;

import org.codedefenders.game.GameHighlightingDTO;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.util.JSONUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * <p>Provides data for the game highlighting game component.</p>
 * <p>Bean Name: {@code gameHighlighting}</p>
 */
@RequestScoped
public class GameHighlightingBean {
    /**
     * Contains the test and mutant information to be displayed in the highlighting.
     */
    // TODO: Move code from GameHighlightingDTO here?
    private GameHighlightingDTO gameHighlightingData;

    /**
     * Show a button to flag a selected mutant as equivalent.
     */
    private Boolean enableFlagging;

    /**
     * The game mode of the currently played game.
     * Used to determine how to flag mutants.
     */
    private GameMode gameMode;

    /**
     * The game id of the currently played game.
     * Used for URL parameters.
     */
    private Integer gameId;

    public GameHighlightingBean() {
        enableFlagging = null;
        gameHighlightingData = null;
        gameMode = null;
        gameId = null;
    }

    public void setGameData(List<Mutant> mutants, List<Test> tests) {
        this.setGameData(mutants, tests, null);
    }

    public void setGameData(List<Mutant> mutants, List<Test> tests, Integer userId) {
        gameHighlightingData = new GameHighlightingDTO(mutants, tests, userId);
    }

    public void setAlternativeTests(List<Test> tests) {
        gameHighlightingData.setAlternativeTestData(tests);
    }

    public void setEnableFlagging(boolean enableFlagging) {
        this.enableFlagging = enableFlagging;
    }

    public void setFlaggingData(GameMode gameMode, int gameId) {
        this.gameMode = gameMode;
        this.gameId = gameId;
    }

    // --------------------------------------------------------------------------------

    public GameMode getGameMode() {
        return gameMode;
    }

    public Boolean getEnableFlagging() {
        return enableFlagging;
    }

    public Integer getGameId() {
        return gameId;
    }

    public String getJSON() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new JSONUtils.MapTypeAdapterFactory())
                .create();
        return gson.toJson(gameHighlightingData);
    }
}
