package org.codedefenders.game.scoring;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.database.GameDAO;
import org.codedefenders.database.MutantDAO;
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
 *
 */
@ManagedBean
@RequestScoped
public class ScoreCalculator {

    @Inject
    @Named("basic")
    private IScoringPolicy scoringPolicy;

    @Inject
    @Named("game")
    private AbstractGame game;

    /**
     * Calculate the score that the players gained by attacking, i.e., creating
     * mutants.
     */
    public Map<Integer, PlayerScore> getMutantScores() {
        Map<Integer, PlayerScore> mutantScores = new HashMap<Integer, PlayerScore>();

        // Create the data structure to host their data
        for (Player player : GameDAO.getAllPlayersForGame(game.getId())) {
            mutantScores.put(player.getId(), new PlayerScore(player.getId()));
        }

        for (Mutant mutant : MutantDAO.getValidMutantsForGame(game.getId())) {
            // Compute the score for the mutant and store it inside the mutant object
            scoringPolicy.scoreMutant(mutant);
            // Update the Map
            Integer playerId = mutant.getPlayerId();
            PlayerScore playerScore = mutantScores.get(playerId);
            playerScore.increaseTotalScore(mutant.getScore());
        }

        return mutantScores;
    }

    /**
     * Calculate the score that the players gained by defending, i.e., killing
     * mutants with tests.
     *
     *
     * @return mapping from playerId to player score.
     */
    public Map<Integer, PlayerScore> getTestScores() {
        final Map<Integer, PlayerScore> testScores = new HashMap<>();

        // Create the data structure to host their data
        for (Player player : GameDAO.getAllPlayersForGame(game.getId())) {
            testScores.put(player.getId(), new PlayerScore(player.getId()));
        }

        for (Test test : TestDAO.getTestsForGame(game.getId())) {
            // Compute the score for the test and store it inside the test object
            scoringPolicy.scoreTest(test);
            // Update the map
            Integer playerId = test.getPlayerId();
            PlayerScore playerScore = testScores.get(playerId);
            playerScore.increaseTotalScore(test.getScore());
        }

        return testScores;
    }

    /**
     * Calculate the score that the players gained by winning equivalence duels,
     * i.e., claiming equivalence
     *
     *
     * @return mapping from playerId to player score.
     */
    public Map<Integer, PlayerScore> getDuelScores() {
        final Map<Integer, PlayerScore> duelScores = new HashMap<>();

        // Create the data structure to host their data
        for (Player player : GameDAO.getAllPlayersForGame(game.getId())) {
            PlayerScore playerScore = new PlayerScore(player.getId());
            scoringPolicy.scoreDuels(playerScore);
            //
            duelScores.put(player.getId(), playerScore);
        }

        return duelScores;
    }
}
