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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.Test;
import org.codedefenders.model.Player;
import org.codedefenders.model.WhitelistElement;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.validation.code.DefaultRuleSets;
import org.codedefenders.validation.code.MutantValidationRuleSet;

import static org.codedefenders.game.Mutant.Equivalence.ASSUMED_YES;
import static org.codedefenders.game.Mutant.Equivalence.DECLARED_YES;
import static org.codedefenders.game.Mutant.Equivalence.PENDING_TEST;
import static org.codedefenders.game.Mutant.Equivalence.PROVEN_NO;

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

    public abstract static class Builder<T extends Builder<T, G>, G extends AbstractMultiplayerGame> {
        // mandatory values
        protected final int classId;
        protected final int creatorId;
        protected final int maxAssertionsPerTest;

        // optional values with default values
        protected GameClass cut = null;
        protected int id = -1;
        protected boolean requiresValidation = false;
        protected boolean capturePlayersIntention = false;
        protected boolean chatEnabled = false;
        protected boolean inviteOnly = false;
        protected Integer inviteId = null;
        protected int gameDurationMinutes;
        protected long startTimeUnixSeconds;
        protected long finishTimeUnixSeconds;
        protected float lineCoverage = 1f;
        protected float mutantCoverage = 1f;
        protected float prize = 1f;
        protected GameState state = GameState.CREATED;
        protected GameLevel level = GameLevel.HARD;
        protected MutantValidationRuleSet mutantValidatorLevel = DefaultRuleSets.STRICT;
        protected int automaticMutantEquivalenceThreshold = 0;
        protected Integer classroomId = null;
        protected Set<WhitelistElement> whitelist = new HashSet<>();

        public Builder(int classId, int creatorId, int maxAssertionsPerTest) {
            this.classId = classId;
            this.creatorId = creatorId;
            this.maxAssertionsPerTest = maxAssertionsPerTest;
        }

        public T cut(GameClass cut) {
            this.cut = cut;
            return self();
        }

        public T id(int id) {
            this.id = id;
            return self();
        }

        public T requiresValidation(boolean requiresValidation) {
            this.requiresValidation = requiresValidation;
            return self();
        }

        public T capturePlayersIntention(boolean capturePlayersIntention) {
            this.capturePlayersIntention = capturePlayersIntention;
            return self();
        }

        public T chatEnabled(boolean chatEnabled) {
            this.chatEnabled = chatEnabled;
            return self();
        }

        public T gameDurationMinutes(int gameDurationMinutes) {
            this.gameDurationMinutes = gameDurationMinutes;
            return self();
        }

        public T startTimeUnixSeconds(long startTimeUnixSeconds) {
            this.startTimeUnixSeconds = startTimeUnixSeconds;
            return self();
        }

        public T finishTimeUnixSeconds(long finishTimeUnixSeconds) {
            this.finishTimeUnixSeconds = finishTimeUnixSeconds;
            return self();
        }

        public T prize(float prize) {
            this.prize = prize;
            return self();
        }

        public T lineCoverage(float lineCoverage) {
            this.lineCoverage = lineCoverage;
            return self();
        }

        public T mutantCoverage(float mutantCoverage) {
            this.mutantCoverage = mutantCoverage;
            return self();
        }

        public T state(GameState state) {
            this.state = state;
            return self();
        }

        public T level(GameLevel level) {
            this.level = level;
            return self();
        }

        public T mutantValidatorLevel(MutantValidationRuleSet mutantValidatorLevel) {
            this.mutantValidatorLevel = mutantValidatorLevel;
            return self();
        }

        public T automaticMutantEquivalenceThreshold(int threshold) {
            this.automaticMutantEquivalenceThreshold = threshold;
            return self();
        }

        public T classroomId(Integer classroomId) {
            this.classroomId = classroomId;
            return self();
        }

        public T inviteOnly(boolean inviteOnly) {
            this.inviteOnly = inviteOnly;
            return self();
        }

        public T inviteId(Integer inviteId) {
            this.inviteId = inviteId;
            return self();
        }

        public T whitelist(Set<WhitelistElement> whitelist) {
            this.whitelist = whitelist;
            return self();
        }

        protected abstract T self();
        public abstract G build();
    }

    protected <T extends AbstractMultiplayerGame.Builder<T, ?>> AbstractMultiplayerGame(T builder) {
        this.cut = builder.cut;
        this.id = builder.id;
        this.classId = builder.classId;
        this.creatorId = builder.creatorId;
        this.state = builder.state;
        this.level = builder.level;
        this.lineCoverage = builder.lineCoverage;
        this.mutantCoverage = builder.mutantCoverage;
        this.prize = builder.prize;
        this.requiresValidation = builder.requiresValidation;
        this.maxAssertionsPerTest = builder.maxAssertionsPerTest;
        this.chatEnabled = builder.chatEnabled;
        this.mutantValidatorLevel = builder.mutantValidatorLevel;
        this.capturePlayersIntention = builder.capturePlayersIntention;
        this.automaticMutantEquivalenceThreshold = builder.automaticMutantEquivalenceThreshold;
        this.gameDurationMinutes = builder.gameDurationMinutes;
        this.startTimeUnixSeconds = builder.startTimeUnixSeconds;
        this.finishTimeUnixSeconds = builder.finishTimeUnixSeconds;
        this.classroomId = builder.classroomId;
        this.inviteOnly = builder.inviteOnly;
        this.inviteId = builder.inviteId;
        this.whitelist = builder.whitelist;
    }


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

    /**
     * This method calculates the test score for every defender in the game.
     *
     * <p>The result is a mapping of playerId to player score.
     *
     * @return mapping from playerId to player score.
     */
    public HashMap<Integer, PlayerScore> getTestScores() {
        final MutantRepository mutantRepo = CDIUtil.getBeanFromCDI(MutantRepository.class);

        List<Player> defenderPlayers;
        if (this instanceof MultiplayerGame multiplayerGame) {
            defenderPlayers = multiplayerGame.getDefenderPlayers();
        } else if (this instanceof MeleeGame meleeGame) {
            defenderPlayers = meleeGame.getPlayers();
        } else {
            throw new IllegalStateException("Unsupported kind of game: " + this.getClass());
        }

        final HashMap<Integer, PlayerScore> testScores = new HashMap<>();
        final HashMap<Integer, Integer> mutantsKilled = new HashMap<>();

        final HashMap<Integer, Integer> challengesOpen = new HashMap<>();
        final HashMap<Integer, Integer> challengesWon = new HashMap<>();
        final HashMap<Integer, Integer> challengesLost = new HashMap<>();

        int defendersTeamId = -1;
        testScores.put(defendersTeamId, new PlayerScore(defendersTeamId));
        mutantsKilled.put(defendersTeamId, 0);
        challengesOpen.put(defendersTeamId, 0);
        challengesWon.put(defendersTeamId, 0);
        challengesLost.put(defendersTeamId, 0);

        for (Player player : defenderPlayers) {
            int defenderId = player.getId();
            testScores.put(defenderId, new PlayerScore(defenderId));
            mutantsKilled.put(defenderId, 0);
            challengesOpen.put(defenderId, 0);
            challengesWon.put(defenderId, 0);
            challengesLost.put(defenderId, 0);
        }

        for (Test test : getTests()) {
            //Check if the test is an equivalence test.
            if (defenderPlayers.stream().noneMatch(player -> player.getId() == test.getPlayerId())) {
                continue;
            }
            if (!testScores.containsKey(test.getPlayerId())) {
                testScores.put(test.getPlayerId(), new PlayerScore(test.getPlayerId()));
                mutantsKilled.put(test.getPlayerId(), 0);
            }
            PlayerScore ps = testScores.get(test.getPlayerId());
            ps.increaseQuantity();
            ps.increaseTotalScore(test.getScore());

            PlayerScore ts = testScores.get(defendersTeamId);
            ts.increaseQuantity();
            ts.increaseTotalScore(test.getScore());

            mutantsKilled.put(test.getPlayerId(), mutantsKilled.get(test.getPlayerId()) + test.getMutantsKilled());
            mutantsKilled.put(defendersTeamId, mutantsKilled.get(defendersTeamId) + test.getMutantsKilled());

        }


        //Increase the duel values (won/lost/pending)
        for (Mutant m : getKilledMutants()) {
            if (!m.getEquivalent().equals(PROVEN_NO)) {
                continue;
            }

            int defenderId = mutantRepo.getEquivalentDefenderId(m);
            challengesLost.put(defenderId, challengesLost.get(defenderId) + 1);
            if (defenderId != defendersTeamId) {
                challengesLost.put(defendersTeamId, challengesLost.get(defendersTeamId) + 1);
            }
        }
        for (Mutant m : getMutantsMarkedEquivalent()) {
            int defenderId = mutantRepo.getEquivalentDefenderId(m);
            challengesWon.put(defenderId, challengesWon.get(defenderId) + 1);
            if (defenderId != defendersTeamId) {
                challengesWon.put(defendersTeamId, challengesWon.get(defendersTeamId) + 1);
            }
        }
        for (Mutant m : getMutantsMarkedEquivalentPending()) {
            int defenderId = mutantRepo.getEquivalentDefenderId(m);
            challengesOpen.put(defenderId, challengesOpen.get(defenderId) + 1);
            if (defenderId != defendersTeamId) {
                challengesOpen.put(defendersTeamId, challengesOpen.get(defendersTeamId) + 1);
            }
        }

        //Increase test scores based on duels won, set the String representation of duel statistics
        for (var player : defenderPlayers) {
            int playerId = player.getId();
            int numberDuelsWon = challengesWon.getOrDefault(playerId, 0);
            testScores.get(playerId).setDuelInformation(numberDuelsWon + " / "
                    + challengesLost.get(playerId) + " / " + challengesOpen.get((playerId)));
            testScores.get(playerId).setDuelsWon(numberDuelsWon);

            testScores.get(playerId).increaseTotalScore(numberDuelsWon);
            testScores.get(defendersTeamId).increaseTotalScore(numberDuelsWon);
        }
        testScores.get(defendersTeamId).setDuelInformation(challengesWon.get(defendersTeamId) + " / "
                + challengesLost.get(defendersTeamId) + " / " + challengesOpen.get((defendersTeamId)));
        testScores.get(defendersTeamId).setDuelsWon(challengesWon.get(defendersTeamId));
        for (int i : mutantsKilled.keySet()) {
            PlayerScore ps = testScores.get(i);
            ps.setMutantKillInformation("" + mutantsKilled.get(i));
        }

        return testScores;
    }

    /**
     * This method calculates the mutant score for every attacker in the game.
     *
     * <p>The result is a mapping of playerId to player score.
     *
     * @return mapping from playerId to player score.
     */
    public HashMap<Integer, PlayerScore> getMutantScores() {
        final HashMap<Integer, PlayerScore> mutantScores = new HashMap<>();

        final HashMap<Integer, Integer> mutantsAlive = new HashMap<>();
        final HashMap<Integer, Integer> mutantsKilled = new HashMap<>();
        final HashMap<Integer, Integer> mutantsEquiv = new HashMap<>();
        final HashMap<Integer, Integer> mutantsChallenged = new HashMap<>();
        final HashMap<Integer, Integer> duelsWon = new HashMap<>();

        // TODO why not getMutants()
        List<Mutant> allMutants = getAliveMutants();
        allMutants.addAll(getKilledMutants());
        allMutants.addAll(getMutantsMarkedEquivalent());
        allMutants.addAll(getMutantsMarkedEquivalentPending());

        mutantScores.put(-1, new PlayerScore(-1));
        mutantsAlive.put(-1, 0);
        mutantsEquiv.put(-1, 0);
        mutantsChallenged.put(-1, 0);
        mutantsKilled.put(-1, 0);
        duelsWon.put(-1, 0);

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
                    // Actually adds points for the player and the team
                    ps.increaseTotalScore(1);
                    ts.increaseTotalScore(1);
                }
            }

        }

        for (int i : mutantsKilled.keySet()) {
            PlayerScore ps = mutantScores.get(i);
            ps.setMutantKillInformation(
                    mutantsAlive.get(i) + " / " + mutantsKilled.get(i) + " / " + mutantsEquiv.get((i)));
            ps.setDuelInformation(duelsWon.get(i) + " / " + mutantsEquiv.get(i) + " / " + mutantsChallenged.get((i)));
            ps.setDuelsWon(duelsWon.get(i));

        }

        return mutantScores;
    }

    public abstract Role getRole(int userId);
}
