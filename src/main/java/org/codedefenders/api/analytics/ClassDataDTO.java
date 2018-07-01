package org.codedefenders.api.analytics;

public class ClassDataDTO {
    private long id;
    private String classname;
    private int games;
    private int testsSubmitted;
    private int mutantsSubmitted;
    private int mutantsAlive;
    private int mutantsEquivalent;

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

    public int getGames() {
        return games;
    }

    public void setGames(int games) {
        this.games = games;
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
}
