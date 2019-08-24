package org.codedefenders.notification.events.server.game;

import com.google.gson.annotations.Expose;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.User;

public class GameLeftEvent extends GameLifecycleEvent {
    private User user;
    @Expose private int userId;
    @Expose private String userName;

    public GameLeftEvent(AbstractGame game, User user) {
        super(game);
        this.user = user;
        this.userId = user.getId();
        this.userName = user.getUsername();
    }

    public User getUser() {
        return user;
    }

    public int getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }
}
