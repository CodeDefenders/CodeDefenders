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
package org.codedefenders.game.scoring;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.PlayerScore;
import org.codedefenders.model.Player;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.persistence.database.PlayerRepository;
import org.codedefenders.persistence.database.TestRepository;

/**
 * This class uses a ScoringPolicy to compute each players' attacking/mutants
 * and defending/tests score.
 * <p>
 * Currently only used for melee games.
 * <p>
 * Ideally Gmae
 *
 * @author gambi
 */
@RequestScoped
public class ScoreCalculator {

    private final IScoringPolicy scoringPolicy;
    private final TestRepository testRepo;
    private final MutantRepository mutantRepo;
    private final GameRepository gameRepo;
    private final PlayerRepository playerRepo;

    @Inject
    public ScoreCalculator(@Named("basic") IScoringPolicy scoringPolicy,
                           TestRepository testRepo, MutantRepository mutantRepo, GameRepository gameRepo,
                           PlayerRepository playerRepo) {
        this.scoringPolicy = scoringPolicy;
        this.testRepo = testRepo;
        this.mutantRepo = mutantRepo;
        this.gameRepo = gameRepo;
        this.playerRepo = playerRepo;
    }

    /**
     * Calculate the score that the players gained by attacking, i.e., creating
     * mutants.
     */
    public Map<Integer, PlayerScore> getMutantScores(int gameId) {
        Map<Integer, PlayerScore> mutantScores = new HashMap<>();

        // Create the data structure to host their data
        for (Player player : gameRepo.getValidPlayersForGame(gameId)) {
            mutantScores.put(player.getId(), new PlayerScore(player.getId()));
        }

        for (Mutant mutant : mutantRepo.getValidMutantsForGame(gameId)) {
            // Compute the score for the mutant and store it inside the mutant object
            scoringPolicy.scoreMutant(mutant);
            // Update the Map
            Integer playerId = mutant.getPlayerId();
            PlayerScore playerScore = mutantScores.get(playerId);

            /* playerScore can be null if the mutant belongs to a system user. */
            if (playerScore != null) {
                playerScore.increaseTotalScore(mutant.getScore());
                playerScore.increaseQuantity();
            }
        }

        return mutantScores;
    }

    /**
     * Calculate the score that the players gained by defending, i.e., killing
     * mutants with tests.
     *
     * @return mapping from playerId to player score.
     */
    public Map<Integer, PlayerScore> getTestScores(int gameId) {
        final Map<Integer, PlayerScore> testScores = new HashMap<>();

        // Create the data structure to host their data
        for (Player player : gameRepo.getValidPlayersForGame(gameId)) {
            testScores.put(player.getId(), new PlayerScore(player.getId()));
        }

        for (Test test : testRepo.getValidTestsForGame(gameId)) {
            // Compute the score for the test and store it inside the test object
            scoringPolicy.scoreTest(test);
            // Update the map
            Integer playerId = test.getPlayerId();
            PlayerScore playerScore = testScores.get(playerId);

            /* playerScore can be null if the mutant belongs to a system user. */
            if (playerScore != null) {
                playerScore.increaseTotalScore(test.getScore());
                playerScore.increaseQuantity();
            }
        }

        return testScores;
    }

    /**
     * Calculate the score that the players gained by winning equivalence duels,
     * i.e., claiming equivalence
     *
     * @return mapping from playerId to player score.
     */
    public Map<Integer, PlayerScore> getDuelScores(int gameId) {
        final Map<Integer, PlayerScore> duelScores = new HashMap<>();

        // Create the data structure to host their data
        for (Player player : gameRepo.getValidPlayersForGame(gameId)) {
            PlayerScore playerScore = new PlayerScore(player.getId());
            scoringPolicy.scoreDuels(playerScore);
            duelScores.put(player.getId(), playerScore);
        }

        return duelScores;
    }

    public void storeScoresToDB(int gameId) {
        for (Mutant mutant : mutantRepo.getValidMutantsForGame(gameId)) {
            // Compute the score for the mutant and store it inside the mutant object
            scoringPolicy.scoreMutant(mutant);
            mutantRepo.updateMutantScore(mutant);
        }
        for (Test test : testRepo.getValidTestsForGame(gameId)) {
            // Compute the score for the test and store it inside the test object
            scoringPolicy.scoreTest(test);
            testRepo.updateTest(test);
        }
        for (Player player : gameRepo.getValidPlayersForGame(gameId)) {
            PlayerScore playerScore = new PlayerScore(player.getId());
            scoringPolicy.scoreDuels(playerScore);
            playerRepo.setPlayerPoints(playerScore.getTotalScore(), player.getId());
        }
    }
}
