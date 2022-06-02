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

    private int calcPercentage(int subject, int total) {
        if (subject > total) throw new IllegalArgumentException("Total must be greater than or equal to the portion.");
        if (subject < 0) throw new IllegalArgumentException("Amount must be positive or zero.");
        if (subject == 0) return 0; // avoid division by 0
        return (subject * 100) / total;
    }

    public int getTotalMutants() {
        return killedMutants + aliveMutants;
    }

    public int getAliveMutantsPercentage() {
        return calcPercentage(aliveMutants, getTotalMutants());
    }

    public int getKilledMutantsPercentage() {
        return calcPercentage(killedMutants, getTotalMutants());
    }

    public int getTotalTests() {
        return killingTests + nonKillingTests;
    }

    public int getKillingTestsPercentage() {
        return calcPercentage(killingTests, getTotalTests());
    }

    public int getNonKillingTestsPercentage() {
        return calcPercentage(nonKillingTests, getTotalTests());
    }

    public int getTotalPoints() {
        return totalPointsTests + totalPointsMutants;
    }

    public int getTestPointsPercentage() {
        return calcPercentage(totalPointsTests, getTotalPoints());
    }

    public int getMutantPointsPercentage() {
        return calcPercentage(totalPointsMutants, getTotalPoints());
    }

    public int getTotalGames() {
        return attackerGames + defenderGames;
    }

    public int getDefenderGamesPercentage() {
        return calcPercentage(defenderGames, getTotalGames());
    }

    public int getAttackerGamesPercentage() {
        return calcPercentage(attackerGames, getTotalGames());
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
