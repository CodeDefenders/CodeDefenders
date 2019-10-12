package org.codedefenders.servlets.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.commons.httpclient.HttpStatus;
import org.codedefenders.database.GameClassDAO;
import org.codedefenders.game.GameClass;
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
 * This {@link HttpServlet} offers an API for {@link GameClass game classes}.
 * <p>
 * A {@code GET} request with the {@code classId} parameter results in a JSON string containing
 * class information, including the source code.
 * <p>
 * Serves on path: {@code /api/class}.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
 * @see org.codedefenders.util.Paths#API_CLASS
 */
@WebServlet("/api/class")
public class GameClassAPI extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final Optional<GameClass> classId = ServletUtils.getIntParameter(request, "classId")
            .map(GameClassDAO::getClassForId);

        if (!classId.isPresent()) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        PrintWriter out = response.getWriter();

        String json = generateJsonForClass(classId.get());

        response.setContentType("application/json");
        out.print(json);
        out.flush();
    }

    private String generateJsonForClass(GameClass gameClass) {
        Gson gson = new Gson();

        JsonObject root = new JsonObject();
        root.add("id", gson.toJsonTree(gameClass.getId(), Integer.class));
        root.add("name", gson.toJsonTree(gameClass.getName(), String.class));
        root.add("alias", gson.toJsonTree(gameClass.getAlias(), String.class));
        root.add("source", gson.toJsonTree(gameClass.getSourceCode(), String.class));

        return gson.toJson(root);

    }

}
