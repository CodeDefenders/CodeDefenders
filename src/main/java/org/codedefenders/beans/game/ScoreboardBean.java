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
package org.codedefenders.beans.game;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import jakarta.enterprise.context.RequestScoped;

import org.codedefenders.game.multiplayer.PlayerScore;
import org.codedefenders.model.Player;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.persistence.database.TestRepository;
import org.codedefenders.util.CDIUtil;

/**
 * <p>Provides data for the scoreboard game component.</p>
 * <p>Bean Name: {@code scoreboard}</p>
 */
@RequestScoped
public class ScoreboardBean {
    private Integer gameId;

    private Map<Integer, PlayerScore> mutantsScores;
    private Map<Integer, PlayerScore> testScores;

    private List<Player> attackers;
    private List<Player> defenders;

    public ScoreboardBean() {
        gameId = null;
        mutantsScores = null;
        testScores = null;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public void setScores(Map<Integer, PlayerScore> mutantsScores, Map<Integer, PlayerScore> testScores) {
        this.mutantsScores = Collections.unmodifiableMap(mutantsScores);
        this.testScores = Collections.unmodifiableMap(testScores);
    }

    public void setPlayers(List<Player> attackers, List<Player> defenders) {
        this.attackers = Collections.unmodifiableList(attackers);
        this.defenders = Collections.unmodifiableList(defenders);
    }

    // --------------------------------------------------------------------------------

    public int getGameId() {
        return gameId;
    }

    public Map<Integer, PlayerScore> getMutantsScores() {
        return mutantsScores;
    }

    public Map<Integer, PlayerScore> getTestScores() {
        return testScores;
    }

    public List<Player> getAttackers() {
        return attackers;
    }

    public List<Player> getDefenders() {
        return defenders;
    }

    // --------------------------------------------------------------------------------

    public int getTotalAttackerScore() {
        return mutantsScores.keySet().stream()
                .filter(id -> id == -1 || id == -2) // TODO: what's id == -2?
                .mapToInt(id -> mutantsScores.get(id).getTotalScore())
                .sum();
    }

    public int getTotalDefenderScore() {
        return testScores.getOrDefault(-1, new PlayerScore(-1)).getTotalScore();
    }

    public PlayerStatus getStatusForPlayer(int playerId) {
        int attackerScore = getTotalAttackerScore();
        int defenderScore = getTotalDefenderScore();

        Predicate<Player> isPlayer = p -> p.getId() == playerId;
        boolean isDefender = defenders.stream().anyMatch(isPlayer);
        boolean isAttacker = attackers.stream().anyMatch(isPlayer);

        if (!isDefender && !isAttacker) {
            return PlayerStatus.NOT_INVOLVED;
        }
        if (attackerScore > defenderScore) {
            return isAttacker ? PlayerStatus.WINNING_ATTACKER : PlayerStatus.LOSING_DEFENDER;
        }
        if (attackerScore < defenderScore) {
            return isDefender ? PlayerStatus.WINNING_DEFENDER : PlayerStatus.LOSING_ATTACKER;
        }
        return isDefender ? PlayerStatus.TIE_DEFENDER : PlayerStatus.TIE_ATTACKER;
    }

    public boolean gameHasPredefinedTests() {
        TestRepository testRepo = CDIUtil.getBeanFromCDI(TestRepository.class);
        return testRepo.gameHasPredefinedTests(gameId);
    }

    public boolean gameHasPredefinedMutants() {
        MutantRepository mutantRepo = CDIUtil.getBeanFromCDI(MutantRepository.class);
        return mutantRepo.gameHasPredefinedMutants(gameId);
    }

    public enum PlayerStatus {
        NOT_INVOLVED, WINNING_ATTACKER, LOSING_ATTACKER, WINNING_DEFENDER, LOSING_DEFENDER, TIE_ATTACKER, TIE_DEFENDER
    }
}
