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
package org.codedefenders.llm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.llm.LlModel;
import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.service.game.GameService;
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
 *
 * <p>
 * The detailed proceedings of an LLM action are managed by {@link AbstractStrategy} and its subclasses. This
 * class only establishes the general structure of an LLM action and handles their scheduling, activation, error
 * handling and deactivation.
 */
@ApplicationScoped
public class LlmOrganizer {
    private static final Logger logger = LoggerFactory.getLogger(LlmOrganizer.class);

    private final ExecutorService llmExecutor;
    private final ScheduledExecutorService organizerExecutor;

    /*
    Maps game ids to the context information that should be used by their LLM players.

    For melee games, attacking and defending can be done using different models, or one side can be disabled.
    The melee player is considered active as long as not both these maps map the game id to null.
 */
    private final GameLlmState activeLlmDefenders = new GameLlmState();
    private final GameLlmState activeLlmAttackers = new GameLlmState();

    /*
        Only for experiments. Lists all gameIds that are in equivalent-only-mode
     */
    private final List<Integer> equivalentOnlyGames = new ArrayList<>();

    @Inject
    private GameRepository gameRepository;

    @Inject
    private MutantRepository mutantRepo;

    @Inject
    private GameService gameService;

    @Inject
    public LlmOrganizer() {
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
        LlmContext context = getContext(gameId, role);
        if (context == null) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(context.model());
        }
    }

    public Optional<LlmStrategy> getStrategyForGame(int gameId, Role role) {
        LlmContext context = getContext(gameId, role);
        if (context == null) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(context.strategy());
        }
    }

    /**
     * Used for llm-vs-llm experiments. When this happens, the LLM defender is deactivated, and all living
     * mutants are marked
     * as equivalent, giving the attacker the chance to defend itself.
     *
     * @param gameId The ID of the game to be set to equivalent-only.
     */
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

    private LlmContext getContext(int gameId, Role role) {
        return switch (role) {
            case ATTACKER -> activeLlmAttackers.getContext(gameId);
            case DEFENDER -> activeLlmDefenders.getContext(gameId);
            default -> throw new IllegalArgumentException("Illegal role: " + role);
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

    public void setPlayerModel(AbstractGame game, Role role,
                               LlModel defendModel,
                               LlModel attackModel,
                               LlmStrategy defendStrategy,
                               LlmStrategy attackStrategy) {

        final boolean attackerAlreadyPresent = activeLlmAttackers.getState(game.getId()) == ThreadState.ACTIVE;
        final boolean defenderAlreadyPresent = activeLlmDefenders.getState(game.getId()) == ThreadState.ACTIVE;

        if (role == Role.DEFENDER || role == Role.PLAYER) {
            activeLlmDefenders.put(game, defendModel, defendStrategy);
        }
        if (role == Role.ATTACKER || role == Role.PLAYER) {
            activeLlmAttackers.put(game, attackModel, attackStrategy);
        }

        int userId = getCorrectUserId(role);


        if (attackModel != null && !attackerAlreadyPresent) {
            game.addPlayer(userId, role);
            organizerExecutor.execute(() -> llmExecutor.execute(() -> runLlmAction(
                    game.getId(), Role.ATTACKER)
            ));
        }
        if (defendModel != null && !defenderAlreadyPresent) {
            game.addPlayer(userId, role);
            organizerExecutor.execute(() -> llmExecutor.execute(() -> runLlmAction(
                    game.getId(), Role.DEFENDER)
            ));
        }
    }

    /**
     * Gracefully stops the LLM player in this game with this role. The current action may finish,
     * but no new actions will be started.
     */
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


    public Optional<String> getErrorMessage(int gameId, Role role) {
        return Optional.ofNullable(getCorrectMap(role).getContext(gameId)).map(LlmContext::getErrorMessage);
    }

    public Optional<String> getErrorMessage(AbstractGame game, Role role) {
        return getErrorMessage(game.getId(), role);
    }

    /**
     * This is supposed to run in a separate thread created by {@link LlmOrganizer#llmExecutor}.
     * It only runs for a single action, i.e. one mutant or one test, and then schedules another execution of itself
     * in the future. If the conditions for running are no longer met, because the game doesn't exist anymore or
     * the model has been deactivated, it terminates itself.
     *
     * <p>
     *
     * @param role may be {@link Role#ATTACKER} or {@link Role#DEFENDER}
     */
    private void runLlmAction(int gameId, Role role) {
        LlmContext context = getContext(gameId, role);


        context.updateGame();


        final AbstractGame game = context.game();
        final LlmStrategy strategy = context.strategy();


        logger.info("Running llmAction for game {} with role {}", gameId, role);

        double timeModifier = strategy.getTimeModifier();

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

        if (gameRepository.isGameActive(gameId) && context.threadState() == ThreadState.ACTIVE) {
            try {
                singleLlmAction(context);
                long timeToWait;
                if (equivalentOnlyGames.contains(game.getId())) {
                    timeToWait = 0;
                } else {
                    timeToWait = Math.max(0, timeToStartNextThread - System.currentTimeMillis());
                }

                organizerExecutor.schedule(() -> llmExecutor.execute(
                                () -> runLlmAction(gameId, role)),
                        timeToWait, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                logger.error("AiPlayerThread for game {} with role {} timed out.",
                        game.getId(), role);
                context.setErrorMessage(e);
            } catch (NoSuchModelException e) {
                logger.error("The model is no longer active, llm player thread will be aborted.");
                context.setErrorMessage(e);
                finishPlayer(game.getId(), role);
            } catch (Exception e) {
                logger.error("""
                                LLM player thread in game {} and role {} threw an exception:
                                {}
                                The thread will be terminated.""",
                        game.getId(), role, e.toString());
                context.setErrorMessage(e);
                finishPlayer(game.getId(), role);
            }
        } else if (gameRepository.isGameCreated(game.getId())) {
            long timeToWait = Math.max(0, timeToStartNextThread - System.currentTimeMillis());
            organizerExecutor.schedule(() -> llmExecutor.execute(
                            () -> runLlmAction(gameId, role)),
                    timeToWait, TimeUnit.MILLISECONDS);
        } else {
            finishPlayer(game.getId(), role);
        }
    }

    private void singleLlmAction(LlmContext context) throws NoSuchModelException {


        AbstractGame game = context.game();
        Role role = context.role();
        LlmStrategy strategy = context.strategy();
        SimpleUser user = context.user();

        if (strategy == null) {
            throw new RuntimeException("Strategies may not be null!");
        }

        RequestContextController requestContextController = CDIUtil.getBeanFromCDI(RequestContextController.class);
        requestContextController.activate();

        try {

            AbstractStrategy strategyClass = CDIUtil.getBeanFromCDI(strategy.getService());

            if (context.role() == Role.DEFENDER) {
                strategyClass.claimEquivalent(context);
                strategyClass.run(context);
            } else {
                CDIUtil.getBeanFromCDI(EquivalenceStrategy.class).run(context);
                if (equivalentOnlyGames.contains(game.getId())
                        && gameService.getFlaggedMutants(user, game).isEmpty()) {
                    equivalentOnlyGames.remove((Integer) game.getId());
                    finishPlayer(game.getId(), role);
                    return;
                }
                if (role == Role.ATTACKER) {
                    if (!equivalentOnlyGames.contains(game.getId())) {
                        strategyClass.run(context);
                    }
                } else {
                    throw new RuntimeException("Unsupported role: " + role
                            + ". Use two different threads for melee games.");
                }
            }
        } finally {
            requestContextController.deactivate();
        }
    }

    public enum ThreadState {
        ACTIVE,
        FINISHING,
        INACTIVE
    }

    private static class GameLlmState {
        private final Map<Integer, LlmContext> map = new HashMap<>();


        /**
         * Sets the model and strategy for this game. Conversations, baggages etc. are reset if they exist.
         */
        private void put(AbstractGame game, LlModel model, LlmStrategy strategy) {
            if (model == null || strategy == null) {
                if (map.containsKey(game.getId())) {
                    map.put(game.getId(), LlmContext.finishingModel(model, strategy, game));
                }
            } else {
                map.put(game.getId(), new LlmContext(model, strategy, game));
            }

        }

        private LlmContext getContext(int gameId) {
            return map.get(gameId);
        }

        private ThreadState getState(int gameId) {
            if (map.containsKey(gameId)) {
                return map.get(gameId).threadState();
            } else {
                return ThreadState.INACTIVE;
            }
        }

        private void completeFinish(int gameId) {
            if (map.containsKey(gameId)) {
                map.get(gameId).setThreadState(ThreadState.INACTIVE);
            }
        }

        private void closeModel(LlModel model) {
            Set<Integer> toRemove = new HashSet<>();
            for (var x : map.entrySet()) {
                if (x.getValue().model().equals(model)) {
                    toRemove.add(x.getKey());
                }
            }
            for (int k : toRemove) {
                map.remove(k);
            }
        }
    }


}
