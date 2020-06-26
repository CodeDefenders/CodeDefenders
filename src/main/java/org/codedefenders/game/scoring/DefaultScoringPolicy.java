/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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

import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.multiplayer.PlayerScore;
import org.codedefenders.model.Event;
import org.codedefenders.util.Constants;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This implementation of a {@link IScoringPolicy} calculates the score based on
 * how many mutants were killed by a test or tests were passed by a mutant. This
 * is the basic scoring policy used in the game.
 *
 * <p>TODO Some caching would be great at this point ?
 */
public class DefaultScoringPolicy implements IScoringPolicy {

    private EventDAO eventDAO;

    // TODO Convert this to PlayerScore so we can keep track of won/lost equivalence
    // duels !
    // TestID, Score
    private Map<Integer, Integer> testsScore = new HashMap<Integer, Integer>();
    // MutantID, Score
    private Map<Integer, Integer> mutantsScore = new HashMap<Integer, Integer>();
    // PlayerID, PointsForWinningDuels
    private Map<Integer, Integer> duelsScore = new HashMap<Integer, Integer>();
    // MutantID, PlayerClaimingEquivalence
    private Map<Integer, Integer> flaggedMutants = new HashMap<Integer, Integer>();

    public DefaultScoringPolicy(EventDAO eventDAO) {
        this.eventDAO = eventDAO;
    }

    // TODO This can be easily cached!
    private void computeScoreForGame(Integer gameId) {
        // Retrieve the event log
        List<Event> eventLog = eventDAO.getEventsForGame(gameId);
        // Sort by Timestamp
        Collections.sort(eventLog, new Comparator<Event>() {
            @Override
            public int compare(Event o1, Event o2) {
                return o1.getTimestamp().compareTo(o2.getTimestamp());
            }
        });
        // Reset Old State
        testsScore.clear();
        mutantsScore.clear();
        duelsScore.clear();
        flaggedMutants.clear();

        // TODO Filter by getUser
        // Process the event log to compute the score of each entity
        for (Event event : eventLog) {
            if (event.getUser().getId() == Constants.DUMMY_CREATOR_USER_ID) {
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
                    case ATTACKER_MUTANT_KILLED_EQUIVALENT:
                        // Remove the mutant from the flagged mutants
                        flaggedMutants.remove(mutantId);
                        // Mutant is killed: we keep the mutant's points and we get an extra point as duels point
                        int mutantOwnerPlayerId = MutantDAO.getMutantById(mutantId).getPlayerId();
                        // Give one point to the player claiming the equivalence
                        int duelScoreWin = duelsScore.getOrDefault(mutantOwnerPlayerId, 0);
                        duelScoreWin = duelScoreWin + 1;
                        duelsScore.put(mutantOwnerPlayerId, duelScoreWin);
                        break;
                    case PLAYER_LOST_EQUIVALENT_DUEL:
                    case DEFENDER_MUTANT_EQUIVALENT:
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
            AbstractGame game = GameDAO.getGameWherePlayerPlays(duelScore.getPlayerId());
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
