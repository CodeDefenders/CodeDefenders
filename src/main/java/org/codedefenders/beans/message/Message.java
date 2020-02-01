package org.codedefenders.beans.message;

import org.apache.commons.lang.StringEscapeUtils;

import java.io.Serializable;

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
            return StringEscapeUtils.escapeHtml(text);
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

    public void fadeOut(boolean fadeOut) {
        this.fadeOut = fadeOut;
    }

    public void escape(boolean escape) {
        this.escape = escape;
    }
}
