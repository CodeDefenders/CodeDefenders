package org.codedefenders.beans.game;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;

import org.codedefenders.database.GameDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.database.TestDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.PlayerScore;
import org.codedefenders.model.Player;

/**
 * This bean takes care of computing the score in each game. Since the rules are
 * still to be fixed (see #574), this class might produce inaccurate results.
 * The focal point is: does this class simply accumulate points that are stored
 * in mutants or do we need to re-compute the score based on the relations of
 * mutants/tests over and over?
 * 
 * @author gambi
 *
 */
@ManagedBean
@RequestScoped
public class ScoreCalculator {

    private AbstractGame game;

    private List<Player> players;

    public void setGame(AbstractGame game) {
        this.game = game;
        // Retrieve all the players in the game.
        this.players = GameDAO.getAllPlayersForGame(game.getId());
    }

    /**
     * Calculate the score that the players gained by attacking, i.e., creating
     * mutants.
     */
    public Map<Integer, PlayerScore> getMutantScores() {
        HashMap<Integer, PlayerScore> mutantScores = new HashMap<Integer, PlayerScore>();

        // Create the data structure to host their data
        for (Player player : players) {
            mutantScores.put(player.getId(), new PlayerScore(player.getId()));
        }

        for (Mutant mutant : MutantDAO.getValidMutantsForGame(game.getId())) {
            Integer playerId = mutant.getPlayerId();
            PlayerScore playerScore = mutantScores.get(playerId);
            switch (mutant.getState()) {
            // If the mutant is alive we can add all its points to the playerScore
            case ALIVE: // Alive, possibly proven to not be equivalent
            case FLAGGED: // Duel is pending so the mutant is not yet killed
                playerScore.increaseTotalScore(mutant.getScore());
                break;
            }
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
    public HashMap<Integer, PlayerScore> getTestScores() {
        final HashMap<Integer, PlayerScore> testScores = new HashMap<>();

        // Create the data structure to host their data
        for (Player player : players) {
            testScores.put(player.getId(), new PlayerScore(player.getId()));
        }

        for (Test test : TestDAO.getTestsForGame(game.getId())) {
            Integer playerId = test.getPlayerId();
            PlayerScore playerScore = testScores.get(playerId);
            playerScore.increaseTotalScore(test.getScore());
        }

        return testScores;
    }
}
