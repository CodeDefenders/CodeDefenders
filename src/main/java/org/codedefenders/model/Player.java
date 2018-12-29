package org.codedefenders.model;

import org.codedefenders.database.UserDAO;
import org.codedefenders.game.Role;

/**
 * This class serves as a container for players.
 *
 * Users in a game as represented as players and have a certain role.
 *
 * @see Role
 */
public class Player {
    private int id;
    private User user;
    private int gameId;
    private int points;
    private Role role;
    private boolean active;

    public Player(int id, User user, int gameId, int points, Role role, boolean active) {
        this.id = id;
        this.user = user;
        this.gameId = gameId;
        this.points = points;
        this.role = role;
        this.active = active;
    }

    public Player(int id, int userId, int gameId, int points, Role role, boolean active) {
        this.id = id;
        this.user = UserDAO.getUserById(userId);
        this.gameId = gameId;
        this.points = points;
        this.role = role;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public int getGameId() {
        return gameId;
    }

    public int getPoints() {
        return points;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }
}
