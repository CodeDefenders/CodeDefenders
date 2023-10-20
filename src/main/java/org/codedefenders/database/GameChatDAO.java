package org.codedefenders.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import javax.annotation.ManagedBean;
import javax.enterprise.context.ApplicationScoped;

import org.codedefenders.game.Role;
import org.codedefenders.notification.events.server.chat.ServerGameChatEvent;
import org.intellij.lang.annotations.Language;

@ApplicationScoped
@ManagedBean
public class GameChatDAO {
    /**
     * Gets all chat messages for a game.
     * @param gameId The ID of the game the messages are from.
     * @param limit The maximum number of chat messages to get.
     * @return A list of all chat messages for the game. Ordered from oldest to newest.
     */
    public List<ServerGameChatEvent> getChatMessages(int gameId, int limit) {
        @Language("SQL") String query = """
                SELECT chat.User_ID AS Sender_ID,
                       users.Username AS Sender_Name,
                       chat.Message AS Message,
                       chat.Role AS Role,
                       chat.IsAllChat AS IsAllChat,
                       chat.Game_ID AS Game_ID
                FROM game_chat_messages AS chat,
                     users
                WHERE chat.User_ID = users.User_ID
                  AND chat.Game_ID = ?
                ORDER BY chat.Message_ID DESC
                LIMIT ?;
        """;

        List<ServerGameChatEvent> messages = DB.executeQueryReturnList(query, GameChatDAO::chatMessageFromRS,
                DatabaseValue.of(gameId),
                DatabaseValue.of(limit));
        Collections.reverse(messages);
        return messages;
    }

    /**
     * Gets chat messages visible by a given role for a game.
     * @param gameId The ID of the game the messages are from.
     * @param role The role the messages should be visible from.
     * @param limit The maximum number of chat messages to get.
     * @return A list of chat messages visible by the given role for the game. Ordered from oldest to newest.
     */
    public List<ServerGameChatEvent> getChatMessages(int gameId, Role role, int limit) {
        if (role == Role.OBSERVER) {
            return getChatMessages(gameId, limit);
        } else if (role == Role.NONE || role == null) {
            throw new IllegalArgumentException("Cannot get chat messages for role " + role + ".");
        }

        @Language("SQL") String query = """
                SELECT chat.User_ID AS Sender_ID,
                       users.Username AS Sender_Name,
                       chat.Message AS Message,
                       chat.Role AS Role,
                       chat.IsAllChat AS IsAllChat,
                       chat.Game_ID AS Game_ID
                FROM game_chat_messages AS chat,
                     users
                WHERE chat.User_ID = users.User_ID
                  AND chat.Game_ID = ?
                  AND (chat.Role = 'OBSERVER' OR chat.Role = ? OR chat.IsAllChat = 1)
                ORDER BY chat.Message_ID DESC
                LIMIT ?;
        """;

        List<ServerGameChatEvent> messages = DB.executeQueryReturnList(query, GameChatDAO::chatMessageFromRS,
                DatabaseValue.of(gameId),
                DatabaseValue.of(role.toString()),
                DatabaseValue.of(limit));
        Collections.reverse(messages);
        return messages;
    }

    public int insertMessage(ServerGameChatEvent message) {
        @Language("SQL") String query = """
                INSERT INTO game_chat_messages
                (User_ID, IsAllChat, Message, Role, Game_ID)
                VALUES (?, ?, ?, ?, ?);
        """;

        DatabaseValue<?>[] values = new DatabaseValue[] {
                DatabaseValue.of(message.getSenderId()),
                DatabaseValue.of(message.isAllChat()),
                DatabaseValue.of(message.getMessage()),
                DatabaseValue.of(message.getRole().toString()),
                DatabaseValue.of(message.getGameId())
        };

        return DB.executeUpdateQueryGetKeys(query, values);
    }

    /**
     * Constructs a chat message from a DB row.
     * <br/>
     * <br/>
     * The required parameters are:
     * <ul>
     *     <li>Sender_ID</li>
     *     <li>Sender_Name</li>
     *     <li>Message</li>
     *     <li>Role</li>
     *     <li>IsAllChat</li>
     *     <li>Game_ID</li>
     * </ul>
     * @param rs The result set.
     * @return The constructed chat message.
     * @throws SQLException If any parameter is missing.
     */
    private static ServerGameChatEvent chatMessageFromRS(ResultSet rs) throws SQLException {
        ServerGameChatEvent message = new ServerGameChatEvent();
        message.setSenderId(rs.getInt("Sender_ID"));
        message.setSenderName(rs.getString("Sender_Name"));
        message.setMessage(rs.getString("Message"));
        message.setRole(Role.valueOf(rs.getString("Role")));
        message.setAllChat(rs.getBoolean("IsAllChat"));
        message.setGameId(rs.getInt("Game_ID"));
        return message;
    }
}
