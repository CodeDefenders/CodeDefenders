package org.codedefenders.beans.game;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.PlayerScore;
import org.codedefenders.game.scoring.ScoreCalculator;
import org.codedefenders.model.Player;
import org.codedefenders.service.UserService;
import org.codedefenders.servlets.games.GameProducer;

/**
 * <p>
 * Provides data for the melee_scoreboard game component.
 * </p>
 * <p>
 * Bean Name: {@code melee_scoreboard}
 * </p>
 */
@Named("meleeScoreboard")
@RequestScoped
public class MeleeScoreboardBean {
    /*
     * TODO This can be uniformed by defining the Team abstraction:
     *  - Team has players and name
     *  - Regular game has 2 teams: - 1 defenders team - 1 attackers team
     *  - Single Player has 1 team:
     *  - Melee game has N teams
     */
    private final List<ScoreItem> currentScore;

    @Inject
    public MeleeScoreboardBean(UserService userService, GameProducer gameProducer, ScoreCalculator scoreCalculator) {
        MeleeGame meleeGame = gameProducer.getMeleeGame();

        final Map<Integer, PlayerScore> mutantsScores = scoreCalculator.getMutantScores(meleeGame.getId());
        final Map<Integer, PlayerScore> testsScores = scoreCalculator.getTestScores(meleeGame.getId());
        final Map<Integer, PlayerScore> duelsScores = scoreCalculator.getDuelScores(meleeGame.getId());

        currentScore = meleeGame.getPlayers().stream()
                .map(Player::getId)
                .map(playerId -> {
                    SimpleUser user = userService.getSimpleUserByPlayerId(playerId).get();

                    return new ScoreItem(
                            user,
                            Optional.ofNullable(mutantsScores.get(playerId)).orElse(new PlayerScore(playerId)),
                            Optional.ofNullable(testsScores.get(playerId)).orElse(new PlayerScore(playerId)),
                            Optional.ofNullable(duelsScores.get(playerId)).orElse(new PlayerScore(playerId))
                    );
                })
                .sorted().collect(Collectors.toList());
    }

    // --------------------------------------------------------------------------------

    /**
     * Sorts the items by their total score and uses alphabetic order for ties.
     *
     * @return The sorted score items
     */
    public List<ScoreItem> getScoreItems() {
        return Collections.unmodifiableList(currentScore);
    }

    // DTO for the score board
    public static class ScoreItem implements Comparable<ScoreItem> {

        private final SimpleUser user;
        // PlayerScore contains the player id
        private final PlayerScore attackScore;
        private final PlayerScore defenseScore;
        private final PlayerScore duelScore;

        public ScoreItem(SimpleUser user, PlayerScore attackScore, PlayerScore defenseScore, PlayerScore duelScore) {
            this.user = user;
            this.attackScore = attackScore;
            this.defenseScore = defenseScore;
            this.duelScore = duelScore;
        }

        public PlayerScore getAttackScore() {
            return attackScore;
        }

        public PlayerScore getDefenseScore() {
            return defenseScore;
        }

        public PlayerScore getDuelScore() {
            return duelScore;
        }

        public SimpleUser getUser() {
            return user;
        }

        @Override
        public int compareTo(@Nonnull ScoreItem other) {
            // We need to reverse the sorting, the higher number is above
            int diff = (other.getAttackScore().getTotalScore() + other.getDefenseScore().getTotalScore() + other.getDuelScore().getTotalScore())
                    - (this.getAttackScore().getTotalScore() + this.getDefenseScore().getTotalScore() + this.getDuelScore().getTotalScore());

            if (diff == 0) {
                return this.getUser().getName().compareTo(other.getUser().getName());
            } else {
                return diff;
            }
        }
    }
}
