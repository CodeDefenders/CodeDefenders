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
package org.codedefenders.service.llm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.model.llm.ChatMessageDTO;
import org.codedefenders.model.llm.LlModel;
import org.codedefenders.model.llm.LlmConversation;
import org.codedefenders.persistence.database.LlmConversationRepository;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * This class is responsible for actually talking to the LLM. It should make as little assumptions as possible
 * about the structure of LLM actions - for example, it should not meddle with prompts and results - and should hide all
 * technical details of communication with the LLM from outside classes.
 */
@ApplicationScoped
public class LlmPromptService {
    private static final Logger logger = LoggerFactory.getLogger(LlmPromptService.class);

    @Inject
    LlmConversationRepository conversationRepository;

    @Inject
    Configuration config;

    /**
     * Queries the LLM specified by {@code model} with the prompts specified by {@code conversation}.
     * This method will block until the LLM has returned a result, or it has timed out
     * (usually after 3 Minutes).
     */
    public String getResponse(LlModel model, LlmConversation conversation) {
        ChatMessage[] chatMessages = conversation.toArray();

        int inputLength = 0;
        for (ChatMessageDTO msg : conversation.getMessages()) {
            inputLength += msg.getText().length();
        }
        logger.info("Sending conversation with {} characters to model {}", inputLength, model);

        ChatModel chatModel = switch (model.getType()) {
            case OLLAMA -> OllamaChatModel.builder()
                    .baseUrl(config.getLlmLocalServer())
                    .modelName(model.getName())
                    .build();
            case OPENAI -> OpenAiChatModel.builder()
                    .apiKey(AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.OPENAI_KEY).getStringValue())
                    .modelName(model.getName())
                    .build();
            default -> throw new IllegalArgumentException("Unsupported model type: " + model.getType());
        };

        if (chatModel != null) {
            conversationRepository.saveConversation(conversation);
            ChatResponse response = chatModel.chat(chatMessages);
            conversation.addAiMessage(response.aiMessage(), model,
                    response.tokenUsage().inputTokenCount(), response.tokenUsage().outputTokenCount());
            conversationRepository.saveConversation(conversation);
            String responseText = response.aiMessage().text();
            logger.info("Model {} responded with {} characters", model, responseText.length());
            return responseText;
        } else {
            throw new IllegalArgumentException("No model with this name in ChatModelMap: " + model.getName());
        }
    }
}
