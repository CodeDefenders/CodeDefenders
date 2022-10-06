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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.MeleeGameDAO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.scoring.ScoreCalculator;
import org.codedefenders.game.scoring.ScoringPolicyProducer;
import org.codedefenders.model.Player;
import org.codedefenders.util.Constants;

@ApplicationScoped
public class MeleeGameService extends AbstractGameService {

    private final ScoreCalculator scoreCalculator;

    @Inject
    public MeleeGameService(EventDAO eventDAO) {
        // create new instance instead of using injection, as games can be closed outside a request context
        this.scoreCalculator = createNewScoreCalculator(eventDAO);
    }

    @Override
    protected boolean isMutantCovered(Mutant mutant, AbstractGame game, Player player) {
        return mutant.getCoveringTests(game.getTests(false)).stream()
                .anyMatch(t -> player != null && t.getPlayerId() == player.getId());
    }

    @Override
    protected boolean canViewMutant(Mutant mutant, AbstractGame game, SimpleUser user, Player player,
            Role playerRole) {
        return playerRole != Role.NONE
                && (game.isFinished()
                || game.getLevel() == GameLevel.EASY
                || (game.getLevel() == GameLevel.HARD
                && (mutant.getCreatorId() == user.getId()
                || playerRole.equals(Role.OBSERVER)
                || mutant.getState().equals(Mutant.State.KILLED)
                || mutant.getState().equals(Mutant.State.EQUIVALENT))));
    }

    @Override
    protected boolean canMarkMutantEquivalent(Mutant mutant, AbstractGame game, SimpleUser user,
            Role playerRole) {
        return game.getState().equals(GameState.ACTIVE)
                && mutant.getState().equals(Mutant.State.ALIVE)
                && mutant.getEquivalent().equals(Mutant.Equivalence.ASSUMED_NO)
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
            scoreCalculator.storeScoresToDB(game.getId());
        }
        return closed;
    }

    private ScoreCalculator createNewScoreCalculator(EventDAO eventDAO) {
        ScoringPolicyProducer scoringPolicyProducer = new ScoringPolicyProducer();
        return new ScoreCalculator(scoringPolicyProducer.getTheBasicPolicy(eventDAO));
    }
}
