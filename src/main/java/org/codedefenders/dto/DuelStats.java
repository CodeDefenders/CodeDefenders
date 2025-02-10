/*
 * Copyright (C) 2023 Code Defenders contributors
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
package org.codedefenders.dto;

/**
 * These Data Transfer Objects contain all duel statistics of a user shown on each profile page.
 */
public class DuelStats {
    private final int duelsWon;
    private final int duelsLost;

    public DuelStats(int duelsWon, int duelsLost) {
        this.duelsWon = duelsWon;
        this.duelsLost = duelsLost;
    }

    public static DuelStats sum(DuelStats defenderDuelStats, DuelStats attackerDuelStats) {
        return new DuelStats(
                defenderDuelStats.getDuelsWon() + attackerDuelStats.getDuelsWon(),
                defenderDuelStats.getDuelsLost() + attackerDuelStats.getDuelsLost()
        );
    }

    public int getDuelsWon() {
        return duelsWon;
    }

    public int getDuelsLost() {
        return duelsLost;
    }

    public int getDuelsTotal() {
        return duelsWon + duelsLost;
    }

    public int getWinPercentage() {
        return calcPercentage(duelsWon, getDuelsTotal());
    }

    public int getLossPercentage() {
        return calcPercentage(duelsLost, getDuelsTotal());
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
}
