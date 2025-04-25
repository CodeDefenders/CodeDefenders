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
package org.codedefenders.beans.game;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.RequestScoped;

import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.FeedbackDAO;
import org.codedefenders.game.Role;
import org.codedefenders.model.Feedback;
import org.codedefenders.model.Feedback.Type;
import org.codedefenders.model.Player;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.servlets.admin.AdminSystemSettings;
import org.codedefenders.util.CDIUtil;

/**
 * <p>Provides data for the player feedback game component.</p>
 * <p>Bean Name: {@code playerFeedback}</p>
 */
@RequestScoped
public class PlayerFeedbackBean {
    private Integer gameId;
    private Integer creatorId;
    private int userId;
    private Role role;

    private Boolean showFeedbackEnabled;

    private Map<Feedback.Type, Integer> ownRatings;
    private Map<Feedback.Type, Double> averageRatings;
    private Map<Player, Map<Feedback.Type, Integer>> ratingsPerPlayer;

    public PlayerFeedbackBean() {
        gameId = null;
        creatorId = null;
        role = null;

        showFeedbackEnabled = null;

        ownRatings = null;
        ratingsPerPlayer = null;
        averageRatings = null;
    }

    public void setGameInfo(int gameId, int creatorId) {
        this.gameId = gameId;
        this.creatorId = creatorId;
    }

    public void setPlayerInfo(int userId, Role role) {
        this.userId = userId;
        this.role = role;
    }

    // --------------------------------------------------------------------------------

    public int getGameId() {
        return gameId;
    }

    public boolean canGiveFeedback() {
        return role == Role.ATTACKER || role == Role.DEFENDER || role == Role.PLAYER;
    }

    public boolean canSeeFeedback() {
        if (showFeedbackEnabled == null) {
            showFeedbackEnabled = AdminDAO.getSystemSetting(AdminSystemSettings.SETTING_NAME.SHOW_PLAYER_FEEDBACK)
                    .getBoolValue();
        }
        return userId == creatorId || showFeedbackEnabled;
    }

    public Map<Feedback.Type, Integer> getOwnRatings() {
        if (ownRatings == null) {
            ownRatings = FeedbackDAO.getFeedbackValues(gameId, userId);
        }
        return ownRatings;
    }

    /**
     * Returns whether the user currently has any valid (> 0) saved ratings.
     */
    public boolean hasOwnRatings() {
        Map<Feedback.Type, Integer> ratings = getOwnRatings();
        return ratings.values().stream()
                .anyMatch(rating -> rating > 0);
    }

    public Map<Player, Map<Feedback.Type, Integer>> getAllRatings() {
        GameRepository gameRepo = CDIUtil.getBeanFromCDI(GameRepository.class);

        if (ratingsPerPlayer == null) {
            ratingsPerPlayer = new HashMap<>();
            for (Player player : gameRepo.getValidPlayersForGame(gameId)) {
                ratingsPerPlayer.put(player, FeedbackDAO.getFeedbackValues(gameId, player.getUser().getId()));
            }
        }
        return ratingsPerPlayer;
    }

    public Map<Feedback.Type, Double> getAverageRatings() {
        if (averageRatings == null) {
            averageRatings = FeedbackDAO.getAverageGameRatings(gameId);
        }
        return averageRatings;
    }

    public List<Type> getAvailableFeedbackTypes() {
        return Feedback.Type.getFeedbackTypesForRole(role);
    }
}
