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
package org.codedefenders.model.llm;

import java.util.HashMap;
import java.util.Map;

import org.codedefenders.dto.SimpleUser;
import org.codedefenders.game.AbstractGame;

/**
 * Contains mutable {@link LlmConversation}s, representing the back and forth
 * conversation between the client and the LLM. There is at most one conversation for each type of conversation.
 * This way history about mutant generation attempts is not lost when trying to solve an
 * equivalence duel, for example.
 */
public class LlmConversationBatch {
    private final Map<String, LlmConversation> messageLists = new HashMap<>();
    private final AbstractGame game;
    private final SimpleUser user;

    private final LlmStrategy strategy;

    /**
     * An undefined item that can be used in any way necessary to transport data between actions.
     */
    private Object baggage;

    public LlmConversationBatch(AbstractGame game, SimpleUser user, LlmStrategy strategy) {
        this.game = game;
        this.user = user;
        this.strategy = strategy;
    }

    public void remove(LlmConversation toRemove) {
        messageLists.remove(toRemove.getType());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("LlmConversationBatch:{");
        for (String t : messageLists.keySet()) {
            sb.append(t).append(":{");
            sb.append(messageLists.get(t));
            sb.append("}");
        }
        return sb.toString();
    }

    /**
     * Returns the conversation associated with the specified type.
     * If no such conversation exists, a new one is created.
     */
    public LlmConversation getConversation(String type) {
        if (!messageLists.containsKey(type)) {
            LlmConversation con = new LlmConversation(type, strategy.getBase().role,
                    game, user, strategy.getName(), true, false, 0, 0);
            messageLists.put(type, con);
        }
        return messageLists.get(type);
    }
}
