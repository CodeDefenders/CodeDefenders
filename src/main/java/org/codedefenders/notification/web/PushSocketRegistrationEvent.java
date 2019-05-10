package org.codedefenders.notification.web;

public class PushSocketRegistrationEvent {

    private int gameID;
    private int playerID;

    // TODO multiple targets and actions to save messages?
    private String target;
    private String action;
    
    public PushSocketRegistrationEvent(String target, int gameID, int playerID, String action) {
        this.gameID = gameID;
        this.target = target;
        this.playerID = playerID;
        this.action = action;
    }
    
    public String getAction() {
        return action;
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
