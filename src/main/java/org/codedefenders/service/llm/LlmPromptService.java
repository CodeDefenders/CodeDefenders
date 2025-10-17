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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.model.llm.LlModel;
import org.codedefenders.model.llm.LlmConversation;
import org.codedefenders.model.llm.LlmType;
import org.codedefenders.persistence.database.LlmRepository;
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
class LlmPromptService {
    private static final Logger logger = LoggerFactory.getLogger(LlmPromptService.class);

    //Maps model name to ChatModel
    private final Map<LlModel, ChatModel> openaiModels = new HashMap<>();
    private final Map<LlModel, ChatModel> ollamaModels = new HashMap<>();

    @Inject
    LlmPromptService(LlmRepository llmRepo, Configuration config) {
        List<LlModel> models = llmRepo.getAllModels();
        for (LlModel m : models) { //TODO eigene Methode
            if (m.getType() == LlmType.OPENAI) {
                openaiModels.put(m, OpenAiChatModel.builder()
                        .apiKey(config.getOpenaiApiKey())
                        .modelName(m.getName())
                        .build());
            }
            if (m.getType() == LlmType.OLLAMA) {
                ollamaModels.put(m, OllamaChatModel.builder()
                        .baseUrl(config.getLlmLocalServer())
                        .modelName(m.getName())
                        .temperature(0.9)
                        .build());
            }
        }
    }

    /**
     * Queries the LLM specified by {@code model} with the prompts specified by {@code conversation}. The
     * {@link LlmConversation#getCurrentType()} has to be specified prior to calling this method, or it will throw
     * a {@link RuntimeException}. This method will block until the LLM has returned a result, or it has timed out
     * (usually after 3 Minutes).
     */
    String getResponse(LlModel model, LlmConversation conversation) {
        logger.info("Sending conversation \n{}\n to model {}.", conversation, model);
        ChatMessage[] chatMessages = conversation.toArray();

        Map<LlModel, ChatModel> chatMap = switch (model.getType()) {
            case OPENAI -> openaiModels;
            case OLLAMA -> ollamaModels;
            default -> throw new IllegalArgumentException("Unsupported model type: " + model.getType());
        };
        ChatModel chatModel = chatMap.get(model);
        if (chatModel != null) {
            ChatResponse response = chatModel.chat(chatMessages);
            conversation.addAiMessage(response.aiMessage());
            String responseText = response.aiMessage().text();
            logger.info("LLM responded with {}", responseText);
            return responseText;
        } else {
            throw new IllegalArgumentException("No model with this name in ChatModelMap: " + model.getName());
        }
    }
}
