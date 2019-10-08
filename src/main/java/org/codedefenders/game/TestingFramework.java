package org.codedefenders.game;

public enum TestingFramework {
    JUNIT4("JUnit 4"),
    JUNIT5("JUnit 5 (currently just JUnit 4 with JUnit 5 assertions)");

    private String description;

    TestingFramework(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
