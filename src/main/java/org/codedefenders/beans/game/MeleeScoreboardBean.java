package org.codedefenders.beans.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.codedefenders.database.PlayerDAO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.multiplayer.PlayerScore;
import org.codedefenders.model.Player;
import org.codedefenders.service.UserService;

/**
 * <p>
 * Provides data for the melee_scoreboard game component.
 * </p>
 * <p>
 * Bean Name: {@code melee_scoreboard}
 * </p>
 */
@ManagedBean
@RequestScoped
public class MeleeScoreboardBean {
    /*
     * TODO This can be uniformed by defining the Team abstraction: - Team has
     * players and name - Regular game has 2 teams: - 1 defenders team - 1 attackers
     * team
     *
     * - Single Player has 1 team:
     *
     * - Meele game has N teams
     *
     */

    private final UserService userService;

    @Inject
    public MeleeScoreboardBean(UserService userService) {
        this.userService = userService;
    }


    private Integer gameId;

    private Set<Player> players;
    private Map<Integer, PlayerScore> mutantsScores;
    private Map<Integer, PlayerScore> testsScores;
    private Map<Integer, PlayerScore> duelsScores;

    // Load data
    public void setGameId(int gameId) {
        this.gameId = gameId;
        this.mutantsScores = new HashMap<>();
        this.testsScores = new HashMap<>();
        this.duelsScores = new HashMap<>();
    }

    public void setScores(Map<Integer, PlayerScore> mutantsScores, Map<Integer, PlayerScore> testsScores,
            Map<Integer, PlayerScore> duelsScores) {
        for (PlayerScore playerScore : mutantsScores.values()) {
            Player p = PlayerDAO.getPlayer(playerScore.getPlayerId());
            if (p != null) {
                this.mutantsScores.put(p.getUser().getId(), playerScore);
            }
        }

        for (PlayerScore playerScore : testsScores.values()) {
            Player p = PlayerDAO.getPlayer(playerScore.getPlayerId());
            if (p != null) {
                this.testsScores.put(p.getUser().getId(), playerScore);
            }
        }

        for (PlayerScore playerScore : duelsScores.values()) {
            Player p = PlayerDAO.getPlayer(playerScore.getPlayerId());
            if (p != null) {
                this.duelsScores.put(p.getUser().getId(), playerScore);
            }
        }

    }

    public void setPlayers(List<Player> players) {
        this.players = new HashSet<>(players);
    }

    // --------------------------------------------------------------------------------

    public int getGameId() {
        return gameId;
    }

    /**
     * Sorts the items by their total score and uses alphabetic order for ties.
     *
     * @return The sorted score items
     */
    public List<ScoreItem> getSortedScoreItems() {
        List<ScoreItem> scoreItems = getScoreItems();
        scoreItems.sort((o1, o2) -> {
            // We need to reverse the sorting, the higher number is above
            int diff = (o2.getAttackScore().getTotalScore() + o2.getDefenseScore().getTotalScore() + o2.getDuelScore().getTotalScore())
                    - (o1.getAttackScore().getTotalScore() + o1.getDefenseScore().getTotalScore() + o1.getDuelScore().getTotalScore());

            if (diff == 0) {
                return o1.getUser().getName().compareTo(o2.getUser().getName());
            } else {
                return diff;
            }
        });
        return scoreItems;

    }

    public List<ScoreItem> getScoreItems() {
        List<ScoreItem> currentScore = new ArrayList<>();

        for (Player player : this.players) {
            // We need user id because User is not Hashable (maybe it does not redefine
            // proper hash/equals?)
            SimpleUser user = userService.getSimpleUserByPlayerId(player.getId()).get();
            int playerId = player.getId();
            PlayerScore attackScore = mutantsScores.get(user.getId());
            if (attackScore == null) {
                attackScore = new PlayerScore(playerId);
            }
            PlayerScore defenseScore = testsScores.get(user.getId());
            if (defenseScore == null) {
                defenseScore = new PlayerScore(playerId);
            }

            PlayerScore duelScores = duelsScores.get(user.getId());
            if (duelScores == null) {
                duelScores = new PlayerScore(playerId);
            }

            currentScore.add(new ScoreItem(user, attackScore, defenseScore, duelScores));
        }

        return currentScore;
    }
}
