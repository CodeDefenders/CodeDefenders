package org.codedefenders.notification.model;

public class Notification {
    private String type;
    private String message = "";

    public Notification(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
}
