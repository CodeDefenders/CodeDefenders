package org.codedefenders.beans.user;

import java.util.Map;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;

import org.codedefenders.dto.UserStats;
import org.codedefenders.game.GameType;
import org.codedefenders.model.UserEntity;

/**
 * Holds information for the user profile page.
 */
@RequestScoped
@ManagedBean
public class UserProfileBean {
    private UserEntity user;
    private Map<GameType, UserStats> stats;
    private boolean isSelf;
    private UserStats.PuzzleStats puzzleStats;

    /**
     * Show the profile page for this user.
     * @return user whose profile page was requested.
     */
    public UserEntity getUser() {
        return user;
    }

    /**
     * Statistics of {@link UserProfileBean#getUser()}.
     *
     * @return the user statistics shown on the profile page.
     */
    public Map<GameType, UserStats> getStats() {
        return stats;
    }

    /**
     * Stores whether the profile page shows the currently logged-in user or someone else.
     * @return {@code true} if the logged-in user views their own profile.
     */
    public boolean isSelf() {
        return isSelf;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public void setStats(Map<GameType, UserStats> userStats) {
        this.stats = userStats;
    }

    public void setSelf(boolean self) {
        isSelf = self;
    }

    public UserStats.PuzzleStats getPuzzleStats() {
        return puzzleStats;
    }

    public void setPuzzleStats(UserStats.PuzzleStats puzzleStats) {
        this.puzzleStats = puzzleStats;
    }
}
