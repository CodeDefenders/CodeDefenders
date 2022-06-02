package org.codedefenders.dto;

/**
 * These Data Transfer Objects contain all statistics of a user shown on each profile page.
 */
public class UserStats {
    private final int userId;

    private final int killedMutants;
    private final int aliveMutants;

    private final int killingTests;
    private final int nonKillingTests;

    private final double avgPointsTests;
    private final int totalPointsTests;

    private final double avgPointsMutants;
    private final int totalPointsMutants;

    private final int attackerGames;
    private final int defenderGames;

    public UserStats(int userId, int killedMutants, int aliveMutants, int killingTests, int nonKillingTests,
                     double avgPointsTests, int totalPointsTests, double avgPointsMutants, int totalPointsMutants,
                     int attackerGames, int defenderGames) {
        this.userId = userId;
        this.killedMutants = killedMutants;
        this.aliveMutants = aliveMutants;
        this.killingTests = killingTests;
        this.nonKillingTests = nonKillingTests;
        this.avgPointsTests = avgPointsTests;
        this.totalPointsTests = totalPointsTests;
        this.avgPointsMutants = avgPointsMutants;
        this.totalPointsMutants = totalPointsMutants;
        this.attackerGames = attackerGames;
        this.defenderGames = defenderGames;
    }

    public int getUserId() {
        return userId;
    }

    public int getKilledMutants() {
        return killedMutants;
    }

    public int getAliveMutants() {
        return aliveMutants;
    }

    public int getKillingTests() {
        return killingTests;
    }

    public int getNonKillingTests() {
        return nonKillingTests;
    }

    public double getAvgPointsTests() {
        return avgPointsTests;
    }

    public int getTotalPointsTests() {
        return totalPointsTests;
    }

    public double getAvgPointsMutants() {
        return avgPointsMutants;
    }

    public int getTotalPointsMutants() {
        return totalPointsMutants;
    }

    public int getAttackerGames() {
        return attackerGames;
    }

    public int getDefenderGames() {
        return defenderGames;
    }
}
