package org.codedefenders.multiplayer;

import org.codedefenders.events.Event;
import org.codedefenders.events.EventStatus;
import org.codedefenders.events.EventType;
import org.codedefenders.util.DatabaseAccess;
import org.codedefenders.GameLevel;
import org.codedefenders.GameState;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;

public class MultiplayerGameSelectionManager extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            HttpSession session = request.getSession();
            // Get their user id from the session.
            int uid = (Integer) session.getAttribute("uid");
            String sId = request.getParameter("id");
            int gameId = Integer.parseInt(sId);
            MultiplayerGame mg = DatabaseAccess.getMultiplayerGame(gameId);

            if (mg != null) {
                mg.notifyPlayers();
                String redirect = "/multiplayer/play?id=" + gameId;
                if (request.getParameter("attacker") != null) {
                    redirect += "&attacker=1";
                } else if (request.getParameter("defender") != null) {
                    redirect += "&defender=1";
                }
                response.sendRedirect(redirect);
            } else {
                response.sendRedirect("/games/user");
            }

        } catch (NumberFormatException nfe) {
            response.sendRedirect("/games/user");
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession();

        ArrayList<String> messages = new ArrayList<String>();
        session.setAttribute("messages", messages);
        try {
            // Get their user id from the session.
            int uid = (Integer) session.getAttribute("uid");


            // Get the identifying information required to create a game from the submitted form.
            switch (request.getParameter("formType")) {

                case "createGame":
                    // Get the identifying information required to create a game from the submitted form.
                    int classId = Integer.parseInt(request.getParameter("class"));
                    String lineCovGoal = request.getParameter("line_cov");
                    String mutCovGoal = request.getParameter("mutant_cov");
                    double lineCoverage = lineCovGoal == null ? 1.1 : Double.parseDouble(lineCovGoal);
                    double mutantCoverage = mutCovGoal == null ? 1.1 : Double.parseDouble(mutCovGoal);
                    GameLevel level = request.getParameter("level") == null ? GameLevel.HARD : GameLevel.EASY;

                    // Create the game with supplied parameters and insert it in the database.
                    MultiplayerGame nGame = new MultiplayerGame(classId, uid, level, (float) lineCoverage,
                            (float) mutantCoverage, 1f, 100, 100,
                            Integer.parseInt(request.getParameter("defenderLimit")), Integer.parseInt(request.getParameter("attackerLimit")),
                            Integer.parseInt(request.getParameter("minDefenders")), Integer.parseInt(request.getParameter("minAttackers")),
                            Long.parseLong(request.getParameter("startTime")), Long.parseLong(request.getParameter("finishTime")), GameState.CREATED.name());
                    if (nGame.insert()) {
                        Event event = new Event(-1, nGame.getId(), uid, "Game" +
                                " Created",
                                EventType.GAME_CREATED, EventStatus.GAME, new
                                Timestamp(System.currentTimeMillis()));
                        event.insert();
                    }

                    //rs.getInt("Defender_Limit"), rs.getInt("Attacker_Limit"),
                    //rs.getInt("Defenders_Needed"), rs.getInt("Attackers_Needed"), rs.getLong("Finish_Time"),
                    //        rs.getString("State")

                    // Redirect to the game selection menu.
                    response.sendRedirect("games");
                    break;
                case "leaveGame":
                    int gameId = Integer.parseInt(request.getParameter("game"));
                    MultiplayerGame game = DatabaseAccess.getMultiplayerGame(gameId);
                    if (game.removePlayer(uid)) {
                        messages.add("Game " + gameId + " left");
                        DatabaseAccess.removePlayerEventsForGame(gameId,
                                uid);
                        EventType notifType = EventType.GAME_PLAYER_LEFT;
                        Event notif = new Event(-1, gameId, uid,
                                "You successfully left" +
                                        " the game.",
                                notifType, EventStatus.NEW,
                                new Timestamp(System.currentTimeMillis()));
                        notif.insert();
                    } else {
                        messages.add("An error occured while leaving game " +
                                gameId);
                    }// Redirect to the game selection menu.
                    response.sendRedirect("games");
                    break;
                default:
                    System.err.println("Action not recognised");
                    response.sendRedirect(request.getHeader("referer"));
                    break;
            }
        } catch (Exception e) {
            messages.add("An error occurred");
            response.sendRedirect(request.getHeader("referer"));
        }
    }
}