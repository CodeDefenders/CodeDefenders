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
package org.codedefenders.game.scoring;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codedefenders.database.EventDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.PlayerScore;
import org.codedefenders.model.Event;
import org.codedefenders.model.EventType;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.persistence.database.PlayerRepository;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation of a {@link IScoringPolicy} calculates the score based on
 * how many mutants were killed by a test or tests were passed by a mutant. This
 * is the basic scoring policy used in the game.
 *
 * <p>TODO Some caching would be great at this point ?
 */
public class DefaultScoringPolicy implements IScoringPolicy {
    private static final Logger logger = LoggerFactory.getLogger(DefaultScoringPolicy.class);

    private final EventDAO eventDAO;
    private final MutantRepository mutantRepo;
    private final GameRepository gameRepo;
    private final PlayerRepository playerRepo;

    // TODO Convert this to PlayerScore so we can keep track of won/lost equivalence
    // duels !
    // TestID, Score
    private Map<Integer, Integer> testsScore = new HashMap<>();
    // MutantID, Score
    private Map<Integer, Integer> mutantsScore = new HashMap<>();
    // PlayerID, PointsForWinningDuels
    private Map<Integer, Integer> duelsScore = new HashMap<>();
    // MutantID, PlayerClaimingEquivalence
    private Map<Integer, Integer> flaggedMutants = new HashMap<>();

    public DefaultScoringPolicy(EventDAO eventDAO, MutantRepository mutantRepo, GameRepository gameRepo,
                                PlayerRepository playerRepo) {
        this.eventDAO = eventDAO;
        this.mutantRepo = mutantRepo;
        this.gameRepo = gameRepo;
        this.playerRepo = playerRepo;
    }

    // TODO This can be easily cached!
    private void computeScoreForGame(Integer gameId) {
        // Retrieve the event log
        List<Event> eventLog = eventDAO.getEventsForGame(gameId);
        // Sort by Timestamp
        eventLog.sort(Comparator.comparing(Event::getTimestamp));
        // Reset Old State
        testsScore.clear();
        mutantsScore.clear();
        duelsScore.clear();
        flaggedMutants.clear();

        // TODO Filter by getUser
        // Process the event log to compute the score of each entity
        for (Event event : eventLog) {
            if (event.getUserId() == Constants.DUMMY_CREATOR_USER_ID) {
                // Special case: Automatic Equivalence Duels events also have event.getUser().getId() == 1 but they
                // have a 'normal' Message Payload so we need to short circuit otherwise the Message Payload extraction
                // in the following lines will fail exceptionally ^^.
                if (!event.getMessage().matches("[-0-9]*:[-0-9]*")) {
                    if (event.getEventType().equals(EventType.DEFENDER_MUTANT_CLAIMED_EQUIVALENT)
                            || event.getEventType().equals(EventType.PLAYER_MUTANT_CLAIMED_EQUIVALENT)) {
                        // For this event types it can happen that the payload is not in the expected format.
                        logger.debug("Ignored automatic triggered equivalence duel event");
                    } else {
                        throw new IllegalStateException("Encountered non-parseable event while computing score. Type: "
                                + event.getEventType() + " and Message: " + event.getMessage());
                    }
                    break;
                }
                // Extract Message Payload
                Integer testId = Integer.parseInt(event.getMessage().split(":")[0]);
                // The first field of the message is overloaded for lost equivalence duels

                Integer mutantId = Integer.parseInt(event.getMessage().split(":")[1]);
                int testScore = testsScore.getOrDefault(testId, 0);
                int mutantScore = mutantsScore.getOrDefault(mutantId, 0);

                switch (event.getEventType()) {
                    case PLAYER_MUTANT_CLAIMED_EQUIVALENT:
                    case DEFENDER_MUTANT_CLAIMED_EQUIVALENT:
                        // Book-keeping that this mutant is flagged as equivalent by a user...
                        Integer playerClaimingEquivalenceId = Integer.parseInt(event.getMessage().split(":")[0]);
                        flaggedMutants.put(mutantId, playerClaimingEquivalenceId);
                        break;
                    case PLAYER_WON_EQUIVALENT_DUEL:
                    case PLAYER_MUTANT_KILLED_EQUIVALENT:
                    case ATTACKER_MUTANT_KILLED_EQUIVALENT:
                        // Remove the mutant from the flagged mutants
                        flaggedMutants.remove(mutantId);
                        // Mutant is killed: we keep the mutant's points and we get an extra point as duels point
                        int mutantOwnerPlayerId = mutantRepo.getMutantById(mutantId).getPlayerId();
                        // Give one point to the player claiming the equivalence
                        int duelScoreWin = duelsScore.getOrDefault(mutantOwnerPlayerId, 0);
                        duelScoreWin = duelScoreWin + 1;
                        duelsScore.put(mutantOwnerPlayerId, duelScoreWin);
                        break;
                    case PLAYER_LOST_EQUIVALENT_DUEL:
                    case DEFENDER_MUTANT_EQUIVALENT:
                    case PLAYER_MUTANT_EQUIVALENT:
                        // Remove the mutant from the flagged mutants but keep the playerId that flagged
                        // it
                        Integer playerId = flaggedMutants.remove(mutantId);

                        // Remove the points from the equivalent mutant
                        mutantScore = 0;
                        mutantsScore.put(mutantId, mutantScore);

                        // Give one point to the player claiming the equivalence
                        int duelScoreLost = duelsScore.getOrDefault(playerId, 0);
                        duelScoreLost = duelScoreLost + 1;
                        duelsScore.put(playerId, duelScoreLost);
                        break;
                    case PLAYER_KILLED_MUTANT:
                    case DEFENDER_KILLED_MUTANT:
                        // We need +1 for killing the mutant +mutantScore for earning the points from
                        // the mutant
                        testScore = testScore + mutantScore + 1;
                        testsScore.put(testId, testScore);
                        break;
                    case PLAYER_MUTANT_SURVIVED:
                    case ATTACKER_MUTANT_SURVIVED:
                        // Apply scoring policy
                        // We need +1 for each test that misses this mutant
                        mutantScore = mutantScore + 1;
                        mutantsScore.put(mutantId, mutantScore);
                        break;
                    default:
                        // Ignore other events

                }
            }
        }
    }

    @Override
    public void scoreTest(Test test) {
        if (testsScore.isEmpty()) {
            // Refresh Game Scoring
            computeScoreForGame(test.getGameId());
        }
        test.setScore(testsScore.getOrDefault(test.getId(), 0));
    }

    @Override
    public void scoreMutant(Mutant mutant) {
        if (mutantsScore.isEmpty()) {
            // Refresh Game Scoring
            computeScoreForGame(mutant.getGameId());
        }
        mutant.setScore(mutantsScore.getOrDefault(mutant.getId(), 0));
    }

    @Override
    public void scoreDuels(PlayerScore duelScore) {
        if (duelsScore.isEmpty()) { // Refresh Game Scoring
            int gameId = playerRepo.getPlayer(duelScore.getPlayerId()).getGameId();
            AbstractGame game = gameRepo.getGame(gameId);
            if (game != null) {
                computeScoreForGame(game.getId());
            } else {
                // TODO Is this possible ?!
                return;
            }
        }
        duelScore.increaseTotalScore(duelsScore.getOrDefault(duelScore.getPlayerId(), 0));
    }
}
