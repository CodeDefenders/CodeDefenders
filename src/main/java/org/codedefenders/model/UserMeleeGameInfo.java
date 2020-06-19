package org.codedefenders.model;

import java.util.List;

import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MeleeGame;

/**
 * This class contains information about a {@link MeleeGame} from the view of a single {@link User}.
 *
 * Additionally to the game itself, this class contains the user's identifier, his role in the game and
 * game creator's name. {@link Type} also specifies whether the user is already active in this game
 * ({@link Type#ACTIVE}) or can join this game ({@link Type#OPEN}).
 *
 * @author gambi
 */
public class UserMeleeGameInfo {
    private Type type;

    private int userId;
    private MeleeGame game;
    private String creatorName;
    
    private Role role;
    /**
     * Use {@link #forOpen(int, MeleeGame, String) forActive()} and
     * {@link #forActive(int, MeleeGame, String) forOpen()} methods.
     *
     */
    private UserMeleeGameInfo() {}

    public static UserMeleeGameInfo forActive(int userId, MeleeGame game, Role role, String creatorName) {
        UserMeleeGameInfo info = new UserMeleeGameInfo();
        info.type = Type.ACTIVE;
        info.userId = userId;
        info.game = game;
        info.creatorName = creatorName;
        info.role = role;
        assert info.role == Role.OBSERVER || info.role == Role.PLAYER;

        return info;
    }

    public static UserMeleeGameInfo forOpen(int userId, MeleeGame game, String creatorName) {
        UserMeleeGameInfo info = new UserMeleeGameInfo();
        info.type = Type.OPEN;
        info.userId = userId;
        info.game = game;
        info.creatorName = creatorName;
        
        return info;
    }
    
    public static UserMeleeGameInfo forFinished(int userId, MeleeGame game, String creatorName) {
        UserMeleeGameInfo info = new UserMeleeGameInfo();
        info.type = Type.FINISHED;
        info.userId = userId;
        info.game = game;
        info.creatorName = creatorName;

        return info;
    }
    
    public Role userRole(){
        assert type != Type.OPEN;
        return role;
    }

    public int userId() {
        return userId;
    }

    public String creatorName() {
        return creatorName;
    }

    public int gameId() {
        return game.getId();
    }

    public int creatorId() {
        return game.getCreatorId();
    }

    public GameState gameState() {
        return game.getState();
    }

    public GameLevel gameLevel() {
        return game.getLevel();
    }

    public List<Player> players() {
        return game.getPlayers();
    }

    public String cutAlias() {
        return game.getCUT().getAlias();
    }

    public String cutSource() {
        return game.getCUT().getAsHTMLEscapedString();
    }

    private enum Type {
        /**
         * The user participates in this game.
         */
        ACTIVE,
        /**
         * The user could join this game.
         */
        OPEN,
        /**
         * The user part of this game, but it is now finished.
         */
        FINISHED
    }
}
