package org.codedefenders.dto.api;

public class MultiplayerScore extends Score {
    private DuelsCount duels;

    public MultiplayerScore(String username, Integer userId, Integer playerId, Integer points, DuelsCount duels) {
        super(username, userId, playerId, points);
        this.duels = duels;
    }
}
