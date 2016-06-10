package org.codedefenders.multiplayer;

import org.codedefenders.DatabaseAccess;
import org.codedefenders.Game;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

public class MultiplayerGameSelectionManager extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            HttpSession session = request.getSession();
            // Get their user id from the session.
            int uid = (Integer) session.getAttribute("uid");
            String sId = request.getParameter("id");
            int gameId = Integer.parseInt(sId);
            MultiplayerGame mg = DatabaseAccess.getMultiplayerGame(gameId);

            System.out.println(sId);

            Participance p = mg.getParticipance(uid);

            System.out.print(gameId + ": " + p);

            switch (p){
                case ATTACKER: case DEFENDER: case CREATOR:
                    response.sendRedirect("/multiplayer/play?id=" + gameId);
                    break;
                default:
                    response.sendRedirect("/games/user");
                    break;
            }

        } catch (NumberFormatException nfe){
            response.sendRedirect("/games/user");
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        HttpSession session = request.getSession();
        // Get their user id from the session.
        int uid = (Integer) session.getAttribute("uid");
        int gameId;

        ArrayList<String> messages = new ArrayList<String>();
        session.setAttribute("messages", messages);

        // Get the identifying information required to create a game from the submitted form.
        int classId = Integer.parseInt(request.getParameter("class"));
        double lineCoverage = Double.parseDouble(request.getParameter("line_cov"));
        double mutantCoverage = Double.parseDouble(request.getParameter("mutant_cov"));
        Game.Level level = request.getParameter("level") == null ? Game.Level.HARD : Game.Level.EASY;

        // Create the game with supplied parameters and insert it in the database.
        MultiplayerGame nGame = new MultiplayerGame(classId, uid, level, (float) lineCoverage,
                (float) mutantCoverage, 1f);
        nGame.insert();

        // Redirect to the game selection menu.
        response.sendRedirect("games");
    }
}