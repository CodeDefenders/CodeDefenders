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

import java.sql.Timestamp;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.EventDAO;
import org.codedefenders.database.MeleeGameDAO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Mutant.Equivalence;
import org.codedefenders.game.Mutant.State;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.scoring.ScoreCalculator;
import org.codedefenders.game.scoring.ScoringPolicyProducer;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.model.Player;
import org.codedefenders.notification.events.server.game.GameCreatedEvent;
import org.codedefenders.notification.impl.NotificationService;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.servlets.admin.AdminSystemSettings.SETTING_NAME.GAME_CREATION;
import static org.codedefenders.util.Constants.DUMMY_ATTACKER_USER_ID;
import static org.codedefenders.util.Constants.DUMMY_DEFENDER_USER_ID;

@ApplicationScoped
public class MeleeGameService extends AbstractGameService {
    private static final Logger logger = LoggerFactory.getLogger(MeleeGameService.class);

    private final GameManagingUtils gameManagingUtils;
    private final EventDAO eventDAO;
    private final MessagesBean messages;
    private final CodeDefendersAuth login;
    private final NotificationService notificationService;

    @Inject
    public MeleeGameService(UserService userService, UserRepository userRepository,
                                  GameManagingUtils gameManagingUtils, EventDAO eventDAO, MessagesBean messages,
                                  CodeDefendersAuth login, NotificationService notificationService) {
        super(userService, userRepository);
        this.gameManagingUtils = gameManagingUtils;
        this.eventDAO = eventDAO;
        this.messages = messages;
        this.login = login;
        this.notificationService = notificationService;
    }

    @Override
    protected boolean isMutantCovered(Mutant mutant, AbstractGame game, Player player) {
        return mutant.getCoveringTests(game.getTests(false)).stream()
                .anyMatch(t -> player != null && t.getPlayerId() == player.getId());
    }

    @Override
    protected boolean canViewMutant(Mutant mutant, AbstractGame game, SimpleUser user, Player player, Role playerRole) {
        if (playerRole == Role.NONE) {
            return false;
        }
        if (game.isFinished() || game.getLevel() == GameLevel.EASY) {
            return true;
        }
        if (game.getLevel() == GameLevel.HARD) {
            return mutant.getCreatorId() == user.getId()
                    || playerRole == Role.OBSERVER
                    || mutant.getState() == State.KILLED
                    || mutant.getState() == State.EQUIVALENT;
        }
        return false;
    }

    @Override
    protected boolean canMarkMutantEquivalent(Mutant mutant, AbstractGame game, SimpleUser user, Role playerRole) {
        return playerRole != Role.NONE
                && game.getState() == GameState.ACTIVE
                && mutant.getState() == State.ALIVE
                && mutant.getEquivalent() == Equivalence.ASSUMED_NO
                && mutant.getCreatorId() != Constants.DUMMY_ATTACKER_USER_ID
                && mutant.getCreatorId() != user.getId()
                && mutant.getLines().size() >= 1;
    }

    @Override
    protected boolean canViewTest(Test test, AbstractGame game, Player player, Role playerRole) {
        return game.isFinished()
                || playerRole == Role.OBSERVER
                || game.getLevel() == GameLevel.EASY
                || test.getPlayerId() == player.getId();
    }

    /**
     * Close the game and additionally write the calculated scores to persistent storage.
     *
     * @param game The game to close.
     *
     * @return {@code true} if the game was closed, {@code false} otherwise.
     */
    @Override
    public boolean closeGame(AbstractGame game) {
        boolean closed = super.closeGame(game);
        if (closed) {
            createScoreCalculator().storeScoresToDB(game.getId());
        }
        return closed;
    }

    private ScoreCalculator createScoreCalculator() {
        ScoringPolicyProducer scoringPolicyProducer = new ScoringPolicyProducer();
        return new ScoreCalculator(scoringPolicyProducer.getTheBasicPolicy(eventDAO));
    }

    public boolean createGame(MeleeGame game, boolean withMutants, boolean withTests, Role creatorRole) {
        final boolean canCreateGames = AdminDAO.getSystemSetting(GAME_CREATION).getBoolValue();
        if (!canCreateGames) {
            logger.warn("User {} tried to create a melee game, but creating games is not permitted.",
                    login.getUserId());
            messages.add("Creating games is currently not enabled.");
            return false;
        }

        game.setEventDAO(eventDAO);
        game.setUserRepository(userRepository);

        int newGameId = MeleeGameDAO.storeMeleeGame(game);
        game.setId(newGameId);

        Event event = new Event(-1, game.getId(), login.getUserId(), "Game Created", EventType.GAME_CREATED,
                EventStatus.GAME, new Timestamp(System.currentTimeMillis()));
        eventDAO.insert(event);

        // Always add system player to send mutants and tests at runtime!
        if (!game.addPlayer(DUMMY_ATTACKER_USER_ID, Role.PLAYER)) {
            return false;
        }
        // TODO: Add two players or only one?
        // TODO: If only one, then GameManagingUtils.addPredefinedMutantsAndTests needs to be changed.
        if (!game.addPlayer(DUMMY_DEFENDER_USER_ID, Role.PLAYER)) {
            return false;
        }

        // Add selected role to game if the creator participates as non-observer (i.e., player)
        if (creatorRole == Role.PLAYER) {
            if (!game.addPlayer(login.getUserId(), Role.PLAYER)) {
                return false;
            }
        }

        if (!gameManagingUtils.addPredefinedMutantsAndTests(game, withMutants, withTests)) {
            return false;
        }

        /* Publish the event that a new game started */
        GameCreatedEvent gce = new GameCreatedEvent();
        gce.setGameId(game.getId());
        notificationService.post(gce);

        return true;
    }

    public Optional<MeleeGame> rematch(MeleeGame game) {
        final boolean canCreateGames = AdminDAO.getSystemSetting(GAME_CREATION).getBoolValue();
        if (!canCreateGames) {
            logger.warn("User {} tried to create a battleground game, but creating games is not permitted.",
                    login.getUserId());
            messages.add("Creating games is currently not enabled.");
            return Optional.empty();
        }

        MeleeGame newGame = new MeleeGame.Builder(
                game.getClassId(),
                login.getUserId(),
                game.getMaxAssertionsPerTest())
                .level(game.getLevel())
                .chatEnabled(game.isChatEnabled())
                .capturePlayersIntention(game.isCapturePlayersIntention())
                .lineCoverage(game.getLineCoverage())
                .mutantCoverage(game.getMutantCoverage())
                .mutantValidatorLevel(game.getMutantValidatorLevel())
                .automaticMutantEquivalenceThreshold(game.getAutomaticMutantEquivalenceThreshold())
                .gameDurationMinutes(game.getGameDurationMinutes())
                .classroomId(game.getClassroomId().orElse(null))
                .build();

        boolean withMutants = gameManagingUtils.hasPredefinedMutants(game);
        boolean withTests = gameManagingUtils.hasPredefinedTests(game);
        Role creatorRole = game.getRole(game.getCreatorId());

        if (!createGame(newGame, withMutants, withTests, creatorRole)) {
            return Optional.empty();
        }

        for (Player player : game.getPlayers()) {
            if (player.getRole() == Role.PLAYER
                    && player.getUser().getId() != game.getCreatorId()) {
                if (!newGame.addPlayer(player.getUser().getId(), player.getRole())) {
                    return Optional.empty();
                }
            }
        }

        return Optional.of(newGame);
    }
}
