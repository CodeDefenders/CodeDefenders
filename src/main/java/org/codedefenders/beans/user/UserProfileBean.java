/*
 * Copyright (C) 2023 Code Defenders contributors
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
package org.codedefenders.beans.user;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;

import org.codedefenders.dto.UserStats;
import org.codedefenders.game.GameType;
import org.codedefenders.model.Achievement;
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
    private Collection<Achievement> achievements;

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

    public Collection<Achievement> getAchievements() {
        return achievements;
    }

    public Collection<Achievement> getUnlockedAchievements() {
        return achievements.stream()
                .filter(achievement -> achievement.getLevel() > 0)
                .toList();
    }

    public Collection<Achievement> getLockedAchievements() {
        return achievements.stream()
                .filter(achievement -> achievement.getLevel() == 0)
                .toList();
    }

    public void setAchievements(Collection<Achievement> achievements) {
        this.achievements = achievements.stream()
                .sorted(Comparator.comparingInt(Achievement::getIndex))
                .toList();
    }
}
