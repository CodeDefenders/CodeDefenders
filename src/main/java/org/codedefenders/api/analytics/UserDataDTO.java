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
package org.codedefenders.api.analytics;

public class UserDataDTO {
    private long id;
    private String username;
    private int gamesPlayed;
    private int attackerGamesPlayed;
    private int defenderGamesPlayed;
    private int attackerScore;
    private int defenderScore;
    private int mutantsSubmitted;
    private int mutantsAlive;
    private int mutantsEquivalent;
    private int testsSubmitted;
    private int mutantsKilled;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public int getAttackerGamesPlayed() {
        return attackerGamesPlayed;
    }

    public void setAttackerGamesPlayed(int attackerGamesPlayed) {
        this.attackerGamesPlayed = attackerGamesPlayed;
    }

    public int getDefenderGamesPlayed() {
        return defenderGamesPlayed;
    }

    public void setDefenderGamesPlayed(int defenderGamesPlayed) {
        this.defenderGamesPlayed = defenderGamesPlayed;
    }

    public int getAttackerScore() {
        return attackerScore;
    }

    public void setAttackerScore(int attackerScore) {
        this.attackerScore = attackerScore;
    }

    public int getDefenderScore() {
        return defenderScore;
    }

    public void setDefenderScore(int defenderScore) {
        this.defenderScore = defenderScore;
    }

    public int getMutantsSubmitted() {
        return mutantsSubmitted;
    }

    public void setMutantsSubmitted(int mutantsSubmitted) {
        this.mutantsSubmitted = mutantsSubmitted;
    }

    public int getMutantsAlive() {
        return mutantsAlive;
    }

    public void setMutantsAlive(int mutantsAlive) {
        this.mutantsAlive = mutantsAlive;
    }

    public int getMutantsEquivalent() {
        return mutantsEquivalent;
    }

    public void setMutantsEquivalent(int equivalentMutantsSubmitted) {
        this.mutantsEquivalent = equivalentMutantsSubmitted;
    }

    public int getTestsSubmitted() {
        return testsSubmitted;
    }

    public void setTestsSubmitted(int testsSubmitted) {
        this.testsSubmitted = testsSubmitted;
    }

    public int getMutantsKilled() {
        return mutantsKilled;
    }

    public void setMutantsKilled(int mutantsKilled) {
        this.mutantsKilled = mutantsKilled;
    }
}
