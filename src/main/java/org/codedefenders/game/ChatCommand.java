package org.codedefenders.game;

/**
 * Represents chat commands in the game chat.
 */
public enum ChatCommand {
    ALL("all"),
    TEAM("team");

    private final String commandString;

    ChatCommand(String commandString) {
        this.commandString = commandString;
    }

    public String getCommandString() {
        return commandString;
    }
}
