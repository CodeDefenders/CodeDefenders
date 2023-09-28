/*
 * Copyright (C) 2020 Code Defenders contributors
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.codedefenders.database.GameRepository;
import org.codedefenders.database.MutantRepository;
import org.codedefenders.database.PlayerDAO;
import org.codedefenders.database.TestRepository;
import org.codedefenders.database.TestSmellsDAO;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.dto.TestDTO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.model.Player;
import org.codedefenders.notification.INotificationService;
import org.codedefenders.notification.events.server.game.GameStartedEvent;
import org.codedefenders.notification.events.server.game.GameStoppedEvent;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.UserService;

public abstract class AbstractGameService implements IGameService {

    // TODO
    // @Inject
    // GameDAO gameDAO;
    // @Inject
    // PlayerDAO playerDAO;
    // @Inject
    // MutantDAO mutantDAO;

    protected UserRepository userRepository;
    protected UserService userService;
    protected TestRepository testRepo;
    protected MutantRepository mutantRepo;
    protected GameRepository gameRepo;

    @Inject
    public AbstractGameService(UserService userService, UserRepository userRepository,
                               TestRepository testRepo, MutantRepository mutantRepo, GameRepository gameRepo) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.testRepo = testRepo;
        this.mutantRepo = mutantRepo;
        this.gameRepo = gameRepo;
    }

    @Inject
    private INotificationService notificationService;

    @Override
    public MutantDTO getMutant(int userId, int mutantId) {
        return getMutant(userId, mutantRepo.getMutantById(mutantId));
    }

    @Override
    public MutantDTO getMutant(int userId, Mutant mutant) {
        AbstractGame game = gameRepo.getGame(mutant.getGameId());
        Player player = PlayerDAO.getPlayerForUserAndGame(userId, mutant.getGameId());
        Optional<SimpleUser> user = userService.getSimpleUserById(userId);
        if (game != null && user.isPresent()) {
            return convertMutant(mutant, user.get(), player, game);
        } else {
            return null;
        }
    }

    @Override
    public List<MutantDTO> getMutants(int userId, int gameId) {
        Optional<SimpleUser> user = userService.getSimpleUserById(userId);
        AbstractGame game = gameRepo.getGame(gameId);
        if (game != null && user.isPresent()) {
            return getMutants(user.get(), game);
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public List<MutantDTO> getMutants(SimpleUser user, AbstractGame game) {
        Player player = PlayerDAO.getPlayerForUserAndGame(user.getId(), game.getId());
        return game.getMutants().stream()
                .map(mutant -> convertMutant(mutant, user, player, game))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // NOTE: This could be split into several methods. Like: canFlag(Mutant mutant, Player player, AbstractGame game);
    //  So the actual building of the MutantDTO could happen in this class.
    protected MutantDTO convertMutant(Mutant mutant, SimpleUser user, Player player, AbstractGame game) {
        Role playerRole = determineRole(user, player, game);

        SimpleUser killedBy;
        int killedByTestId;
        String killMessage;
        Test killingTest = mutant.getKillingTest();
        if (killingTest != null) {
            killedBy = userService.getSimpleUserByPlayerId(killingTest.getPlayerId()).orElse(null);
            killedByTestId = killingTest.getId();
            killMessage = mutant.getKillMessage();
        } else {
            killedBy = null;
            killedByTestId = -1;
            killMessage = null;
        }
        boolean canView = canViewMutant(mutant, game, user, player, playerRole);
        boolean canMarkEquivalent = canMarkMutantEquivalent(mutant, game, user, playerRole);
        boolean isCovered = isMutantCovered(mutant, game, player);

        return new MutantDTO(
                mutant.getId(),
                new SimpleUser(mutant.getCreatorId(), mutant.getCreatorName()),
                mutant.getState(),
                mutant.getScore(),
                mutant.getHTMLReadout().stream()
                        .filter(Objects::nonNull).collect(Collectors.joining("<br>")),
                mutant.getLines().stream().map(String::valueOf).collect(Collectors.joining(",")),
                isCovered,
                canView,
                canMarkEquivalent,
                killedBy,
                killedByTestId,
                killMessage,
                mutant.getGameId(),
                mutant.getPlayerId(),
                mutant.getLines(),
                mutant.getPatchString()
        );
    }

    protected abstract boolean isMutantCovered(Mutant mutant, AbstractGame game, Player player);

    protected abstract boolean canViewMutant(Mutant mutant, AbstractGame game, SimpleUser user, Player player,
            Role playerRole);

    protected abstract boolean canMarkMutantEquivalent(Mutant mutant, AbstractGame game, SimpleUser user,
            Role playerRole);


    @Override
    public TestDTO getTest(int userId, int testId) {
        return getTest(userId, testRepo.getTestById(testId));
    }

    @Override
    public TestDTO getTest(int userId, Test test) {
        AbstractGame game = gameRepo.getGame(test.getGameId());
        Player player = PlayerDAO.getPlayerForUserAndGame(userId, test.getGameId());
        Optional<SimpleUser> user = userService.getSimpleUserById(userId);
        if (game != null && user.isPresent()) {
            return convertTest(test, user.get(), player, game);
        } else {
            return null;
        }
    }

    @Override
    public List<TestDTO> getTests(int userId, int gameId) {
        Optional<SimpleUser> user = userService.getSimpleUserById(userId);
        AbstractGame game = gameRepo.getGame(gameId);
        if (game != null && user.isPresent()) {
            return getTests(user.get(), game);
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public List<TestDTO> getTests(SimpleUser user, AbstractGame game) {
        Player player = PlayerDAO.getPlayerForUserAndGame(user.getId(), game.getId());
        return game.getTests().stream()
                .map(test -> convertTest(test, user, player, game))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected TestDTO convertTest(Test test, SimpleUser user, Player player, AbstractGame game) {
        Role playerRole = determineRole(user, player, game);

        boolean viewable = canViewTest(test, game, player, playerRole);

        SimpleUser creator = userService.getSimpleUserByPlayerId(test.getPlayerId()).orElse(null);

        return new TestDTO(test.getId(), creator, test.getScore(), viewable,
                test.getCoveredMutants(game.getMutants()).stream().map(Mutant::getId).collect(Collectors.toList()),
                test.getKilledMutants().stream().map(Mutant::getId).collect(Collectors.toList()),
                (new TestSmellsDAO()).getDetectedTestSmellsForTest(test),
                test.getGameId(),
                test.getPlayerId(),
                test.getLineCoverage().getLinesCovered(),
                test.getAsString()
        );
    }

    protected abstract boolean canViewTest(Test test, AbstractGame game, Player player, Role playerRole);

    // TODO:
    protected Role determineRole(SimpleUser user, Player player, AbstractGame game) {
        Role result = null;
        if (game != null) {
            if (player != null) {
                result = player.getRole();
            }
            if (player == null || result == null) {
                if (game.getCreatorId() == user.getId()) {
                    result = Role.OBSERVER;
                } else {
                    result = Role.NONE;
                }
            }
        }
        return result;
    }

    // Todo: only receive the game-Id as parameter and do not update the whole game
    @Override
    public boolean closeGame(AbstractGame game) {
        game.setState(GameState.FINISHED);
        boolean updated = game.update();

        if (updated) {
            GameStoppedEvent gse = new GameStoppedEvent();
            gse.setGameId(game.getId());
            notificationService.post(gse);
        }

        return updated;
    }

    // Todo: only receive the game-Id as parameter and do not update the whole game
    @Override
    public boolean startGame(AbstractGame game) {
        if (game.getState() == GameState.CREATED) {
            game.setState(GameState.ACTIVE);
            boolean updated = game.update();

            if (updated) {
                gameRepo.storeStartTime(game.getId());

                GameStartedEvent gse = new GameStartedEvent();
                gse.setGameId(game.getId());
                notificationService.post(gse);
            }

            return updated;
        } else {
            return false;
        }
    }
}
