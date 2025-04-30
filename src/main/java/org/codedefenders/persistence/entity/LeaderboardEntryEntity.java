/*
 * Copyright (C) 2016-2025 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.persistence.entity;

public class LeaderboardEntryEntity {
    private final String username;
    private final int mutantsSubmitted;
    private final int attackerScore;
    private final int testsSubmitted;
    private final int defenderScore;
    private final int mutantsKilled;
    private final int totalPoints;

    public LeaderboardEntryEntity(String username, int mutantsSubmitted, int attackerScore, int testsSubmitted,
            int defenderScore, int mutantsKilled, int totalPoints) {
        this.username = username;
        this.mutantsSubmitted = mutantsSubmitted;
        this.attackerScore = attackerScore;
        this.testsSubmitted = testsSubmitted;
        this.defenderScore = defenderScore;
        this.mutantsKilled = mutantsKilled;
        this.totalPoints = totalPoints;
    }

    public String getUsername() {
        return username;
    }

    public int getMutantsSubmitted() {
        return mutantsSubmitted;
    }

    public int getAttackerScore() {
        return attackerScore;
    }

    public int getTestsSubmitted() {
        return testsSubmitted;
    }

    public int getDefenderScore() {
        return defenderScore;
    }

    public int getMutantsKilled() {
        return mutantsKilled;
    }

    public int getTotalPoints() {
        return totalPoints;
    }
}
