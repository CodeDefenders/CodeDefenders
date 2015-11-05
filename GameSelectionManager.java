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

        switch (request.getParameter("formType")) {
            
            case "createGame" :

                // Get the identifying information required to create a game from the submitted form.
                int classId = Integer.parseInt((String)request.getParameter("class"));
                int rounds = Integer.parseInt((String)request.getParameter("rounds"));
                String role = (String)request.getParameter("role");

                // Create the game with supplied parameters and insert it in the database.
                Game newGame = new Game(classId, uid, rounds, role);
                newGame.insert();

                // Redirect to the game selection menu.
                response.sendRedirect("games");
                
                break;

            case "joinGame" :

                // Get the identifying information required to create a game from the submitted form.
                gameId = Integer.parseInt((String)request.getParameter("game"));

                Game activeGame = DatabaseAccess.getGameForKey("Game_ID", gameId);

                if (activeGame.getAttackerId() == 0) {activeGame.setAttackerId(uid);}
                else {activeGame.setDefenderId(uid);}

                activeGame.setStatus("IN PROGRESS");
                activeGame.setActivePlayer("ATTACKER");

                activeGame.update();

                HttpSession session = request.getSession();
                session.setAttribute("gid", gameId);

                response.sendRedirect("play");

                break;

            case "enterGame" :

                gameId = Integer.parseInt((String)request.getParameter("game"));
                Game activeGame = DatabaseAccess.getGameForKey("Game_ID", gameId);

                if (activeGame.isUserInGame(uid)) {

                    HttpSession session = request.getSession();
                    session.setAttribute("gid", gameId);
                    
                    response.sendRedirect("play");
                }

                else {response.sendRedirect(request.getHeader("referer"));}

                break;
        }
    }
}