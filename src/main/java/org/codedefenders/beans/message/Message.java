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
        this.fadeOut = true;
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

    public Message setTitle(String title) {
        this.title = title;
        return this;
    }

    public Message setSecondary(String secondary) {
        this.secondary = secondary;
        return this;
    }
}
