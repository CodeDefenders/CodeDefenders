package org.codedefenders.notification.model;

import java.util.HashSet;
import java.util.Set;

public class ChatEvent extends PushEvent {

    private int senderID;
    private Set<Integer> recipients = new HashSet<>();

    // TODO Escaping, size limit, etc..
    private String message;

    public int getSenderUserID() {
        return senderID;
    }

    public String getMessage() {
        return message;
    }

    // TODO: do we need recipients or is the gameId and team sufficient?
    public ChatEvent(int userID, String message, int... recipients) {
        this.senderID = userID;
        this.message = message;
        for (int recipient : recipients) {
            this.recipients.add(recipient);
        }
    }

    public boolean hasRecipient(int userID) {
        return recipients.contains(userID);
    }
}
