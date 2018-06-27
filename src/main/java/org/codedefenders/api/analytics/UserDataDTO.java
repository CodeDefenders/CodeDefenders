package org.codedefenders.api.analytics;

public class UserDataDTO {
    private long id;
    private String username;
    private int mutantsSubmitted;
    private int mutantsAlive;
    private int equivalentMutantsSubmitted;
    private int testsSubmitted;
    private int mutantsKilled;
    private int attackerScore;
    private int defenderScore;
    private int totalScore;
    private int gamesPlayed;

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

    public int getEquivalentMutantsSubmitted() {
        return equivalentMutantsSubmitted;
    }

    public void setEquivalentMutantsSubmitted(int equivalentMutantsSubmitted) {
        this.equivalentMutantsSubmitted = equivalentMutantsSubmitted;
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

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }
}
