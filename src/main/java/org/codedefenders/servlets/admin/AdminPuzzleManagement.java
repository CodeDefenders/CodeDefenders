package org.codedefenders.servlets.admin;

import org.codedefenders.servlets.util.ServletUtils;
import org.codedefenders.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This {@link HttpServlet} handles admin management of puzzles.
 *
 * <p>{@code GET} requests redirect to the admin puzzle management page.
 * and {@code POST} requests handle puzzle related management.
 *
 * <p>Serves under {@code /admin/puzzles} and {@code /admin/puzzles/management}.
 *
 * @author <a href=https://github.com/werli>Phil Werli</a>
 */
@WebServlet({"/admin/puzzles", "/admin/puzzles/management"})
public class AdminPuzzleManagement extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminPuzzleManagement.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher(Constants.ADMIN_PUZZLE_MANAGEMENT_JSP).forward(request, response);
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        final String formType = ServletUtils.formType(request);
        switch (formType) {
            case "updatePuzzleChapter": {

            }
            case "inactivePuzzleChapter": {

            }
            case "removePuzzleChapter": {

            }
            case "rearrangePuzzleChapter": {

            }
            case "updatePuzzle": {

            }
            case "inactivePuzzle": {

            }
            case "removePuzzle": {

            }
        }
    }
}
