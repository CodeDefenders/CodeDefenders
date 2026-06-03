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
package org.codedefenders.notification.events.server.equivalence;

import com.google.gson.annotations.Expose;

/**
 * Equivalence duel result event.
 */
public class EquivalenceDuelResultEvent extends EquivalenceDuelEvent {

    @Expose
    private boolean attackerWon;

    public boolean hasAttackerWon() {
        return attackerWon;
    }

    public void setAttackerWon() {
        this.attackerWon = true;
    }

    public boolean hasDefenderWon() {
        return !attackerWon;
    }

    public void setDefenderWon() {
        this.attackerWon = false;
    }

    /**
     * @return the winner's user ID
     */
    public int getWinnerId() {
        return attackerWon ? getAttackerId() : getDefenderId();
    }

    /**
     * @return the loser's user ID
     */
    public int getLoserId() {
        return attackerWon ? getDefenderId() : getAttackerId();
    }

}
