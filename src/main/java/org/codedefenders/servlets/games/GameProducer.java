package org.codedefenders.servlets.games;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.game.AbstractGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// https://stackoverflow.com/questions/28855122/how-to-inject-a-http-session-attribute-to-a-bean-using-cdi
@RequestScoped // This is for injecting the GameProducer into the filter the sets it up
public class GameProducer implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(GameProducer.class);

    @Inject
    private EventDAO eventDAO;

    private Integer gameId;

    private AbstractGame game;

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
        if (game == null) {
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
