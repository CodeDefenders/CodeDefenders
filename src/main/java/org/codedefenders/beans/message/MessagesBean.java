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

    public void addAll(Collection<? extends String> texts) {
        for (String text : texts) {
            add(text);
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
