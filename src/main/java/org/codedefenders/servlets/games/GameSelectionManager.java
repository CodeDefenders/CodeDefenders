/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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
 * along with Code Defenders.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.servlets.games;

import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.Role;
import org.codedefenders.database.DatabaseAccess;
import org.codedefenders.game.duel.DuelGame;
import org.codedefenders.game.singleplayer.PrepareAI;
import org.codedefenders.game.singleplayer.SinglePlayerGame;
import org.codedefenders.game.singleplayer.automated.attacker.AiAttacker;
import org.codedefenders.game.singleplayer.automated.defender.AiDefender;
import org.codedefenders.servlets.util.Redirect;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class GameSelectionManager extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		String contextPath = request.getContextPath();
		response.sendRedirect(contextPath + "/games/user");
	}

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String contextPath = request.getContextPath();
        HttpSession session = request.getSession();
        // Get their user id from the session.
        int uid = (Integer) session.getAttribute("uid");
        int gameId;

        ArrayList<String> messages = new ArrayList<String>();
        session.setAttribute("messages", messages);

        switch (request.getParameter("formType")) {

            case "createGame":

                // Get the identifying information required to create a game from the submitted form.

                try {
                    int classId = Integer.parseInt(request.getParameter("class"));

                    int rounds = Integer.parseInt(request.getParameter("rounds"));

                    /* Disable mode selection for release. */
                    /* String modeName = request.getParameter("mode"); */
                    Role role = request.getParameter("role") == null ? Role.DEFENDER : Role.ATTACKER;
                    GameLevel level = request.getParameter("level") == null ? GameLevel.HARD : GameLevel.EASY;
                    GameMode mode = GameMode.DUEL;

                    if (rounds < 1 || rounds > 10) {
                        messages.add("Invalid rounds amount");
                        response.sendRedirect(contextPath+"/games");
                        return;
                    }

                    /* Disable mode selection for release. */
                    /*
                    switch (modeName) {
                        case "sing":
                            mode = GameMode.SINGLE;
                            break;
                        case "duel":
                            mode = GameMode.DUEL;
                            break;
                        case "prty":
                            mode = GameMode.PARTY;
                            break;
                        case "utst":
                            mode = GameMode.UTESTING;
                            break;
                        default:
                            mode = GameMode.SINGLE;
                    }
                    */

                    if (classId != 0 && DatabaseAccess.getClassForKey("Class_ID", classId) != null) {
                        //Valid class selected.

                        if (mode.equals(GameMode.SINGLE)) {
                            //Create singleplayer game.
                            if (PrepareAI.isPrepared(DatabaseAccess.getClassForKey("Class_ID", classId))) {
                                SinglePlayerGame nGame = new SinglePlayerGame(classId, uid, rounds, role, level);
                                nGame.insert();
                                if (role.equals(Role.ATTACKER)) {
                                    nGame.addPlayer(uid, Role.ATTACKER);
                                    nGame.addPlayer(AiDefender.ID, Role.DEFENDER);
                                } else {
                                    nGame.addPlayer(uid, Role.DEFENDER);
                                    nGame.addPlayer(AiAttacker.ID, Role.ATTACKER);
                                }
                                nGame.tryFirstTurn();
                            } else {
                                //Not prepared, show a message and redirect.
                                messages.add("AI has not been prepared for class. Please select PREPARE AI on the classes page.");
                                Redirect.redirectBack(request, response);
                                return;
                            }
                        } else {
                            // Create the game with supplied parameters and insert it in the database.
                            DuelGame nGame = new DuelGame(classId, uid, rounds, role, level);
                            nGame.insert();
                            if (nGame.getAttackerId() != 0)
                                nGame.addPlayer(uid, Role.ATTACKER);
                            else
                                nGame.addPlayer(uid, Role.DEFENDER);
                        }


                        // Redirect to the game selection menu.
                    }
                } catch (Exception e) {
                    messages.add("There was a problem with the form.");
                }

                response.sendRedirect(contextPath+"/games");


                break;

            case "joinGame":

                // Get the identifying information required to create a game from the submitted form.
                try {
                    gameId = Integer.parseInt(request.getParameter("game"));

                    DuelGame jGame = DatabaseAccess.getGameForKey("ID", gameId);

                    if ((jGame.getAttackerId() == uid) || (jGame.getDefenderId() == uid)) {
                        // uid is already in the game
                        if (jGame.getDefenderId() == uid)
                            messages.add("Already a defender in this game!");
                        else
                            messages.add("Already an attacker in this game!");
                        // either way, reload list of open games
                        Redirect.redirectBack(request, response);
                        break;
                    } else {
                        if (jGame.getAttackerId() == 0) {
                            jGame.addPlayer(uid, Role.ATTACKER);
                            messages.add("Joined game as an attacker.");
                        } else if (jGame.getDefenderId() == 0) {
                            messages.add("Joined game as a defender.");
                            jGame.addPlayer(uid, Role.DEFENDER);
                        } else {
                            messages.add("DuelGame is no longer open.");
                            Redirect.redirectBack(request, response);
                            break;
                        }
                        // user joined, update game
                        jGame.setState(GameState.ACTIVE);
                        jGame.setActiveRole(Role.ATTACKER);
                        jGame.update();
                        // go to play view
                        session.setAttribute("gid", gameId);
                        response.sendRedirect(contextPath+"/"+jGame.getClass().getSimpleName().toLowerCase());
                    }
                } catch (Exception e) {
                    messages.add("There was a problem joining the game.");
                    Redirect.redirectBack(request, response);
                }

                break;

            case "enterGame":

                try {
                    gameId = Integer.parseInt(request.getParameter("game"));
                    DuelGame eGame = DatabaseAccess.getGameForKey("ID", gameId);

                    if (eGame.isUserInGame(uid)) {
                        session.setAttribute("gid", gameId);
                        if (eGame.getMode().equals(GameMode.UTESTING))
                            response.sendRedirect(contextPath+"/utesting");
                        else
                            response.sendRedirect(contextPath+"/"+eGame.getClass().getSimpleName().toLowerCase());
                    } else {
                        Redirect.redirectBack(request, response);
                    }
                } catch (Exception e) {
                    messages.add("There was a problem entering the game");
                    Redirect.redirectBack(request, response);
                }
                break;
            default:
                System.err.println("Action not recognised");
                Redirect.redirectBack(request, response);
                break;
        }
    }
}