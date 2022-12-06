package org.codedefenders.dto.api;

public class DefenderScore extends MultiplayerScore {
    private TestsCount tests;

    public DefenderScore(String username, Integer userId, Integer playerId, Integer points, DuelsCount duels, TestsCount tests) {
        super(username, userId, playerId, points, duels);
        this.tests = tests;
    }
}
