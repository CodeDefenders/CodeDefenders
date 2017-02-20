package org.codedefenders.multiplayer;

/**
 * Created by gordon on 20/02/2017.
 */
public class PlayerScore {
    private int playerId;
    private int totalScore;
    private int quantity;
    private String additionalInformation;

    public String toString() {
        return playerId + ": " + totalScore + ", " + quantity + "," + additionalInformation;
    }

    public String getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(String additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    public PlayerScore(int playerId) {
        this.playerId = playerId;
        this.totalScore = 0;
        this.quantity = 0;
    }

    public int getPlayerId() {
        return playerId;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public void increaseTotalScore(int score) {
        this.totalScore += score;
    }

    public void increaseQuantity() {
        quantity++;
    }
}
