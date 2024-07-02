package org.codedefenders.persistence.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.game.Role;
import org.codedefenders.notification.events.server.chat.ServerGameChatEvent;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.intellij.lang.annotations.Language;

import com.google.common.collect.Lists;

import static org.codedefenders.persistence.database.util.ResultSetUtils.generatedKeyFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.listFromRS;

@ApplicationScoped
public class GameChatRepository {
    private final QueryRunner queryRunner;

    @Inject
    public GameChatRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

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
                -- ORDER descending here, so the LIMIT limits to the n newest messages.
                ORDER BY chat.Message_ID DESC
                LIMIT ?;
        """;

        var messages = queryRunner.query(query, listFromRS(GameChatRepository::chatMessageFromRS),
                gameId, limit);
        // Reverse the list, so the newest message is the last.
        return Lists.reverse(messages);
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
                -- ORDER descending here, so the LIMIT limits to the n newest messages.
                ORDER BY chat.Message_ID DESC
                LIMIT ?;
        """;


        var messages = queryRunner.query(query, listFromRS(GameChatRepository::chatMessageFromRS),
                gameId,
                role.toString(),
                limit);
        // Reverse the list, so the newest message is the last.
        return Lists.reverse(messages);
    }

    public int insertMessage(ServerGameChatEvent message) {
        @Language("SQL") String query = """
                INSERT INTO game_chat_messages
                (User_ID, IsAllChat, Message, Role, Game_ID)
                VALUES (?, ?, ?, ?, ?);
        """;

        return queryRunner.insert(query, generatedKeyFromRS(),
                message.getSenderId(),
                message.isAllChat(),
                message.getMessage(),
                message.getRole().toString(),
                message.getGameId())
                .orElseThrow();
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
