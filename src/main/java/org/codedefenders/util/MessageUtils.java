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
package org.codedefenders.util;

public class MessageUtils {
    /**
     * Choose the singular or plural form of a string according to the given amount.
     * The plural form is returned if {@code (amount == 0 | amount > 1)}
     *
     * @param amount   The amount.
     * @param singular The singular form of the string.
     * @param plural   The plural form of the string.
     * @return The singular or plural from of the string.
     */
    public static String pluralize(int amount, String singular, String plural) {
        if (amount == 0 || amount > 1) {
            return plural;
        } else {
            return singular;
        }
    }

    /**
     * Choose the singular or plural form of a string according to the given amount.
     * The plural form is returned if {@code (amount == 0 | amount > 1)}
     * The plural form is the singular form with an appended 's'.
     *
     * @param amount   The amount.
     * @param singular The singular form of the string.
     * @return The singular or plural from of the string.
     */
    public static String pluralize(int amount, String singular) {
        return pluralize(amount, singular, singular + "s");
    }

    /**
     * Returns the ordinal suffix (st, nd, rd, th) of a number.
     *
     * @param i The number.
     * @return The ordinal suffix of the number.
     */
    public static String ordinalSuffix(int i) {
        var j = i % 10;
        var k = i % 100;

        if (j == 1 && k != 11) {
            return "st";
        }
        if (j == 2 && k != 12) {
            return "nd";
        }
        if (j == 3 && k != 13) {
            return "rd";
        }
        return "th";
    }
}
