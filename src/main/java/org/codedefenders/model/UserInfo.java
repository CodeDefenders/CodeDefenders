package org.codedefenders.model;

import org.codedefenders.game.Role;

import java.sql.Timestamp;

/**
 * This class serves as a container class for {@link User Users} and
 * additional information.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 */
public class UserInfo {
    private User user;
    private Timestamp lastLogin;
    private Role lastRole;
    private int totalScore;

    public UserInfo(User user, Timestamp lastLogin, Role lastRole, int totalScore) {
        this.user = user;
        this.lastLogin = lastLogin;
        this.lastRole = lastRole;
        this.totalScore = totalScore;
    }

    public User getUser() {
        return user;
    }

    public String getLastLoginString() {
        return lastLogin == null ? "-- never --" : lastLogin.toString().substring(0, lastLogin.toString().length() - 5);
    }

    public Role getLastRole() {
        return lastRole;
    }

    public int getTotalScore() {
        return totalScore;
    }
}
