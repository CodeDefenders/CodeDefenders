package org.codedefenders.servlets.games;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.game.AbstractGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequestScoped
public class GameProducer implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(GameProducer.class);

    @Inject
    private EventDAO eventDAO;

    private Integer gameId = null;
    private AbstractGame game = null;
    private boolean gameQueried = false;

    /**
     * Tries to find a game for the former set gameId {@link #setGameId(Integer)} and cast it to the required type.
     *
     * @param <GameT> The type of game you need.
     * @return The game with the required type if it exists otherwise null.
     */
    public <GameT extends AbstractGame> GameT getGame() {
        if (gameId == null) {
            return null;
        }
        if (game == null && !gameQueried) {
            gameQueried = true;
            game = GameDAO.getGame(gameId);
            if (game != null) {
                game.setEventDAO(eventDAO);
            }
        }
        try {
            //This has to be an unchecked cast, because the (game instanceOf GameT) check doesn't work
            //noinspection unchecked
            return (GameT) game;
        } catch (ClassCastException e) {
            return null;
        }
    }

    public void setGameId(Integer gameId) {
        this.gameId = gameId;
    }
}
