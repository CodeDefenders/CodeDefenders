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

public class ClassDataDTO {
    private long id;
    private String classname;
    private String classalias;
    private int nrGames;
    private int attackerWins;
    private int defenderWins;
    private int nrPlayers;
    private int testsSubmitted;
    private int mutantsSubmitted;
    private int mutantsAlive;
    private int mutantsEquivalent;
    private ClassRatings ratings;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getClassname() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public String getClassalias() {
        return classalias;
    }

    public void setClassalias(String classalias) {
        this.classalias = classalias;
    }

    public int getNrGames() {
        return nrGames;
    }

    public int getAttackerWins() {
        return attackerWins;
    }

    public void setAttackerWins(int attackerWins) {
        this.attackerWins = attackerWins;
    }

    public int getDefenderWins() {
        return defenderWins;
    }

    public void setDefenderWins(int defenderWins) {
        this.defenderWins = defenderWins;
    }

    public void setNrGames(int nrGames) {
        this.nrGames = nrGames;
    }

    public int getNrPlayers() {
        return nrPlayers;
    }

    public void setNrPlayers(int nrPlayers) {
        this.nrPlayers = nrPlayers;
    }

    public int getTestsSubmitted() {
        return testsSubmitted;
    }

    public void setTestsSubmitted(int testsSubmitted) {
        this.testsSubmitted = testsSubmitted;
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

    public void setMutantsEquivalent(int mutantsEquivalent) {
        this.mutantsEquivalent = mutantsEquivalent;
    }

    public ClassRatings getRatings() {
        return ratings;
    }

    public void setRatings(ClassRatings ratings) {
        this.ratings = ratings;
    }

    public static class ClassRatings {
        private ClassRating cutMutationDifficulty;
        private ClassRating cutTestDifficulty;
        private ClassRating gameEngaging;

        public ClassRating getCutMutationDifficulty() {
            return cutMutationDifficulty;
        }

        public void setCutMutationDifficulty(ClassRating cutMutationDifficulty) {
            this.cutMutationDifficulty = cutMutationDifficulty;
        }

        public ClassRating getCutTestDifficulty() {
            return cutTestDifficulty;
        }

        public void setCutTestDifficulty(ClassRating cutTestDifficulty) {
            this.cutTestDifficulty = cutTestDifficulty;
        }

        public ClassRating getGameEngaging() {
            return gameEngaging;
        }

        public void setGameEngaging(ClassRating gameEngaging) {
            this.gameEngaging = gameEngaging;
        }
    }

    public static class ClassRating {
        private int count;
        private int sum;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public int getSum() {
            return sum;
        }

        public void setSum(int sum) {
            this.sum = sum;
        }
    }
}
