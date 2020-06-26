package org.codedefenders.beans.game;

import org.codedefenders.game.multiplayer.PlayerScore;
import org.codedefenders.model.User;

// DTO for the score board
public class ScoreItem {

    private final User user;
    // PlayerScore contains the player id
    private final PlayerScore attackScore;
    private final PlayerScore defenseScore;
    private final PlayerScore duelScore;

    public ScoreItem(User user, PlayerScore attackScore, PlayerScore defenseScore, PlayerScore duelScore) {
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

    public User getUser() {
        return user;
    }

}
