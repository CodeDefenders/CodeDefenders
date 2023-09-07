package org.codedefenders.beans.game;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;

import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.FeedbackDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.game.Role;
import org.codedefenders.model.Feedback;
import org.codedefenders.model.Feedback.Type;
import org.codedefenders.model.Player;
import org.codedefenders.servlets.admin.AdminSystemSettings;

/**
 * <p>Provides data for the player feedback game component.</p>
 * <p>Bean Name: {@code playerFeedback}</p>
 */
@ManagedBean
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
        if (ratingsPerPlayer == null) {
            ratingsPerPlayer = new HashMap<>();
            for (Player player : GameDAO.getValidPlayersForGame(gameId)) {
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
