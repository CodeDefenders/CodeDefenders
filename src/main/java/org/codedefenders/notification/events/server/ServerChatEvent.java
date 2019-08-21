package org.codedefenders.notification.events.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Set;

public class ServerChatEvent extends ServerEvent {
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
    public ServerChatEvent(int userID, String message, int... recipients) {
        this.senderID = userID;
        this.message = message;
        for (int recipient : recipients) {
            this.recipients.add(recipient);
        }
    }

    public boolean hasRecipient(int userID) {
        return recipients.contains(userID);
    }

    @Override
    public JsonElement toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("message", message);
        return obj;
    }
}
