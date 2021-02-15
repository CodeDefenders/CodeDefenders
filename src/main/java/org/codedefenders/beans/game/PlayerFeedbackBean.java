package org.codedefenders.beans.game;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;

import org.codedefenders.database.AdminDAO;
import org.codedefenders.database.FeedbackDAO;
import org.codedefenders.database.GameDAO;
import org.codedefenders.game.Role;
import org.codedefenders.model.Feedback.Type;
import org.codedefenders.model.Player;
import org.codedefenders.model.User;
import org.codedefenders.servlets.admin.AdminSystemSettings;

/**
 * <p>Provides data for the player feedback game component.</p>
 * <p>Bean Name: {@code playerFeedback}</p>
 */
// TODO: Put some of this logic into the FeedbackDAO methods. This is a mess.
// TODO: Also work with maps of feedback values instead of lists.
@ManagedBean
@RequestScoped
public class PlayerFeedbackBean {
    private Integer gameId;
    private Integer creatorId;
    private User user;
    private Role role;

    private Boolean showFeedbackEnabled;

    private List<Integer> ownRatings;
    private Map<Player, List<Integer>> ratingsPerPlayer;
    private List<Double> averageRatings;

    private List<Type> availableFeedbackTypes;

    public PlayerFeedbackBean() {
        gameId = null;
        creatorId = null;
        role = null;

        showFeedbackEnabled = null;

        ownRatings = null;
        ratingsPerPlayer = null;
        averageRatings = null;

        availableFeedbackTypes = Collections.emptyList();
    }

    public void setGameInfo(int gameId, int creatorId) {
        this.gameId = gameId;
        this.creatorId = creatorId;
    }

    public void setPlayerInfo(User user, Role role) {
        this.user = user;
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
        return user.getId() == creatorId || showFeedbackEnabled;
    }

    public List<Integer> getOwnRatings() {
        if (ownRatings == null) {
            ownRatings = FeedbackDAO.getFeedbackValues(gameId, user.getId());
        }
        return ownRatings;
    }

    public Map<Player, List<Integer>> getAllRatings() {
        if (ratingsPerPlayer == null) {
            ratingsPerPlayer = new HashMap<>();
            for (Player player : GameDAO.getValidPlayersForGame(gameId)) {
                ratingsPerPlayer.put(player, FeedbackDAO.getFeedbackValues(gameId, player.getUser().getId()));
            }
        }
        return ratingsPerPlayer;
    }

    public List<Double> getAverageRatings() {
        if (averageRatings == null) {
            averageRatings = FeedbackDAO.getAverageGameRatings(gameId);
        }
        return averageRatings;
    }

    public void setAvailableFeedbackTypesForRole(Role role) {
        availableFeedbackTypes = Type.getFeedbackTypesForRole(role);
    }

    public List<Type> getAvailableFeedbackTypes() {
        return availableFeedbackTypes;
    }
}
