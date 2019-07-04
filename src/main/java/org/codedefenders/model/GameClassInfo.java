package org.codedefenders.model;

import org.codedefenders.game.GameClass;

/**
 * This class serves as a container class for {@link GameClass GameClasses} and
 * additional information.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 */
public class GameClassInfo {
    private GameClass gameClass;
    private int gamesWithClass;

    public GameClassInfo(GameClass gameClass, int gamesWithClass) {
        this.gameClass = gameClass;
        this.gamesWithClass = gamesWithClass;
    }

    public GameClass getGameClass() {
        return gameClass;
    }

    public int getGamesWithClass() {
        return gamesWithClass;
    }

    public boolean isDeletable() {
        return gamesWithClass == 0;
    }
}
