/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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

import static org.codedefenders.game.Mutant.Equivalence.ASSUMED_YES;
import static org.codedefenders.game.Mutant.Equivalence.DECLARED_YES;
import static org.codedefenders.game.Mutant.Equivalence.PENDING_TEST;
import static org.codedefenders.game.Mutant.Equivalence.PROVEN_NO;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.UserDAO;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;
import org.codedefenders.model.Player;
import org.codedefenders.model.User;

public class MeleeGame extends MultiplayerGame {

    public static class Builder extends MultiplayerGame.Builder<Builder> {

        public Builder(int classId, int creatorId, int maxAssertionsPerTest, boolean forceHamcrest) {
            super(classId, creatorId, maxAssertionsPerTest, forceHamcrest);
        }

        public MeleeGame build() {
            return new MeleeGame(this);
        }
    }

    protected MeleeGame(Builder builder) {
        super(builder);
        this.mode = GameMode.MELEE;
    }

    /*
     * Every user has two players, one as defender and one as attacker
     */
    public List<Player> getPlayers() {
        List<Player> players = GameDAO.getPlayersForGame(getId(), Role.DEFENDER);
        players.addAll(GameDAO.getPlayersForGame(getId(), Role.ATTACKER));
        return players;
    }

    public boolean addPlayer(int userId) {
        if (canJoinGame(userId) && addPlayerForce(userId, Role.ATTACKER) && addPlayerForce(userId, Role.DEFENDER)) {
            User u = UserDAO.getUserById(userId);
            final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            Event e = new Event(-1, id, userId, u.getUsername() + " joined melee game", EventType.GAME_PLAYER_JOINED,
                    EventStatus.GAME, timestamp);
            e.insert();
            Event notif = new Event(-1, id, userId, "You joined melee game", EventType.GAME_PLAYER_JOINED,
                    EventStatus.NEW, timestamp);
            notif.insert();
            //
            return true;
        } else {
            return false;
        }
    }

    public boolean addPlayerForce(int userId, Role role) {
        if (state == GameState.FINISHED) {
            return false;
        }
        if (!GameDAO.addPlayerToGame(id, userId, role)) {
            return false;
        }

        return true;
    }

    public boolean removePlayer(int userId) {
        if (state == GameState.CREATED) {
            return GameDAO.removeUserFromGame(id, userId);
        }
        return false;
    }

    /**
     * 
     * TODO This most likely need an update to account for the fact that the same
     * user plays attack and defense !
     * 
     * This method calculates the mutant score for every attacker in the game. The
     * result is a mapping of playerId to player score.
     *
     * @return mapping from playerId to player score.
     */
    public HashMap<Integer, PlayerScore> getMutantScores() {
        HashMap<Integer, PlayerScore> mutantScores = new HashMap<Integer, PlayerScore>();

        HashMap<Integer, Integer> mutantsAlive = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> mutantsKilled = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> mutantsEquiv = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> mutantsChallenged = new HashMap<Integer, Integer>();
        HashMap<Integer, Integer> duelsWon = new HashMap<Integer, Integer>();

        // TODO why not getMutants()
        List<Mutant> allMutants = getAliveMutants();
        allMutants.addAll(getKilledMutants());
        allMutants.addAll(getMutantsMarkedEquivalent());
        allMutants.addAll(getMutantsMarkedEquivalentPending());

        if (!mutantScores.containsKey(-1)) {
            mutantScores.put(-1, new PlayerScore(-1));
            mutantsAlive.put(-1, 0);
            mutantsEquiv.put(-1, 0);
            mutantsChallenged.put(-1, 0);
            mutantsKilled.put(-1, 0);
            duelsWon.put(-1, 0);
        }

        for (Mutant mm : allMutants) {

            if (!mutantScores.containsKey(mm.getPlayerId())) {
                mutantScores.put(mm.getPlayerId(), new PlayerScore(mm.getPlayerId()));
                mutantsAlive.put(mm.getPlayerId(), 0);
                mutantsEquiv.put(mm.getPlayerId(), 0);
                mutantsChallenged.put(mm.getPlayerId(), 0);
                mutantsKilled.put(mm.getPlayerId(), 0);
                duelsWon.put(mm.getPlayerId(), 0);
            }

            PlayerScore ps = mutantScores.get(mm.getPlayerId());
            ps.increaseQuantity();
            ps.increaseTotalScore(mm.getScore());

            PlayerScore ts = mutantScores.get(-1);
            ts.increaseQuantity();
            ts.increaseTotalScore(mm.getScore());

            if (mm.getEquivalent().equals(ASSUMED_YES) || mm.getEquivalent().equals(DECLARED_YES)) {
                mutantsEquiv.put(mm.getPlayerId(), mutantsEquiv.get(mm.getPlayerId()) + 1);
                mutantsEquiv.put(-1, mutantsEquiv.get(-1) + 1);
            } else if (mm.isAlive()) {
                // This includes mutants marked equivalent
                mutantsAlive.put(mm.getPlayerId(), mutantsAlive.get(mm.getPlayerId()) + 1);
                mutantsAlive.put(-1, mutantsAlive.get(-1) + 1);
                if (mm.getEquivalent().equals(PENDING_TEST)) {
                    mutantsChallenged.put(mm.getPlayerId(), mutantsChallenged.get(mm.getPlayerId()) + 1);
                    mutantsChallenged.put(-1, mutantsChallenged.get(-1) + 1);
                }
            } else {
                mutantsKilled.put(mm.getPlayerId(), mutantsKilled.get(mm.getPlayerId()) + 1);
                mutantsKilled.put(-1, mutantsKilled.get(-1) + 1);
                if (mm.getEquivalent().equals(PROVEN_NO)) {
                    duelsWon.put(mm.getPlayerId(), duelsWon.get(mm.getPlayerId()) + 1);
                    duelsWon.put(-1, duelsWon.get(-1) + 1);
                }
            }

        }

        for (int i : mutantsKilled.keySet()) {
            PlayerScore ps = mutantScores.get(i);
            ps.setMutantKillInformation(
                    mutantsAlive.get(i) + " / " + mutantsKilled.get(i) + " / " + mutantsEquiv.get((i)));
            ps.setDuelInformation(duelsWon.get(i) + " / " + mutantsEquiv.get(i) + " / " + mutantsChallenged.get((i)));

        }

        return mutantScores;
    }

    /**
     * 
     * TODO This most likely need an update to account for the fact that the same
     * user plays attack and defense !
     * 
     * This method calculates the test score for every defender in the game. The
     * result is a mapping of playerId to player score.
     * 
     *
     * @return mapping from playerId to player score.
     */
    public HashMap<Integer, PlayerScore> getTestScores() {
        HashMap<Integer, PlayerScore> testScores = new HashMap<>();
        HashMap<Integer, Integer> mutantsKilled = new HashMap<>();

        HashMap<Integer, Integer> challengesOpen = new HashMap<>();
        HashMap<Integer, Integer> challengesWon = new HashMap<>();
        HashMap<Integer, Integer> challengesLost = new HashMap<>();

        int defendersTeamId = -1;
        testScores.put(defendersTeamId, new PlayerScore(defendersTeamId));
        mutantsKilled.put(defendersTeamId, 0);
        challengesOpen.put(defendersTeamId, 0);
        challengesWon.put(defendersTeamId, 0);
        challengesLost.put(defendersTeamId, 0);

        for (Player player : getDefenderPlayers()) {
            int defenderId = player.getId();
            testScores.put(defenderId, new PlayerScore(defenderId));
            mutantsKilled.put(defenderId, 0);
            challengesOpen.put(defenderId, 0);
            challengesWon.put(defenderId, 0);
            challengesLost.put(defenderId, 0);
        }

        for (Test test : getTests()) {
            if (getAttackerPlayers().stream().anyMatch(p -> p.getId() == test.getPlayerId())) {
                continue;
            }
            if (!testScores.containsKey(test.getPlayerId())) {
                testScores.put(test.getPlayerId(), new PlayerScore(test.getPlayerId()));
                mutantsKilled.put(test.getPlayerId(), 0);
            }
            PlayerScore ps = testScores.get(test.getPlayerId());
            ps.increaseQuantity();
            ps.increaseTotalScore(test.getScore());

            int teamKey = defendersTeamId;

            PlayerScore ts = testScores.get(teamKey);
            ts.increaseQuantity();
            ts.increaseTotalScore(test.getScore());

            mutantsKilled.put(test.getPlayerId(), mutantsKilled.get(test.getPlayerId()) + test.getMutantsKilled());
            mutantsKilled.put(teamKey, mutantsKilled.get(teamKey) + test.getMutantsKilled());

        }

        for (int playerId : mutantsKilled.keySet()) {
            if (playerId < 0 || getAttackerPlayers().stream().anyMatch(p -> p.getId() == playerId)) {
                continue;
            }
            int teamKey = defendersTeamId;

            PlayerScore ps = testScores.get(playerId);
            int playerScore = DatabaseAccess.getPlayerPoints(playerId);
            ps.increaseTotalScore(playerScore);

            PlayerScore ts = testScores.get(teamKey);
            ts.increaseTotalScore(playerScore);
        }

        for (Mutant m : getKilledMutants()) {
            if (!m.getEquivalent().equals(PROVEN_NO))
                continue;

            int defenderId = DatabaseAccess.getEquivalentDefenderId(m);
            challengesLost.put(defenderId, challengesLost.get(defenderId) + 1);
            challengesLost.put(defendersTeamId, challengesLost.get(defendersTeamId) + 1);
        }
        for (Mutant m : getMutantsMarkedEquivalent()) {
            int defenderId = DatabaseAccess.getEquivalentDefenderId(m);
            challengesWon.put(defenderId, challengesWon.get(defenderId) + 1);
            challengesWon.put(defendersTeamId, challengesWon.get(defendersTeamId) + 1);
        }
        for (Mutant m : getMutantsMarkedEquivalentPending()) {
            int defenderId = DatabaseAccess.getEquivalentDefenderId(m);
            challengesOpen.put(defenderId, challengesOpen.get(defenderId) + 1);
            challengesOpen.put(defendersTeamId, challengesOpen.get(defendersTeamId) + 1);
        }

        for (int playerId : testScores.keySet()) {
            testScores.get(playerId).setDuelInformation(challengesWon.get(playerId) + " / "
                    + challengesLost.get(playerId) + " / " + challengesOpen.get((playerId)));
        }
        testScores.get(defendersTeamId).setDuelInformation(challengesWon.get(defendersTeamId) + " / "
                + challengesLost.get(defendersTeamId) + " / " + challengesOpen.get((defendersTeamId)));

        for (int i : mutantsKilled.keySet()) {
            PlayerScore ps = testScores.get(i);
            ps.setMutantKillInformation("" + mutantsKilled.get(i));
        }

        return testScores;
    }

    // We do not check that the user is in both roles !!
    public boolean hasUserJoined(int userId) {
        for (Player p : GameDAO.getAllPlayersForGame(this.getId())) {
            if (p.getUser().getId() == userId) {
                return true;
            }
        }
        return false;
    }
}
