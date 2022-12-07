package org.codedefenders.dto.api;

public abstract class Score {
    private String username;
    private Integer userId;
    private Integer playerId;
    private Integer points;

    public Score(String username, Integer userId, Integer playerId, Integer points) {
        this.username = username;
        this.userId = userId;
        this.playerId = playerId;
        this.points = points;
    }
}
