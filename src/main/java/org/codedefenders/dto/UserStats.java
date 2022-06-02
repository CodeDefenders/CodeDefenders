package org.codedefenders.dto;

/**
 * These Data Transfer Objects contain all statistics of a user shown on each profile page.
 */
public class UserStats {
    private final int userId;

    public UserStats(int userId) {
        this.userId = userId;
    }
}
