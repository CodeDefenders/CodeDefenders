/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.service.game;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.dto.MutantDTO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.dto.TestDTO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.game.puzzle.PuzzleGame;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.persistence.database.TestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class GameService implements IGameService {

    private static final Logger logger = LoggerFactory.getLogger(GameService.class);

    private final MultiplayerGameService multiplayerGameService;
    private final MeleeGameService meleeGameService;
    private final PuzzleGameService puzzleGameService;
    protected TestRepository testRepo;
    protected MutantRepository mutantRepo;
    protected GameRepository gameRepo;

    @Inject
    public GameService(MultiplayerGameService multiplayerGameService, MeleeGameService meleeGameService,
                       PuzzleGameService puzzleGameService, TestRepository testRepo, MutantRepository mutantRepo,
                       GameRepository gameRepo) {
        this.multiplayerGameService = multiplayerGameService;
        this.meleeGameService = meleeGameService;
        this.puzzleGameService = puzzleGameService;
        this.testRepo = testRepo;
        this.mutantRepo = mutantRepo;
        this.gameRepo = gameRepo;
    }

    @Override
    public MutantDTO getMutant(int userId, int mutantId) {
        // I can't delegate this to the other services, as the game type is still unknown.
        Mutant mutant = mutantRepo.getMutantById(mutantId);
        if (mutant != null) {
            return getMutant(userId, mutant);
        } else {
            return null;
        }
    }

    @Override
    public MutantDTO getMutant(int userId, Mutant mutant) {
        IGameService gameService = getGameServiceForGameId(mutant.getGameId());
        if (gameService != null) {
            return gameService.getMutant(userId, mutant);
        } else {
            return null;
        }
    }

    @Override
    public List<MutantDTO> getMutants(int userId, int gameId) {
        IGameService gameService = getGameServiceForGameId(gameId);
        if (gameService != null) {
            return gameService.getMutants(userId, gameId);
        } else {
            return null;
        }
    }

    @Override
    public List<MutantDTO> getMutants(SimpleUser user, AbstractGame game) {
        IGameService gameService = getGameServiceForGame(game);
        if (gameService != null) {
            return gameService.getMutants(user, game);
        } else {
            return null;
        }
    }

    @Override
    public TestDTO getTest(int userId, int testId) {
        // I can't delegate this to the other services, as the game type is still unknown.
        Test test = testRepo.getTestById(testId);
        if (test != null) {
            return getTest(userId, test);
        } else {
            return null;
        }
    }

    @Override
    public TestDTO getTest(int userId, Test test) {
        IGameService gameService = getGameServiceForGameId(test.getGameId());
        if (gameService != null) {
            return gameService.getTest(userId, test);
        } else {
            return null;
        }
    }

    @Override
    public List<TestDTO> getTests(int userId, int gameId) {
        IGameService gameService = getGameServiceForGameId(gameId);
        if (gameService != null) {
            return gameService.getTests(userId, gameId);
        } else {
            return null;
        }
    }

    @Override
    public List<TestDTO> getTests(SimpleUser user, AbstractGame game) {
        IGameService gameService = getGameServiceForGame(game);
        if (gameService != null) {
            return gameService.getTests(user, game);
        } else {
            return null;
        }
    }

    @Override
    public boolean closeGame(AbstractGame game) {
        IGameService gameService = getGameServiceForGame(game);
        if (gameService != null) {
            boolean closed = gameService.closeGame(game);
            if (closed) {
                logger.info("Closed game with id {}", game.getId());
            } else {
                logger.warn("Failed to close game with id {}", game.getId());
            }
            return closed;
        }
        return false;
    }

    @Override
    public boolean startGame(AbstractGame game) {
        IGameService gameService = getGameServiceForGame(game);
        if (gameService != null) {
            return gameService.startGame(game);
        } else {
            return false;
        }
    }

    /**
     * Fetches and closes all expired multiplayer and melee games.
     *
     * @return the amount of games closed.
     */
    public int closeExpiredGames() {
        final List<AbstractGame> expiredGames = gameRepo.getExpiredGames();
        int closedGames = 0;

        for (AbstractGame game : expiredGames) {
            final boolean closed = closeGame(game);
            closedGames += closed ? 1 : 0;
        }
        return closedGames;
    }

    private IGameService getGameServiceForGameId(int gameId) {
        GameMode mode = gameRepo.getGameMode(gameId).orElseThrow();
        switch (mode) {
            case PARTY:
                return multiplayerGameService;
            case MELEE:
                return meleeGameService;
            case PUZZLE:
                return puzzleGameService;
            default:
                // TODO This might require some love, like an exception...
                return null;
        }
    }

    // TODO Honestly, this smells like some anti-pattern... the right service should
    // be instantiated by the CI directly
    private IGameService getGameServiceForGame(AbstractGame game) {
        if (game instanceof MultiplayerGame) {
            return multiplayerGameService;
        } else if (game instanceof MeleeGame) {
            return meleeGameService;
        } else if (game instanceof PuzzleGame) {
            return puzzleGameService;
        }
        // TODO This might require some love, like an exception...
        return null;
    }
}
