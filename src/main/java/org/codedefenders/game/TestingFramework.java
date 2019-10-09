package org.codedefenders.game;

public enum TestingFramework {
    JUNIT4("JUnit 4");
    // JUNIT5("JUnit 5");

    private String description;

    TestingFramework(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
