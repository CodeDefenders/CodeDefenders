package org.codedefenders.game;

public enum AssertionLibrary {
    JUNIT4("JUnit 4"),
    JUNIT5("JUnit 5"),
    HAMCREST("Hamcrest"),
    JUNIT4_HAMCREST("JUnit 4 + Hamcrest"),
    JUNIT5_HAMCREST("JUnit 5 + Hamcrest");

    private String description;

    AssertionLibrary(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
