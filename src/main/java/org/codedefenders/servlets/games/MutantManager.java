package org.codedefenders.servlets.games;

import com.google.gson.Gson;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameLevel;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Role;
import org.codedefenders.database.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MutantManager extends HttpServlet {

    private static final Logger logger =
            LoggerFactory.getLogger(MutantManager.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String contextPath = request.getContextPath();
        int gameId = Integer.parseInt(request.getParameter("gameId"));

        AbstractGame game = DatabaseAccess.getMultiplayerGame(gameId);

        int userId = (int) request.getSession().getAttribute("uid");

        Gson gson = new Gson();
        PrintWriter out = response.getWriter();

        try {
            if (!canAccess(request)) {
                ArrayList<Mutant> ms = new ArrayList<Mutant>();

                out.print(gson.toJson(ms));
                out.flush();

            } else {

                response.setContentType("text/json");

                List<Mutant> mutants = game.getMutants();

                boolean showDiff = !DatabaseAccess.getRole(userId, gameId)
                        .equals(Role.DEFENDER) || game.getLevel().equals(
                        GameLevel.EASY);

                for (Mutant m : mutants){
                    m.prepareForSerialise(showDiff);
                }

                out.print(gson.toJson(mutants));
                out.flush();
            }
        } catch (Exception e) {
            response.sendRedirect(contextPath+"/games/user");
        }
    }

    private boolean canAccess(HttpServletRequest request) {
        //TODO: Implement heavy load/DDOS handling
        if (request.getParameter("gameId") != null) {
            int pId = DatabaseAccess.getPlayerIdForMultiplayerGame(
                    (int)request.getSession().getAttribute("uid"),
                    Integer.parseInt(request.getParameter("gameId"))
            );
            return pId >= 0;
        }
        return false;
    }

}