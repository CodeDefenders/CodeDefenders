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
package org.codedefenders.servlets.games.duel;

import org.codedefenders.database.DuelGameDAO;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.Role;
import org.codedefenders.game.duel.DuelGame;
import org.codedefenders.game.singleplayer.PrepareAI;
import org.codedefenders.game.singleplayer.SinglePlayerGame;
import org.codedefenders.game.singleplayer.automated.attacker.AiAttacker;
import org.codedefenders.game.singleplayer.automated.defender.AiDefender;
import org.codedefenders.servlets.util.Redirect;
import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * This {@link HttpServlet} handles selection of {@link DuelGame DuelGames}.
 * <p>
 * {@code GET} requests redirect to the game overview page and {@code POST} requests handle creating, joining
 * and entering {@link DuelGame duel games}.
 * <p>
 * Serves under {@code /duel/games}.
 *
 * @see org.codedefenders.util.Paths#DUEL_SELECTION
 */
public class DuelGameSelectionManager extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(DuelGameSelectionManager.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect(ServletUtils.ctx(request) + Paths.GAMES_OVERVIEW);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        final String action = ServletUtils.formType(request);
        switch (action) {
            case "createGame":
                createGame(request, response);
                return;
            case "joinGame":
                joinGame(request, response);
                return;
            default:
                logger.info("Action not recognised: {}", action);
                Redirect.redirectBack(request, response);
        }
    }

    private void createGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final int userId = ServletUtils.userId(request);

        HttpSession session = request.getSession();
        ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);
        // Get the identifying information required to create a game from the submitted form.

        int classId;
        int rounds;
        try {
            classId = ServletUtils.getIntParameter(request, "class").get();
            rounds = ServletUtils.getIntParameter(request, "rounds").get();
        } catch (NoSuchElementException e) {
            logger.error("'class' or 'rounds' request parameters were missing or no valid integer value.");
            Redirect.redirectBack(request, response);
            return;
        }
        final GameClass cut = GameClassDAO.getClassForId(classId);
        if (cut == null) {
            logger.error("Selected class could not be found.");
            Redirect.redirectBack(request, response);
            return;
        }
        if (rounds < 1 || rounds >= 10) {
            messages.add("Invalid rounds amount. Minimal 1, maximal 10 rounds.");
            Redirect.redirectBack(request, response);
            return;
        }

        Role role = ServletUtils.parameterThenOrOther(request, "role", Role.ATTACKER, Role.DEFENDER);
        GameLevel level = ServletUtils.parameterThenOrOther(request, "level", GameLevel.EASY, GameLevel.HARD);
        GameMode mode = GameMode.DUEL;

        /* Disable mode selection for release. */
        /* String modeName = request.getParameter("mode");
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

        if (mode.equals(GameMode.SINGLE)) {
            //Create singleplayer game.
            if (PrepareAI.isPrepared(cut)) {
                SinglePlayerGame nGame = new SinglePlayerGame(classId, userId, rounds, role, level);
                nGame.insert();
                if (role.equals(Role.ATTACKER)) {
                    nGame.addPlayer(userId, Role.ATTACKER);
                    nGame.addPlayer(AiDefender.ID, Role.DEFENDER);
                } else {
                    nGame.addPlayer(userId, Role.DEFENDER);
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
            DuelGame nGame = new DuelGame(classId, userId, rounds, role, level);
            nGame.insert();
            if (nGame.getAttackerId() != 0) {
                nGame.addPlayer(userId, Role.ATTACKER);
            } else {
                nGame.addPlayer(userId, Role.DEFENDER);
            }
        }

        response.sendRedirect(ServletUtils.ctx(request) + Paths.GAMES_OVERVIEW);
    }

    private void joinGame(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final int userId = ServletUtils.userId(request);

        HttpSession session = request.getSession();
        ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        final Optional<Integer> gameIdOpt = ServletUtils.gameId(request);
        if (!gameIdOpt.isPresent()) {
            logger.error("No gameId parameter. Aborting request.");
            Redirect.redirectBack(request, response);
            return;
        }
        final int gameId = gameIdOpt.get();

        final DuelGame game = DuelGameDAO.getDuelGameForId(gameId);
        if (game == null) {
            logger.error("No game found for gameId={}. Aborting request.", gameId);
            Redirect.redirectBack(request, response);
            return;
        }
        if (game.isUserInGame(userId)) {
            messages.add("You are already in this game!");
            Redirect.redirectBack(request, response);
            return;
        }

        if (game.getAttackerId() == 0) {
            if (!game.addPlayer(userId, Role.ATTACKER)) {
                messages.add("Failed to join requested duel game.");
                Redirect.redirectBack(request, response);
                return;
            }
            messages.add("Joined game as an attacker.");
        } else if (game.getDefenderId() == 0) {
            messages.add("Joined game as a defender.");
            if (!game.addPlayer(userId, Role.DEFENDER)) {

                messages.add("Failed to join requested duel game.");
                Redirect.redirectBack(request, response);
                return;
            }
        } else {
            messages.add("DuelGame is no longer open.");
            Redirect.redirectBack(request, response);
            return;
        }

        game.start();
        game.update();

        response.sendRedirect(ServletUtils.ctx(request) + Paths.DUEL_GAME + "?gameId=" + gameId);
    }
}