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
package org.codedefenders.service;

import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

@ApplicationScoped
public class LlmService {

    private static final Logger logger = LoggerFactory.getLogger(LlmService.class);

    Configuration config;
    ChatModel model;

    /*
        The player ids of all llm players whose threads are supposed to be active. If they are not on
        this list, their thread will terminate after the current iteration.
     */
    private final Set<Integer> activeLlmPlayers;


    @Inject
    public LlmService(Configuration config) {
        this.config = config;
        activeLlmPlayers = new HashSet<>();

        if (config.isLlmOpenAI()) {
            this.model = OpenAiChatModel.builder()
                    .apiKey(config.getOpenaiApiKey())
                    .modelName(config.getOpenaiChatgptModel())
                    .build();
        } else if (config.isLlmLocal()) {
            this.model = OllamaChatModel.builder()
                    .baseUrl("http://127.0.0.1:11434")
                    .modelName("gemma3n:e2b")
                    .build();
        }
    }


    public String getResponse(String userMessage, String... systemMessages) {
        logger.info("Send message: \n {} to LLM with system messages:\n{}", userMessage,
                String.join("\n", systemMessages));
        ChatMessage[] chatMessages = new ChatMessage[systemMessages.length + 1];
        for (int i = 0; i < systemMessages.length; i++) {
            chatMessages[i] = SystemMessage.from(systemMessages[i]);
        }
        chatMessages[chatMessages.length - 1] = UserMessage.from(userMessage);
        ChatResponse response = model.chat(chatMessages);
        String responseText = response.aiMessage().text();
        logger.info("LLM responded with {}", responseText);
        return responseText;
    }

    public boolean isThreadActive(Player player) {
        return activeLlmPlayers.contains(player.getId());
    }

    public void setPlayerActive(Player p, boolean active) {
        if (active) {
            activeLlmPlayers.add(p.getId());
        } else {
            activeLlmPlayers.remove(p.getId());
        }
    }
}
