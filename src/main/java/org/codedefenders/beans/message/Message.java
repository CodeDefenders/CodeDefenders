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
 * Represents a message shown on to a user on page load. By default, the message is HTML-escaped and fades out after a
 * few seconds, this can be changed by using the builder-style setter methods.
 */
public class Message implements Serializable {
    private long id;
    private String text;
    private boolean alert;
    private boolean escape;
    private String title;
    private String secondary;

    /**
     * Constructs a new message with the given text. Use {@link MessagesBean#add(String)} instead of calling the
     * constructor directly.
     * @param text The text of the message.
     */
    public Message(String text, long id) {
        this.id = id;
        this.text = text;
        this.alert = false;
        this.escape = true;
        this.title = "";
        this.secondary = "";
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
     * Returns the title, escaped if set to.
     */
    public String getTitle() {
        if (isEscape()) {
            return StringEscapeUtils.escapeHtml4(title);
        } else {
            return title;
        }
    }

    /**
     * Returns the secondary text, escaped if set to.
     */
    public String getSecondary() {
        if (isEscape()) {
            return StringEscapeUtils.escapeHtml4(secondary);
        } else {
            return secondary;
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
     * @return If true, the message will stay on the screen.
     */
    public boolean isAlert() {
        return alert;
    }

    /* Builder-style setter methods. */

    public Message alert() {
        this.alert = true;
        return this;
    }

    public Message escape(boolean escape) {
        this.escape = escape;
        return this;
    }

    public Message setTitle(String title) {
        this.title = title;
        return this;
    }

    public Message setSecondary(String secondary) {
        this.secondary = secondary;
        return this;
    }
}
