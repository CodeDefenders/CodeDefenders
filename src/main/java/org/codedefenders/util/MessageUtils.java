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

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

public class MessageUtils {
    /**
     * Choose the singular or plural form of a string according to the given amount.
     * The plural form is returned if {@code (amount == 0 | amount > 1)}
     * @param amount The amount.
     * @param singular The singular form of the string.
     * @param plural The plural form of the string.
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
     * Adds a message that is displayed on the next page the user sees.
     * @param session The session of the user.
     * @param message The message.
     */
    public static void addMessage(HttpSession session, String message) {
        @SuppressWarnings("unchecked")
        List<String> messages = (List<String>) session.getAttribute("messages");
        if (messages == null) {
            messages = new ArrayList<>();
            session.setAttribute("messages", messages);
        }
        messages.add(message);
    }
}
