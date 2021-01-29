package org.codedefenders.servlets.games;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// https://stackoverflow.com/questions/28855122/how-to-inject-a-http-session-attribute-to-a-bean-using-cdi
@RequestScoped // This is for injecting the GameProducer into the filter the sets it up
public class GameProducer implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(GameProducer.class);

    @Inject
    private EventDAO eventDAO;

    private AbstractGame game;


    public AbstractGame getGame() {
        return this.game;
    }

    public MultiplayerGame getMultiplayerGame() {
        return (MultiplayerGame) this.game;
    }

    public MeleeGame getMeleeGAme() {
        return (MeleeGame) this.game;
    }

    public PuzzleGame getPuzzleGAme() {
        return (PuzzleGame) this.game;
    }

    public void setGameId(Integer gameId) {
        this.game = GameDAO.getGame(gameId);
        if (this.game != null) {
            this.game.setEventDAO(eventDAO);
        }
    }
}
