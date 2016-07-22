package org.codedefenders.singleplayer;

import org.codedefenders.AntRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;

public class AiPreparer extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AntRunner.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect("games/upload");
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession();
        ArrayList<String> messages = new ArrayList<>();
        session.setAttribute("messages", messages);

        switch (request.getParameter("formType")) {
            case "runPrepareAi":
                int cutId = Integer.parseInt(request.getParameter("cutID"));
                System.out.println("Running PrepareAI on class " + cutId);
                PrepareAI.createTestsAndMutants(cutId);
                break;
            default:
                break;
        }

        response.sendRedirect("games/upload");
    }
}
