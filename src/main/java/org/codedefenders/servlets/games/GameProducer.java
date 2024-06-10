package org.codedefenders.servlets.games;

import java.io.Serializable;

import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.codedefenders.database.EventDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequestScoped
@Named("gameProducer")
public class GameProducer implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(GameProducer.class);

    private final EventDAO eventDAO;
    private final UserRepository userRepo;
    private final GameRepository gameRepo;

    private Integer gameId = null;
    private AbstractGame game = null;
    private boolean gameQueried = false;

    @Inject
    public GameProducer(EventDAO eventDAO, UserRepository userRepo, GameRepository gameRepo) {
        this.eventDAO = eventDAO;
        this.userRepo = userRepo;
        this.gameRepo = gameRepo;
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
            game = gameRepo.getGame(gameId);
            gameQueried = true;
            if (game == null) {
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
