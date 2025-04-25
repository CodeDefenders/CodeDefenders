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
package org.codedefenders.game.multiplayer;

public class PlayerScore {
    private int playerId;
    private int totalScore;
    private int quantity;
    private String mutantKillInformation;

    private String duelInformation;

    public PlayerScore(int playerId) {
        this.playerId = playerId;
        this.totalScore = 0;
        this.quantity = 0;
    }

    public String toString() {
        return playerId + ": " + totalScore + ", " + quantity + "," + mutantKillInformation + "," + duelInformation;
    }

    public String getMutantKillInformation() {
        return mutantKillInformation;
    }

    // TODO What's this?
    public void setMutantKillInformation(String mutantKillInformation) {
        this.mutantKillInformation = mutantKillInformation;
    }

    public String getDuelInformation() {
        return duelInformation;
    }

    // TODO What's this?
    public void setDuelInformation(String duelInformation) {
        this.duelInformation = duelInformation;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public int getQuantity() {
        return quantity;
    }

    // TODO What's this?
    public void increaseTotalScore(int score) {
        this.totalScore += score;
    }

    // TODO What's this? Quantity of what?
    // Answer: Quantity of mutants (if attacker) or tests (if defender).
    public void increaseQuantity() {
        quantity++;
    }
}
