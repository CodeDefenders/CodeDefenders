package org.codedefenders.beans;

import javax.annotation.ManagedBean;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * <p>Implements a container for messages that are displayed to the user on page load.</p>
 * <p>
 * This bean is session-scoped so messages can be kept over multiple request when PGR (post redirect get) is applied.
 * The messages are cleared whenever they are rendered in the JSP (see messages.jsp).
 * </p>
 */
@ManagedBean
@SessionScoped
public class MessageBean implements Iterable<Message>, Serializable {
    private long currentId = 0;
    private List<Message> messages = new ArrayList<>();

    /**
     * Returns an iterator over the messages.
     * @return An iterator over the messages.
     */
    @Override
    public Iterator<Message> iterator() {
        return messages.iterator();
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
            MessageBean.this.add(text);
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends String> texts) {
            for (String text : texts) {
                MessageBean.this.add(text);
            }
            return true;
        }
    }
}
