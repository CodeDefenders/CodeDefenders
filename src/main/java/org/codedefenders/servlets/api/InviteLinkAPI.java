package org.codedefenders.servlets.api;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.util.URLUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@WebServlet("/api/invite-link")
public class InviteLinkAPI extends HttpServlet {

    @Inject
    private GameRepository gameRepository;
    @Inject
    private URLUtils urlUtils;


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer gameId = null;
        if(request.getParameter("gameId") != null) {
            try {
                gameId = Integer.parseInt(request.getParameter("gameId"));
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid game ID");
                return;
            }
        }
        int inviteId = gameRepository.storeInvitationLink(gameId);
        String inviteLink = urlUtils.getAbsoluteURLForPath("invite?inviteId=" + inviteId);

        JsonObject root = new JsonObject();
        root.addProperty("inviteLink", inviteLink);
        root.addProperty("inviteId", inviteId);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Gson gson = new Gson();

        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(root));
            out.flush();
        } catch (IOException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generating invite link");
        }

    }
}
