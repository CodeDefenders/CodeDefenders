package org.codedefenders.database;

import org.codedefenders.game.Role;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventStatus;
import org.codedefenders.model.EventType;

import javax.annotation.ManagedBean;
import javax.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

// TODO Probably this should expose some specific functions, like handleChatEvent, handle gameEvent, and the like
@ApplicationScoped
@ManagedBean
public class EventDAO {

    // Split this is possibly different calls, maybe there no need to expose Event
    // class to callers
    public boolean insert(Event event) {
        String query;
        DatabaseValue[] valueList;

        EventType eventType = event.getEventType();

        if (eventType.equals(EventType.ATTACKER_MUTANT_KILLED_EQUIVALENT)
                || eventType.equals(EventType.DEFENDER_MUTANT_EQUIVALENT)
                || eventType.equals(EventType.DEFENDER_MUTANT_CLAIMED_EQUIVALENT)) {
            query = String.join("\n",
                    "INSERT INTO events (Game_ID, Player_ID, Event_Type, Event_Status, Event_Message)",
                    "VALUES (?, ?, ?, ?, ?);");
            valueList = new DatabaseValue[]{DatabaseValue.of(event.gameId()),
                    DatabaseValue.of(event.getUser().getId()), DatabaseValue.of(eventType.toString()),
                    DatabaseValue.of(event.getEventStatus().toString()), DatabaseValue.of(event.getMessage())};
        } else if (eventType.equals(EventType.PLAYER_LOST_EQUIVALENT_DUEL) // Melee Game
                || eventType.equals(EventType.PLAYER_WON_EQUIVALENT_DUEL)
                || eventType.equals(EventType.PLAYER_KILLED_MUTANT)
                || eventType.equals(EventType.PLAYER_MUTANT_SURVIVED)
                || eventType.equals(EventType.PLAYER_MUTANT_CLAIMED_EQUIVALENT)) {
            query = String.join("\n",
                    "INSERT INTO events (Game_ID, Player_ID, Event_Type, Event_Status, Event_Message)",
                    "VALUES (?, ?, ?, ?, ?);");
            valueList = new DatabaseValue[]{DatabaseValue.of(event.gameId()),
                    DatabaseValue.of(event.getUser().getId()), DatabaseValue.of(eventType.toString()),
                    DatabaseValue.of(event.getEventStatus().toString()), DatabaseValue.of(event.getMessage())};
        } else {
            query = String.join("\n", "INSERT INTO events (Game_ID, Player_ID, Event_Type, Event_Status)",
                    "VALUES (?, ?, ?, ?);");
            valueList = new DatabaseValue[]{DatabaseValue.of(event.gameId()),
                    DatabaseValue.of(event.getUser().getId()), DatabaseValue.of(eventType.toString()),
                    DatabaseValue.of(event.getEventStatus().toString())};
        }

        final Connection conn1 = DB.getConnection();
        final PreparedStatement stmt1 = DB.createPreparedStatement(conn1, query, valueList);
        // The execute* returns the Connection to the ConnectionPool so we must not reuse it
        int eventId = DB.executeUpdateGetKeys(stmt1, conn1);

        if (eventId >= 0) {
            if (event.getChatMessage() != null) {
                query = "INSERT INTO event_chat (Event_Id, Message) VALUES (?, ?);";
                valueList = new DatabaseValue[]{DatabaseValue.of(eventId), DatabaseValue.of(event.getChatMessage())};
                // We need to get a second connection as the execute* returned the Connection to the ConnectionPool
                // so we must not reuse it
                final Connection conn = DB.getConnection();
                final PreparedStatement stmt2 = DB.createPreparedStatement(conn, query, valueList);
                // This automatically returns the connection to the ConnectionPool
                DB.executeUpdate(stmt2, conn);

            }
        }
        return eventId >= 0;
    }

    public boolean update(Event event) {
        EventType eventType = event.getEventType();

        String query = String.join("\n", "UPDATE events",
                "SET Game_ID=?, Player_ID=?, Event_Type=?, Event_Status=?, Timestamp=FROM_UNIXTIME(?)",
                "WHERE Event_ID=?");
        DatabaseValue[] valueList = new DatabaseValue[]{DatabaseValue.of(event.gameId()),
                DatabaseValue.of(event.getUser().getId()), DatabaseValue.of(eventType.toString()),
                DatabaseValue.of(event.getEventStatus().toString()), DatabaseValue.of((Long) event.getTimestamp()),
                DatabaseValue.of(event.getId())};

        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
        return DB.executeUpdate(stmt, conn);
    }

    static Event eventFromRS(ResultSet rs) throws SQLException {

        int eventId = rs.getInt("Event_ID");
        int gameId = rs.getInt("Game_ID");
        // TODO Is this correct? The columns is called Player_ID but the data is
        // User_ID?
        int userId = rs.getInt("Player_ID");
        String message = rs.getString("Event_Message");
        EventType eventType = EventType.valueOf(rs.getString("Event_Type"));
        EventStatus eventStatus = EventStatus.valueOf(rs.getString("Event_Status"));
        Timestamp timestamp = rs.getTimestamp("Timestamp");

        return new Event(eventId, gameId, userId, message, eventType, eventStatus, timestamp);
    }

    public List<Event> getEventsForGame(Integer gameId) {
        // TODO Use SOME VIEW INSTEAD OF EVENT TABLE?
        String query = String.join("\n", "SELECT * from events", "WHERE Game_ID=?");
        DatabaseValue[] values = new DatabaseValue[]{DatabaseValue.of(gameId)};
        return DB.executeQueryReturnList(query, EventDAO::eventFromRS, values);
    }

    public List<Event> getNewEventsForGame(int gameId, long timestamp, Role role) {
        String query = String.join("\n", "SELECT *", "FROM events", "LEFT JOIN event_messages AS em",
                "  ON events.Event_Type = em.Event_Type ", "LEFT JOIN event_chat AS ec",
                "  ON events.Event_Id = ec.Event_Id", "WHERE Game_ID=?", "  AND Event_Status=? ",
                "  AND Timestamp >= FROM_UNIXTIME(?)");
        if (role.equals(Role.ATTACKER)) {
            query += " AND events.Event_Type!='DEFENDER_MESSAGE'";
        } else if (role.equals(Role.DEFENDER)) {
            query += " AND events.Event_Type!='ATTACKER_MESSAGE'";
        }

        DatabaseValue[] values = new DatabaseValue[]{DatabaseValue.of(gameId),
                DatabaseValue.of(EventStatus.GAME.toString()), DatabaseValue.of(timestamp)};

        return DB.executeQueryReturnList(query, EventDAO::eventFromRS, values);
    }
}
