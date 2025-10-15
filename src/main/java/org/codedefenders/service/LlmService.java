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
import java.time.LocalDateTime;
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
import org.codedefenders.model.llm.LlModel;
import org.codedefenders.model.llm.LlmConversation;
import org.codedefenders.model.llm.LlmType;
import org.codedefenders.model.llm.PromptType;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.LlmRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.servlets.games.GameManagingUtils;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.util.Constants;
import org.codedefenders.util.LlmUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.data.message.ChatMessage;
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

    private static final String OUTSIDE_OF_METHOD_DESCRIPTION = "(The code outside of methods)";
    private static final int EQUIVALENT_POINT_RESTRICTION = 10; //TODO Als system setting??

    Configuration config;
    GameRepository gameRepository;
    GameManagingUtils gameManagingUtils;
    GameService gameService;
    MutantRepository mutantRepository;
    LlmRepository llmRepo;

    //Maps model name to ChatModel
    Map<String, ChatModel> openaiModels = new HashMap<>();
    Map<String, ChatModel> ollamaModels = new HashMap<>();

    /*
        Maps game ids to the models that should be used by their LLM players.
        If the key is present but the value is null, a thread is still running, but will finish after the current
            iteration (and remove the key-value pair)
        If the key is not present, no thread is running at all.

        For melee games, attacking and defending can be done using different models, or one side can be disabled.
        The melee player is considered active as long as not both these maps map the game id to null.
     */
    private final Map<Integer, LlModel> activeLlmDefenders;
    private final Map<Integer, LlModel> activeLlmAttackers;

    /*
     * Maps game ids to the last error message of failed llm actions.
     */
    private final Map<Integer, String> defenderErrorMessages;
    private final Map<Integer, String> attackerErrorMessages;

    @Inject
    public LlmService(Configuration config,
                      GameRepository gameRepository,
                      MutantRepository mutantRepository,
                      GameManagingUtils gameManagingUtils,
                      GameService gameService,
                      LlmRepository llmRepo) {
        this.config = config;
        this.gameRepository = gameRepository;
        this.gameManagingUtils = gameManagingUtils;
        this.gameService = gameService;
        this.mutantRepository = mutantRepository;
        this.llmRepo = llmRepo;

        activeLlmDefenders = new HashMap<>();
        activeLlmAttackers = new HashMap<>();

        defenderErrorMessages = new HashMap<>();
        attackerErrorMessages = new HashMap<>();

        organizerExecutor = Executors.newSingleThreadScheduledExecutor();
        llmExecutor = Executors.newCachedThreadPool();

        List<LlModel> models = llmRepo.getAllModels();
        for (LlModel m : models) {
            if (m.getType() == LlmType.OPENAI) {
                openaiModels.put(m.getName(), OpenAiChatModel.builder()
                        .apiKey(config.getOpenaiApiKey())
                        .modelName(m.getName())
                        .build());
            }
            if (m.getType() == LlmType.OLLAMA) {
                ollamaModels.put(m.getName(), OllamaChatModel.builder()
                        .baseUrl(config.getLlmLocalServer())
                        .modelName(m.getName())
                        .temperature(0.9)
                        .build());
            }
        }
    }


    public String getResponse(LlModel model, LlmConversation conversation) {
        logger.info("Sending conversation \n{}\n to model {}.", conversation, model);
        ChatMessage[] chatMessages = conversation.toArray();

        Map<String, ChatModel> chatMap = switch (model.getType()) {
            case OPENAI -> openaiModels;
            case OLLAMA -> ollamaModels;
            default -> throw new IllegalArgumentException("Unsupported model type: " + model.getType());
        };
        ChatModel chatModel = chatMap.get(model.getName());
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

    private Map<Integer, LlModel> getCorrectMap(Role r) {
        return switch (r) {
            case ATTACKER -> activeLlmAttackers;
            case DEFENDER -> activeLlmDefenders;
            default -> throw new IllegalArgumentException("Illegal role: " + r);
        };
    }

    private Map<Integer, String> getCorrectErrorMap(Role r) {
        return switch (r) {
            case ATTACKER -> attackerErrorMessages;
            case DEFENDER -> defenderErrorMessages;
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
            return getModelForGame(game, Role.DEFENDER).isPresent() || getModelForGame(game, Role.ATTACKER).isPresent();
        } else {
            return getModelForGame(game, role).isPresent();
        }
    }

    /**
     * Returns the minimum number of seconds between two actions of the same llm thread.
     */
    public int getLlmActionInterval() {
        return AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.LLM_INTERVAL_SECONDS)
                .getIntValue();
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

    /**
     * Returns the model currently active for a specific game, or an empty Optional if there is no active model.
     */
    public Optional<LlModel> getModelForGame(AbstractGame game, Role role) {
        return getModelForGame(game.getId(), role);
    }

    public Optional<LlModel> getModelForGame(int gameId, Role role) {
        return Optional.ofNullable(getCorrectMap(role).get(gameId));
    }

    public void setPlayerModel(AbstractGame game, Role role, LlModel model) {
        Map<Integer, LlModel> m = getCorrectMap(role);
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

            organizerExecutor.execute(() -> llmExecutor.execute(() -> runLlmAction(game, finalRole,
                    new LlmConversation(), new Random())));
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

    private String testTemplateFromResponse(String response, AbstractGame game) {
        response = LlmUtils.extractTestContentFromReply(response);
        return game.getCUT().getTestTemplate().replace(Constants.TEST_TEMPLATE_PLACEHOLDER, response);
    }

    private PromptType getCorrectDefendPromptType(LlModel model, AbstractGame game, SimpleUser user) {
        if (model.isDefenderMethodFocus() && hasLivingMutants(game, user)) {
            return PromptType.DEFEND_FOCUS;
        }
        if (model.isDefenderDependencies() && !game.getCUT().getDependencyNames().isEmpty()) {
            return PromptType.DEFEND_DEPENDENCIES;
        }
        return PromptType.DEFEND_DEFAULT;
    }

    private PromptType getCorrectAttackPromptType(LlModel model, AbstractGame game) {
        if (model.isAttackerDependencies() && !game.getCUT().getDependencyNames().isEmpty()) {
            return PromptType.ATTACK_DEPENDENCIES;
        } else {
            return PromptType.ATTACK_DEFAULT;
        }
    }

    /**
     * Returns the model's system prompt of this type, or the default prompt of this type if the model has no prompt
     * of its own. The model is updated before.
     */
    private String getSystemPrompt(LlModel model, PromptType type) throws NoSuchModelException {
        llmRepo.loadModel(model);
        Optional<String> specificPrompt = model.getPrompt(type);
        if (specificPrompt.isPresent()) {
            return specificPrompt.get();
        } else {
            LlModel defaultModel = llmRepo.getDefaultModel().orElseThrow();
            return defaultModel.getPrompt(type).orElseThrow();
        }
    }

    /**
     * Adds the source code of the CuT as the user message. If the prompt specifies that dependencies have to be
     * included, the source code of the dependencies is included as well.
     */
    private void addUserMessage(AbstractGame game, LlmConversation conversation) {
        if (conversation.getCurrentType() == PromptType.DEFEND_DEPENDENCIES
                || conversation.getCurrentType() == PromptType.ATTACK_DEPENDENCIES) {
            conversation.addUserMessage(game.getCUT().getSourceCode() + "\n" + String.join("\n", game.getCUT().getDependencyCode()));
        } else {
            conversation.addUserMessage(game.getCUT().getSourceCode());
        }

    }

    private String generateTest(AbstractGame game, SimpleUser user, LlmConversation conversation, Random random)
            throws NoSuchModelException {
        final LlModel model = activeLlmDefenders.get(game.getId());
        if (model == null) {
            return null;
        }
        if (!model.isActive()) {
            throw new NoSuchModelException(model.getType(), model.getName());
        }

        PromptType promptType = getCorrectDefendPromptType(model, game, user);
        conversation.setCurrentType(promptType);
        if (conversation.isEmpty()) {
            String systemMessage = getSystemPrompt(model, promptType);
            if (promptType == PromptType.DEFEND_FOCUS) {
                systemMessage = String.format(systemMessage, getRandomMethodWithLivingMutant(game, user, random));
            }
            conversation.addSystemMessage(systemMessage);

            addUserMessage(game, conversation);
        }

        String response = getResponse(model, conversation);
        String testSrc = testTemplateFromResponse(response, game);
        logger.info("AI defender generated test: {}", testSrc);
        return testSrc;
    }

    /**
     * This method submits the generated test code to the game. It should only be called from inside an LLM action,
     * after the test code has been generated and the game has been refreshed.
     *
     * @param game         The game to which the test is submitted.
     * @param testSrc      The formatted test code. All formatting heuristics should have already been performed.
     * @param conversation An existing conversation. Its {@link LlmConversation#getCurrentType()} must have already
     *                     been set by the generating method. If the submission is successful, the current conversation
     *                     will be cleared, if not, the conversation will be appended with additional messages
     *                     explaining the failure reasons.
     */
    private void submitTest(AbstractGame game, String testSrc, LlmConversation conversation) {
        RequestContextController requestContextController = CDIUtil.getBeanFromCDI(RequestContextController.class);
        requestContextController.activate();
        switch (conversation.getCurrentType()) {
            case DEFEND_DEFAULT, DEFEND_DEPENDENCIES, DEFEND_FOCUS -> {
            }
            default -> throw new RuntimeException("Conversation during test submission may not be of type " +
                    conversation.getCurrentType());
        }
        try {
            GameManagingUtils.CreateBattlegroundTestResult result;
            if (game instanceof MultiplayerGame multiplayerGame) {
                result = gameManagingUtils.createBattlegroundTest(multiplayerGame, Constants.AI_DEFENDER_USER_ID, testSrc);
            } else {
                result = gameManagingUtils.createBattlegroundTest(game, Constants.AI_PLAYER_USER_ID, testSrc);
            }
            if (result.isSuccess()) {
                conversation.clear();
            } else {
                StringBuilder correction = new StringBuilder();
                switch (result.failureReason().orElseThrow()) {
                    case VALIDATION_FAILED -> {
                        correction.append("Your test has violated these rules: \n");
                        result.validationErrorMessages().orElseThrow().forEach(
                                correction::append
                        );

                    }
                    case COMPILATION_FAILED ->
                            correction.append("Your test failed to compile for this reason: ").append(result.compilationError());
                    case TEST_DID_NOT_PASS_ON_CUT ->
                            correction.append("Your test did not pass on the original code for the following reason: ")
                                    .append(result.testCutError().orElseThrow());
                }
                correction.append("\nFix these problems.");
                conversation.addSystemMessage(correction.toString());
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

    private String generateMutant(AbstractGame game, LlmConversation conversation) throws NoSuchModelException {
        LlModel model = activeLlmAttackers.get(game.getId());

        PromptType promptType = getCorrectAttackPromptType(model, game);
        conversation.setCurrentType(promptType);
        if (conversation.isEmpty()) {
            conversation.addSystemMessage(getSystemPrompt(model, promptType));
            addUserMessage(game, conversation);
        }

        String result = getResponse(model, conversation);
        String formattedResult = LlmUtils.extractMutantFromReply(result, true, game);
        logger.info("LLM attacker generated mutant: {}", formattedResult);
        return formattedResult;
    }

    private void submitMutant(AbstractGame game, String mutantSrc, LlmConversation conversation) {
        RequestContextController requestContextController = CDIUtil.getBeanFromCDI(RequestContextController.class);
        requestContextController.activate();
        try {
            GameManagingUtils.CreateBattlegroundMutantResult result;
            if (game instanceof MultiplayerGame multiplayerGame) {
                result = gameManagingUtils.createBattlegroundMutant(multiplayerGame, Constants.AI_ATTACKER_USER_ID, mutantSrc);
            } else if (game instanceof MeleeGame meleeGame) {
                result = gameManagingUtils.createMeleeMutant(meleeGame, Constants.AI_PLAYER_USER_ID, mutantSrc);
            } else {
                throw new RuntimeException("No LLMs in Puzzles allowed!");
            }
            if (result.isSuccess()) {
                conversation.clear();
                logger.info("LLM successfully submitted mutant.");

            } else {
                conversation.addSystemMessage(
                        switch (result.failureReason().orElseThrow()) {
                            case VALIDATION_FAILED -> "Your mutant has violated the following rule: \n"
                                    + result.validationErrorMessage().orElseThrow();
                            case DUPLICATE_MUTANT_FOUND -> "Your mutant already exists. Create another one.";
                            case COMPILATION_FAILED -> "Your mutant failed to compile. Compilation error: "
                                    + result.compilationError().orElseThrow();
                        } + "\n Fix this.");
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
     * Generate a test that should kill a mutant that was flagged as equivalent. Waits until the generation by the
     * llm is complete.
     */
    private String generateEquivalenceTest(AbstractGame game, MutantDTO flagged, LlmConversation conversation)
            throws NoSuchModelException {
        LlModel model = activeLlmAttackers.get(game.getId());

        if (conversation.isEmpty()) {
            conversation.addSystemMessage(getSystemPrompt(model, PromptType.ATTACK_EQUIVALENCE));
            conversation.addUserMessage(game.getCUT().getSourceCode() + "\n" + flagged.getPatchString());
        }
        String response = getResponse(model, conversation);
        return testTemplateFromResponse(response, game);
    }

    private void submitEquivalenceTest(AbstractGame game, String testSource, MutantDTO equivalentMutantDTO, int userId,
                                       LlmConversation conversation) {
        if (conversation.getCurrentType() != PromptType.ATTACK_EQUIVALENCE) {
            logger.error("Conversation may not be of type {} in submitEquivalenceTest", conversation.getCurrentType());
            throw new RuntimeException("Conversation may not be of type " + conversation.getCurrentType()
                    + " in submitEquivalenceTest");
        }

        RequestContextController requestContextController = CDIUtil.getBeanFromCDI(RequestContextController.class);
        requestContextController.activate();
        Mutant equivalentMutant = mutantRepository.getMutantById(equivalentMutantDTO.getId());
        try {
            GameManagingUtils.RejectBattlegroundEquivalenceResult result =
                    gameManagingUtils.rejectBattlegroundEquivalence(game, userId, equivalentMutant, testSource);

            if (result.testValid()) { //TODO Duplicate code can be removed after refactoring Results and FailureReasons to common types
                conversation.clear();
            } else {
                StringBuilder correction = new StringBuilder();
                switch (result.failureReason().orElseThrow()) {
                    case VALIDATION_FAILED -> {
                        correction.append("Your test has violated these rules: \n");
                        result.validationErrorMessages().orElseThrow().forEach(
                                correction::append
                        );

                    }
                    case COMPILATION_FAILED ->
                            correction.append("Your test failed to compile for this reason: ").append(result.compilationError());
                    case TEST_DID_NOT_PASS_ON_CUT ->
                            correction.append("Your test did not pass on the original code for the following reason: ")
                                    .append(result.testCutError().orElseThrow());
                }
                correction.append("\nFix these problems.");
                conversation.addSystemMessage(correction.toString());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            requestContextController.deactivate();
        }
    }

    /**
     * Try to claim a random potentially equivalent mutant as equivalent, or do nothing if no such mutant is available.
     */
    private void claimEquivalent(SimpleUser user, AbstractGame game, Random random) {
        Optional<MutantDTO> potentialEquivalent = getRandomPossiblyEquivalentMutant(game, user, random);
        if (potentialEquivalent.isPresent()) {
            RequestContextController requestContextController = CDIUtil.getBeanFromCDI(RequestContextController.class);
            requestContextController.activate();
            try {
                logger.info("Claiming equivalence on mutant {}", potentialEquivalent.get());
                gameManagingUtils.claimBattlegroundEquivalence(game, user.getId(),
                        potentialEquivalent.get().getLines());
            } finally {
                requestContextController.deactivate();
            }
        }
    }

    /**
     * Return one random mutant from the set of all mutants that can be marked as equivalent by the user
     * and have acquired at least {@link LlmService#EQUIVALENT_POINT_RESTRICTION} points.
     * Returns an empty Optional if there is no mutant that can be marked as equivalent.
     */
    private Optional<MutantDTO> getRandomPossiblyEquivalentMutant(AbstractGame game, SimpleUser user, Random random) {
        List<MutantDTO> candidates = gameService.getMutants(user, game).stream()
                .filter(MutantDTO::isCanMarkEquivalent)
                .filter(m -> m.getPoints() >= EQUIVALENT_POINT_RESTRICTION)
                .filter(m -> !m.getCreator().equals(user))
                .toList();
        if (candidates.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(candidates.get(random.nextInt(candidates.size())));
        }
    }

    /**
     * Returns a list of all methods descriptions (as in {@link MethodDescription#getDescription()}) that contain a
     * living mutant that haven't been created by the user.
     * If one method contains several living mutants, it occurs several times in the list.
     * If a mutant exists outside a method, the String is {@link LlmService#OUTSIDE_OF_METHOD_DESCRIPTION}.
     */
    private List<String> getMethodsWithLivingMutants(AbstractGame game, SimpleUser user) {
        List<MutantDTO> mutants = gameService.getMutants(user, game);
        List<MethodDescription> methods = game.getCUT().getMethodDescriptions();
        GameAccordionMapping mapping = GameAccordionMapping.computeForMutants(methods, mutants);
        HashMap<MethodDescription, SortedSet<Integer>> map = mapping.elementsPerMethod;
        List<String> listOfPossibilities = new ArrayList<>();
        for (MethodDescription m : map.keySet()) {
            for (Integer mutantId : map.get(m)) {
                Mutant mutant = mutantRepository.getMutantById(mutantId);
                if (mutant.isAlive() && mutant.getCreatorId() != user.getId()) {
                    listOfPossibilities.add(m.getDescription());
                }
            }
        }
        mapping.elementsOutsideMethods.stream().
                map(mutantId -> mutantRepository.getMutantById(mutantId))
                .filter(mutant -> mutant.isAlive() && mutant.getCreatorId() != user.getId())
                .forEach(mutant -> listOfPossibilities.add(OUTSIDE_OF_METHOD_DESCRIPTION));
        return listOfPossibilities;
    }

    /**
     * Returns if the game has living mutants that are not created by the user.
     */
    private boolean hasLivingMutants(AbstractGame game, SimpleUser user) {
        return gameService.getMutants(user, game).stream().anyMatch(
                mutantDTO -> mutantDTO.getState() == Mutant.State.ALIVE);
    }

    /**
     * Stop all llm players with that model.
     */
    public void closeModel(@NotNull LlModel model) {
        closeModel(model, Role.ATTACKER);
        closeModel(model, Role.DEFENDER);
    }

    private void closeModel(LlModel model, Role role) {
        new HashSet<>(getCorrectMap(role).entrySet()).stream()
                .filter(entry -> model.equals(entry.getValue()))
                .mapToInt(Map.Entry::getKey)
                .forEach(gameId -> finishPlayer(gameId, role));
    }

    /**
     * Returns a random method that contains a living mutant that hasn't been created by the user. The more living
     * mutants there are in a method, the more likely it is to be selected.
     */
    private Optional<String> getRandomMethodWithLivingMutant(AbstractGame game, SimpleUser user, Random random) {
        List<String> methods = getMethodsWithLivingMutants(game, user);
        if (!methods.isEmpty()) {
            return Optional.of(methods.get(random.nextInt(methods.size())));
        } else {
            return Optional.empty();
        }
    }

    private void addErrorMessage(AbstractGame game, Role role, Exception e) {
        String timestamp = LocalDateTime.now().toString();
        getCorrectErrorMap(role).put(game.getId(), timestamp + ": " + e.getMessage());
    }

    public Optional<String> getErrorMessage(int gameId, Role role) {
        return Optional.ofNullable(getCorrectErrorMap(role).get(gameId));
    }

    /**
     * This is supposed to run in a separate thread created by {@link LlmService#llmExecutor}.
     * It only runs for a single action, i.e. one mutant or one test, and then schedules another execution of itself
     * in the future. If the conditions for running are no longer met, because the game doesn't exist anymore or
     * the model has been deactivated, it terminates itself.
     */
    public void runLlmAction(AbstractGame game, final Role role, final LlmConversation conversation,
                             final Random random) {

        int userId = switch (role) {
            case ATTACKER -> Constants.AI_ATTACKER_USER_ID;
            case DEFENDER -> Constants.AI_DEFENDER_USER_ID;
            case PLAYER -> Constants.AI_PLAYER_USER_ID;
            default -> throw new IllegalArgumentException("No such role allowed for LLM: " + role);
        };
        final SimpleUser user = new SimpleUser(userId, "PLACEHOLDER");

        logger.info("Starting LlmPlayerThread for game {} with role {}", game.getId(), role);
        long timeToStartNextThread = getLlmActionInterval() * 1000L + System.currentTimeMillis();
        if (isLlmPlayerActive(game, role) && gameRepository.isGameActive(game.getId())) {
            try {
                if (role == Role.DEFENDER) {
                    claimEquivalent(user, game, random);
                    String testSrc = generateTest(game, user, conversation, random);
                    game = gameRepository.getGame(game.getId()); //Refresh game data before submitting
                    submitTest(game, testSrc, conversation);
                } else {
                    for (MutantDTO flagged : gameService.getFlaggedMutants(user, game)) {
                        String killingTestSource = generateEquivalenceTest(game, flagged, conversation);
                        logger.info("LLM player with role {} in game {}" +
                                " submitted the following test in an equivalence duel: " +
                                "\n{}", role, game.getId(), killingTestSource);
                        submitEquivalenceTest(game, killingTestSource, flagged, userId, conversation);
                    }
                    if (role == Role.ATTACKER) {
                        String mutantSrc = generateMutant(game, conversation);
                        game = gameRepository.getGame(game.getId());
                        submitMutant(game, mutantSrc, conversation);
                    } else {
                        claimEquivalent(user, game, random);
                        boolean attackAvailable = activeLlmAttackers.get(game.getId()) != null;
                        boolean defendAvailable = activeLlmDefenders.get(game.getId()) != null;
                        if (!attackAvailable && !defendAvailable) {
                            finishPlayer(game, role);
                            return;
                        }
                        boolean attack = attackAvailable && !defendAvailable || attackAvailable && random.nextBoolean();

                        if (attack) { //TODO mistakes for mutants
                            String mutantSrc = generateMutant(game, conversation);
                            game = gameRepository.getGame(game.getId());
                            submitMutant(game, mutantSrc, conversation);
                        } else {
                            String testSrc = generateTest(game, user, conversation, random);
                            game = gameRepository.getGame(game.getId()); //Refresh game data before submitting
                            submitTest(game, testSrc, conversation);
                        }
                    }
                }
            } catch (TimeoutException e) {
                logger.error("AiPlayerThread for game {} with role {} timed out.",
                        game.getId(), role);
                addErrorMessage(game, role, e);
            } catch (NoSuchModelException e) {
                logger.error("The model is no longer active, llm player thread will be aborted.");
                addErrorMessage(game, role, e);
                finishPlayer(game, role);
            } catch (Exception e) {
                logger.error("""
                                LLM player thread in game {} and role {} threw an exception:
                                {}
                                The thread will be terminated.""",
                        game.getId(), role, e.getMessage());
                addErrorMessage(game, role, e);
                finishPlayer(game, role);
            } finally {
                long timeToWait = Math.max(0, timeToStartNextThread - System.currentTimeMillis());
                final AbstractGame finalGame = game;
                conversation.resetCurrentType();

                organizerExecutor.schedule(() -> llmExecutor.execute(
                                () -> runLlmAction(finalGame, role, conversation, random)),
                        timeToWait, TimeUnit.MILLISECONDS);
            }
        } else {
            finishPlayer(game, role);
        }
    }

    public static class NoSuchModelException extends Exception {
        LlmType type;
        String name;

        public NoSuchModelException(LlmType type, String name) {
            super("No such model: type: " + type + ", name: " + name);
            this.type = type;
            this.name = name;
        }
    }
}
