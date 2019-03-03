package org.codedefenders.notification.model;

import java.util.HashSet;
import java.util.Set;

public class ChatEvent {

    private int senderID;
    private Set<Integer> recipients = new HashSet<>();
    private String eventType;
    
    // TODO Escaping, size limit, etc..
    private String message;

    public int getSenderUserID() {
        return senderID;
    }

    public String getMessage() {
        return message;
    }

    public String getEventType() {
        return eventType;
    }
    
    public ChatEvent(int userID, String message, int... recipients) {
        super();
        this.senderID = userID;
        this.message = message;
        for (int recipient : recipients) {
            this.recipients.add(recipient);
        }
    }

    public boolean sendTo(int userID) {
        return recipients.contains(userID);
    }

}
