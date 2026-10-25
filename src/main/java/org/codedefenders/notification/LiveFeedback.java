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
package org.codedefenders.notification;

import java.util.Collection;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.highlighting.GHMutantDTO;
import org.codedefenders.game.highlighting.GHTestDTO;
import org.codedefenders.model.Player;
import org.codedefenders.notification.events.server.highlighting.MutantAddedEvent;
import org.codedefenders.notification.events.server.highlighting.MutantUpdatedEvent;
import org.codedefenders.notification.events.server.highlighting.TestAddedEvent;
import org.codedefenders.notification.impl.NotificationService;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.servlets.games.GameManagingUtils;

/**
 * Holds methods for sending live updates on the game state to the players. In its own {@link ApplicationScoped} class
 * as opposed to {@link GameManagingUtils} so that it can be used without needing a working RequestFocus.
 */
@ApplicationScoped
public class LiveFeedback {

    @Inject
    NotificationService notificationService;

    @Inject
    GameRepository gameRepo;

    public void notifyPlayersOnMutantUpdated(AbstractGame game, Mutant mutant) {
        notifyPlayersOnMutantUpdated(mutant, GameManagingUtils.getAllPlayersInGame(game), game.getId());
    }

    public void notifyPlayersOnMutantUpdated(int gameId, Mutant mutant) {
        var game = gameRepo.getGame(gameId);
        if (game != null) {
            notifyPlayersOnMutantUpdated(game, mutant);
        }
    }

    public void notifyPlayersOnTestAdded(Test test, int gameId) {
        var tae = new TestAddedEvent();
        tae.setTest(new GHTestDTO(test));
        tae.setGameId(gameId);
        tae.setCoveredLines(test.getLineCoverage().getLinesCovered().toArray(new Integer[]{}));
        notificationService.post(tae);
    }

    public void notifyPlayersOnMutantAdded(Mutant mutant, AbstractGame game) {
        notifyPlayersOnMutantAdded(mutant, GameManagingUtils.getAllPlayersInGame(game), game.getId());
    }

    public void notifyPlayersOnMutantAdded(Mutant mutant, Collection<Player> gamePlayers, int gameId) {
        var creatorId = mutant.getCreatorId();
        gamePlayers.removeIf(p -> p.getUser().getId() == creatorId);
        gamePlayers.stream().map(player -> {
            var uid = player.getUser().getId();
            var mte = new MutantAddedEvent();
            mte.setUserId(uid);
            mte.setGameId(gameId);
            mte.setMutant(new GHMutantDTO(mutant, uid));
            return mte;
        }).forEach(notificationService::post);
    }

    private void notifyPlayersOnMutantUpdated(Mutant mutant, Collection<Player> gamePlayers, int gameId) {
        gamePlayers.stream().map(player -> {
            var uid = player.getUser().getId();
            var mue = new MutantUpdatedEvent();
            mue.setUserId(uid);
            mue.setGameId(gameId);
            mue.setMutant(new GHMutantDTO(mutant, uid));
            return mue;
        }).forEach(notificationService::post);
    }
}
