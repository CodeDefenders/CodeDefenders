/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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
package org.codedefenders.persistence.database;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.dbutils.ResultSetHandler;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.llm.ChatMessageDTO;
import org.codedefenders.model.llm.LlModel;
import org.codedefenders.model.llm.LlmConversation;
import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.model.llm.LlmType;
import org.codedefenders.model.llm.PromptType;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.persistence.database.util.ResultSetUtils;
import org.codedefenders.service.UserService;
import org.codedefenders.service.llm.NoSuchModelException;
import org.intellij.lang.annotations.Language;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

@ApplicationScoped
public class LlmConversationRepository {

    @Inject
    QueryRunner queryRunner;

    @Inject
    UserService userService;

    @Inject
    LlmRepository llmRepository;

    public void saveConversation(LlmConversation conversation) {
        if (conversation.getId() < 0) {
            @Language("SQL")
            String sql = """
                    INSERT INTO llm_conversations(
                        Strategy, Type, Game_ID, User_ID, Is_Active, Is_Success, Test_ID, Mutant_ID)
                        value (?, ?, ?, ?, ?, ?, ?, ?);
                    """;
            conversation.setId(queryRunner.insert(sql, ResultSetUtils.generatedKeyFromRS(),
                    conversation.getStrategy().name(),
                    conversation.getType().toString(),
                    conversation.getGame().getId(),
                    conversation.getUser().getId(),
                    conversation.isActive(),
                    conversation.isSuccess(),
                    conversation.getTestId() > 0 ? conversation.getTestId() : null,
                    conversation.getMutantId() > 0 ? conversation.getMutantId() : null
            ).orElseThrow());
        } else {
            @Language("SQL")
            String sql = """
                    UPDATE llm_conversations SET Is_Active=?, Is_Success = ?, Test_ID = ?, Mutant_ID = ?
                                             WHERE Conversation_ID = ?;
                    """;
            queryRunner.update(sql, conversation.isActive(), conversation.isSuccess(),
                    conversation.getTestId() > 0 ? conversation.getTestId() : null,
                    conversation.getMutantId() > 0 ? conversation.getMutantId() : null,
                    conversation.getId()
            );
        }

        for (int i = 0; i < conversation.getMessages().size(); i++) {
            ChatMessageDTO dto = conversation.getMessages().get(i);
            @Language("SQL")
            String messageSql = """
                    INSERT INTO llm_messages
                        (Conversation_ID,
                         Index_in_conversation,
                         Message_type,
                         Input_tokens,
                         Output_tokens,
                         timestamp,
                         Model_name,
                         Model_type,
                         Content) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE Conversation_ID = Conversation_ID
                    """;
            queryRunner.update(messageSql,
                    conversation.getId(),
                    i,
                    dto.msg().type().toString(),
                    dto.inputTokens(),
                    dto.outputTokens(),
                    dto.time(),
                    dto.target().getName(),
                    dto.target().getType().toString(),
                    dto.getText()
            );
        }
    }

    public List<LlmConversation> getConversations(AbstractGame game) {
        @Language("SQL")
        String sql = """
                SELECT * FROM llm_conversations c JOIN llm_messages m ON c.Conversation_ID = m.Conversation_ID
                WHERE c.Game_ID = ? ORDER BY m.Conversation_ID, m.Index_in_conversation
                """;
        ResultSetHandler<List<LlmConversation>> handler = rs -> {
            List<LlmConversation> result = new ArrayList<>();
            LlmConversation currentConversation = null;
            while (rs.next()) {
                int id = rs.getInt("Conversation_ID");
                if (currentConversation == null || currentConversation.getId() != id) {
                    LlmStrategy strategy = Optional.of(LlmStrategy.valueOf(rs.getString("Strategy"))).orElse(LlmStrategy.INVALID);
                    PromptType promptType = PromptType.valueOf(rs.getString("Type"));
                    int userId = rs.getInt("User_ID");
                    SimpleUser user = userService.getSimpleUserById(userId).orElseThrow();
                    boolean active = rs.getBoolean("Is_active");
                    boolean success = rs.getBoolean("Is_success");
                    int testId = rs.getInt("TEST_ID");
                    int mutantId = rs.getInt("MUTANT_ID");
                    currentConversation = new LlmConversation(
                            promptType, game, user, strategy, active, success, testId, mutantId);
                    currentConversation.setId(id);
                    result.add(currentConversation);
                }

                LlmType modelTypeName = LlmType.valueOf(rs.getString("Model_type"));
                String modelName = rs.getString("Model_name");
                LlModel model = new LlModel(modelName, modelTypeName);
                try {
                    llmRepository.loadModel(model);
                } catch (
                        NoSuchModelException ignored) { //TODO Überprüfen, ob das bei gelöschten Modellen alles funkt
                }
                Timestamp timestamp = rs.getTimestamp("Timestamp");
                ChatMessageType type = ChatMessageType.valueOf(rs.getString("Message_type"));
                String content = rs.getString("Content");
                ChatMessage chatMessage = switch (type) {
                    case AI -> AiMessage.from(content);
                    case USER -> UserMessage.from(content);
                    case SYSTEM -> SystemMessage.from(content);
                    default -> throw new IllegalArgumentException("No such message type supported: " + type);
                };
                int inputTokens = rs.getInt("Input_tokens");
                int outputTokens = rs.getInt("Output_tokens");
                ChatMessageDTO messageDTO =
                        new ChatMessageDTO(chatMessage, timestamp, model, inputTokens, outputTokens);
                currentConversation.add(messageDTO);

            }

            return result;
        };

        return queryRunner.query(sql, handler, game.getId());
    }
}
