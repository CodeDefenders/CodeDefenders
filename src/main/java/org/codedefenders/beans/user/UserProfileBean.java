package org.codedefenders.beans.user;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;

import org.codedefenders.dto.UserStats;
import org.codedefenders.model.UserEntity;

/**
 * Holds information for the user profile page.
 */
@RequestScoped
@ManagedBean
public class UserProfileBean {
    private UserEntity user;
    private UserStats stats;
    private boolean isSelf;

    /**
     * Show the profile page for this user.
     * @return user whose profile page was requested.
     */
    public UserEntity getUser() {
        return user;
    }

    /**
     * Statistics of {@link UserProfileBean#getUser()}.
     * @return the user statistics shown on the profile page.
     */
    public UserStats getStats() {
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

    public void setStats(UserStats userStats) {
        this.stats = userStats;
    }

    public void setSelf(boolean self) {
        isSelf = self;
    }
}
