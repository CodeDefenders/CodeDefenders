package org.codedefenders.events;

import org.codedefenders.User;
import org.codedefenders.util.DatabaseAccess;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by thomas on 06/03/2017.
 */
public class Event {

    private int eventId;

    private int playerId = 0;

    private int gameId = 0;

    private User user = null;

    private String message = null;

    private String parsedMessage = null;

    private EventType eventType = null;

    private EventStatus eventStatus = null;

    private Timestamp time;

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
        this.playerId = playerId;
        this.message = message;
        this.eventType = eventType;
        this.eventStatus = eventStatus;
        this.time = timestamp;
        this.gameId = gameId;
        this.eventId = eventId;
    }

    public void parse(){
        //TODO: This is a placeholder method and in future
        // will handle complex tokens in a message (e.g. [user:101] will be
        // replaced with the name of user 101).

        this.parsedMessage = getUser().getUsername() + ": " + message;
    }

    public String getParsedMessage(){
        if (parsedMessage == null){
            parse();
        }

        return parsedMessage;
    }

    public User getUser(){
        if (user == null){
            user = DatabaseAccess.getUser(playerId);
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
                        "(Game_ID, Player_ID, Event_Message, Event_Type, " +
                        "Event_Status) " +
                        "VALUES (%d, %d, '%s', '%s', '%s') ",
                gameId, playerId, message, eventType, eventStatus);

        sql = sql.replaceAll("\\<[^>]*>","");
        return DatabaseAccess.executeUpdate(sql);
    }


    public boolean update(){
        String sql = String.format("UPDATE events SET " +
                        "Game_ID=%d, " +
                        "Player_ID=%d', " +
                        "Event_Message='%s', " +
                        "Event_Type='%s', " +
                        "Event_Status='%s', " +
                        "Timestamp=FROM_UNIXTIME(%d) WHERE " +
                        "Event_ID=%d",
                gameId, playerId, message, eventType, eventStatus, time.getTime
                        (), eventId);
        return DatabaseAccess.executeUpdate(sql);
    }

}
