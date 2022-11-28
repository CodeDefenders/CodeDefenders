package org.codedefenders.dto.api;

import java.util.List;

public class Team {
    private List<Integer> userIds;
    private org.codedefenders.game.Role role;

    public List<Integer> getUserIds() {
        return userIds;
    }

    public org.codedefenders.game.Role getRole() {
        return role;
    }
}
