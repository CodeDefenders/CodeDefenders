/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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

    ATTACKER_JOINED, DEFENDER_JOINED, GAME_MESSAGE, GAME_MESSAGE_ATTACKER,
    GAME_MESSAGE_DEFENDER
}
