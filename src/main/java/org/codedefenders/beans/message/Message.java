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

import org.apache.commons.text.StringEscapeUtils;

/**
 * Represents a message shown on to a user on page load.
 */
public class Message implements Serializable {
    private long id;
    private String text;
    private boolean fadeOut;
    private boolean escape;

    /**
     * Constructs a new message with the given text. Use {@link MessagesBean#add(String)} instead of calling the
     * constructor directly.
     * @param text The text of the message.
     */
    public Message(String text, long id) {
        this.id = id;
        this.text = text;
        this.fadeOut = true;
        this.escape = true;
    }

    /**
     * Returns the id of the message.
     * @return The id of the message.
     */
    public long getId() {
        return id;
    }

    /**
     * Returns the escaped or unescaped text of the message, depending on if the message is set to escape the text.
     * @return The text of the message.
     */
    public String getText() {
        if (isEscape()) {
            return StringEscapeUtils.escapeHtml4(text);
        } else {
            return text;
        }
    }

    /**
     * Returns if the message should be HTML-escaped.
     * @return If the message should be HTML-escaped.
     */
    public boolean isEscape() {
        return escape;
    }

    /**
     * Returns if the message should fade out or stay on screen.
     * @return If the message should fade out or stay on screen.
     */
    public boolean isFadeOut() {
        return fadeOut;
    }

    /* Builder-style setter methods. */

    public Message fadeOut(boolean fadeOut) {
        this.fadeOut = fadeOut;
        return this;
    }

    public Message escape(boolean escape) {
        this.escape = escape;
        return this;
    }
}
