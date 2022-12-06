package org.codedefenders.dto.api;

public class AttackerScore extends MultiplayerScore {
    private MutantsCount mutants;

    public AttackerScore(String username, Integer userId, Integer playerId, Integer points, DuelsCount duels, MutantsCount mutants) {
        super(username, userId, playerId, points, duels);
        this.mutants = mutants;
    }
}
