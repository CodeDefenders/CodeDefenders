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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.RequestContextController;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.NotImplementedException;
import org.codedefenders.analysis.gameclass.MethodDescription;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameAccordionMapping;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.util.Constants;
import org.codedefenders.util.LlmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.TimeoutException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

@Named("llmService")
@ApplicationScoped
public class LlmService {

    private static final Logger logger = LoggerFactory.getLogger(LlmService.class);

    private static final String DEFENDER_SYSTEM_PROMPT =
            """
                    Write a single test for the first class of the following Java code using a maximum of 2 assertions.
                    The other classes are dependencies of the first class, you don't need to test them.
                    Write only the content of the test method, without including formatting, comments,
                    the header or the method declaration. Use JUnit 4.
                    """.trim().stripIndent();

    private static final String ATTACKER_SYSTEM_PROMPT =
            """
                    Change the first class of the following java code in a significant way.
                    The behaviour of the program should change.
                    Only change existing methods and fields.
                    Write nothing but the changed java code of the first class.
                    Make sure to introduce at least one change.""".trim().stripIndent();

    Configuration config;
    GameRepository gameRepository;
    GameManagingUtils gameManagingUtils;
    GameService gameService;
    MutantRepository mutantRepository;

    ChatModel model;

    /*
        Maps game ids to a boolean that determines whether the game should have an active llm thread of the respective
        sort.
        If true, a thread is currently running and will continue to run.
        If false, a thread is still running, but will finish after the current iteration (and remove the key-value pair)
        If not present, a thread is not running at all.
     */
    private final Map<Integer, Boolean> activeLlmPlayers;
    private final Map<Integer, Boolean> activeLlmDefenders;
    private final Map<Integer, Boolean> activeLlmAttackers;


    @Inject
    public LlmService(Configuration config,
                      GameRepository gameRepository,
                      MutantRepository mutantRepository,
                      GameManagingUtils gameManagingUtils,
                      RequestContextController requestContextController,
                      GameService gameService) {
        this.config = config;
        this.gameRepository = gameRepository;
        this.gameManagingUtils = gameManagingUtils;
        this.gameService = gameService;
        this.mutantRepository = mutantRepository;

        activeLlmPlayers = new HashMap<>();
        activeLlmDefenders = new HashMap<>();
        activeLlmAttackers = new HashMap<>();

        if (config.isLlmOpenAI()) {
            this.model = OpenAiChatModel.builder()
                    .apiKey(config.getOpenaiApiKey())
                    .modelName(config.getOpenaiChatgptModel())
                    .build();
        } else if (config.isLlmLocal()) {
            this.model = OllamaChatModel.builder()
                    .baseUrl("http://127.0.0.1:11434")
                    .modelName("gemma3n:e2b")
                    .temperature(0.9)
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

    private Map<Integer, Boolean> getCorrectMap(Role r) {
        return switch (r) {
            case ATTACKER -> activeLlmAttackers;
            case DEFENDER -> activeLlmDefenders;
            case PLAYER -> activeLlmPlayers;
            default -> throw new IllegalArgumentException("Illegal role: " + r);
        };
    }

    private int getCorrectUserId(Role r) {
        return switch (r) {
            case ATTACKER -> Constants.AI_ATTACKER_USER_ID;
            case DEFENDER -> Constants.AI_DEFENDER_USER_ID;
            case PLAYER -> Constants.AI_PLAYER_USER_ID;
            default -> throw new IllegalArgumentException("Illegal role: " + r);
        };
    }

    public boolean isLlmPlayerActive(AbstractGame game, Role role) {
        return getCorrectMap(role).containsKey(game.getId())
                && getCorrectMap(role).get(game.getId());
    }

    public void setPlayerActive(AbstractGame game, Role role, boolean active) {
        Map<Integer, Boolean> m = getCorrectMap(role);
        boolean alreadyPresent = m.containsKey(game.getId());

        if (active || alreadyPresent) { //Never put a new 'false' value, it wouldn't be deleted
            m.put(game.getId(), active);
        }

        if (active && !alreadyPresent) {
            game.addPlayer(getCorrectUserId(role), role);
            new LlmPlayerThread(game, role).start();
        }

    }

    public void finishThread(AbstractGame game, Role role) {
        getCorrectMap(role).remove(game.getId());
    }

    private String generateTest(AbstractGame game, SimpleUser user) {
        StringBuilder input = new StringBuilder(game.getCUT().getSourceCode());
        for (String d : game.getCUT().getDependencyCode()) {
            input.append(d);
        }

        String systemPromptAddition = "";
        Optional<MethodDescription> methodDescription = getRandomMethodWithLivingMutant(game, user);


        if (methodDescription.isPresent()) { //TODO Mutants outside of methods
            systemPromptAddition = "\nFocus on the method " + methodDescription.get().getDescription();
        }

        String result = getResponse(input.toString(), DEFENDER_SYSTEM_PROMPT + systemPromptAddition);
        String formattedResult = LlmUtils.extractTestContentFromReply(result);
        String testTemplate = game.getCUT().getTestTemplate();
        String testSrc = testTemplate.replace(Constants.TEST_TEMPLATE_PLACEHOLDER, formattedResult);
        logger.info("AI defender generated test: {}", testSrc);
        return testSrc;
    }

    private void submitTest(AbstractGame game, String testSrc) {
        RequestContextController requestContextController = CDIUtil.getBeanFromCDI(RequestContextController.class);
        requestContextController.activate();
        try {
            if (game instanceof MultiplayerGame multiplayerGame) {
                gameManagingUtils.createBattlegroundTest(multiplayerGame, Constants.AI_DEFENDER_USER_ID, testSrc);
            } else {//TODO
                throw new NotImplementedException("TODO");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            requestContextController.deactivate();
        }
    }

    private String generateMutant(AbstractGame game, SimpleUser user) {
        StringBuilder input = new StringBuilder(game.getCUT().getSourceCode());
        String firstDependencyName = null;//TODO Gibt's hierfür bessere Möglichkeiten? Gefahr,
        // wenn CUT und Dependency gleichen Namen haben?
        for (String d : game.getCUT().getDependencyCode()) {
            input.append(System.lineSeparator()).append(d);
            if (firstDependencyName == null) {
                firstDependencyName = game.getCUT().getDependencyNames().get(0);
            }
        }

        String result = getResponse(input.toString(), ATTACKER_SYSTEM_PROMPT);
        String formattedResult = result.replace("```java", "").replace("```", "");
        if (firstDependencyName != null) {
            int classDeclaration = formattedResult.indexOf("class " + firstDependencyName);
            if (classDeclaration > 0) {
                formattedResult = formattedResult.substring(0, classDeclaration);
                int lastNewline = formattedResult.lastIndexOf(System.lineSeparator());
                formattedResult = formattedResult.substring(0, lastNewline);
            }
        }
        logger.info("LLM attacker generated test: {}", formattedResult);
        return formattedResult;
    }

    private void submitMutant(AbstractGame game, String mutantSrc) {
        RequestContextController requestContextController = CDIUtil.getBeanFromCDI(RequestContextController.class);
        requestContextController.activate();
        try {
            if (game instanceof MultiplayerGame multiplayerGame) {
                gameManagingUtils.createBattlegroundMutant(multiplayerGame, Constants.AI_ATTACKER_USER_ID, mutantSrc);
            } else {//TODO
                throw new NotImplementedException("TODO");
            }
        } catch (IOException | GameManagingUtils.MutantCreationException e) {
            throw new RuntimeException(e);
        } finally {
            requestContextController.deactivate();
        }
    }

    /**
     * Returns a list of all methods that contain a living mutant that haven't been created by the user.
     * If one method contains several living mutants, it occurs several times in the list.
     */
    private List<MethodDescription> getMethodsWithLivingMutants(AbstractGame game, SimpleUser user) {
        List<MutantDTO> mutants = gameService.getMutants(user, game);
        List<MethodDescription> methods = game.getCUT().getMethodDescriptions();
        GameAccordionMapping mapping = GameAccordionMapping.computeForMutants(methods, mutants);
        HashMap<MethodDescription, SortedSet<Integer>> map = mapping.elementsPerMethod;
        List<MethodDescription> listOfPossibilities = new ArrayList<>();
        for (MethodDescription m : map.keySet()) {
            for (Integer mutantId : map.get(m)) {
                Mutant mutant = mutantRepository.getMutantById(mutantId);
                if (mutant.isAlive() && mutant.getCreatorId() != user.getId()) {
                    listOfPossibilities.add(m);
                }
            }
        }
        return listOfPossibilities;
    }

    private Optional<MethodDescription> getRandomMethodWithLivingMutant(AbstractGame game, SimpleUser user) {
        List<MethodDescription> methods = getMethodsWithLivingMutants(game, user);
        if (!methods.isEmpty()) {
            return Optional.of(methods.get((int) (Math.random() * methods.size())));
        } else {
            return Optional.empty();
        }
    }

    class LlmPlayerThread extends Thread {
        private static final int secondsBetweenTests = 10;//TODO Veränderbar machen
        private AbstractGame game;
        private final Role role;
        private final SimpleUser user;

        LlmPlayerThread(AbstractGame game, Role role) {
            this.game = game;
            this.role = role;
            int userId = switch (role) {
                case ATTACKER -> Constants.AI_ATTACKER_USER_ID;
                case DEFENDER -> Constants.AI_DEFENDER_USER_ID;
                case PLAYER -> Constants.AI_PLAYER_USER_ID;
                default -> throw new IllegalArgumentException("No such role allowed for LLM: " + role);
            };
            this.user = new SimpleUser(userId, "PLACEHOLDER");
        }

        @Override
        public void run() {
            logger.info("Starting LlmPlayerThread for game {} with role {}", game.getId(), role);
            while (isLlmPlayerActive(game, role) && gameRepository.isGameActive(game.getId())) {
                try {
                    if (role == Role.DEFENDER) {
                        String testSrc = generateTest(game, user);
                        game = gameRepository.getGame(game.getId());
                        submitTest(game, testSrc);
                    } else if (role == Role.ATTACKER) {
                        String mutantSrc = generateMutant(game, user);
                        game = gameRepository.getGame(game.getId());
                        submitMutant(game, mutantSrc);
                    } else if (role == Role.PLAYER) {
                        //TODO
                    } else throw new IllegalArgumentException("No support for this role: " + role);
                    sleep((long) secondsBetweenTests * 1000);
                } catch (InterruptedException e) {
                    logger.warn("AiPlayerThread interrupted");
                    break;
                } catch (TimeoutException e) {
                    logger.error("AiPlayerThread for game {} with role {} timed out after.",
                            game.getId(), role);
                    break;
                }
            }
            finishThread(game, role);
        }

    }
}
