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
package org.codedefenders.game.multiplayer;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.model.Player;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.util.CDIUtil;

public abstract class AbstractMultiplayerGame extends AbstractGame {

    protected float lineCoverage;
    protected float mutantCoverage;
    protected float prize;

    protected boolean chatEnabled;

    protected int gameDurationMinutes;
    protected long startTimeUnixSeconds;
    protected long finishTimeUnixSeconds;

    // 0 means disabled
    protected int automaticMutantEquivalenceThreshold = 0;

    protected Integer classroomId;


    @Override
    public boolean isChatEnabled() {
        return chatEnabled;
    }

    public int getGameDurationMinutes() {
        return gameDurationMinutes;
    }

    public void setGameDurationMinutes(int gameDurationMinutes) {
        this.gameDurationMinutes = gameDurationMinutes;
    }

    public long getStartTimeUnixSeconds() {
        return startTimeUnixSeconds;
    }

    public long getFinishTimeUnixSeconds() {
        return finishTimeUnixSeconds;
    }

    public float getLineCoverage() {
        return lineCoverage;
    }

    public float getMutantCoverage() {
        return mutantCoverage;
    }

    public float getPrize() {
        return prize;
    }


    public boolean isLineCovered(int lineNumber) {
        for (Test test : getTests(true)) {
            if (test.getLineCoverage().getLinesCovered().contains(lineNumber)) {
                return true;
            }
        }
        return false;
    }

    public boolean removePlayer(int userId) {
        GameRepository gameRepo = CDIUtil.getBeanFromCDI(GameRepository.class);

        if (state == GameState.CREATED) {
            return gameRepo.removeUserFromGame(id, userId);
        }
        return false;
    }

    public boolean hasUserJoined(int userId) {
        GameRepository gameRepo = CDIUtil.getBeanFromCDI(GameRepository.class);

        for (Player p : gameRepo.getValidPlayersForGame(this.getId())) {
            if (p.getUser().getId() == userId) {
                return true;
            }
        }
        return false;
    }

    public abstract Role getRole(int userId);
}
