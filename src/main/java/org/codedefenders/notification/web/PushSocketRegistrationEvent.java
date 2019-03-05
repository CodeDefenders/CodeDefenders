package org.codedefenders.notification.web;

public class PushSocketRegistrationEvent {

    private int gameID;
    private int playerID;
    private String target;
    
    public PushSocketRegistrationEvent(String target, int gameID, int playerID) {
        this.gameID = gameID;
        this.target = target;
        this.playerID = playerID;
    }
    
    public String getTarget() {
        return target;
    }
    
    public int getGameID() {
        return gameID;
    }

    public int getPlayerID() {
        return playerID;
    }
}
