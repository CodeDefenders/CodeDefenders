package org.codedefenders.game.scoring;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.database.GameDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.PlayerDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.PlayerScore;
import org.codedefenders.model.Player;

/**
 * This class uses a ScoringPolicy to compute each players' attacking/mutants
 * and defending/tests score.
 *
 * <p>Ideally Gmae
 *
 * @author gambi
 */
@ManagedBean
@RequestScoped
public class ScoreCalculator {

    @Inject
    @Named("basic")
    private IScoringPolicy scoringPolicy;

    /**
     * Calculate the score that the players gained by attacking, i.e., creating
     * mutants.
     */
    public Map<Integer, PlayerScore> getMutantScores(int gameId) {
        Map<Integer, PlayerScore> mutantScores = new HashMap<Integer, PlayerScore>();

        // Create the data structure to host their data
        for (Player player : GameDAO.getValidPlayersForGame(gameId)) {
            mutantScores.put(player.getId(), new PlayerScore(player.getId()));
        }

        for (Mutant mutant : MutantDAO.getValidMutantsForGame(gameId)) {
            // Compute the score for the mutant and store it inside the mutant object
            scoringPolicy.scoreMutant(mutant);
            // Update the Map
            Integer playerId = mutant.getPlayerId();
            PlayerScore playerScore = mutantScores.get(playerId);

            /* playerScore can be null if the mutant belongs to a system user. */
            if (playerScore != null) {
                playerScore.increaseTotalScore(mutant.getScore());
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
        for (Player player : GameDAO.getValidPlayersForGame(gameId)) {
            testScores.put(player.getId(), new PlayerScore(player.getId()));
        }

        for (Test test : TestDAO.getTestsForGame(gameId)) {
            // Compute the score for the test and store it inside the test object
            scoringPolicy.scoreTest(test);
            // Update the map
            Integer playerId = test.getPlayerId();
            PlayerScore playerScore = testScores.get(playerId);

            /* playerScore can be null if the mutant belongs to a system user. */
            if (playerScore != null) {
                playerScore.increaseTotalScore(test.getScore());
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
        for (Player player : GameDAO.getValidPlayersForGame(gameId)) {
            PlayerScore playerScore = new PlayerScore(player.getId());
            scoringPolicy.scoreDuels(playerScore);
            duelScores.put(player.getId(), playerScore);
        }

        return duelScores;
    }

    public void storeScoresToDB(int gameId) {
        for (Mutant mutant : MutantDAO.getValidMutantsForGame(gameId)) {
            // Compute the score for the mutant and store it inside the mutant object
            scoringPolicy.scoreMutant(mutant);
            MutantDAO.updateMutantScore(mutant);
        }
        for (Test test : TestDAO.getTestsForGame(gameId)) {
            // Compute the score for the test and store it inside the test object
            scoringPolicy.scoreTest(test);
            TestDAO.updateTest(test);
        }
        for (Player player : GameDAO.getValidPlayersForGame(gameId)) {
            PlayerScore playerScore = new PlayerScore(player.getId());
            scoringPolicy.scoreDuels(playerScore);
            PlayerDAO.setPlayerPoints(playerScore.getTotalScore(), player.getId());
        }
    }
}
