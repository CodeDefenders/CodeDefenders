package org.codedefenders.dto.api;

import java.util.List;

public class NewGameRequest {
    private int classId;
    private List<Team> teams;
    private APIGameSettings settings;
    private String returnUrl;

    public int getClassId() {
        return classId;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public APIGameSettings getSettings() {
        return settings;
    }

    public String getReturnUrl() {
        return returnUrl;
    }
}
