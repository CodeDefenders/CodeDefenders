/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
package org.codedefenders.notification.web;

public class PushSocketRegistrationEvent {

    private int gameID;
    private int playerID;

    // TODO multiple targets and actions to save messages?
    private String target;
    private String action;

    public PushSocketRegistrationEvent(String target, int gameID, int playerID, String action) {
        this.gameID = gameID;
        this.target = target;
        this.playerID = playerID;
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public String getTarget() {
        return target;
    }

    public int getGameID() {
        return gameID;
    }

    public int getPlayerID() {
        return playerID;
    }
}
