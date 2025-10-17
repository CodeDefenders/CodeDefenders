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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.RequestContextController;
import jakarta.inject.Inject;

import org.codedefenders.database.AdminDAO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.llm.LlModel;
import org.codedefenders.model.llm.LlmConversation;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.util.Constants;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.exception.TimeoutException;

/**
 * This class manages actions of llm players. All information about which llm players are activated in which games
 * are stored in fields of this class. It is a conscious decision not to store this information persistently, so that
 * potentially costly llm players are not unknowingly reawakened after restarting the server.
 * <p>
 * The detailed proceedings of an LLM action are managed by {@link LlmSubActionService} and its subclasses. This
 * class only establishes the general structure of an LLM action and handles their scheduling, activation, error
 * handling and deactivation.
 */
@ApplicationScoped
public class LlmManagerService {
    private static final Logger logger = LoggerFactory.getLogger(LlmManagerService.class);

    private final ExecutorService llmExecutor;
    private final ScheduledExecutorService organizerExecutor;

    /*
    Maps games to the models that should be used by their LLM players.
    If the key is present but the value is null, a thread is still running, but will finish after the current
        iteration (and remove the key-value pair)
    If the key is not present, no thread is running at all.

    For melee games, attacking and defending can be done using different models, or one side can be disabled.
    The melee player is considered active as long as not both these maps map the game id to null.
 */
    private final Map<AbstractGame, LlModel> activeLlmDefenders = new HashMap<>();
    private final Map<AbstractGame, LlModel> activeLlmAttackers = new HashMap<>();

    /*
     * Maps games to the last error message of failed llm actions.
     */
    private final Map<AbstractGame, String> defenderErrorMessages = new HashMap<>();
    private final Map<AbstractGame, String> attackerErrorMessages = new HashMap<>();


    @Inject
    private GameRepository gameRepository;

    @Inject
    public LlmManagerService() {
        organizerExecutor = Executors.newSingleThreadScheduledExecutor();
        llmExecutor = Executors.newCachedThreadPool();
    }


    /**
     * Returns the model currently active for a specific game and role, or an empty Optional if there is no
     * active model.
     */
    public Optional<LlModel> getModelForGame(AbstractGame game, Role role) {
        return Optional.ofNullable(getCorrectMap(role).get(game));
    }


    private Map<AbstractGame, LlModel> getCorrectMap(Role r) {
        return switch (r) {
            case ATTACKER -> activeLlmAttackers;
            case DEFENDER -> activeLlmDefenders;
            default -> throw new IllegalArgumentException("Illegal role: " + r);
        };
    }

    private Map<AbstractGame, String> getCorrectErrorMap(Role r) {
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

    /**
     * Returns the minimum number of seconds between two actions of the same llm thread.
     */
    private int getLlmActionInterval() {
        return AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.LLM_INTERVAL_SECONDS)
                .getIntValue();
    }

    private boolean isLlmPlayerActive(AbstractGame game, Role role) {
        if (!gameRepository.isGameActive(game.getId())) {
            return false;
        }
        if (role == Role.PLAYER) {
            return getModelForGame(game, Role.DEFENDER).isPresent() || getModelForGame(game, Role.ATTACKER).isPresent();
        } else {
            return getModelForGame(game, role).isPresent();
        }
    }

    /**
     * This only checks if values for this game and role have been added, they might be zero.
     * This way it can be avoided to add multiple threads for melee players.
     */
    public boolean isLlmPlayerPresent(AbstractGame game, Role role) {
        if (game instanceof MultiplayerGame) {
            return getCorrectMap(role).containsKey(game);
        } else {
            return activeLlmDefenders.containsKey(game) || activeLlmAttackers.containsKey(game);
        }
    }

    public void setPlayerModel(AbstractGame game, Role role, LlModel model) {
        Map<AbstractGame, LlModel> m = getCorrectMap(role);
        boolean alreadyPresent = isLlmPlayerPresent(game, role);

        if (model != null || alreadyPresent) { //Never put a new 'null' value, it wouldn't be deleted
            m.put(game, model);
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
        if (role == Role.PLAYER) {
            activeLlmAttackers.remove(game);
            activeLlmDefenders.remove(game);
        } else {
            getCorrectMap(role).remove(game);
        }
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
                .map(Map.Entry::getKey)
                .forEach(gameId -> finishPlayer(gameId, role));
    }

    private void addErrorMessage(AbstractGame game, Role role, Exception e) {
        String timestamp = LocalDateTime.now().toString();
        getCorrectErrorMap(role).put(game, timestamp + ": " + e.getMessage());
    }

    public Optional<String> getErrorMessage(AbstractGame game, Role role) {
        return Optional.ofNullable(getCorrectErrorMap(role).get(game));
    }

    /**
     * This is supposed to run in a separate thread created by {@link LlmManagerService#llmExecutor}.
     * It only runs for a single action, i.e. one mutant or one test, and then schedules another execution of itself
     * in the future. If the conditions for running are no longer met, because the game doesn't exist anymore or
     * the model has been deactivated, it terminates itself.
     */
    public void runLlmAction(AbstractGame game, final Role role, final LlmConversation conversation,
                             final Random random) {
        logger.info("Running llmAction for game {} with role {}", game.getId(), role);
        long timeToStartNextThread = getLlmActionInterval() * 1000L + System.currentTimeMillis();

        int userId = switch (role) {
            case ATTACKER -> Constants.AI_ATTACKER_USER_ID;
            case DEFENDER -> Constants.AI_DEFENDER_USER_ID;
            case PLAYER -> Constants.AI_PLAYER_USER_ID;
            default -> throw new IllegalArgumentException("No such role allowed for LLM: " + role);
        };
        final SimpleUser user = new SimpleUser(userId, "PLACEHOLDER");

        if (isLlmPlayerActive(game, role)) {
            try {
                singleLlmAction(game, user, role, conversation, random);
                long timeToWait = Math.max(0, timeToStartNextThread - System.currentTimeMillis());
                organizerExecutor.schedule(() -> llmExecutor.execute(
                                () -> runLlmAction(gameRepository.getGame(game.getId()), role, conversation, random)),
                        timeToWait, TimeUnit.MILLISECONDS);
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
            }
        } else {
            finishPlayer(game, role);
        }
    }

    private void singleLlmAction(AbstractGame game, final SimpleUser user, final Role role,
                                 final LlmConversation conversation,
                                 final Random random) throws NoSuchModelException {
        RequestContextController requestContextController = CDIUtil.getBeanFromCDI(RequestContextController.class);
        requestContextController.activate();

        try {
            LlModel model = getModelForGame(game, role).orElseThrow();

            LlmEquivalenceService equivalenceService = CDIUtil.getBeanFromCDI(LlmEquivalenceService.class);
            LlmMutantService mutantService = CDIUtil.getBeanFromCDI(LlmMutantService.class);
            LlmTestService testService = CDIUtil.getBeanFromCDI(LlmTestService.class);

            equivalenceService.init(game, user, model, conversation, random);
            mutantService.init(game, user, model, conversation, random);
            testService.init(game, user, model, conversation, random);
            if (role == Role.DEFENDER) {
                testService.createTest();
            } else {
                equivalenceService.runEquivalenceTests();
                if (role == Role.ATTACKER) {
                    mutantService.createMutant();
                } else {
                    boolean attackAvailable = activeLlmAttackers.get(game) != null;
                    boolean defendAvailable = activeLlmDefenders.get(game) != null;
                    if (!attackAvailable && !defendAvailable) {
                        finishPlayer(game, role);
                        return;
                    }
                    boolean attack = attackAvailable && !defendAvailable || attackAvailable && random.nextBoolean();

                    if (attack) {
                        mutantService.createMutant();
                    } else {
                        testService.createTest();
                    }
                }
            }
        } finally {
            requestContextController.deactivate();
            conversation.resetCurrentType();
        }
    }
}
