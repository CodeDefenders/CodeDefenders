/*
 * Copyright (C) 2016-2019 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.model;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.HashMap;

import org.codedefenders.database.UserDAO;
import org.codedefenders.game.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by thomas on 06/03/2017.
 */
public class Event {

    private static final HashMap<Role, String> ROLE_COLORS = new HashMap<>();
    protected static final Logger logger = LoggerFactory.getLogger(Event.class);

    static {
        ROLE_COLORS.put(Role.ATTACKER, "#FF0000");
        ROLE_COLORS.put(Role.DEFENDER, "#0000FF");
        ROLE_COLORS.put(Role.OBSERVER, "#00FF00");
    }

    @Override
    public String toString() {
        return "Event " + getEventType() + " " + getEventStatus() + // " from user " + getUser().getId() +
                " with message " + getMessage();
    }

    private int eventId;

    private int userId = 0;

    private String currentUserName = "";

    public String getCurrentUserName() {
        return currentUserName;
    }

    public void setCurrentUserName(String currentUserName) {
        this.currentUserName = currentUserName;
    }

    private int gameId = 0;

    private transient User user = null;

    private String message = null;

    private String chatMessage = null;

    private String parsedChatMessage = null;

    private String parsedMessage = null;

    private EventType eventType = null;

    private EventStatus eventStatus = null;

    private Timestamp time;

    private Role role;

    public Event(int eventId, int gameId, int userId, String message, String eventType, String eventStatus,
            Timestamp timestamp) {
        this(eventId, gameId, userId, message, EventType.valueOf(eventType), EventStatus.valueOf(eventStatus),
                timestamp);
    }

    public Event(int eventId, int gameId, int userId, String message, EventType eventType, EventStatus eventStatus,
            Timestamp timestamp) {
        String eventString = eventType.toString();
        if (eventString.contains("ATTACKER")) {
            role = Role.ATTACKER;
        } else if (eventString.contains("DEFENDER")) {
            role = Role.DEFENDER;
        } else if (eventString.contains("PLAYER")) {
            role = Role.PLAYER;
        } else {
            role = Role.OBSERVER;
        }

        this.userId = userId;
        this.message = message;
        this.eventType = eventType;
        this.eventStatus = eventStatus;
        this.time = timestamp;
        this.gameId = gameId;
        this.eventId = eventId;
    }

    public void setChatMessage(String message) {
        chatMessage = message;
    }

    public String parse(HashMap<String, String> replacements, String message, boolean emphasise) {

        String procMessage = message;

        for (String s : replacements.keySet()) {
            procMessage = procMessage.replace(s, replacements.get(s));
        }

        if (procMessage.contains("@event_user")) {
            User user = getUser();

            String userLabel = (user == null) ? "Unknown"
                    : (user.getUsername().equals(currentUserName)) ? "You" : user.getUsername();
            String color = (user == null) ? "#000000" : ROLE_COLORS.get(role);

            procMessage = procMessage.replace("@event_user",
                    "<span style='color: " + color + "'>@" + userLabel + "</span>");
        }

        if (procMessage.contains("@chat_message")) {
            procMessage = procMessage.replace("@chat_message", getChatMessage());
        } else if (emphasise) {
            procMessage = "<span style='font-style: italic; font-weight: " + "bold;'>" + procMessage + "</span>";
        }

        return procMessage;
    }

    public String getChatMessage() {
        if (chatMessage == null) {
            return "";
        }
        if (parsedChatMessage == null) {
            parsedChatMessage = parse(new HashMap<>(), chatMessage, false);
        }
        return parsedChatMessage;
    }

    public void parse(HashMap<String, String> replacements, boolean emphasise) {

        this.parsedMessage = parse(replacements, message, emphasise);
    }

    public void parse(boolean emphasise) {
        parse(new HashMap<>(), emphasise);
    }

    public String getMessage() {
        return message;
    }

    public String getParsedMessage() {
        if (parsedMessage == null) {
            parse(true);
        }

        return parsedMessage;
    }

    public User getUser() {
        if (user == null) {
            user = UserDAO.getUserById(userId);
        }

        return user;
    }

    public EventStatus getEventStatus() {
        return eventStatus;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setStatus(EventStatus e) {
        eventStatus = e;
    }

    public static final Comparator<Event> MAX_ID_COMPARATOR = Comparator.comparingInt(o -> o.eventId);

    public int getId() {
        return this.eventId;
    }

    public Integer gameId() {
        return this.gameId;
    }

    public Long getTimestamp() {
        return time.getTime();
    }
}
