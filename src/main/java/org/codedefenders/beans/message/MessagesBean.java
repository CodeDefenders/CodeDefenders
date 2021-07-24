package org.codedefenders.beans.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.ManagedBean;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

/**
 * <p>Implements a container for messages that are displayed to the user on page load.</p>
 * <p>
 * This bean is session-scoped so messages can be kept over multiple request when PGR (post redirect get) is applied.
 * The messages are cleared whenever they are rendered in the JSP (see messages.jsp).
 * </p>
 */
// TODO: Find a way to make this request scoped, so messages are not mixed when multiple tabs are used.
@ManagedBean
@SessionScoped
@Named("messages")
public class MessagesBean implements Serializable {
    private long currentId;
    private List<Message> messages;

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

    /**
     * Clears the messages.
     */
    public synchronized void clear() {
        messages.clear();
    }

    /**
     * Ugly bridge that enables us to treat the bean as a list of strings for compatibility with the backend.
     * @return A object that inherits from {@link ArrayList}, but forwards the calls for {@code add} and
     *         {@code addAll} to the bean.
     */
    public ArrayList<String> getBridge() {
        return new MessageBridge();
    }

    private class MessageBridge extends ArrayList<String> {
        @Override
        public boolean add(String text) {
            MessagesBean.this.add(text);
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends String> texts) {
            for (String text : texts) {
                MessagesBean.this.add(text);
            }
            return true;
        }
    }
}
