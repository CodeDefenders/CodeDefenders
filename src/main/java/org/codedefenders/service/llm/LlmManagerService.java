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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.RequestContextController;
import jakarta.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.codedefenders.database.AdminDAO;
import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.llm.LlModel;
import org.codedefenders.model.llm.LlmConversationBatch;
import org.codedefenders.model.llm.LlmDefaultStrategy;
import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.service.UserService;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.util.CDIUtil;
import org.codedefenders.util.Constants;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.exception.TimeoutException;

import static org.codedefenders.model.llm.LlmDefaultStrategy.EQUIVALENCE_DEFAULT;
import static org.codedefenders.model.llm.LlmDefaultStrategy.MUTANT_ANNOTATED_SINGLE_METHOD;
import static org.codedefenders.model.llm.LlmDefaultStrategy.TEST_FULL_SUITE_PLUS_DEFAULT;

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
    Maps game ids to the models that should be used by their LLM players.
    If the key is present but the value is null, a thread is still running, but will finish after the current
        iteration (and remove the key-value pair)
    If the key is not present, no thread is running at all.

    For melee games, attacking and defending can be done using different models, or one side can be disabled.
    The melee player is considered active as long as not both these maps map the game id to null.
 */
    private final GameLlmState activeLlmDefenders = new GameLlmState();
    private final GameLlmState activeLlmAttackers = new GameLlmState();

    /*
     * Maps game ids to the last error message of failed llm actions.
     */
    private final Map<Integer, String> defenderErrorMessages = new HashMap<>();
    private final Map<Integer, String> attackerErrorMessages = new HashMap<>();

    /*
        Only for the thesis. Lists all gameIds that are in equivalent-only-mode
     */
    private final List<Integer> equivalentOnlyGames = new ArrayList<>();

    @Inject
    private GameRepository gameRepository;

    @Inject
    private MutantRepository mutantRepo;

    @Inject
    private UserService userService;

    @Inject
    private GameService gameService;

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
        return getModelForGame(game.getId(), role);
    }

    public Optional<LlModel> getModelForGame(int gameId, Role role) {
        return Optional.ofNullable(getCorrectMap(role).getModel(gameId));
    }

    public Optional<LlmStrategy> getStrategyForGame(int gameId, Role role) {
        return Optional.ofNullable(getCorrectMap(role).getStrategy(gameId));
    }

    public void setEquivalentOnly(int gameId) {
        if (activeLlmAttackers.getState(gameId) != ThreadState.INACTIVE) {
            equivalentOnlyGames.add(gameId);
        }
        if (activeLlmDefenders.getState(gameId) != ThreadState.INACTIVE) {
            finishPlayer(gameId, Role.DEFENDER);
        }
    }


    private GameLlmState getCorrectMap(Role r) {
        return switch (r) {
            case ATTACKER -> activeLlmAttackers;
            case DEFENDER -> activeLlmDefenders;
            default -> throw new IllegalArgumentException("Illegal role: " + r);
        };
    }

    private Map<Integer, String> getCorrectErrorMap(Role r) {
        return switch (r) {
            case ATTACKER, PLAYER -> attackerErrorMessages;
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
        return (AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.LLM_INTERVAL_SECONDS)
                .getIntValue());
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
            return getCorrectMap(role).getState(game.getId()) != ThreadState.INACTIVE;
        } else {
            return activeLlmDefenders.getState(game.getId()) != ThreadState.INACTIVE
                    || activeLlmAttackers.getState(game.getId()) != ThreadState.INACTIVE;
        }
    }

    public void setPlayerModel(AbstractGame game, Role role,
                               LlModel defendModel,
                               LlModel attackModel,
                               LlmStrategy defendStrategy,
                               LlmStrategy attackStrategy) {
        boolean alreadyPresent = isLlmPlayerPresent(game, role);

        LlmStrategy equivalenceStrategy = LlmStrategy.of(EQUIVALENCE_DEFAULT); //TODO Adjustable

        //TODO No default strategies, fail with an error if strategies are null when they shouldn't be

        if ((role == Role.DEFENDER || role == Role.PLAYER)
                && (defendModel != null || activeLlmDefenders.getState(game.getId()) != ThreadState.INACTIVE)) { //Never put a new 'null' value, it wouldn't be deleted
            activeLlmDefenders.put(game.getId(), defendModel, defendStrategy);
        }
        if ((role == Role.ATTACKER || role == Role.PLAYER)
                && (attackModel != null || activeLlmAttackers.getState(game.getId()) != ThreadState.INACTIVE)) { //Never put a new 'null' value, it wouldn't be deleted
            activeLlmAttackers.put(game.getId(), attackModel, attackStrategy);
        }

        if ((attackModel != null || defendModel != null) && !alreadyPresent) {


            //Role userRole = game instanceof MeleeGame ? Role.PLAYER : role;

            int userId = getCorrectUserId(role);
            SimpleUser user = userService.getSimpleUserById(userId).orElseThrow();
            game.addPlayer(userId, role);

            organizerExecutor.execute(() -> llmExecutor.execute(() -> runLlmAction(game, role,
                    new LlmConversationBatch(game, user, defendStrategy),
                    new LlmConversationBatch(game, user, attackStrategy),
                    new Random())));
        }
    }

    public void finishPlayer(int gameId, Role role) {
        if (role == Role.PLAYER) {
            activeLlmAttackers.completeFinish(gameId);
            activeLlmDefenders.completeFinish(gameId);
        } else {
            getCorrectMap(role).completeFinish(gameId);
        }
    }

    /**
     * Stop all llm players with that model.
     */
    public void closeModel(@NotNull LlModel model) {
        activeLlmAttackers.closeModel(model);
        activeLlmDefenders.closeModel(model);
    }

    private void addErrorMessage(AbstractGame game, Role role, Exception e) {
        logger.error("LLM thread had an error: ", e);
        String timestamp = LocalDateTime.now().toString();
        getCorrectErrorMap(role).put(game.getId(), timestamp + ": " + e.toString());
    }

    public Optional<String> getErrorMessage(int gameId, Role role) {
        return Optional.ofNullable(getCorrectErrorMap(role).get(gameId));
    }

    public Optional<String> getErrorMessage(AbstractGame game, Role role) {
        return getErrorMessage(game.getId(), role);
    }

    /**
     * This is supposed to run in a separate thread created by {@link LlmManagerService#llmExecutor}.
     * It only runs for a single action, i.e. one mutant or one test, and then schedules another execution of itself
     * in the future. If the conditions for running are no longer met, because the game doesn't exist anymore or
     * the model has been deactivated, it terminates itself.
     *
     * @param role May be {@link Role#ATTACKER}, {@link Role#DEFENDER} or {@link Role#PLAYER}
     */
    private void runLlmAction(AbstractGame game, final Role role,
                              final LlmConversationBatch defendConversation,
                              final LlmConversationBatch attackConversation,
                              final Random random) {
        logger.info("Running llmAction for game {} with role {}", game.getId(), role);
        final int gameId = game.getId();

        LlmStrategy equivalenceStrategy = LlmStrategy.of(LlmDefaultStrategy.EQUIVALENCE_DEFAULT);
        LlmStrategy testStrategy = activeLlmDefenders.getStrategy(gameId);
        LlmStrategy mutantStrategy = activeLlmAttackers.getStrategy(gameId);

        LlmStrategy timeModStrategy = role == Role.DEFENDER ? testStrategy : mutantStrategy;
        double timeModifier = timeModStrategy != null ? timeModStrategy.getTimeModifier() : 1;

        //TODO For melee games, put this after attack/defend has been decided
        long timeToStartNextThread = (int) (getLlmActionInterval() * timeModifier) * 1000L + System.currentTimeMillis();

        if (equivalentOnlyGames.contains(game.getId())) {
            game.getAliveMutants().stream()
                    .filter(m -> m.getCreatorId() == Constants.AI_ATTACKER_USER_ID)
                    .forEach(m -> {
                        m.setEquivalent(Mutant.Equivalence.PENDING_TEST);
                        mutantRepo.updateMutant(m);
                        mutantRepo.insertEquivalence(m, Constants.DUMMY_CREATOR_USER_ID);
                    });
        }

        int userId = switch (role) {
            case ATTACKER -> Constants.AI_ATTACKER_USER_ID;
            case DEFENDER -> Constants.AI_DEFENDER_USER_ID;
            case PLAYER -> Constants.AI_PLAYER_USER_ID;
            default -> throw new IllegalArgumentException("No such role allowed for LLM: " + role);
        };
        final SimpleUser user = new SimpleUser(userId, "PLACEHOLDER");

        if (isLlmPlayerActive(game, role)) {
            try {
                singleLlmAction(game, user, role, defendConversation, attackConversation,
                        testStrategy, mutantStrategy, equivalenceStrategy, random);
                long timeToWait;
                if (equivalentOnlyGames.contains(game.getId())) {
                    timeToWait = 0;
                } else {
                    timeToWait = Math.max(0, timeToStartNextThread - System.currentTimeMillis());
                }

                organizerExecutor.schedule(() -> llmExecutor.execute(
                                () -> runLlmAction(
                                        gameRepository.getGame(game.getId()), role, defendConversation,
                                        attackConversation, random)),
                        timeToWait, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                logger.error("AiPlayerThread for game {} with role {} timed out.",
                        game.getId(), role);
                addErrorMessage(game, role, e);
            } catch (NoSuchModelException e) {
                logger.error("The model is no longer active, llm player thread will be aborted.");
                addErrorMessage(game, role, e);
                finishPlayer(game.getId(), role);
            } catch (Exception e) {
                logger.error("""
                                LLM player thread in game {} and role {} threw an exception:
                                {}
                                The thread will be terminated.""",
                        game.getId(), role, e.toString());
                addErrorMessage(game, role, e);
                finishPlayer(game.getId(), role);
            }
        } else if (gameRepository.isGameCreated(game.getId())) {
            long timeToWait = Math.max(0, timeToStartNextThread - System.currentTimeMillis());
            organizerExecutor.schedule(() -> llmExecutor.execute(
                            () -> runLlmAction(
                                    gameRepository.getGame(game.getId()), role, defendConversation,
                                    attackConversation, random)),
                    //testStrategy, mutantStrategy, equivalenceStrategy, random)),
                    timeToWait, TimeUnit.MILLISECONDS);
        } else {
            finishPlayer(game.getId(), role);
        }
    }

    private void singleLlmAction(AbstractGame game, final SimpleUser user, final Role role,
                                 final LlmConversationBatch defendConversation,
                                 final LlmConversationBatch attackConversation,
                                 LlmStrategy testStrategy, LlmStrategy mutantStrategy, LlmStrategy equivalenceStrategy,
                                 final Random random) throws NoSuchModelException {
        if (testStrategy == null) {
            testStrategy = LlmStrategy.of(TEST_FULL_SUITE_PLUS_DEFAULT); //TODO Define default strategies elsewhere
        }
        if (mutantStrategy == null) {
            mutantStrategy = LlmStrategy.of(LlmDefaultStrategy.MUTANT_DEFAULT);
        }
        if (equivalenceStrategy == null) {
            equivalenceStrategy = LlmStrategy.of(LlmDefaultStrategy.EQUIVALENCE_DEFAULT);
        }

        RequestContextController requestContextController = CDIUtil.getBeanFromCDI(RequestContextController.class);
        requestContextController.activate();

        try {
            //int normalNumberOfTries = strategy.getNormalNumberOfTries();
            //int equivalenceNumberOfTries = strategy.getEquivalenceNumberOfTries();

            LlmEquivalenceService equivalenceService;
            LlmMutantService mutantService;
            LlmTestService testService;

            equivalenceService = (LlmEquivalenceService) CDIUtil.getBeanFromCDI(equivalenceStrategy.getService());
            mutantService = (LlmMutantService) CDIUtil.getBeanFromCDI(mutantStrategy.getService());
            testService = (LlmTestService) CDIUtil.getBeanFromCDI(testStrategy.getService());

            Optional<LlModel> attackModel = getModelForGame(game, Role.ATTACKER);
            Optional<LlModel> defendModel = getModelForGame(game, Role.DEFENDER);

            equivalenceService.init(game, user, attackModel, attackConversation, random);
            mutantService.init(game, user, attackModel, attackConversation, random);
            testService.init(game, user, defendModel, defendConversation, random);

            if (role == Role.DEFENDER) {
                testService.claimEquivalent();
                testService.run(testStrategy);
            } else {
                equivalenceService.run(equivalenceStrategy);
                if (equivalentOnlyGames.contains(game.getId()) && gameService.getFlaggedMutants(user, game).isEmpty()) {
                    equivalentOnlyGames.remove((Integer) game.getId());
                    finishPlayer(game.getId(), role);
                    return;
                }
                if (role == Role.ATTACKER) {
                    if (!equivalentOnlyGames.contains(game.getId())) {
                        mutantService.run(mutantStrategy);
                    }
                } else {
                    boolean attackAvailable = activeLlmAttackers.getState(game.getId()) == ThreadState.ACTIVE;
                    boolean defendAvailable = activeLlmDefenders.getState(game.getId()) == ThreadState.ACTIVE;
                    if (!attackAvailable && !defendAvailable) {
                        finishPlayer(game.getId(), role);
                        return;
                    }
                    boolean attack = attackAvailable && !defendAvailable || attackAvailable && random.nextBoolean();

                    mutantService.claimEquivalent();
                    if (attack) {
                        mutantService.run(mutantStrategy);
                    } else {
                        testService.run(testStrategy);
                    }
                }
            }
        } finally {
            requestContextController.deactivate();
        }
    }

    private static class GameLlmState {
        private final Map<Integer, ImmutablePair<LlModel, LlmStrategy>> map = new HashMap<>();

        private void put(int gameId, LlModel model, LlmStrategy strategy) {
            map.put(gameId, ImmutablePair.of(model, strategy));
        }

        private void setModel(int gameId, LlModel model) {
            if (map.containsKey(gameId)) {
                LlmStrategy strategy = map.get(gameId).right;
                put(gameId, model, strategy);
            } else {
                throw new IllegalStateException("No game with id " + gameId + " in this map.");
            }
        }

        private void setStrategy(int gameId, LlmStrategy strategy) {
            if (map.containsKey(gameId)) {
                LlModel model = map.get(gameId).left;
                put(gameId, model, strategy);
            } else {
                throw new IllegalStateException("No game with id " + gameId + " in this map.");
            }
        }

        private LlModel getModel(int gameId) {
            if (map.get(gameId) == null) {
                return null;
            }
            return map.get(gameId).left;
        }

        private LlmStrategy getStrategy(int gameId) {
            if (map.get(gameId) == null) {
                return null;
            }
            return map.get(gameId).right;
        }

        private ThreadState getState(int gameId) {
            if (map.containsKey(gameId)) {
                if (map.get(gameId) != null) {
                    return ThreadState.ACTIVE;
                } else {
                    return ThreadState.FINISHING;
                }
            } else {
                return ThreadState.INACTIVE;
            }
        }

        private void completeFinish(int gameId) {
            map.remove(gameId);
        }

        private void closeModel(LlModel model) {
            Set<Integer> toRemove = new HashSet<>();
            for (var x : map.entrySet()) {
                if (x.getValue().left.equals(model)) {
                    toRemove.add(x.getKey());
                }
            }
            for (int k : toRemove) {
                map.remove(k);
            }
        }
    }

    private enum ThreadState {
        ACTIVE, FINISHING, INACTIVE
    }
}
