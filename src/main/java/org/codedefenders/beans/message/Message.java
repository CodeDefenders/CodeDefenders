package org.codedefenders.beans.message;

import org.codedefenders.beans.message.MessagesBean;

/**
 * Represents a message shown on to a user on page load.
 */
public class Message {
    private long id;
    private String text;
    private boolean fadeOut;

    /**
     * Constructs a new message with the given text. Use {@link MessagesBean#add(String)} instead of calling the
     * constructor directly.
     * @param text The text of the message.
     */
    public Message(String text, long id) {
        this.id = id;
        this.text = text;
        this.fadeOut = true;
    }

    /**
     * Returns the id of the message.
     * @return The id of the message.
     */
    public long getId() {
        return id;
    }

    /**
     * Returns the text of the message.
     * @return The text of the message.
     */
    public String getText() {
        return text;
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
}
