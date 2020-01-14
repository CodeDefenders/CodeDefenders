package org.codedefenders.beans.game;

import edu.emory.mathcs.backport.java.util.Collections;
import org.codedefenders.game.multiplayer.PlayerScore;
import org.codedefenders.model.Player;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;
import java.util.List;
import java.util.Map;

/**
 * <p>Provides data for the scoreboard game component.</p>
 * <p>Bean Name: {@code scoreboard}</p>
 */
@ManagedBean
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
}
