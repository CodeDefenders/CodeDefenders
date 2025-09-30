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
package org.codedefenders.model;

/**
 * This enum represents the role a user is whitelisted for. Games can be created so that users cannot choose their
 * own role, in this case the game creator can whitelist users for a specific role. In games where users can choose
 * their role this enum is ignored.
 * <p>
 * Note: The names of these enum values are equal to the names used in the DB. Be careful before changing the names of
 * these values.
 */
public enum WhitelistType {
    /**
     * The user is whitelisted for the defender role.
     */
    DEFENDER,

    /**
     * The user is whitelisted for the attacker role.
     */
    ATTACKER,

    /**
     * The user is automatically added to the team with fewer players when joining.
     */
    FLEX,

    /**
     * The user can choose their role when joining the game.
     */
    CHOICE;


    public static WhitelistType fromString(String string) {
        return valueOf(string.toUpperCase().trim());
    }
}
