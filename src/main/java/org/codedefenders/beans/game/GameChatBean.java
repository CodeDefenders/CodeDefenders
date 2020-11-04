package org.codedefenders.beans.game;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.beans.user.LoginBean;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.util.Paths;

@ManagedBean
@RequestScoped
@Named("gameChat")
public class GameChatBean {
    public static final int MAX_MESSAGE_LENGTH = 500;

    @Inject
    @Named("game")
    private AbstractGame game;

    @Inject
    LoginBean login;

    public boolean isChatEnabled() {
        return game.isChatEnabled();
    }

    public int getGameId() {
        return game.getId();
    }

    public boolean isShowTabs() {
        if (game instanceof MultiplayerGame) {
            MultiplayerGame multiplayerGame = (MultiplayerGame) game;
            Role role = multiplayerGame.getRole(login.getUserId());
            return role == Role.OBSERVER;
        } else {
            return false;
        }
    }

    public String getChatApiUrl() {
        return Paths.API_GAME_CHAT.substring(1) + "?gameId=" + game.getId();
    }

    public int getMaxMessageLength() {
        return MAX_MESSAGE_LENGTH;
    }
}
