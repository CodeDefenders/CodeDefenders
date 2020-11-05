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
    /**
     * The maximum allowed message length.
     */
    public static final int MAX_MESSAGE_LENGTH = 500;

    @Inject
    @Named("game")
    private AbstractGame game;

    @Inject
    LoginBean login;

    /**
     * Checks whether the chat is enabled in the current game.
     * @return Whether the chat is enabled.
     */
    public boolean isChatEnabled() {
        return game.isChatEnabled();
    }

    /**
     * Returns the game id of the current game.
     * @return The game id.
     */
    public int getGameId() {
        return game.getId();
    }

    /**
     * Checks whether the all/attacker/defender tabs should be shown in the chat for the current game.
     * @return Whether the tabs should be shown.
     */
    public boolean isShowTabs() {
        if (game instanceof MultiplayerGame) {
            MultiplayerGame multiplayerGame = (MultiplayerGame) game;
            Role role = multiplayerGame.getRole(login.getUserId());
            return role == Role.OBSERVER;
        } else {
            return false;
        }
    }

    /**
     * Returns the URL for the chat API for the current game.
     * @return The URL for the chat API.
     */
    public String getChatApiUrl() {
        return Paths.API_GAME_CHAT.substring(1) + "?gameId=" + game.getId();
    }

    /**
     * Returns the maximum length for chat messages.
     * @return The maximum length for chat messages.
     */
    public int getMaxMessageLength() {
        return MAX_MESSAGE_LENGTH;
    }

    // TODO: Create beans for enum constants
    public enum ChatCommand {
        ALL("all"),
        TEAM("team");

        String commandString;

        ChatCommand(String commandString) {
            this.commandString = commandString;
        }

        public String getCommandString() {
            return commandString;
        }
    }
}
