package org.codedefenders.servlets.games;

import java.io.Serializable;

import javax.annotation.Nullable;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.persistence.database.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequestScoped
@Named("gameProducer")
public class GameProducer implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(GameProducer.class);

    private final EventDAO eventDAO;

    @Inject
    private UserRepository userRepo;

    private Integer gameId = null;
    private AbstractGame game = null;
    private boolean gameQueried = false;

    @Inject
    public GameProducer(EventDAO eventDAO) {
        this.eventDAO = eventDAO;
    }

    /**
     * Tries to find a game for the former set gameId {@link #setGameId(Integer)}.
     *
     * <p>If you only need a specific game type use on of {@link #getMultiplayerGame()},
     * {@link #getMeleeGame()}, or {@link #getPuzzleGame()}.
     *
     * @return The game if it exists otherwise null.
     */
    @Nullable
    public AbstractGame getGame() {
        if (gameId == null) {
            return null;
        }
        if (game == null && !gameQueried) {
            game = GameDAO.getGame(gameId);
            gameQueried = true;
            if (game != null) {
                game.setEventDAO(eventDAO);
                game.setUserRepository(userRepo);
            } else {
                logger.debug("Could not retrieve game with id {} from database", gameId);
            }
        }
        return game;
    }

    @Nullable
    public MultiplayerGame getMultiplayerGame() {
        AbstractGame game = getGame();
        if (game instanceof MultiplayerGame) {
            return (MultiplayerGame) game;
        } else {
            return null;
        }
    }

    @Nullable
    public MeleeGame getMeleeGame() {
        AbstractGame game = getGame();
        if (game instanceof MeleeGame) {
            return (MeleeGame) game;
        } else {
            return null;
        }
    }

    @Nullable
    public PuzzleGame getPuzzleGame() {
        AbstractGame game = getGame();
        if (game instanceof PuzzleGame) {
            return (PuzzleGame) game;
        } else {
            return null;
        }
    }


    public void setGameId(Integer gameId) {
        this.gameId = gameId;
    }
}
