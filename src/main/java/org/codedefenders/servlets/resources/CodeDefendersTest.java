package org.codedefenders.servlets.resources;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.database.TestDAO;
import org.codedefenders.dto.TestDTO;
import org.codedefenders.game.Test;

import com.google.gson.Gson;

/*
 * Note that you cannot easily have /shiro/test/<test_id>/src unless you forcefully parse the url
 * https://adndevblog.typepad.com/cloud_and_mobile/2015/08/become-a-java-ee-developer-part-ii-basic-restful-api-from-a-servlet.html
 */
@WebServlet("/shiro/tests/*")
public class CodeDefendersTest extends HttpServlet {

    // GET /shiro/tests/ -> All tests by the current user
    // GET /shiro/tests/ID -> info about the current test

    @Inject
    private TestDAO testDao;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {

        }
        // TODO Manually parse the request. Shall we forward this to another servlet to
        // serve /tests/ID/src ?
        String[] splits = pathInfo.split("/");

        if (splits.length != 2) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        int testId = Integer.parseInt(splits[1]);
        Test test = testDao.getTestById(testId);

        if (test == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        sendAsJson(resp, new TestDTO(test));
    }

    // a utility method to send object
    // as JSON response
    private void sendAsJson(HttpServletResponse response, Object obj) throws IOException {

        response.setContentType("application/json");
        Gson gson = new Gson();
        String res = gson.toJson(obj);

        PrintWriter out = response.getWriter();

        out.print(res);
        out.flush();
    }
}
