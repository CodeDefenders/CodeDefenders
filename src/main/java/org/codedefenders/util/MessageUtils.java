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
