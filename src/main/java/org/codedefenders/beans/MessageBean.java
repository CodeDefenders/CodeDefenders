package org.codedefenders.beans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.RequestScoped;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@RequestScoped
public class MessageBean implements Iterable<Message> {
    private static final Logger logger = LoggerFactory.getLogger(MessageBean.class);
    private List<Message> messages = new ArrayList<>();

    @Override
    public Iterator<Message> iterator() {
        return messages.iterator();
    }

    public int count() {
        logger.info("COUNT IS: " + messages.size());
        return messages.size();
    }

    public Message add(String text) {
        logger.info("ADD MESSAGE: " + text);
        Message message = new Message(text);
        messages.add(message);
        return message;
    }

    public void addTextMessages(Collection<String> texts) {
        for (String text : texts) {
            add(text);
        }
    }

}
