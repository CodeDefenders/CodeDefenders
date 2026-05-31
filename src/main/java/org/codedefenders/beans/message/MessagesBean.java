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
package org.codedefenders.beans.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;

import org.codedefenders.util.PreparedMessage;

/**
 * <p>Implements a container for messages that are displayed to the user on page load.</p>
 * <p>
 * This bean is session-scoped so messages can be kept over multiple request when PRG (post redirect get) is applied.
 * The messages are cleared whenever they are rendered in the JSP (see messages.jsp).
 * </p>
 */
// TODO: Find a way to make this request scoped, so messages are not mixed when multiple tabs are used.
@SessionScoped
@Named("messages")
public class MessagesBean implements Serializable {
    private long currentId;
    private final List<Message> messages;

    public MessagesBean() {
        currentId = 0;
        messages = new ArrayList<>();
    }

    /**
     * Returns a new list containing the messages.
     * @return A new list containing the messages.
     */
    public synchronized List<Message> getMessages() {
        return new ArrayList<>(messages);

    }

    public synchronized List<Message> getAlertMessages(boolean alert) {
        return messages.stream()
                .filter(msg -> msg.isAlert() == alert)
                .toList();
    }

    /**
     * Returns the number of messages.
     * @return The number of messages.
     */
    public int getCount() {
        return messages.size();
    }

    /**
     * Adds a message. The new message is returned so that it can be modified via builder-style methods.
     * The text of the message will be HTML escaped.
     * @param text The text of the message.
     * @return The newly created message.
     */
    public synchronized Message add(String text) {
        Message message = new Message(text, currentId++);
        messages.add(message);
        return message;
    }

    public synchronized Message add(String text, String title) {
        return add(text).setTitle(title);
    }

    public synchronized Message add(PreparedMessage pMessage) {
        var msg = add(pMessage.pattern());
        if (pMessage.hasArguments()) msg.setArgs(pMessage.arguments());
        return msg;
    }

    public synchronized Message addFormatted(String text, Object... args) {
        return add(text).setArgs(args);
    }

    /**
     * Adds all messages in the given collection.
     * The collection can contain both {@link String} and {@link PreparedMessage} objects.
     *
     * @param texts The collection of messages to add.
     *              Each message needs to be either a {@link String} or a {@link PreparedMessage}.
     * @throws IllegalArgumentException If the collection contains an object that is not a {@link String} or
     *                                  a {@link PreparedMessage}.
     */
    public void addAll(Collection<?> texts) {
        for (var text : texts) {
            if (text instanceof PreparedMessage pMessage) {
                add(pMessage);
            } else if (text instanceof String str) {
                add(str);
            } else {
                throw new IllegalArgumentException("Unsupported message type: " + text.getClass());
            }
        }
    }

    public Message addDirect(Message msg) {
        Message message = new Message(
                msg.getText(),
                currentId++)
                .escape(msg.isEscape());
        if (msg.isAlert()) {
            message.alert();
        }
        messages.add(message);
        return message;
    }

    public void addAllDirect(List<Message> messages) {
        for (var msg : messages) {
            addDirect(msg);
        }
    }

    /**
     * Clears the messages.
     */
    public synchronized void clear() {
        messages.clear();
    }
}
