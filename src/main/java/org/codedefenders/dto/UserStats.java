package org.codedefenders.dto;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.codedefenders.game.puzzle.PuzzleChapter;

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

    private int attackerGames;
    private int defenderGames;
    private int totalGames;

    public UserStats(int userId, int killedMutants, int aliveMutants, int killingTests, int nonKillingTests,
                     double avgPointsTests, int totalPointsTests, double avgPointsMutants, int totalPointsMutants) {
        this.userId = userId;
        this.killedMutants = killedMutants;
        this.aliveMutants = aliveMutants;
        this.killingTests = killingTests;
        this.nonKillingTests = nonKillingTests;
        this.avgPointsTests = avgPointsTests;
        this.totalPointsTests = totalPointsTests;
        this.avgPointsMutants = avgPointsMutants;
        this.totalPointsMutants = totalPointsMutants;
    }

    public void setAttackerDefenderGames(int attackerGames, int defenderGames) {
        this.attackerGames = attackerGames;
        this.defenderGames = defenderGames;
        this.totalGames = attackerGames + defenderGames;
    }

    public void setTotalGames(int totalGames) {
        this.totalGames = totalGames;
    }

    private int calcPercentage(int subject, int total) {
        if (subject > total) {
            throw new IllegalArgumentException("Total must be greater than or equal to the portion.");
        }
        if (subject < 0) {
            throw new IllegalArgumentException("Amount must be positive or zero.");
        }
        if (subject == 0) {
            return 0; // avoid division by 0
        }
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
        return totalGames;
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

    public static class PuzzleStats {
        Map<Integer, Integer> maxPuzzlePerChapter;
        Collection<PuzzleChapter> chapters;

        public PuzzleStats() {
            maxPuzzlePerChapter = new HashMap<>();
        }

        /**
         * Updates the highest puzzle number of a chapter, if the given puzzle number is higher than the current one.
         *
         * @param chapter The chapter of the puzzle.
         * @param puzzle  The puzzle number.
         */
        public void addPuzzle(int chapter, int puzzle) {
            if (maxPuzzlePerChapter.containsKey(chapter)) {
                if (maxPuzzlePerChapter.get(chapter) < puzzle) {
                    maxPuzzlePerChapter.put(chapter, puzzle);
                }
            } else {
                maxPuzzlePerChapter.put(chapter, puzzle);
            }
        }

        /**
         * Returns the highest puzzle number of a chapter.
         *
         * @param chapter The chapter of the puzzle.
         * @return The highest puzzle number of the chapter or 0 if the chapter hasn't been played yet.
         */
        public int getMaxPuzzle(int chapter) {
            return maxPuzzlePerChapter.getOrDefault(chapter, 0);
        }

        public void setChapters(Collection<PuzzleChapter> chapters) {
            this.chapters = chapters;
        }

        public Collection<PuzzleChapter> getChapters() {
            return chapters;
        }
    }
}
