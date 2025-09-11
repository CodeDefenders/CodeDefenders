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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.SortedSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.RequestContextController;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.codedefenders.analysis.gameclass.MethodDescription;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameAccordionMapping;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.LLMType;
import org.codedefenders.model.LLModel;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.LLMRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.admin.AdminSystemSettings;
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

    private final ExecutorService llmExecutor;
    private final ScheduledExecutorService organizerExecutor;

    Configuration config;
    GameRepository gameRepository;
    GameManagingUtils gameManagingUtils;
    GameService gameService;
    MutantRepository mutantRepository;
    LLMRepository llmRepo;

    //Maps model name to ChatModel
    Map<String, ChatModel> openaiModels = new HashMap<>();
    Map<String, ChatModel> ollamaModels = new HashMap<>();

    //Maps game id to interval in seconds for game-specific intervals
    private final Map<Integer, Integer> llmActionInterval = new HashMap<>();

    /*
        Maps game ids to the models that should be used by their LLM players.
        If the key is present but the value is null, a thread is still running, but will finish after the current
            iteration (and remove the key-value pair)
        If the key is not present, no thread is running at all.

        For melee games, attacking and defending can be done using different models, or one side can be disabled.
        The melee player is considered active as long as not both these maps map the game id to null.
     */
    private final Map<Integer, LLModel> activeLlmDefenders;
    private final Map<Integer, LLModel> activeLlmAttackers;


    @Inject
    public LlmService(Configuration config,
                      GameRepository gameRepository,
                      MutantRepository mutantRepository,
                      GameManagingUtils gameManagingUtils,
                      RequestContextController requestContextController,
                      GameService gameService,
                      LLMRepository llmRepo) {
        this.config = config;
        this.gameRepository = gameRepository;
        this.gameManagingUtils = gameManagingUtils;
        this.gameService = gameService;
        this.mutantRepository = mutantRepository;
        this.llmRepo = llmRepo;

        activeLlmDefenders = new HashMap<>();
        activeLlmAttackers = new HashMap<>();

        organizerExecutor = Executors.newSingleThreadScheduledExecutor();
        llmExecutor = Executors.newCachedThreadPool();

        List<LLModel> models = llmRepo.getAllModels();
        for (LLModel m : models) {
            if (m.getType() == LLMType.OPENAI) {
                openaiModels.put(m.getName(), OpenAiChatModel.builder()
                        .apiKey(config.getOpenaiApiKey())
                        .modelName(m.getName())
                        .build());
            }
            if (m.getType() == LLMType.OLLAMA) {
                ollamaModels.put(m.getName(), OllamaChatModel.builder()
                        .baseUrl(config.getLlmLocalServer())
                        .modelName(m.getName())
                        .temperature(0.9)
                        .build());
            }
        }
    }


    public String getResponse(LLModel model, String userMessage, String... systemMessages) {
        logger.info("Send message: \n {} to LLM with system messages:\n{}", userMessage,
                String.join("\n", systemMessages));
        ChatMessage[] chatMessages = new ChatMessage[systemMessages.length + 1];
        for (int i = 0; i < systemMessages.length; i++) {
            chatMessages[i] = SystemMessage.from(systemMessages[i]);
        }
        chatMessages[chatMessages.length - 1] = UserMessage.from(userMessage);

        Map<String, ChatModel> chatMap = switch (model.getType()) {
            case OPENAI -> openaiModels;
            case OLLAMA -> ollamaModels;
            default -> throw new IllegalArgumentException("Unsupported model type: " + model.getType());
        };
        ChatModel chatModel = chatMap.get(model.getName());
        if (chatModel != null) {
            ChatResponse response = chatModel.chat(chatMessages);
            String responseText = response.aiMessage().text();
            logger.info("LLM responded with {}", responseText);
            return responseText;
        } else {
            throw new IllegalArgumentException("No model with this name in ChatModelMap: " + model.getName());
        }
    }

    private Map<Integer, LLModel> getCorrectMap(Role r) {
        return switch (r) {
            case ATTACKER -> activeLlmAttackers;
            case DEFENDER -> activeLlmDefenders;
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
        if (role == Role.PLAYER) {
            return getModelForGame(game, Role.DEFENDER) != null || getModelForGame(game, Role.ATTACKER) != null;
        } else {
            return getModelForGame(game, role) != null;
        }
    }

    /**
     * Returns the minimum number of seconds between two actions of the same llm thread.
     * Returns the game-specific time, if it exists, otherwise returns the default time.
     */
    public int getLlmActionInterval(AbstractGame game) {
        Integer result = llmActionInterval.get(game.getId());
        if (result == null) {
            return AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.LLM_INTERVAL_SECONDS)
                    .getIntValue();
        } else {
            return result;
        }
    }

    /**
     * This only checks if values for this game and role have been added, they might be zero.
     * This way it can be avoided to add multiple threads for melee players.
     */
    public boolean isLlmPlayerPresent(AbstractGame game, Role role) {
        if (game instanceof MultiplayerGame) {
            return getCorrectMap(role).containsKey(game.getId());
        } else {
            return activeLlmDefenders.containsKey(game.getId()) || activeLlmAttackers.containsKey(game.getId());
        }
    }

    public LLModel getModelForGame(AbstractGame game, Role role) {
        return getCorrectMap(role).get(game.getId());
    }

    public void setPlayerModel(AbstractGame game, Role role, LLModel model) {
        Map<Integer, LLModel> m = getCorrectMap(role);
        boolean alreadyPresent = isLlmPlayerPresent(game, role);//m.containsKey(game.getId());

        if (model != null || alreadyPresent) { //Never put a new 'null' value, it wouldn't be deleted
            m.put(game.getId(), model);
        }

        if (model != null && !alreadyPresent) {
            if (game instanceof MeleeGame) {
                role = Role.PLAYER;
            }

            game.addPlayer(getCorrectUserId(role), role);
            final Role finalRole = role;

            organizerExecutor.execute(() -> llmExecutor.execute(() -> runLlmAction(game, finalRole, new Random())));
        }
    }

    public void finishPlayer(AbstractGame game, Role role) {
        finishPlayer(game.getId(), role);
    }

    public void finishPlayer(int gameId, Role role) {
        if (role == Role.PLAYER) {
            activeLlmAttackers.remove(gameId);
            activeLlmDefenders.remove(gameId);
        } else {
            getCorrectMap(role).remove(gameId);
        }
    }

    private String generateTest(AbstractGame game, SimpleUser user) throws NoSuchModelException {
        final LLModel model = activeLlmDefenders.get(game.getId());
        if (model == null) {
            return null;
        }
        llmRepo.loadModel(model);
        if (!model.isActive()) {
            throw new NoSuchModelException(model.getType(), model.getName());
        }
        LLModel defaultModel = llmRepo.getDefaultModel().orElseThrow();
        String systemMessage = model.getDefenderPrompt().orElse(defaultModel.getDefenderPrompt().orElseThrow());

        StringBuilder userMessage = new StringBuilder(game.getCUT().getSourceCode());
        if (model.isDefenderDependencies()) {
            List<String> dependencyCode = game.getCUT().getDependencyCode();
            if (!dependencyCode.isEmpty()) {
                systemMessage = model.getDefenderDependencyPrompt().
                        orElse(defaultModel.getDefenderDependencyPrompt().orElseThrow());
                for (String d : game.getCUT().getDependencyCode()) {
                    userMessage.append(d);
                }
            }
        }

        if (model.isDefenderMethodFocus()) {
            Optional<MethodDescription> methodDescription = getRandomMethodWithLivingMutant(game, user);
            if (methodDescription.isPresent()) { //TODO Mutants outside of methods
                systemMessage = String.format(model.getDefenderMethodFocusPrompt().
                        orElse(defaultModel.getDefenderMethodFocusPrompt().orElseThrow()), methodDescription.get());
            }
        }

        String result = getResponse(model, userMessage.toString(), systemMessage);
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
                gameManagingUtils.createBattlegroundTest(game, Constants.AI_PLAYER_USER_ID, testSrc);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            requestContextController.deactivate();
        }
    }

    @PreDestroy
    public void preDestroy() {
        organizerExecutor.shutdownNow();
        llmExecutor.shutdownNow();
    }

    private String generateMutant(AbstractGame game, SimpleUser user) throws NoSuchModelException {
        StringBuilder userMessage = new StringBuilder(game.getCUT().getSourceCode());

        LLModel model = activeLlmAttackers.get(game.getId());
        if (model == null) {
            return null;
        }
        LLModel defaultModel = llmRepo.getDefaultModel().orElseThrow();
        llmRepo.loadModel(model);
        if (!model.isActive()) {
            throw new NoSuchModelException(model.getType(), model.getName());
        }
        String systemMessage = model.getAttackerPrompt().orElse(defaultModel.getAttackerPrompt().orElseThrow());

        String firstDependencyName = null;//TODO Gibt's hierfür bessere Möglichkeiten? Gefahr,
        if (model.isAttackerDependencies()) {
            // wenn CUT und Dependency gleichen Namen haben?
            List<String> dependencies = game.getCUT().getDependencyCode();
            if (!dependencies.isEmpty()) {
                systemMessage = model.getAttackerDependencyPrompt().
                        orElse(defaultModel.getAttackerDependencyPrompt().orElseThrow());
                for (String d : game.getCUT().getDependencyCode()) {
                    userMessage.append(System.lineSeparator()).append(d);
                    if (firstDependencyName == null) {
                        firstDependencyName = game.getCUT().getDependencyNames().get(0);
                    }
                }
            }
        }

        //TODO Method Focus

        String result = getResponse(model, userMessage.toString(), systemMessage);
        String formattedResult = result.replace("```java", "").replace("```", "");
        if (firstDependencyName != null) {
            int classDeclaration = formattedResult.indexOf("class " + firstDependencyName);
            if (classDeclaration > 0) {
                formattedResult = formattedResult.substring(0, classDeclaration);
                int lastNewline = formattedResult.lastIndexOf(System.lineSeparator());
                formattedResult = formattedResult.substring(0, lastNewline);
            }
        }
        logger.info("LLM attacker generated mutant: {}", formattedResult);
        return formattedResult;
    }

    private void submitMutant(AbstractGame game, String mutantSrc) {
        RequestContextController requestContextController = CDIUtil.getBeanFromCDI(RequestContextController.class);
        requestContextController.activate();
        try {
            if (game instanceof MultiplayerGame multiplayerGame) {
                gameManagingUtils.createBattlegroundMutant(multiplayerGame, Constants.AI_ATTACKER_USER_ID, mutantSrc);
            } else if (game instanceof MeleeGame meleeGame) {
                gameManagingUtils.createMeleeMutant(meleeGame, Constants.AI_PLAYER_USER_ID, mutantSrc);
            } else {
                throw new RuntimeException("No LLMs in Puzzles allowed!");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (GameManagingUtils.MutantCreationException e) {
            logger.warn("Could not submit mutant. Reason: {} \nFull mutant text:\n{}",
                    e.getDetailedReason().orElse("Unknown"), mutantSrc);
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

    /**
     * Stop all llm players with that model.
     */
    public void closeModel(LLModel model) {
        closeModel(model, Role.ATTACKER);
        closeModel(model, Role.DEFENDER);
    }

    private void closeModel(LLModel model, Role role) {
        new HashSet<>(getCorrectMap(role).entrySet()).stream()
                .filter(entry -> model.equals(entry.getValue()))
                .mapToInt(Map.Entry::getKey)
                .forEach(gameId -> finishPlayer(gameId, role));
    }

    private Optional<MethodDescription> getRandomMethodWithLivingMutant(AbstractGame game, SimpleUser user) {
        List<MethodDescription> methods = getMethodsWithLivingMutants(game, user);
        if (!methods.isEmpty()) {
            return Optional.of(methods.get((int) (Math.random() * methods.size())));
        } else {
            return Optional.empty();
        }
    }

    public void runLlmAction(AbstractGame game, final Role role, final Random random) {

        int userId = switch (role) {
            case ATTACKER -> Constants.AI_ATTACKER_USER_ID;
            case DEFENDER -> Constants.AI_DEFENDER_USER_ID;
            case PLAYER -> Constants.AI_PLAYER_USER_ID;
            default -> throw new IllegalArgumentException("No such role allowed for LLM: " + role);
        };
        final SimpleUser user = new SimpleUser(userId, "PLACEHOLDER");

        logger.info("Starting LlmPlayerThread for game {} with role {}", game.getId(), role);
        long timeToStartNextThread = getLlmActionInterval(game) * 1000L + System.currentTimeMillis();
        if (isLlmPlayerActive(game, role) && gameRepository.isGameActive(game.getId())) {
            try {
                if (role == Role.DEFENDER) {
                    String testSrc = generateTest(game, user);
                    game = gameRepository.getGame(game.getId()); //Refresh game data before submitting
                    submitTest(game, testSrc);
                } else if (role == Role.ATTACKER) {
                    String mutantSrc = generateMutant(game, user);
                    game = gameRepository.getGame(game.getId());
                    submitMutant(game, mutantSrc);
                } else {
                    boolean attackAvailable = activeLlmAttackers.get(game.getId()) != null;
                    boolean defendAvailable = activeLlmDefenders.get(game.getId()) != null;
                    if (!attackAvailable && !defendAvailable) {
                        finishPlayer(game, role);
                        return;
                    }
                    boolean attack = attackAvailable && !defendAvailable || attackAvailable && random.nextBoolean();

                    if (attack) {
                        String mutantSrc = generateMutant(game, user);
                        game = gameRepository.getGame(game.getId());
                        submitMutant(game, mutantSrc);
                    } else {
                        String testSrc = generateTest(game, user);
                        game = gameRepository.getGame(game.getId()); //Refresh game data before submitting
                        submitTest(game, testSrc);
                    }
                }
                long timeToWait = Math.max(0, timeToStartNextThread - System.currentTimeMillis());
                final AbstractGame finalGame = game;

                organizerExecutor.schedule(() -> llmExecutor.execute(() -> runLlmAction(finalGame, role, random)),
                        timeToWait, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                logger.error("AiPlayerThread for game {} with role {} timed out.",
                        game.getId(), role);
            } catch (NoSuchModelException e) {
                logger.error("The model is no longer active, llm player thread has been aborted.");
            }
        } else {
            finishPlayer(game, role);
        }
    }

    public static class NoSuchModelException extends Exception {
        LLMType type;
        String name;

        public NoSuchModelException(LLMType type, String name) {
            super("No such model: type: " + type + ", name: " + name);
            this.type = type;
            this.name = name;
        }
    }
}
