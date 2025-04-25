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
package org.codedefenders.game;

import java.util.Arrays;
import java.util.List;

/**
 * This enumeration represents roles players can have in a game.
 */
public enum Role {
    ATTACKER("Attacker"),
    DEFENDER("Defender"),
    OBSERVER("Observer"),
    PLAYER("Player"),
    NONE;

    private final String displayName;

    Role() {
        displayName = null;
    }

    Role(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Similar to {@link #valueOf(String)} but returns {@code null} if
     * {@link #valueOf(String) valueOf()} does not match.
     *
     * @param name the name of the requested enum.
     * @return the enum for the given name, or {@code null} if none was found.
     */
    public static Role valueOrNull(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getFormattedString() {
        return this.displayName;
    }

    /**
     * Returns a list of all possible roles for multiplayer games.
     * @return A list of all possible roles for multiplayer games.
     */
    public static List<Role> multiplayerRoles() {
        return Arrays.asList(ATTACKER, DEFENDER, OBSERVER);
    }

    /**
     * Returns a list of all possible roles for melee games.
     * @return A list of all possible roles for melee games.
     */
    public static List<Role> meleeRoles() {
        return Arrays.asList(PLAYER, OBSERVER);
    }
}
