package gammut;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.sql.*;

public class GameSelectionManager extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        response.sendRedirect("games/user");
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        // Get their user id from the session.
        int uid = (Integer)request.getSession().getAttribute("uid");
        int gameId;
        HttpSession session;

        switch (request.getParameter("formType")) {
            
            case "createGame" :

                // Get the identifying information required to create a game from the submitted form.
                int classId = Integer.parseInt((String)request.getParameter("class"));
                int rounds = Integer.parseInt((String)request.getParameter("rounds"));
                String role = (String)request.getParameter("role");

                // Create the game with supplied parameters and insert it in the database.
                Game nGame = new Game(classId, uid, rounds, role);
                nGame.insert();

                // Redirect to the game selection menu.
                response.sendRedirect("games");
                
                break;

            case "joinGame" :

                // Get the identifying information required to create a game from the submitted form.
                gameId = Integer.parseInt((String)request.getParameter("game"));

                Game jGame = DatabaseAccess.getGameForKey("Game_ID", gameId);

                if (jGame.getAttackerId() == 0) {jGame.setAttackerId(uid);}
                else {jGame.setDefenderId(uid);}

                jGame.setState("IN PROGRESS");
                jGame.setActivePlayer("ATTACKER");

                jGame.update();

                session = request.getSession();
                session.setAttribute("gid", gameId);

                response.sendRedirect("play");

                break;

            case "enterGame" :

                gameId = Integer.parseInt((String)request.getParameter("game"));
                Game eGame = DatabaseAccess.getGameForKey("Game_ID", gameId);

                if (eGame.isUserInGame(uid)) {

                    session = request.getSession();
                    session.setAttribute("gid", gameId);
                    
                    response.sendRedirect("play");
                }
                else {response.sendRedirect(request.getHeader("referer"));}

                break;
        }
    }
}