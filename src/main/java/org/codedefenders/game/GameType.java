package org.codedefenders.game;

public enum GameType {
    MULTIPLAYER("Multiplayer"),
    MELEE("Melee");

    private final String name;

    GameType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}