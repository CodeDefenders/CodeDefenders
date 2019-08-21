package org.codedefenders.notification.events.server;

import com.google.gson.JsonElement;

public abstract class ServerEvent {
    public abstract JsonElement toJson();
}
