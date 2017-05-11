package org.codedefenders;

/**
 * Created by joe on 29/03/2017.
 */

import org.codedefenders.story.StoryGame;
import org.codedefenders.util.DatabaseAccess;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;

public class PuzzleSelectionManager extends HttpServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        response.sendRedirect("story/view");

    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        HttpSession session = request.getSession();
        int uid = (Integer) session.getAttribute("uid");
        int puzzleId;

        ArrayList<String> messages = new ArrayList<String>();
        session.setAttribute("messages", messages);

        switch (request.getParameter("formType")) {

            case "enterPuzzle":

                try {

                    puzzleId = Integer.parseInt(request.getParameter("puzzleId"));
                    session.setAttribute("pid", puzzleId);

                    response.sendRedirect("/puzzles");

                } catch (Exception e) {
                    messages.add("There was a problem entering the puzzle");
                    response.sendRedirect(request.getHeader("referer"));
                    System.out.println(e);
                }
                break;
            default:
                System.out.println("Error");
                response.sendRedirect(request.getHeader("referer"));
                break;
        }

    }

}
