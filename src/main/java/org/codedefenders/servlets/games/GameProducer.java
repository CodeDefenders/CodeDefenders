package org.codedefenders.servlets.games;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.game.AbstractGame;

// https://stackoverflow.com/questions/28855122/how-to-inject-a-http-session-attribute-to-a-bean-using-cdi
@RequestScoped
public class GameProducer implements Serializable {

    @Inject
    EventDAO eventDAO;

    private AbstractGame theGame;

    @Produces
    @Named("game")
    public AbstractGame getTheGame() {
        return this.theGame;
    }

    public void setTheGame(Integer gameId) {
        this.theGame = GameDAO.getGame(gameId);
        if (this.theGame != null) {
            this.theGame.setEventDAO(eventDAO);
        }
    }
}
