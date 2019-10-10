package org.codedefenders.servlets.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.commons.httpclient.HttpStatus;
import org.codedefenders.database.MutantDAO;
import org.codedefenders.game.Mutant;
import org.codedefenders.servlets.util.ServletUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This {@link HttpServlet} offers an API for {@link Mutant mutants}.
 * <p>
 * A {@code GET} request with the {@code mutantId} parameter results in a JSON string containing
 * mutant information, including the source code diff.
 * <p>
 * Serves on path: {@code /api/mutant}.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 * @see org.codedefenders.util.Paths#API_MUTANT
 */
@WebServlet("/api/mutant")
public class MutantAPI extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final Optional<Mutant> mutant = ServletUtils.getIntParameter(request, "mutantId")
            .map(MutantDAO::getMutantById);

        if (!mutant.isPresent()) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        PrintWriter out = response.getWriter();

        String json = generateJsonForMutant(mutant.get());

        response.setContentType("application/json");
        out.print(json);
        out.flush();
    }

    private String generateJsonForMutant(Mutant mutant) {
        Gson gson = new Gson();

        JsonObject root = new JsonObject();
        root.add("id", gson.toJsonTree(mutant.getId(), Integer.class));
        root.add("playerId", gson.toJsonTree(mutant.getPlayerId(), Integer.class));
        root.add("gameId", gson.toJsonTree(mutant.getGameId(), Integer.class));
        root.add("diff", gson.toJsonTree(mutant.getHTMLEscapedPatchString(), String.class));

        return gson.toJson(root);

    }

}
