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
package org.codedefenders.model;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;

public abstract class GameInfo {
    protected Type type;
    protected int userId;
    protected Role role;
    protected String creatorName;

    protected abstract AbstractGame getGame();

    public int userId() {
        return userId;
    }

    /**
     * @return the role of the user in the game. Can only be retrieved when the user is active in
     * this game, i.e. creator or player ({@link Type#ACTIVE}).
     */
    public Role userRole() {
        assert type != Type.OPEN;
        return role;
    }

    public String creatorName() {
        return creatorName;
    }

    public int gameId() {
        return getGame().getId();
    }

    public int creatorId() {
        return getGame().getCreatorId();
    }

    public GameState gameState() {
        return getGame().getState();
    }

    public GameLevel gameLevel() {
        return getGame().getLevel();
    }

    public int cutId() {
        return getGame().getCUT().getId();
    }

    public String cutAlias() {
        return getGame().getCUT().getAlias();
    }

    public String cutSource() {
        return getGame().getCUT().getAsHTMLEscapedString();
    }

    protected enum Type {
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
