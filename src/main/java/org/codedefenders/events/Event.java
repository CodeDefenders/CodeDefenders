package org.codedefenders.events;

import org.codedefenders.Role;
import org.codedefenders.User;
import org.codedefenders.util.DatabaseAccess;

import java.sql.Timestamp;
import java.util.HashMap;

/**
 * Created by thomas on 06/03/2017.
 */
public class Event {

    private static final HashMap<Role, String> ROLE_COLORS =
            new HashMap<>();

    static {
        ROLE_COLORS.put(Role.ATTACKER, "#FF0000");
        ROLE_COLORS.put(Role.DEFENDER, "#0000FF");
        ROLE_COLORS.put(Role.CREATOR, "#00FF00");
    }

    private int eventId;

    private int userId = 0;

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

    public Event(int eventId, int gameId, int playerId, String message, String
            eventType,
                 String
            eventStatus, Timestamp timestamp){
        this(eventId, gameId, playerId, message, EventType.valueOf(eventType),
                EventStatus.valueOf(eventStatus), timestamp);
    }

    public Event(int eventId, int gameId, int playerId, String message,
                 EventType
            eventType,
                 EventStatus
            eventStatus, Timestamp timestamp){
        String eString = eventType.toString();
        if (eString.contains("ATTACKER")){
            role = Role.ATTACKER;
        } else if (eString.contains("DEFENDER")){
            role = Role.DEFENDER;
        } else {
            role = Role.CREATOR;
        }

        this.userId = playerId;
        this.message = message;
        this.eventType = eventType;
        this.eventStatus = eventStatus;
        this.time = timestamp;
        this.gameId = gameId;
        this.eventId = eventId;
    }

    public void setChatMessage(String message){
        chatMessage = message;
    }

    public String parse(HashMap<String, String> replacements, String message){

        String procMessage = message;

        for (String s : replacements.keySet()){
            procMessage = procMessage.replace(s, replacements.get(s));
        }

        if (procMessage.contains("@event_user")) {
            User user = getUser();
            procMessage = procMessage.replace("@event_user",
                    user != null ? user.printFriendly(ROLE_COLORS.get(role)) :
            "<span style='color:#000000'>@Unknown</span>");
        }

        if (procMessage.contains("@chat_message")){
            procMessage = procMessage.replace("@chat_message",
                    getChatMessage());
        }
        return procMessage;
    }

    public String getChatMessage(){
        if (chatMessage == null){
            return "";
        }
        if (parsedChatMessage == null){
            parsedChatMessage = parse(new HashMap<String, String>(),
                    chatMessage);
        }
        return parsedChatMessage;
    }

    public void parse(HashMap<String, String> replacements){

        this.parsedMessage = parse(replacements, message);
    }

    public void parse(){
        parse(new HashMap<String, String>());
    }

    public String getParsedMessage(){
        if (parsedMessage == null){
            parse();
        }

        return parsedMessage;
    }

    public User getUser(){
        if (user == null){
            user = DatabaseAccess.getUser(userId);
        }

        return user;
    }

    public EventType getEventType(){
        return eventType;
    }

    public void setStatus(EventStatus e){
        eventStatus = e;
    }

    public boolean insert(){
        String sql = String.format("INSERT INTO events " +
                        "(Game_ID, Player_ID, Event_Type, " +
                        "Event_Status) " +
                        "VALUES (%d, %d, '%s', '%s') ",
                gameId, userId,
                eventType,
                eventStatus);

        eventId = DatabaseAccess.executeInsert(sql);

        if (chatMessage != null && eventId >= 0) {
            String sqlMessage = String.format("INSERT INTO event_chat " +
                            "(Event_Id, Message) " +
                            "VALUES (%d, '%s') ",
                    eventId,
                    chatMessage);

            DatabaseAccess.executeInsert(sqlMessage);

        }
        return eventId >= 0;
    }




    public boolean update(){
        String sql = String.format("UPDATE events SET " +
                        "Game_ID=%d, " +
                        "Player_ID=%d', " +
                        "Event_Type='%s', " +
                        "Event_Status='%s', " +
                        "Timestamp=FROM_UNIXTIME(%d) WHERE " +
                        "Event_ID=%d",
                gameId, userId, eventType,
                eventStatus, time
                        .getTime
                                (), eventId);
        return DatabaseAccess.executeUpdate(sql);
    }

}
