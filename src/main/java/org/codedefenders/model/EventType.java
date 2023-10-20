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
package org.codedefenders.model;

/**
 * Created by thomas on 06/03/2017.
 */
public enum EventType {
    GAME_CREATED, GAME_STARTED, GAME_FINISHED, GAME_GRACE_ONE, GAME_GRACE_TWO,
    GAME_PLAYER_LEFT,

    ATTACKER_MUTANT_CREATED, ATTACKER_MUTANT_SURVIVED, ATTACKER_MUTANT_ERROR,
    ATTACKER_MUTANT_KILLED_EQUIVALENT, ATTACKER_MESSAGE,

    DEFENDER_MUTANT_EQUIVALENT, DEFENDER_TEST_CREATED, DEFENDER_TEST_READY,
    DEFENDER_TEST_ERROR, DEFENDER_MUTANT_CLAIMED_EQUIVALENT,
    DEFENDER_KILLED_MUTANT, DEFENDER_MESSAGE,

    // Same as ATTACKER and DEFENDER BUT FOR MELEE MODE
    PLAYER_MUTANT_CREATED, PLAYER_MUTANT_ERROR,
    // Resolve Equivalence Duel
    PLAYER_WON_EQUIVALENT_DUEL,
    // Mutant Accepted as equivalent. Lost Equivalent duel
    PLAYER_LOST_EQUIVALENT_DUEL,
    // Test covered but not killed someone else's mutant
    PLAYER_MUTANT_SURVIVED,
    // Test killed someone else's mutant
    PLAYER_KILLED_MUTANT,

    PLAYER_MUTANT_CLAIMED_EQUIVALENT,

    PLAYER_TEST_CREATED, PLAYER_TEST_ERROR, PLAYER_TEST_READY,
    PLAYER_MUTANT_EQUIVALENT, PLAYER_MUTANT_KILLED_EQUIVALENT,
    PLAYER_MESSAGE,

    ATTACKER_JOINED, DEFENDER_JOINED, PLAYER_JOINED,
    GAME_MESSAGE, GAME_MESSAGE_ATTACKER, GAME_MESSAGE_DEFENDER, GAME_MESSAGE_PLAYER,
}
