package org.codedefenders.servlets.api.llm;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codedefenders.game.Role;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.service.game.GameService;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.servlets.util.ServletUtils;

@WebServlet()
public class GameAPI extends HttpServlet {
    @Inject
    GameService gameService;

    @Inject
    GameRepository gameRepo;

    @Inject
    GameProducer gameProducer;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        final var gameId = ServletUtils.getIntParameter(request, "gameId");
        final var userId = ServletUtils.getIntParameter(request, "userId");

        if (gameId.isEmpty() || userId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        gameProducer.setGameId(gameId.get());
        final MultiplayerGame game = gameProducer.getMultiplayerGame();

        if (game == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (game.getRole(userId.get()) != Role.NONE) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }


        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        // generate json
        // out.print(json);
        out.flush();
    }

    public record ReadGameStateRequestDTO(
            int userId,
            int gameId
    ) {
    }
}
