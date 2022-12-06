package org.codedefenders.dto.api;

public class MeleeScore extends Score {
    private Integer attackPoints;
    private Integer defensePoints;
    private Integer duelPoints;

    public MeleeScore(String username, Integer userId, Integer playerId, Integer points, Integer attackPoints, Integer defensePoints, Integer duelPoints) {
        super(username, userId, playerId, points);
        this.attackPoints = attackPoints;
        this.defensePoints = defensePoints;
        this.duelPoints = duelPoints;
    }
}
