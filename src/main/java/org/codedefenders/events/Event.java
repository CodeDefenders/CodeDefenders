package org.codedefenders.events;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.HashMap;

import org.codedefenders.Role;
import org.codedefenders.User;
import org.codedefenders.util.DB;
import org.codedefenders.util.DatabaseAccess;
import org.codedefenders.util.DatabaseValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by thomas on 06/03/2017.
 */
public class Event {

	private static final HashMap<Role, String> ROLE_COLORS =
			new HashMap<>();
	protected static final Logger logger = LoggerFactory.getLogger(Event.class);


	static {
		ROLE_COLORS.put(Role.ATTACKER, "#FF0000");
		ROLE_COLORS.put(Role.DEFENDER, "#0000FF");
		ROLE_COLORS.put(Role.CREATOR, "#00FF00");
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

	public Event(int eventId, int gameId, int playerId, String message, String
			eventType,
	             String
			             eventStatus, Timestamp timestamp) {
		this(eventId, gameId, playerId, message, EventType.valueOf(eventType),
				EventStatus.valueOf(eventStatus), timestamp);
	}

	public Event(int eventId, int gameId, int playerId, String message,
	             EventType
			             eventType,
	             EventStatus
			             eventStatus, Timestamp timestamp) {
		String eString = eventType.toString();
		if (eString.contains("ATTACKER")) {
			role = Role.ATTACKER;
		} else if (eString.contains("DEFENDER")) {
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

	public void setChatMessage(String message) {
		chatMessage = message;
	}

	public String parse(HashMap<String, String> replacements, String message,
	                    boolean emphasise) {

		String procMessage = message;

		for (String s : replacements.keySet()) {
			procMessage = procMessage.replace(s, replacements.get(s));
		}

		if (procMessage.contains("@event_user")) {
			User user = getUser();
			procMessage = procMessage.replace("@event_user",
					user != null ? user.printFriendly(ROLE_COLORS.get(role)) :
							"<span style='color:#000000'>@Unknown</span>");
		}

		if (procMessage.contains("@chat_message")) {
			procMessage = procMessage.replace("@chat_message",
					getChatMessage());
		} else if (emphasise) {
			procMessage = "<span style='font-style: italic; font-weight: " +
					"bold;'>" + procMessage +
					"</span>";
		}

		if (currentUserName.length() > 0 && procMessage.contains
				(currentUserName)) {
			procMessage = procMessage.replace(currentUserName, "@You");
		}

		return procMessage;
	}

	public String getChatMessage() {
		if (chatMessage == null) {
			return "";
		}
		if (parsedChatMessage == null) {
			parsedChatMessage = parse(new HashMap<String, String>(),
					chatMessage, false);
		}
		return parsedChatMessage;
	}

	public void parse(HashMap<String, String> replacements, boolean emphasise) {

		this.parsedMessage = parse(replacements, message, emphasise);
	}

	public void parse(boolean emphasise) {
		parse(new HashMap<String, String>(), emphasise);
	}

	public String getParsedMessage() {
		if (parsedMessage == null) {
			parse(true);
		}

		return parsedMessage;
	}

	public User getUser() {
		if (user == null) {
			user = DatabaseAccess.getUser(userId);
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

	public boolean insert() {
		Connection conn = DB.getConnection();
		String query = "INSERT INTO events (Game_ID, Player_ID, Event_Type, Event_Status) VALUES (?, ?, ?, ?);";
		DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(gameId),
				DB.getDBV(userId),
				DB.getDBV(eventType.toString()),
				DB.getDBV(eventStatus.toString())};
		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
		if (DB.executeUpdate(stmt, conn)) {
			eventId = gameId;
			if (chatMessage != null) {
				query = "INSERT INTO event_chat (Event_Id, Message) VALUES (?, ?);";
				valueList = new DatabaseValue[]{DB.getDBV(eventId),
						DB.getDBV(chatMessage)};
				stmt = DB.createPreparedStatement(conn, query, valueList);
				DB.executeUpdate(stmt, conn);
			}
		} else {
			eventId = -1;
		}
		return eventId >= 0;
	}


	public boolean update() {
		Connection conn = DB.getConnection();
		String query = "UPDATE events SET Game_ID=?, Player_ID=?, Event_Type=?, Event_Status=?, Timestamp=FROM_UNIXTIME(?) WHERE Event_ID=?";
		DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(gameId),
				DB.getDBV(userId),
				DB.getDBV(eventType.toString()),
				DB.getDBV(eventStatus.toString()),
				DB.getDBV((Long) time.getTime()),
				DB.getDBV(eventId)};
		PreparedStatement stmt = DB.createPreparedStatement(conn, query, valueList);
		return DB.executeUpdate(stmt, conn);
	}

}
