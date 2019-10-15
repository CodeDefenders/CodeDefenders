package org.codedefenders.servlets.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.commons.httpclient.HttpStatus;
import org.codedefenders.database.TestDAO;
import org.codedefenders.game.Test;
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
 * This {@link HttpServlet} offers an API for {@link Test tests}.
 * <p>
 * A {@code GET} request with the {@code testId} parameter results in a JSON string containing
 * test information, including the source code.
 * <p>
 * Serves on path: {@code /api/test}.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 * @see org.codedefenders.util.Paths#API_TEST
 */
@WebServlet("/api/test")
public class TestAPI extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final Optional<Test> test = ServletUtils.getIntParameter(request, "testId")
            .map(TestDAO::getTestById);

        if (!test.isPresent()) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        PrintWriter out = response.getWriter();

        String json = generateJsonForTest(test.get());

        response.setContentType("application/json");
        out.print(json);
        out.flush();
    }

    private String generateJsonForTest(Test test) {
        Gson gson = new Gson();

        JsonObject root = new JsonObject();
        root.add("id", gson.toJsonTree(test.getId(), Integer.class));
        root.add("playerId", gson.toJsonTree(test.getPlayerId(), Integer.class));
        root.add("gameId", gson.toJsonTree(test.getGameId(), Integer.class));
        root.add("source", gson.toJsonTree(test.getAsString(), String.class));

        return gson.toJson(root);

    }

}
