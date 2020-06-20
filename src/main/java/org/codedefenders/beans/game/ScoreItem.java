package org.codedefenders.beans.game;

import org.codedefenders.game.multiplayer.PlayerScore;
import org.codedefenders.model.User;

// DTO for the score board
public class ScoreItem {

    final private User user;
    // PlayerScore contains the player id
    final private PlayerScore attackScore;
    final private PlayerScore defenseScore;

    public ScoreItem(User user, PlayerScore attackScore, PlayerScore defenseScore) {
        this.user = user;
        this.attackScore = attackScore;
        this.defenseScore = defenseScore;
    }

    public PlayerScore getAttackScore() {
        return attackScore;
    }

    public PlayerScore getDefenseScore() {
        return defenseScore;
    }

    public User getUser() {
        return user;
    }

}
