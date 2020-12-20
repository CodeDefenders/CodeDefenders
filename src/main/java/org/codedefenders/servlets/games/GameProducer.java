package org.codedefenders.servlets.games;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

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

    private AbstractGame theGame;

    @Produces
    @RequestScoped
    @Named("AbstractGame")
    public AbstractGame getTheGame() {
        return this.theGame;
    }

    @Produces
    @RequestScoped
    @Named("MultiplayerGame")
    public MultiplayerGame getTheMPGame() {
        return (MultiplayerGame) this.theGame;
    }
    
    @Produces
    @RequestScoped
    @Named("MeleeGame")
    public MeleeGame getTheGameAsMeleeGame() {
        return (MeleeGame) this.theGame;
    }
    
    @Produces
    @RequestScoped
    @Named("PuzzleGame")
    public PuzzleGame getTheGameAsPuzzleGame() {
        return (PuzzleGame) this.theGame;
    }
    
    public void setTheGame(Integer gameId) {
        this.theGame = GameDAO.getGame(gameId);
        if (this.theGame != null) {
            this.theGame.setEventDAO(eventDAO);
        }
    }
}
