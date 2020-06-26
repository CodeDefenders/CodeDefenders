package org.codedefenders.servlets.games;

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.codedefenders.database.GameDAO;
import org.codedefenders.game.AbstractGame;

// https://stackoverflow.com/questions/28855122/how-to-inject-a-http-session-attribute-to-a-bean-using-cdi
@RequestScoped
public class GameProducer implements Serializable {

    private AbstractGame theGame;

    @Produces
    @Named("game")
    public AbstractGame getTheGame() {
        return this.theGame;
    }

    public void setTheGame(Integer gameId) {
        // TODO Replace this with @Inject gameDAO
        this.theGame = GameDAO.getGame(gameId);
    }

}
